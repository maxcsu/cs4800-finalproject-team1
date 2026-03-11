/*
 * AI Tools Use Transparency Disclosure:
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron;

/** Shared menu-screen visual constants. */
public final class MenuVisuals {
    public static final boolean ENABLE_GLOW = false;
    public static final float BG_TILE_SCALE = 2f;
    public static final float PARALLAX_SCROLL_X_SPEED = 0.075f;
    public static final float PARALLAX_SCROLL_Y_SPEED = 0.045f;
    public static final float DOWNWARD_SCROLL_SPEED = 0.045f;

    private MenuVisuals() {}

    public static float backgroundURepeat(float worldWidth, float textureWidth) {
        return worldWidth / textureWidth / BG_TILE_SCALE;
    }

    public static float backgroundVRepeat(float worldHeight, float textureHeight) {
        return worldHeight / textureHeight / BG_TILE_SCALE;
    }
}
