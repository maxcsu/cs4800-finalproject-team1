/*
 * AI Assistance Disclosure:
 * Portions of this file were drafted with assistance from OpenAI ChatGPT.
 * All architecture, design, and final review were performed by Maxwell Nield.
 */

package edu.csu.javatron.server.match;

import edu.csu.javatron.server.net.Protocol;

/**
 * Core match constants. Keep these aligned with Protocol so client/server agree on
 * arena size and tick rate.
 */
public final class MatchRules {
    public static final int GRID_W = Protocol.ARENA_COLS;   // 48
    public static final int GRID_H = Protocol.ARENA_ROWS;   // 80

    /** Authoritative fixed tick rate (per technical doc: ~60 ticks/sec). */
    public static final int TICK_RATE = Protocol.TICK_RATE; // 60

    /**
     * Moves are applied every N ticks. (Example: 12 cells/sec at 60 tps => 5 ticks/move.)
     */
    public static final int TICKS_PER_MOVE = Protocol.TICKS_PER_MOVE;

    /** Best-of-3 rounds (first to 2 wins). */
    public static final int ROUNDS_TO_WIN_MATCH = 2;

    /** Heartbeat timeout (ms) after which a client is considered dropped. */
    public static final long HEARTBEAT_TIMEOUT_MS = 12_000L;

    /** How long we wait for rematch votes after a match ends. */
    public static final long REMATCH_VOTE_TIMEOUT_MS = 30_000L;

    private MatchRules() {}
}
