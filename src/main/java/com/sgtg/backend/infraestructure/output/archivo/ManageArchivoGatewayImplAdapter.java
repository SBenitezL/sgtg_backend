package com.sgtg.backend.infraestructure.output.archivo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sgtg.backend.application.input.trabajo_grado.ArchivoAdjunto;
import com.sgtg.backend.application.output.ManageAlertaGatewayIntPort;
import com.sgtg.backend.application.output.ManageArchivoGatewayIntPort;
import com.sgtg.backend.domain.models.auditoria.AlertaSeguridad;
import com.sgtg.backend.domain.models.auditoria.TipoAlerta;
import com.sgtg.backend.infraestructure.input.auditoria.DetalleAuditoria;
import com.sgtg.backend.infraestructure.output.percistence.entities.HuellaArchivoEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.HuellaArchivoRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Almacena los archivos de los trabajos de grado en el sistema de archivos,
 * bajo el directorio configurado en {@code sgtg.archivos.directorio}, con la
 * estructura {@code <trabajoId>/<carpeta del tipo>/v<iteración>/<nombre>}
 * (ej. {@code 15/PP-A/v2/propuesta.pdf}). Las rutas que devuelve son relativas
 * a ese directorio y nunca pueden salir de él.
 * <p>
 * De cada archivo se guarda su huella SHA-256 ({@code huellas_archivos}) al
 * guardarlo. Al leerlo se vuelve a calcular: la huella queda en la auditoría
 * de la petición y, si no coincide (el archivo se cambió en el disco), se
 * avisa a los administradores.
 */
@Slf4j
@Service
public class ManageArchivoGatewayImplAdapter implements ManageArchivoGatewayIntPort {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    /** Carpeta de los archivos que se cargan antes de que el trabajo tenga identificador */
    private static final String CARPETA_TEMPORAL = "temporal";
    private static final Pattern CARPETA_ITERACION = Pattern.compile("v(\\d+)");
    private static final int LONGITUD_MAXIMA_NOMBRE = 100;

    private final Path raiz;
    private final HuellaArchivoRepository huellas;
    private final ManageAlertaGatewayIntPort alertas;

    public ManageArchivoGatewayImplAdapter(@Value("${sgtg.archivos.directorio}") String directorio,
            HuellaArchivoRepository huellas, ManageAlertaGatewayIntPort alertas) {
        this.raiz = Paths.get(directorio).toAbsolutePath().normalize();
        this.huellas = huellas;
        this.alertas = alertas;
    }

