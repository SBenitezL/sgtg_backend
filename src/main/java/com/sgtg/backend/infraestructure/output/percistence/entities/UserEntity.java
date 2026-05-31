package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Esta clase representa la entidad de usuario en la base de datos. Contiene los
 * campos id, email y password, con las anotaciones necesarias para mapearlos a
 * una tabla en la base de datos.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity implements UserDetails {
    /** Identificador único del usuario en el sistema, se genera automáticamente */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cusuari;
    /** Código con el que el usuario se presenta frente a la Universidad. */
    private String codigo;
    /** Correo electrónico del usuario, es único y no puede ser nulo */
    @Column(unique = true, nullable = false)
    private String email;
    /** Contraseña del usuario, no puede ser nula */
    @Column(nullable = false)
    private String password;
    /** Nombres del usuario */
    @Column(nullable = false)
    private String nombres;
    /** Apellidos del usuario */
    @Column(nullable = false)
    private String apellidos;
    /** Rol del usuario, puede ser ADMIN o USER */
    @Enumerated(EnumType.STRING)
    private RoleEntity role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

}
