package com.sgtg.backend.application.output;

import java.util.List;

import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;

/**
 * Puerto de salida para consultar y actualizar los usuarios del sistema.
 */
public interface ManageUserGatewayIntPort {

    /**
     * @param email correo del usuario
     * @return el usuario, o null si no existe
     */
    public Usuario findByEmail(String email);

    /**
     * @param usuarioId identificador del usuario
     * @return el usuario, o null si no existe
     */
    public Usuario findById(Long usuarioId);

    /**
     * Reemplaza los roles del usuario por los que tiene el objeto de dominio.
     *
     * @param usuario usuario con sus roles ya cargados (con identificador)
     */
    public void actualizarRoles(Usuario usuario);

    /**
     * Guarda el estado (activo o inactivo) que tiene el objeto de dominio.
     *
     * @param usuario usuario con su identificador
     */
    public void actualizarEstado(Usuario usuario);

    /** @return todos los usuarios, en orden de creación */
    public List<Usuario> listar();

    /**
     * Guarda los datos de identificación, contacto y roles del usuario (no la
     * contraseña ni el estado).
     *
     * @param usuario usuario con su identificador
     */
    public void actualizarDatos(Usuario usuario);

    /** @return true si otro usuario (distinto de {@code usuarioId}) tiene ese correo o código */
    public boolean existeEmailOCodigoEnOtro(String email, String codigo, Long usuarioId);

    /** @return true si otro usuario (distinto de {@code usuarioId}) tiene ese documento */
    public boolean existeDocumentoEnOtro(TipoDocumentoIdentidad tipoDocumento, String numeroDocumento,
            Long usuarioId);
}
