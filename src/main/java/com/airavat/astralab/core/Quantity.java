package com.airavat.astralab.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Quantity {
    private static final Pattern VALUE_PATTERN = Pattern.compile("([-+]?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?)(.*)");

    private Quantity() {
    }

    public static double parse(String text, double fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        Matcher matcher = VALUE_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            return fallback;
        }
        double value = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2).trim().toLowerCase(Locale.ROOT);
        return switch (unit) {
            case "", "m", "meter", "meters", "n", "kg", "s", "k" -> value;
            case "cm" -> value / 100.0;
            case "mm" -> value / 1000.0;
            case "g" -> value / 1000.0;
            case "m2", "m^2" -> value;
            case "cm2", "cm^2" -> value / 10_000.0;
            case "deg", "degree", "degrees" -> value;
            case "rad", "radian", "radians" -> Math.toDegrees(value);
            case "km" -> value * 1000.0;
            case "ms" -> value / 1000.0;
            case "kpa" -> value * 1000.0;
            case "pa" -> value;
            case "m/s", "ms-1" -> value;
            case "km/h", "kph" -> value / 3.6;
            default -> value;
        };
    }

    public static int parseInt(String text, int fallback) {
        return (int) Math.round(parse(text, fallback));
    }
}
