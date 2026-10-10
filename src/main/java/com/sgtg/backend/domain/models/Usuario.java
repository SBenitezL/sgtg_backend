package com.sgtg.backend.domain.models;

import java.util.List;

import com.sgtg.backend.domain.utils.CheckString;

public class Usuario {
    /** Identificador único del usuario en el sistema, se genera automáticamente */
    private Long cusuari;
    /** Código con el que el usuario se presenta frente a la Universidad. */
    private String codigo;
    /** Tipo de documento de identidad */
    private TipoDocumentoIdentidad tipoDocumento;
    /** Número del documento de identidad, solo dígitos */
    private String numeroDocumento;
    /** Correo electrónico del usuario, es único y no puede ser nulo */
    private String email;
    /** Contraseña del usuario, no puede ser nula */
    private String password;
    /** Primer nombre, obligatorio */
    private String primerNombre;
    /** Segundo nombre, opcional */
    private String segundoNombre;
    /** Primer apellido, obligatorio */
    private String primerApellido;
    /** Segundo apellido, opcional */
    private String segundoApellido;
    /** Roles del usuario */
    private List<Role> role;
    /** Indica si el usuario puede iniciar sesión; un usuario inactivo conserva sus datos */
    private boolean activo;

    /** Longitud mínima de una contraseña nueva */
    public static final int LONGITUD_MINIMA_PASSWORD = 8;

    /** Crea un usuario activo. */
    public Usuario(Long cusuari, String codigo, TipoDocumentoIdentidad tipoDocumento, String numeroDocumento,
            String email, String password, String primerNombre, String segundoNombre, String primerApellido,
            String segundoApellido, List<Role> role) {
        this(cusuari, codigo, tipoDocumento, numeroDocumento, email, password, primerNombre, segundoNombre,
                primerApellido, segundoApellido, role, true);
    }

    public Usuario(Long cusuari, String codigo, TipoDocumentoIdentidad tipoDocumento, String numeroDocumento,
            String email, String password, String primerNombre, String segundoNombre, String primerApellido,
            String segundoApellido, List<Role> role, boolean activo) {
        this.activo = activo;
        this.cusuari = cusuari;
        this.codigo = codigo;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.email = email;
        this.password = password;
        this.primerNombre = primerNombre;
        this.segundoNombre = CheckString.isBlank(segundoNombre) ? null : segundoNombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = CheckString.isBlank(segundoApellido) ? null : segundoApellido;
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

    public boolean updateRole(List<Role> newRole) {
        if (newRole == null || newRole.isEmpty())
            return false;
        this.role = newRole;
        return true;
    }

    public void cargarRoles(List<Role> roles) {

        for (Role current : this.role)
            if (current.getCrole() != null)
                return;
        this.role = roles;
    }

    public Long getCusuari() {
        return cusuari;
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoDocumentoIdentidad getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public List<Role> getRole() {
        return this.role;
    }

    /**
     * @return true si el usuario puede iniciar sesión
     */
    public boolean isActivo() {
        return this.activo;
    }

    /**
     * Activa o inactiva al usuario. Un usuario inactivo no puede iniciar sesión
     * y sus tokens dejan de ser válidos.
     *
     * @param activo nuevo estado
     */
    public void cambiarEstado(boolean activo) {
        this.activo = activo;
    }

    /**
     * Reemplaza los datos de identificación y contacto del usuario (lo hace el
     * administrador). La contraseña, los roles y el estado se cambian aparte.
     */
    public void actualizarDatos(String codigo, TipoDocumentoIdentidad tipoDocumento, String numeroDocumento,
            String email, String primerNombre, String segundoNombre, String primerApellido, String segundoApellido) {
        this.codigo = codigo;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.email = email;
        this.primerNombre = primerNombre;
        this.segundoNombre = CheckString.isBlank(segundoNombre) ? null : segundoNombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = CheckString.isBlank(segundoApellido) ? null : segundoApellido;
    }

    /**
     * @param password contraseña nueva en texto plano
     * @return true si cumple la longitud mínima y no tiene espacios al inicio ni al final
     */
    public static boolean isPasswordNuevaValida(String password) {
        return password != null && password.length() >= LONGITUD_MINIMA_PASSWORD
                && password.equals(password.strip());
    }

    /**
     * @return true si el número de documento tiene solo dígitos
     */
    public boolean isNumeroDocumentoValido() {
        return !CheckString.isBlank(this.numeroDocumento) && this.numeroDocumento.matches("\\d+");
    }

    public boolean isValidRegister() {
        if (CheckString.isBlank(this.codigo))
            return false;
        if (this.tipoDocumento == null || !isNumeroDocumentoValido())
            return false;
        if (CheckString.isBlank(this.email))
            return false;
        if (CheckString.isBlank(this.password))
            return false;
        if (CheckString.isBlank(this.primerNombre))
            return false;
        if (CheckString.isBlank(this.primerApellido))
            return false;
        if (this.role == null || CheckString.isBlank(this.role.stream().map(role -> role.getTrole()).toList()))
            return false;

        return true;
    }
}
