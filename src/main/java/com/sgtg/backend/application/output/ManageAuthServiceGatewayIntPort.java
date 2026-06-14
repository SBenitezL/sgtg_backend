package com.sgtg.backend.application.output;

/*
    * Puerto de salida para la gestión de autenticación, define las operaciones
    * relacionadas con el inicio de sesión y registro de usuarios
*/
public interface ManageAuthServiceGatewayIntPort {
    String login(String email, String password);
}
