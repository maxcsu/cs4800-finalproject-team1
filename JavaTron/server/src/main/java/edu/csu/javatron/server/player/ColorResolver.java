/*
 * AI Tools Use Transparency Disclosure:
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron.server.player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class ColorResolver {

    private static final List<String> COLORS = List.of(
            "Green", "Blue", "Orange", "Red", "Yellow", "Purple", "Cyan", "Pink", "White", "Black"
    );

    private ColorResolver() {}

    public static String resolveColor(String requested, boolean enforceUnique, Set<String> alreadyUsed) {
        String candidate = normalizeColor(requested);

        if (!enforceUnique) return candidate;

        Set<String> used = (alreadyUsed == null) ? new HashSet<>() : alreadyUsed;
        if (!used.contains(candidate)) return candidate;

        java.util.ArrayList<String> available = new java.util.ArrayList<>();
        for (String c : COLORS) {
            if (!used.contains(c)) {
                available.add(c);
            }
        }
        if (!available.isEmpty()) {
            return available.get(ThreadLocalRandom.current().nextInt(available.size()));
        }
        return candidate; // fallback if all are used
    }

    public static String normalizeColor(String requested) {
        if (requested == null || requested.isBlank()) {
            return "Green";
        }

        String trimmed = requested.trim();
        for (String color : COLORS) {
            if (color.equalsIgnoreCase(trimmed)) {
                return color;
            }
        }
        return "Green";
    }
}
