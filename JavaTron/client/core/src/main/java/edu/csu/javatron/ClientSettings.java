/*
 * AI Tools Use Transparency Disclosure:
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Preferences;

/** Persistent client settings backed by LibGDX preferences. */
public final class ClientSettings {
    private static final String PREFS_NAME = "javatron-client-settings";
    private static final String KEY_MUSIC_ENABLED = "audio.music.enabled";
    private static final String KEY_MENU_SFX_ENABLED = "audio.menuSfx.enabled";
    private static final String KEY_GAME_SFX_ENABLED = "audio.gameSfx.enabled";
    private static final String KEY_UP = "input.up";
    private static final String KEY_DOWN = "input.down";
    private static final String KEY_LEFT = "input.left";
    private static final String KEY_RIGHT = "input.right";
    private static final String KEY_COLOR = "player.color";
    private static final String KEY_NAME = "player.name";

    private final Preferences preferences;

    public ClientSettings() {
        this.preferences = Gdx.app.getPreferences(PREFS_NAME);
    }

    public boolean isMusicEnabled() {
        return preferences.getBoolean(KEY_MUSIC_ENABLED, true);
    }

    public void setMusicEnabled(boolean enabled) {
        preferences.putBoolean(KEY_MUSIC_ENABLED, enabled);
    }

    public boolean isMenuSoundEffectsEnabled() {
        return preferences.getBoolean(KEY_MENU_SFX_ENABLED, true);
    }

    public void setMenuSoundEffectsEnabled(boolean enabled) {
        preferences.putBoolean(KEY_MENU_SFX_ENABLED, enabled);
    }

    public boolean isGameSoundEffectsEnabled() {
        return preferences.getBoolean(KEY_GAME_SFX_ENABLED, true);
    }

    public void setGameSoundEffectsEnabled(boolean enabled) {
        preferences.putBoolean(KEY_GAME_SFX_ENABLED, enabled);
    }

    public boolean isAudioEnabled() {
        return isMusicEnabled();
    }

    public void setAudioEnabled(boolean enabled) {
        setMusicEnabled(enabled);
    }

    public int getUpKey() {
        return preferences.getInteger(KEY_UP, Keys.W);
    }

    public void setUpKey(int keycode) {
        preferences.putInteger(KEY_UP, keycode);
    }

    public int getDownKey() {
        return preferences.getInteger(KEY_DOWN, Keys.S);
    }

    public void setDownKey(int keycode) {
        preferences.putInteger(KEY_DOWN, keycode);
    }

    public int getLeftKey() {
        return preferences.getInteger(KEY_LEFT, Keys.A);
    }

    public void setLeftKey(int keycode) {
        preferences.putInteger(KEY_LEFT, keycode);
    }

    public int getRightKey() {
        return preferences.getInteger(KEY_RIGHT, Keys.D);
    }

    public void setRightKey(int keycode) {
        preferences.putInteger(KEY_RIGHT, keycode);
    }

    public String getPlayerColor() {
        return preferences.getString(KEY_COLOR, "Blue");
    }

    public void setPlayerColor(String color) {
        preferences.putString(KEY_COLOR, color == null || color.isBlank() ? "Blue" : color.trim());
    }

    public String getPlayerName() {
        return preferences.getString(KEY_NAME, "Player");
    }

    public void setPlayerName(String name) {
        preferences.putString(KEY_NAME, name == null || name.isBlank() ? "Player" : name.trim());
    }

    public void flush() {
        preferences.flush();
    }
}
