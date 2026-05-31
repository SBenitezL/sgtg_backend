package com.sgtg.backend.infraestructure.input.auth.dto.input;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;

/** DTO para la solicitud de inicio de sesión */
@Data
@Builder
@AllArgsConstructor
public class LoginDTORequest {
    /**
     * Correo electrónico del usuario, es obligatorio y debe cumplir con formato de
     * correo electrónico
     */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no es válido")
    String email;
    /**
     * Contraseña del usuario, es obligatoria
     */
    @NotBlank(message = "La contraseña es obligatoria")
    String password;

}
