/*
 * AI Assistance Disclosure:
 * Portions of this file were drafted with assistance from OpenAI ChatGPT.
 * All architecture, design, and final review were performed by Maxwell Nield.
 */

package edu.csu.javatron.server.player;

public final class PlayerProfile {
    public final int playerNumber;
    public final String ip;
    public final String color;

    public PlayerProfile(int playerNumber, String ip, String color) {
        this.playerNumber = playerNumber;
        this.ip = ip;
        this.color = color;
    }
}
