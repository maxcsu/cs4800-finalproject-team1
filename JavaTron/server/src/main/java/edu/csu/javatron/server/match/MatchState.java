/*
 * AI Tools Use Transparency Disclosure:
 * This file was handled by Maxwell Nield using Codex.
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
    public int ax = DEFAULT_GRID_W / 2, ay = DEFAULT_GRID_H / 4;
    public int bx = DEFAULT_GRID_W / 2, by = (DEFAULT_GRID_H * 3) / 4;

    public MatchState(int width, int height, int trailTimeSeconds) {
        this.grid = new TrailGrid(width, height);
        this.trailTimeSeconds = trailTimeSeconds;
    }

    /** Clears the arena and resets player positions. */
    public void reset() {
        int w = grid.getWidth();
        int h = grid.getHeight();
        int spawnAx = w / 2;
        int spawnBx = w / 2;
        int startAy = h / 4;
        int startBy = (h * 3) / 4;
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

        boolean aMoves = dax != 0 || day != 0;
        boolean bMoves = dbx != 0 || dby != 0;

        int nax = ax + dax;
        int nay = ay + day;
        int nbx = bx + dbx;
        int nby = by + dby;

        CollisionResult ca = CollisionResult.NONE;
        CollisionResult cb = CollisionResult.NONE;

        if (aMoves && !grid.isInside(nax, nay)) ca = CollisionResult.HIT_WALL;
        if (bMoves && !grid.isInside(nbx, nby)) cb = CollisionResult.HIT_WALL;

        if (aMoves && ca == CollisionResult.NONE && grid.isOccupied(nax, nay)) ca = CollisionResult.HIT_TRAIL;
        if (bMoves && cb == CollisionResult.NONE && grid.isOccupied(nbx, nby)) cb = CollisionResult.HIT_TRAIL;

        // Head-on collision (same next cell) counts as simultaneous collision.
        if (aMoves && bMoves && ca == CollisionResult.NONE && cb == CollisionResult.NONE) {
            if (nax == nbx && nay == nby) {
                ca = CollisionResult.HIT_TRAIL;
                cb = CollisionResult.HIT_TRAIL;
            }
        }

        // Leave trail behind only when that player actually attempted to move.
        if (aMoves) {
            grid.occupy(ax, ay, nowMillis);
        }
        if (bMoves) {
            grid.occupy(bx, by, nowMillis);
        }

        // Only update positions if that player did not collide.
        if (aMoves && ca == CollisionResult.NONE) { ax = nax; ay = nay; }
        if (bMoves && cb == CollisionResult.NONE) { bx = nbx; by = nby; }

        return new StepResult(ca, cb);
    }
}