    @Override
    public String guardar(Long trabajoId, String carpeta, String nombre, ArchivoAdjunto archivo) {
        try {
            Path iteracion = crearSiguienteIteracion(carpetaDelTipo(trabajoId, carpeta));
            Path destino = iteracion.resolve(nombreConVersion(nombre, iteracion));
            Files.write(destino, archivo.contenido(), StandardOpenOption.CREATE_NEW);
            return registrarHuella(relativa(destino), archivo.contenido(), "Archivo guardado");
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
    }

    @Override
    public String guardarTemporal(ArchivoAdjunto archivo) {
        try {
            Path carpeta = Files.createDirectories(this.raiz.resolve(CARPETA_TEMPORAL)
                    .resolve(UUID.randomUUID().toString()));
            Path destino = carpeta.resolve(nombreSeguro(archivo.nombre()));
            Files.write(destino, archivo.contenido(), StandardOpenOption.CREATE_NEW);
            return registrarHuella(relativa(destino), archivo.contenido(), "Archivo guardado");
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo temporal", e);
        }
    }

    @Override
    public String reubicar(String rutaTemporal, Long trabajoId, String carpeta, String nombre) {
        Path origen = resolver(rutaTemporal);
        if (origen == null || !Files.isRegularFile(origen))
            throw new IllegalArgumentException("El archivo temporal no existe: " + rutaTemporal);
        try {
            Path iteracion = crearSiguienteIteracion(carpetaDelTipo(trabajoId, carpeta));
            Path destino = Files.move(origen, iteracion.resolve(nombreConVersion(nombre, iteracion)));
            eliminarSiVacia(origen.getParent());
            huellas.deleteById(rutaTemporal);
            return registrarHuella(relativa(destino), Files.readAllBytes(destino), "Archivo guardado");
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo reubicar el archivo", e);
        }
    }

    @Override
    public ArchivoAdjunto obtener(String ruta) {
        Path archivo = resolver(ruta);
        if (archivo == null || !Files.isRegularFile(archivo))
            return null;
        try {
            String nombre = archivo.getFileName().toString();
            byte[] contenido = Files.readAllBytes(archivo);
            revisarHuella(relativa(archivo), contenido);
            return new ArchivoAdjunto(nombre, tipoContenido(archivo), contenido);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
    }

    @Override
    public void eliminar(String ruta) {
        Path archivo = resolver(ruta);
        if (archivo == null)
            return;
        try {
            Files.deleteIfExists(archivo);
            huellas.deleteById(relativa(archivo));
            // La carpeta de la iteración queda libre para la siguiente carga
            eliminarSiVacia(archivo.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar el archivo", e);
        }
    }

    /**
     * Registra la huella de los archivos que todavía no la tienen (los guardados
     * antes de calcularlas) y revisa las de todos.
     */
    @Override
    public List<String> verificarHuellas() {
        List<String> alterados = new ArrayList<>();
        if (Files.isDirectory(this.raiz)) {
            try (Stream<Path> archivos = Files.walk(this.raiz)) {
                for (Path archivo : archivos.filter(Files::isRegularFile)
                        // Archivos propios del sistema (ej. el desplazamiento del reloj de desarrollo)
                        .filter(a -> !a.getFileName().toString().startsWith(".")).toList()) {
                    String ruta = relativa(archivo);
                    if (!huellas.existsById(ruta))
                        registrarHuella(ruta, Files.readAllBytes(archivo), null);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudieron recorrer los archivos", e);
            }
        }
        for (HuellaArchivoEntity huella : huellas.findAll()) {
            Path archivo = resolver(huella.getRuta());
            try {
                if (archivo == null || !Files.isRegularFile(archivo))
                    alterados.add(huella.getRuta() + ": ya no existe");
                else if (!huella.getSha256().equals(sha256(Files.readAllBytes(archivo))))
                    alterados.add(huella.getRuta() + ": no coincide con su huella");
            } catch (IOException e) {
                alterados.add(huella.getRuta() + ": no se pudo leer");
            }
        }
        return alterados;
    }

    // ----- Huellas -----

    /** Guarda la huella del archivo y la deja en la auditoría de la petición; devuelve la ruta. */
    private String registrarHuella(String ruta, byte[] contenido, String accion) {
        String sha256 = sha256(contenido);
        huellas.save(new HuellaArchivoEntity(ruta, sha256, contenido.length, LocalDateTime.now(ZONA)));
        if (accion != null)
            DetalleAuditoria.agregar(accion + ": " + ruta + " (SHA-256 " + sha256 + ")");
        return ruta;
    }

    /**
     * Compara el archivo leído con su huella. Uno sin huella (guardado antes de
     * calcularlas) la recibe ahora; uno que no coincide se avisa, pero se
     * entrega igual para no bloquear el trámite.
     */
    private void revisarHuella(String ruta, byte[] contenido) {
        String sha256 = sha256(contenido);
        Optional<HuellaArchivoEntity> huella = huellas.findById(ruta);
        if (huella.isEmpty()) {
            huellas.save(new HuellaArchivoEntity(ruta, sha256, contenido.length, LocalDateTime.now(ZONA)));
            DetalleAuditoria.agregar("Archivo leído: " + ruta + " (SHA-256 " + sha256 + ", huella registrada ahora)");
            return;
        }
        if (huella.get().getSha256().equals(sha256)) {
            DetalleAuditoria.agregar("Archivo leído: " + ruta + " (SHA-256 " + sha256 + ")");
            return;
        }
        DetalleAuditoria.agregar("Archivo leído ALTERADO: " + ruta + " (SHA-256 " + sha256 + ", se guardó con "
                + huella.get().getSha256() + ")");
        log.error("El archivo {} no coincide con su huella", ruta);
        alertas.alertar(new AlertaSeguridad(TipoAlerta.ARCHIVO_ALTERADO, ruta + ": no coincide con su huella",
                "El archivo " + ruta + " no coincide con la huella que se le calculó al guardarlo: se cambió en el"
                        + " disco fuera de la aplicación.", LocalDateTime.now(ZONA)));
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    // ----- Apoyo -----

    private Path carpetaDelTipo(Long trabajoId, String carpeta) {
        if (trabajoId == null)
            throw new IllegalArgumentException("El trabajo de grado no tiene identificador");
        return this.raiz.resolve(trabajoId.toString()).resolve(segmentoSeguro(carpeta));
    }

    /**
     * Crea la carpeta {@code v<n>} siguiente a la mayor existente. La creación
     * de la carpeta es atómica: si otra carga la toma primero se intenta con la
     * siguiente, así nunca se sobrescribe un archivo.
     */
    private Path crearSiguienteIteracion(Path carpetaTipo) throws IOException {
        Files.createDirectories(carpetaTipo);
        int siguiente = mayorIteracion(carpetaTipo) + 1;
        while (true) {
            try {
                return Files.createDirectory(carpetaTipo.resolve("v" + siguiente));
            } catch (FileAlreadyExistsException e) {
                siguiente++;
            }
        }
    }

    private static int mayorIteracion(Path carpetaTipo) throws IOException {
        try (Stream<Path> hijos = Files.list(carpetaTipo)) {
            return hijos.filter(Files::isDirectory)
                    .map(hijo -> CARPETA_ITERACION.matcher(hijo.getFileName().toString()))
                    .filter(Matcher::matches)
                    .mapToInt(coincidencia -> Integer.parseInt(coincidencia.group(1)))
                    .max().orElse(0);
        }
    }

    /** Ruta absoluta dentro del directorio raíz; null si la ruta es vacía o sale de él. */
    private Path resolver(String ruta) {
        if (ruta == null || ruta.isBlank())
            return null;
        Path absoluta = this.raiz.resolve(ruta.replace('\\', '/')).normalize();
        return absoluta.startsWith(this.raiz) && !absoluta.equals(this.raiz) ? absoluta : null;
    }

    private String relativa(Path archivo) {
        return this.raiz.relativize(archivo).toString().replace('\\', '/');
    }

    private void eliminarSiVacia(Path carpeta) throws IOException {
        if (carpeta == null || carpeta.equals(this.raiz) || !Files.isDirectory(carpeta))
            return;
        try (Stream<Path> hijos = Files.list(carpeta)) {
            if (hijos.findAny().isPresent())
                return;
        }
        Files.deleteIfExists(carpeta);
    }

    /**
     * Nombre descriptivo del archivo con su versión: "Formato A" en la carpeta
     * {@code v2} → "Formato A - v2.pdf" (sin tildes; se conservan los espacios).
     */
    private static String nombreConVersion(String nombre, Path iteracion) {
        String base = Normalizer.normalize(nombre == null || nombre.isBlank() ? "Documento" : nombre,
                Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9 _-]", "").trim();
        if (base.isEmpty())
            base = "Documento";
        return base + " - " + iteracion.getFileName() + ".pdf";
    }

    /** Nombre sin directorios, sin tildes y solo con letras, dígitos, punto, guion y guion bajo. */
    private static String nombreSeguro(String nombreOriginal) {
        String nombre = nombreOriginal == null ? "" : Paths.get(nombreOriginal.replace('\\', '/')).getFileName()
                .toString();
        nombre = segmentoSeguro(nombre);
        if (nombre.isEmpty() || nombre.chars().allMatch(c -> c == '.' || c == '_'))
            nombre = "archivo.pdf";
        if (nombre.length() > LONGITUD_MAXIMA_NOMBRE)
            nombre = nombre.substring(nombre.length() - LONGITUD_MAXIMA_NOMBRE);
        return nombre;
    }

    private static String segmentoSeguro(String texto) {
        String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpio = sinTildes.replaceAll("[^A-Za-z0-9._-]", "_");
        return limpio.equals(".") || limpio.equals("..") ? "_" : limpio;
    }

    private static String tipoContenido(Path archivo) throws IOException {
        String tipo = Files.probeContentType(archivo);
        if (tipo != null)
            return tipo;
        return archivo.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf") ? "application/pdf"
                : "application/octet-stream";
    }
}
