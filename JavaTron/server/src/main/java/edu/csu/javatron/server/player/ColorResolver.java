/*
 * AI Assistance Disclosure:
 * Portions of this file were drafted with assistance from OpenAI ChatGPT.
 * All architecture, design, and final review were performed by Maxwell Nield.
 */

package edu.csu.javatron.server.player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ColorResolver {

    private static final List<String> COLORS = List.of(
            "Green", "Blue", "Orange", "Red", "Yellow", "Purple", "Cyan", "Pink", "White", "Black"
    );

    public static String resolveColor(String requested, boolean enforceUnique, Set<String> alreadyUsed) {
        String candidate = (requested == null || requested.isBlank()) ? "Green" : requested.trim();

        if (!enforceUnique) return candidate;

        Set<String> used = (alreadyUsed == null) ? new HashSet<>() : alreadyUsed;
        if (!used.contains(candidate)) return candidate;

        for (String c : COLORS) {
            if (!used.contains(c)) return c;
        }
        return candidate; // fallback if all are used
    }
}
