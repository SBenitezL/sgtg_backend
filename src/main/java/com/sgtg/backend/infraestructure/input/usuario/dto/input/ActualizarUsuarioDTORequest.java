package com.sgtg.backend.infraestructure.input.usuario.dto.input;

import java.util.List;

import com.sgtg.backend.application.input.usuario.DatosPerfil;
import com.sgtg.backend.application.input.usuario.NuevoUsuario;
import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Datos actualizados de un usuario. Igual que al crear, pero la contraseña es
 * opcional: vacía conserva la actual. El estado se cambia con
 * {@code PUT /usuarios/{id}/estado}.
 */
public record ActualizarUsuarioDTORequest(
        @NotBlank(message = "El código es obligatorio") String codigo,
        @NotNull(message = "El tipo de documento es obligatorio") TipoDocumentoIdentidad tipoDocumento,
        @NotBlank(message = "El número de documento es obligatorio")
        @Pattern(regexp = "\\d+", message = "El número de documento solo puede contener dígitos") String numeroDocumento,
        @NotBlank(message = "El correo electrónico es obligatorio") @Email(message = "El correo electrónico no es válido") String email,
        String password,
        @NotBlank(message = "El primer nombre es obligatorio") String primerNombre,
        String segundoNombre,
        @NotBlank(message = "El primer apellido es obligatorio") String primerApellido,
        String segundoApellido,
        @NotEmpty(message = "Se debe indicar al menos un rol") List<String> roles,
        TipoVinculacion tipoVinculacion,
        Long departamentoId,
        Long programaId) {

    public NuevoUsuario toDatosUsuario() {
        return new NuevoUsuario(codigo, tipoDocumento, numeroDocumento, email, password, primerNombre, segundoNombre,
                primerApellido, segundoApellido, roles, new DatosPerfil(tipoVinculacion, departamentoId, programaId));
    }
}
