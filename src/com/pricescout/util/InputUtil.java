package com.pricescout.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Small helpers for turning request parameters into safe values. */
public final class InputUtil {

    /** Up to 8 digits before the decimal point and 2 after, so it always fits DECIMAL(10,2). */
    private static final Pattern PRICE = Pattern.compile("\\d{1,8}(\\.\\d{1,2})?");
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");

    private InputUtil() { }

    /** Returns the int, or null if the text is missing or not a whole number. */
    public static Integer parseInt(String s) {
        if (s == null) return null;
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a price such as "749.99", "$1,299" or "15". Returns a BigDecimal with 2 decimals,
     * or null when the text is not a valid price (negative, zero, 3+ decimals, too large, junk).
     */
    public static BigDecimal parsePrice(String raw) {
        if (raw == null) return null;
        String s = raw.trim().replace("$", "").replace(",", "");
        if (!PRICE.matcher(s).matches()) return null;
        BigDecimal value = new BigDecimal(s).setScale(2);
        return value.compareTo(MIN_PRICE) >= 0 ? value : null;
    }

    /** Only http(s) links are allowed as outbound "buy" links (blocks javascript: and data: URLs). */
    public static String webUrlOrNull(String url) {
        if (url == null) return null;
        String u = url.trim();
        String lower = u.toLowerCase();
        return (lower.startsWith("http://") || lower.startsWith("https://")) ? u : null;
    }
}
