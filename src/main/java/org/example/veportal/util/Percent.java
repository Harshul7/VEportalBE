package org.example.veportal.util;

public final class Percent {

    private Percent() {
    }

    public static Double of(double part, double total) {
        if (total <= 0) {
            return null;
        }
        return Math.round((part * 1000.0) / total) / 10.0;
    }
}
