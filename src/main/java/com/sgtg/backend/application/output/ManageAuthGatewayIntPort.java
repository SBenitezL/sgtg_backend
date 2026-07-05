package com.sgtg.backend.application.output;

import java.util.List;

import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Usuario;

/**
 * Interfaz de puerto de salida para la gestión de autenticación, que define los
 * métodos que deben ser implementados por cualquier adaptador que se encargue
 * de interactuar con servicios externos de persistencia de datos.
 */
public interface ManageAuthGatewayIntPort {

    /**
     * Busca un usuario por su correo electrónico, devuelve el usuario si se
     * encuentra, de lo contrario, null o una excepción.
     * 
     * @param email Correo electrónico del usuario a buscar.
     * @return Usuario encontrado o null si no se encuentra, o una excepción en caso
     *         de error.
     */
    public Usuario findByEmail(String email);

    /**
     * Registra un nuevo usuario con la información proporcionada, devuelve true si
     * el registro es exitoso, de lo contrario, false o una excepción.
     * 
     * @param user Objeto que contiene la información del usuario a registrar.
     * @return true si el registro es exitoso, de lo contrario, false o una
     *         excepción.
     */
    public boolean register(Usuario user);

    /**
     * Verifica si existen usuarios registrados con la información única de correo o
     * código
     * 
     * @param email  email a verificar en el sistema.
     * @param codigo codigo a verificar en el sistema.
     * @return true en caso de encontrar conincidencias y false en caso contrario.
     */
    public boolean existsByEmailorCodigo(String email, String codigo);

    /**
     * Recupera la lista de roles solicitados por nombre.
     * 
     * @param trole Nombre de los roles a buscar.
     * @return Lista con los roles encontrados.
     */
    public List<Role> findRolesByTrole(List<String> trole);
}
