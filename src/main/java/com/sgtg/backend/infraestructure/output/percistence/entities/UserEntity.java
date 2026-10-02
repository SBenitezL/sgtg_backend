package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
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
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_documento", columnNames = {
        "tipo_documento", "numero_documento" }))
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
    /** Tipo de documento de identidad */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumentoIdentidad tipoDocumento;
    /** Número del documento de identidad, solo dígitos */
    @Column(nullable = false)
    private String numeroDocumento;
    /** Correo electrónico del usuario, es único y no puede ser nulo */
    @Column(unique = true, nullable = false)
    private String email;
    /** Contraseña del usuario, no puede ser nula */
    @Column(nullable = false)
    private String password;
    /** Primer nombre del usuario */
    @Column(nullable = false)
    private String primerNombre;
    /** Segundo nombre del usuario, opcional */
    private String segundoNombre;
    /** Primer apellido del usuario */
    @Column(nullable = false)
    private String primerApellido;
    /** Segundo apellido del usuario, opcional */
    private String segundoApellido;
    /** Rol del usuario, puede ser ADMIN o USER */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "cusuari"), inverseJoinColumns = @JoinColumn(name = "crole"))
    // @Builder.Default
    private List<RoleEntity> role; // = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.stream().map(role -> new SimpleGrantedAuthority(role.getTrole().name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return email;
    }

}
