package com.sgtg.backend.infraestructure.input.auth.dto.input;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import lombok.AllArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTORequest {

    /** Código con el que el usuario se presenta frente a la Universidad. */
    private String codigo;
    /** Correo electrónico del usuario, es único y no puede ser nulo */
    private String email;
    /** Contraseña del usuario, no puede ser nula */
    private String password;
    /** Nombres del usuario */
    private String nombres;
    /** Apellidos del usuario */
    private String apellidos;
    /** Roles del usuario @see RoleEntity */
    private List<String> role;

}
