package com.sgtg.backend.domain.models.acceso;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sgtg.backend.domain.models.acceso.PoliticaPassword.Resultado;

class PoliticaPasswordTest {

    private static final String CORREO = "ana.perez@unicauca.edu.co";
    private static final String DOCUMENTO = "1061234567";

    private static Resultado revisar(String password) {
        return PoliticaPassword.revisar(password, CORREO, DOCUMENTO);
    }

    @Test
    void unaContrasenaConLasCuatroClasesDeCaracteresEsValida() {
        assertEquals(Resultado.VALIDA, revisar("Clave123*"));
        assertEquals(Resultado.VALIDA, revisar("Montaña verde 7!"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "Cort1*", // menos de 8 caracteres
            "sinmayuscula1*", "SINMINUSCULA1*", "SinNumero**", "SinSimbolo123",
            " Clave123*", "Clave123* " // espacios al borde
    })
    void exigeLongitudYCombinacionDeCaracteres(String password) {
        assertEquals(Resultado.INVALIDA, revisar(password));
    }

    @Test
    void tieneUnLimiteDeLongitudParaBCrypt() {
        String sesentaYCuatro = "Aa1*" + "x".repeat(60);
        assertEquals(Resultado.VALIDA, revisar(sesentaYCuatro));
        assertEquals(Resultado.INVALIDA, revisar(sesentaYCuatro + "x"));
        // 64 caracteres pero más de 72 bytes
        assertEquals(Resultado.INVALIDA, revisar("Aa1*" + "ñ".repeat(60)));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Ana.Perez1*", "Clave1061234567*", "MiPassword1*", "Contraseña1*", "miCONTRASENA1*",
            "Unicauca2026*", "Clave123456*", "Qwerty12*" })
    void rechazaLasContrasenasConDatosDelUsuarioOPalabrasComunes(String password) {
        assertEquals(Resultado.PREDECIBLE, revisar(password));
    }

    @Test
    void lasPartesCortasDelCorreoNoCuentan() {
        assertEquals(Resultado.VALIDA, PoliticaPassword.revisar("Clave123*", "cla@unicauca.edu.co", "123"));
    }
}
