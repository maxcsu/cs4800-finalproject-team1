/*
 * AI Tools Use Transparency Disclosure:
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron;

import com.badlogic.gdx.graphics.Color;
import java.util.Map;

/** Canonical cycle/trail palette shared across client screens. */
public final class CycleColors {
    private static final Map<String, Color> COLORS = Map.ofEntries(
            Map.entry("BLACK", Color.valueOf("000000")),
            Map.entry("BLUE", Color.valueOf("99d9ea")),
            Map.entry("CYAN", Color.valueOf("c4fff0")),
            Map.entry("GREEN", Color.valueOf("6af059")),
            Map.entry("ORANGE", Color.valueOf("ff7f27")),
            Map.entry("PINK", Color.valueOf("ff4ab3")),
            Map.entry("PURPLE", Color.valueOf("ba5bee")),
            Map.entry("RED", Color.valueOf("ff4a68")),
            Map.entry("WHITE", Color.valueOf("ffffff")),
            Map.entry("YELLOW", Color.valueOf("f0e859")));

    private CycleColors() {}

    public static Color get(String colorName, Color defaultColor) {
        if (colorName == null) {
            return defaultColor;
        }
        Color color = COLORS.get(colorName.trim().toUpperCase());
        return color == null ? defaultColor : color;
    }
}
