package com.licoreria.pos.security;

import com.licoreria.pos.exception.ReglaNegocioException;
import org.springframework.stereotype.Component;

@Component
public class PoliticaPassword {

    public void validar(String password, String username) {
        if (password == null || password.length() < 8) {
            throw new ReglaNegocioException("PASSWORD_DEBIL", "La contraseña debe tener al menos 8 caracteres");
        }
        if (username != null && password.equalsIgnoreCase(username)) {
            throw new ReglaNegocioException("PASSWORD_DEBIL", "La contraseña no puede ser igual al usuario");
        }
        boolean tieneLetra = password.chars().anyMatch(Character::isLetter);
        boolean tieneNumero = password.chars().anyMatch(Character::isDigit);
        if (!tieneLetra || !tieneNumero) {
            throw new ReglaNegocioException("PASSWORD_DEBIL", "La contraseña debe incluir letras y números");
        }
    }
}
