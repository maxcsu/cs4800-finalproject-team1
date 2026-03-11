/*
 * AI Tools Use Transparency Disclosure:
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

/** Enforces the portrait virtual aspect ratio on desktop window resizes. */
public final class WindowAspectEnforcer {
    private WindowAspectEnforcer() {}

    public static boolean enforce(int width, int height) {
        if (width <= 0 || height <= 0) {
            return true;
        }
        if (Gdx.app.getType() != Application.ApplicationType.Desktop) {
            return false;
        }

        float targetAspect = JavaTronGame.VIRTUAL_WIDTH / JavaTronGame.VIRTUAL_HEIGHT;
        float currentAspect = (float) width / (float) height;

        int newWidth = width;
        int newHeight = height;

        if (currentAspect > targetAspect) {
            newWidth = Math.round(height * targetAspect);
        } else if (currentAspect < targetAspect) {
            newHeight = Math.round(width / targetAspect);
        }

        if (newWidth != width || newHeight != height) {
            Gdx.graphics.setWindowedMode(newWidth, newHeight);
            return true;
        }

        return false;
    }
}
