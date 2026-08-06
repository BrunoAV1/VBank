package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.domain.Enums;
import org.springframework.http.HttpStatus;

import java.text.Normalizer;
import java.util.Locale;

public final class DomainNormalizer {
    private DomainNormalizer() {
    }

    public static String email(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String username(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("@") ? normalized.substring(1) : normalized;
    }

    public static String pixKey(Enums.PixKeyType type, String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return switch (type) {
            case EMAIL -> email(trimmed);
            case USERNAME -> "@" + username(trimmed);
            case PHONE -> normalizePhone(trimmed);
            case RANDOM -> trimmed.toLowerCase(Locale.ROOT);
        };
    }

    public static String resolvePixKey(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.contains("@") && !trimmed.startsWith("@")) return email(trimmed);
        if (trimmed.startsWith("@")) return "@" + username(trimmed);
        if (trimmed.matches("[+()0-9 .-]+")) return normalizePhone(trimmed);
        return trimmed.toLowerCase(Locale.ROOT);
    }

    public static String ascii(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static String normalizePhone(String value) {
        String digits = value.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 15) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Informe um telefone interno com 10 a 15 dígitos.");
        }
        return "+" + digits;
    }
}

