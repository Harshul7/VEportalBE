package org.example.veportal.util;

import java.util.Locale;
import org.example.veportal.exception.BusinessException;

public final class SizeFormat {

    private static final long KB = 1024L;
    private static final long MB = KB * 1024;

    private SizeFormat() {
    }

    public static Long parse(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String normalized = label.trim().toUpperCase(Locale.ROOT);
        try {
            if (normalized.endsWith("KB")) {
                return Math.round(Double.parseDouble(normalized.substring(0, normalized.length() - 2).trim()) * KB);
            }
            if (normalized.endsWith("MB")) {
                return Math.round(Double.parseDouble(normalized.substring(0, normalized.length() - 2).trim()) * MB);
            }
            if (normalized.endsWith("B")) {
                return (long) Double.parseDouble(normalized.substring(0, normalized.length() - 1).trim());
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    public static String format(Long bytes) {
        if (bytes == null) {
            return "—";
        }
        if (bytes >= MB) {
            return trim(bytes / (double) MB) + " MB";
        }
        if (bytes >= KB) {
            return trim(bytes / (double) KB) + " KB";
        }
        return bytes + " B";
    }

    private static String trim(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
