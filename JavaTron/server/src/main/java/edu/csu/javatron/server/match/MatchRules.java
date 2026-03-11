/*
 * AI Tools Use Transparency Disclosure:
 * This file was handled by Maxwell Nield using Codex.
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

    /** Delay after matchmaking before clients are transitioned from lobby to gameplay. */
    public static final long MATCH_FOUND_DELAY_MS = 5_000L;

    /** Server-side freeze after a non-final round collision. */
    public static final long ROUND_COLLISION_FREEZE_MS = 5_616L;

    /** Delay before the start cue sound should fire ahead of gameplay unfreezing. */
    public static final long ROUND_START_SOUND_LEAD_MS = 2_067L;

    /** Server-side freeze after the match-winning collision before voting begins. */
    public static final long MATCH_CONCLUSION_FREEZE_MS = 6_500L;

    /** Short delay before remaining clients are returned to lobby after no/disconnect. */
    public static final long RETURN_TO_LOBBY_DELAY_MS = 1_500L;

    /** How long we wait for rematch votes after a match ends. */
    public static final long REMATCH_VOTE_TIMEOUT_MS = 30_000L;

    private MatchRules() {}
}
