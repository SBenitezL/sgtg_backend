package com.sgtg.backend.domain.models;

public class Usuario {
    /** Identificador único del usuario en el sistema, se genera automáticamente */
    private Long cusuari;
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
    /** Rol del usuario, puede ser ADMIN o USER */
    private String role;

    public Usuario(Long cusuari, String codigo, String email, String password, String nombres, String apellidos,
            String role) {
        this.cusuari = cusuari;
        this.codigo = codigo;
        this.email = email;
        this.password = password;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.role = role;
    }

    public boolean isEqualCodigo(Usuario other) {
        return this.codigo.equals(other.codigo);
    }

    public boolean isEqualEmail(Usuario other) {
        return this.email.equals(other.email);
    }

    public boolean updatePassword(String newPassword) {
        if (newPassword == null || newPassword.isEmpty())
            return false;
        this.password = newPassword;
        return true;
    }

    public boolean updateRole(String newRole) {
        if (newRole == null || newRole.isEmpty())
            return false;
        this.role = newRole;
        return true;
    }

    public Long getCusuari() {
        return cusuari;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getRole() {
        return role;
    }

}
