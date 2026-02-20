/*
 * AI Assistance Disclosure:
 * Portions of this file were drafted with assistance from OpenAI ChatGPT.
 * All architecture, design, and final review were performed by Maxwell Nield.
 */

package edu.csu.javatron.server.match;

public final class TrailGrid {

    private final int width;
    private final int height;

    // 0 = empty, 1 = occupied
    private final byte[] occupied;
    private final long[] placedAtMillis;

    public TrailGrid(int width, int height) {
        this.width = width;
        this.height = height;
        this.occupied = new byte[width * height];
        this.placedAtMillis = new long[width * height];
    }


    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void clear() {
        for (int i = 0; i < occupied.length; i++) {
            occupied[i] = 0;
            placedAtMillis[i] = 0L;
        }
    }


    public boolean isInside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean isOccupied(int x, int y) {
        int idx = (y * width) + x;
        return occupied[idx] != 0;
    }

    public void occupy(int x, int y, long nowMillis) {
        int idx = (y * width) + x;
        occupied[idx] = 1;
        placedAtMillis[idx] = nowMillis;
    }

    public void decayIfNeeded(int trailTimeSeconds, long nowMillis) {
        if (trailTimeSeconds < 0) return;

        long maxAge = trailTimeSeconds * 1000L;
        for (int i = 0; i < occupied.length; i++) {
            if (occupied[i] == 0) continue;
            long age = nowMillis - placedAtMillis[i];
            if (age >= maxAge) {
                occupied[i] = 0;
                placedAtMillis[i] = 0L;
            }
        }
    }
}
