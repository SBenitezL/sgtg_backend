package com.sgtg.backend.infraestructure.output.percistence.gateway;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.application.output.ManageAuditoriaGatewayIntPort;
import com.sgtg.backend.domain.models.auditoria.RegistroAuditoria;
import com.sgtg.backend.domain.models.auditoria.ResultadoAuditoria;
import com.sgtg.backend.domain.models.auditoria.SelloAuditoria;
import com.sgtg.backend.domain.models.auditoria.VerificacionCadena;
import com.sgtg.backend.infraestructure.output.percistence.entities.AuditoriaCadenaEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.AuditoriaEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.AuditoriaCadenaRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.AuditoriaConsultaRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.AuditoriaRepository;

import io.jsonwebtoken.io.Decoders;
import lombok.extern.slf4j.Slf4j;

/**
 * Guarda cada registro de auditoría en su propia transacción, sellado a
 * continuación del último de la cadena ({@link SelloAuditoria}).
 * <p>
 * La clave de los sellos es {@code sgtg.auditoria.clave} (variable
 * {@code SGTG_AUDITORIA_CLAVE}). Si no se define, se deriva de la clave de los
 * tokens; conviene definir una propia, porque al cambiar la de los tokens la
 * verificación de los registros anteriores dejaría de coincidir.
 */
@Slf4j
@Service
public class ManageAuditoriaGatewayImplAdapter implements ManageAuditoriaGatewayIntPort {

    private static final String LOGIN = "AuthController.login";
    /** Registros que se leen por consulta al verificar la cadena */
    private static final int PAGINA = 1000;
    private static final int LONGITUD_MINIMA_CLAVE = 32;

    private final AuditoriaRepository auditoriaRepository;
    private final AuditoriaCadenaRepository cadenaRepository;
    private final AuditoriaConsultaRepository consultaRepository;
    private final byte[] clave;

    public ManageAuditoriaGatewayImplAdapter(AuditoriaRepository auditoriaRepository,
            AuditoriaCadenaRepository cadenaRepository, AuditoriaConsultaRepository consultaRepository,
            @Value("${sgtg.auditoria.clave:}") String clave, @Value("${jwt.secret}") String claveTokens) {
        this.auditoriaRepository = auditoriaRepository;
        this.cadenaRepository = cadenaRepository;
        this.consultaRepository = consultaRepository;
        this.clave = claveDeLosSellos(clave, claveTokens);
    }

    private static byte[] claveDeLosSellos(String clave, String claveTokens) {
        if (clave != null && !clave.isBlank()) {
            if (clave.length() < LONGITUD_MINIMA_CLAVE)
                throw new IllegalStateException("sgtg.auditoria.clave (variable SGTG_AUDITORIA_CLAVE) debe tener al"
                        + " menos " + LONGITUD_MINIMA_CLAVE + " caracteres");
            return clave.getBytes(StandardCharsets.UTF_8);
        }
        log.warn("No se definió SGTG_AUDITORIA_CLAVE: los sellos de la auditoría usan una clave derivada de"
                + " JWT_SECRET. Conviene definir una propia");
        // Derivada y no la misma: un sello nunca sirve como firma de un token
        return SelloAuditoria.derivarClave(Decoders.BASE64.decode(claveTokens));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(RegistroAuditoria r) {
        AuditoriaCadenaEntity cadena = cadenaRepository.bloquear(AuditoriaCadenaEntity.UNICA)
                .orElseGet(() -> cadenaRepository.save(new AuditoriaCadenaEntity(AuditoriaCadenaEntity.UNICA, 0, null)));
        long secuencia = cadena.getUltimaSecuencia() + 1;
        String sello = SelloAuditoria.sellar(clave, secuencia, cadena.getUltimoSello(), r);
        auditoriaRepository.save(AuditoriaEntity.sellado(r, secuencia, cadena.getUltimoSello(), sello));
        cadena.setUltimaSecuencia(secuencia);
        cadena.setUltimoSello(sello);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> ipsConLoginsFallidos(LocalDateTime desde, long minimo) {
        return consultaRepository.ipsConLoginsFallidos(LOGIN, desde, minimo, ResultadoAuditoria.EXITOSA);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> correosConLoginsFallidos(LocalDateTime desde, long minimo) {
        return consultaRepository.correosConLoginsFallidos(LOGIN, desde, minimo, ResultadoAuditoria.EXITOSA);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> usuariosConAccesosDenegados(LocalDateTime desde, long minimo) {
        return consultaRepository.usuariosConResultado(desde, minimo, ResultadoAuditoria.SIN_PERMISO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> sesionesRechazadas(LocalDateTime desde) {
        return consultaRepository.sesionesRechazadas(desde, ResultadoAuditoria.NO_AUTENTICADO);
    }

    /**
     * Deja el resultado en el log, con el último sello: guardado fuera de la
     * base, permite saber después si se borraron los registros finales junto con
     * el final de la cadena.
     */
    @Override
    @Transactional(readOnly = true)
    public VerificacionCadena verificarCadena() {
        VerificacionCadena resultado = recorrerCadena();
        if (resultado.integra())
            log.info("Auditoría íntegra: {} registro(s) sellados, último sello {}", resultado.registros(),
                    resultado.ultimoSello());
        else
            log.error("Auditoría alterada: {} (íntegra hasta el registro {}, sello {})", resultado.falla(),
                    resultado.registros(), resultado.ultimoSello());
        return resultado;
    }

    private VerificacionCadena recorrerCadena() {
        long revisados = 0;
        String selloAnterior = null;
        List<AuditoriaEntity> pagina = consultaRepository.selladosDespuesDe(0, PageRequest.of(0, PAGINA));
        while (!pagina.isEmpty()) {
            for (AuditoriaEntity registro : pagina) {
                long esperada = revisados + 1;
                if (registro.getSecuencia() != esperada)
                    return new VerificacionCadena(revisados, selloAnterior, "falta el registro " + esperada
                            + " de la cadena (sigue el " + registro.getSecuencia() + ")");
                if (!Objects.equals(registro.getSelloAnterior(), selloAnterior))
                    return new VerificacionCadena(revisados, selloAnterior,
                            "el registro " + esperada + " no continúa la cadena desde el anterior");
                if (!SelloAuditoria.sellar(clave, esperada, selloAnterior, registro.registro())
                        .equals(registro.getSello()))
                    return new VerificacionCadena(revisados, selloAnterior,
                            "el registro " + esperada + " (id " + registro.getId() + ") fue modificado");
                selloAnterior = registro.getSello();
                revisados = esperada;
            }
            pagina = consultaRepository.selladosDespuesDe(revisados, PageRequest.of(0, PAGINA));
        }
        AuditoriaCadenaEntity cadena = cadenaRepository.findById(AuditoriaCadenaEntity.UNICA).orElse(null);
        long ultima = cadena == null ? 0 : cadena.getUltimaSecuencia();
        if (ultima != revisados || (cadena != null && !Objects.equals(cadena.getUltimoSello(), selloAnterior)))
            return new VerificacionCadena(revisados, selloAnterior, "la cadena llega hasta el registro " + ultima
                    + " pero solo se encontraron " + revisados + ": se borraron los últimos registros");
        long sinSello = consultaRepository.sinSelloDentroDeLaCadena();
        if (sinSello > 0)
            return new VerificacionCadena(revisados, selloAnterior,
                    sinSello + " registro(s) posteriores al inicio de la cadena no tienen sello");
        return new VerificacionCadena(revisados, selloAnterior, null);
    }
}
