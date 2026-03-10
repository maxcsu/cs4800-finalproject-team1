/*
 * AI Assistance Disclosure:
 * Portions of this file were drafted with assistance from OpenAI ChatGPT.
 * All architecture, design, and final review were performed by Maxwell Nield.
 */

package edu.csu.javatron.server.match;

/**
 * Minimal authoritative match state (grid-aligned).
 *
 * NOTE: This is intentionally "server-only" logic. The client should render based on snapshots.
 */
public final class MatchState {

    public enum CollisionResult {
        NONE,
        HIT_WALL,
        HIT_TRAIL
    }

    public static final int DEFAULT_GRID_W = MatchRules.GRID_W;
    public static final int DEFAULT_GRID_H = MatchRules.GRID_H;

    private final TrailGrid grid;
    private final int trailTimeSeconds;

    // Positions (grid coords)
    public int ax = 5, ay = 5;
    public int bx = DEFAULT_GRID_W - 6, by = DEFAULT_GRID_H - 6;

    public MatchState(int width, int height, int trailTimeSeconds) {
        this.grid = new TrailGrid(width, height);
        this.trailTimeSeconds = trailTimeSeconds;
    }

    /** Clears the arena and resets player positions. */
    public void reset() {
        int w = grid.getWidth();
        // Spawn A at left-third, facing right (D=down in server = up on screen, but direction is set by MatchRoom)
        // Spawn B at right-third, facing left
        // Using different x positions prevents immediate head-on collision
        int spawnAx = w / 4;            // ~12 for 48-wide grid
        int spawnBx = (w * 3) / 4;     // ~36 for 48-wide grid
        int startAy = 10;
        int startBy = grid.getHeight() - 11; // ~69 for 80-tall grid
        resetWithSpawns(spawnAx, startAy, spawnBx, startBy);
    }

public void resetWithSpawns(int spawnAx, int spawnAy, int spawnBx, int spawnBy) {
    grid.clear();
    ax = spawnAx;
    ay = spawnAy;
    bx = spawnBx;
    by = spawnBy;
}


    public TrailGrid getGrid() {
        return grid;
    }

    public static final class StepResult {
        public final CollisionResult a;
        public final CollisionResult b;

        public StepResult(CollisionResult a, CollisionResult b) {
            this.a = a;
            this.b = b;
        }

        public boolean anyCollision() {
            return a != CollisionResult.NONE || b != CollisionResult.NONE;
        }
    }

    /**
     * Steps both players "at the same time" so collision evaluation is fair.
     * Trails are left behind at each player's current cell when a move is applied.
     */
    public StepResult stepBoth(int dax, int day, int dbx, int dby, long nowMillis) {
        grid.decayIfNeeded(trailTimeSeconds, nowMillis);

        int nax = ax + dax;
        int nay = ay + day;
        int nbx = bx + dbx;
        int nby = by + dby;

        CollisionResult ca = CollisionResult.NONE;
        CollisionResult cb = CollisionResult.NONE;

        if (!grid.isInside(nax, nay)) ca = CollisionResult.HIT_WALL;
        if (!grid.isInside(nbx, nby)) cb = CollisionResult.HIT_WALL;

        if (ca == CollisionResult.NONE && grid.isOccupied(nax, nay)) ca = CollisionResult.HIT_TRAIL;
        if (cb == CollisionResult.NONE && grid.isOccupied(nbx, nby)) cb = CollisionResult.HIT_TRAIL;

        // Head-on collision (same next cell) counts as simultaneous collision.
        if (ca == CollisionResult.NONE && cb == CollisionResult.NONE) {
            if (nax == nbx && nay == nby) {
                ca = CollisionResult.HIT_TRAIL;
                cb = CollisionResult.HIT_TRAIL;
            }
        }

        // Leave trail behind only if move was attempted (even if collision occurs, the round ends anyway).
        grid.occupy(ax, ay, nowMillis);
        grid.occupy(bx, by, nowMillis);

        // Only update positions if that player did not collide.
        if (ca == CollisionResult.NONE) { ax = nax; ay = nay; }
        if (cb == CollisionResult.NONE) { bx = nbx; by = nby; }

        return new StepResult(ca, cb);
    }
}
