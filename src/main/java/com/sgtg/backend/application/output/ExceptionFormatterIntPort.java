package com.sgtg.backend.application.output;

/*
    * Puerto de salida para formatear excepciones, define las operaciones
    * relacionadas con la conversión de excepciones a formatos específicos
*/
public interface ExceptionFormatterIntPort {

    /**
     * Devuelve una respuesta de error indicando que la entidad ya existe.
     * 
     * @param message Mensaje personalizado del error.
     */
    public void returnResponseErrorEntityExists(String message);

    /**
     * Devuelve una respuesta de error indicando que la entidad no fue encontrada.
     * 
     * @param message Mensaje personalizado del error.
     */
    public void returnResponseErrorEntityNotFound(String message);

    /**
     * Devuelve una respuesta de error indicando que se ha violado una regla de
     * negocio.
     * 
     * @param message Mensaje personalizado del error.
     */
    public void returnResponseBusinessRuleViolated(String message);

    /**
     * Devuelve una respuesta de error indicando que las credenciales proporcionadas
     * son incorrectas.
     * 
     * @param message Mensaje personalizado del error.
     */
    public void returnResponseBadCredentials(String message);

    /**
     * Devuelve una respuesta de error indicando que el formato de la solicitud es
     * incorrecto.
     * 
     * @param message Mensaje personalizado del error.
     */
    public void returnResponseBadFormat(String message);

    /**
     * Devuelve una respuesta de error indicando que no se encontraron datos para la
     * consulta realizada.
     * 
     * @param message Mensaje personalizado del error.
     */
    public void returnNoData(String message);

    /**
     * Devuelve una respuesta de error indicando que el usuario no tiene acceso a la
     * operación solicitada.
     * 
     * @param message Mensaje personalizado del error.
     */
    public void returnNoAccess(String message);

}
