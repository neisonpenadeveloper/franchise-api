package com.accenture.franchise.domain.model;

import com.accenture.franchise.domain.exception.InvalidDataException;

/**
 * Utilidades de validacion y normalizacion de nombres compartidas por el modelo.
 *
 * <p>Los nombres se comparan sin distinguir mayusculas ni espacios sobrantes,
 * para que "Sucursal Norte" y "sucursal norte " se consideren el mismo nombre.</p>
 */
final class Names {

    private static final int MAX_LENGTH = 120;

    private Names() {
    }

    /** Normaliza (trim) y valida que el nombre no este vacio ni sea excesivamente largo. */
    static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException("El campo '" + field + "' es obligatorio");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_LENGTH) {
            throw new InvalidDataException(
                    "El campo '" + field + "' no puede superar los " + MAX_LENGTH + " caracteres");
        }
        return normalized;
    }

    /** Igualdad de nombres: sin distinguir mayusculas ni espacios en los extremos. */
    static boolean sameName(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    static String requireId(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException("El campo '" + field + "' es obligatorio");
        }
        return value.trim();
    }
}
