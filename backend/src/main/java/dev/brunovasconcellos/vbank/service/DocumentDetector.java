package dev.brunovasconcellos.vbank.service;

public final class DocumentDetector {
    private DocumentDetector() {
    }

    public static boolean looksLikeCpfOrCnpj(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        return isCpf(digits) || isCnpj(digits);
    }

    private static boolean isCpf(String value) {
        if (value.length() != 11 || value.chars().distinct().count() == 1) return false;
        int d1 = cpfDigit(value, 9, 10);
        int d2 = cpfDigit(value, 10, 11);
        return d1 == value.charAt(9) - '0' && d2 == value.charAt(10) - '0';
    }

    private static int cpfDigit(String value, int length, int weight) {
        int sum = 0;
        for (int i = 0; i < length; i++) sum += (value.charAt(i) - '0') * (weight - i);
        int result = 11 - (sum % 11);
        return result >= 10 ? 0 : result;
    }

    private static boolean isCnpj(String value) {
        if (value.length() != 14 || value.chars().distinct().count() == 1) return false;
        int[] firstWeights = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] secondWeights = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return cnpjDigit(value, firstWeights) == value.charAt(12) - '0'
                && cnpjDigit(value, secondWeights) == value.charAt(13) - '0';
    }

    private static int cnpjDigit(String value, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) sum += (value.charAt(i) - '0') * weights[i];
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}

