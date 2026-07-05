package com.sgtg.backend.application.output;

import com.sgtg.backend.domain.models.Usuario;

/**
 * Interfaz para la gestión de operaciones relacionadas con usuarios, como la
 * búsqueda de usuarios por correo electrónico. Esta interfaz actúa como un
 * puerto
 * de salida para la capa de dominio, permitiendo que los casos de uso
 * interactúen con la capa de infraestructura sin acoplarse a detalles
 * específicos de implementación.
 */
public interface ManageUserGatewayIntPort {
    /**
     * Busca un usuario en el sistema utilizando su correo electrónico. Este método
     * es esencial para operaciones como el inicio de sesión, donde se necesita
     * verificar la existencia del usuario y su contraseña.
     * 
     * @param email correo electrónico del usuario a buscar, es obligatorio y debe
     *              cumplir con formato de correo electrónico.
     * @return el usuario encontrado o null si no se encuentra.
     */
    public Usuario findByEmail(String email);

}
