/*
 * AI Tools Use Transparency Disclosure:
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron.server.match;

import edu.csu.javatron.server.ServerConfig;
import edu.csu.javatron.server.StatsManager;
import edu.csu.javatron.server.net.ClientSession;
import edu.csu.javatron.server.net.Protocol;
import edu.csu.javatron.server.net.SnapshotSerializer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns one active match "box" on the server.
 *
 * - Runs an authoritative fixed tick loop
 * - Applies player turns at a fixed movement cadence
 * - First to 2 round wins (best-of-3) wins the match
 * - After match end, offers a rematch vote window; if both vote YES, a fresh match starts
 */
public final class MatchRoom {

    public static final class MatchResult {
        public final ClientSession playerA;
        public final ClientSession playerB;
        public final String summaryLine;

        public MatchResult(ClientSession playerA, ClientSession playerB, String summaryLine) {
            this.playerA = playerA;
            this.playerB = playerB;
            this.summaryLine = summaryLine;
        }
    }

    public interface MatchEndedCallback {
        void onMatchEnded(int boxId, MatchResult result);
    }

    public static final class Snapshot {
        public final int boxId;
        public final long tick;
        public final int ax, ay, bx, by;
        public final char aDir, bDir;
        public final int aWins, bWins;
        public final int roundNumber;

        public Snapshot(int boxId, long tick, int ax, int ay, int bx, int by,
                        char aDir, char bDir, int aWins, int bWins, int roundNumber) {
            this.boxId = boxId;
            this.tick = tick;
            this.ax = ax;
            this.ay = ay;
            this.bx = bx;
            this.by = by;
            this.aDir = aDir;
            this.bDir = bDir;
            this.aWins = aWins;
            this.bWins = bWins;
            this.roundNumber = roundNumber;
        }
    }

    private final ServerConfig config;
    private final ServerConfig.Logger logger;
    private final StatsManager statsManager;

    private final int boxId;
    private final ClientSession a;
    private final ClientSession b;

    private final MatchEndedCallback onEnded;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    private final MatchState state;

    // Current movement direction (U/D/L/R) for each player
    private volatile char aDir = 'D';
    private volatile char bDir = 'U';

    // Latest requested turns (applied on move ticks)
    private volatile char aRequested = '\0';
    private volatile char bRequested = '\0';

    // Movement budgets (accumulators) in "grid steps"
    private float aMoveBudget = 0.0f;
    private float bMoveBudget = 0.0f;

    // Set true when a player presses the direction they are already traveling (acceleration request)
    private boolean aAccelRequested = false;
    private boolean bAccelRequested = false;
    private boolean aSlowRequested = false;
    private boolean bSlowRequested = false;

    // Constants
    private static final float ACCEL_MULTIPLIER = 1.25f;
    private static final float SLOW_MULTIPLIER = 0.75f;
    private long tick = 0;

    private int aWins = 0;
    private int bWins = 0;
        // Safety valves (optional)
    private int consecutiveDraws = 0;
    private long aLastInputMillis = 0L;
    private long bLastInputMillis = 0L;
    private boolean useCustomSpawnsNextRound = false;
    private int nextSpawnAx;
    private int nextSpawnAy;
    private int nextSpawnBx;
    private int nextSpawnBy;

private int roundNumber = 1;

    private final RematchVoteManager rematchVotes;

    // Rematch phase
    private volatile boolean waitingForRematchVotes = false;

    // The previous versions had a server-side bot. Per spec clarification, bot games are CLIENT-SIDE ONLY.
    public MatchRoom(ServerConfig config, ServerConfig.Logger logger, StatsManager statsManager, int boxId,
                     ClientSession a, ClientSession b,
                     Object botUnused,
                     MatchEndedCallback onEnded) {
        this.config = Objects.requireNonNull(config);
        this.logger = Objects.requireNonNull(logger);
        this.statsManager = Objects.requireNonNull(statsManager);
        this.boxId = boxId;
        this.a = a;
        this.b = b;
        this.onEnded = onEnded;

        this.state = new MatchState(MatchRules.GRID_W, MatchRules.GRID_H, config.trailTimeSeconds);
        this.rematchVotes = new RematchVoteManager(a, b);
    }

    public void start() {
        if (running.getAndSet(true)) return;
        thread = new Thread(this::runLoop, "MatchRoom-" + boxId);
        thread.setDaemon(true);
        thread.start();
    }

    public void shutdown() {
        running.set(false);

        if (logger != null) {
		logger.warn("[Box " + boxId + "] END: match terminated");
        }

    }

    /** Called by LobbyManager when it receives a C_TURN from a client. */
    public void onClientTurn(ClientSession who, char dir) {
        if (!running.get()) return;
        if (who == null) return;
        char d = Character.toUpperCase(dir);

        if (!isValidDir(d)) return;

        long now = System.currentTimeMillis();
        if (who.getPlayerNumber() == a.getPlayerNumber()) {
            aRequested = d;
            aLastInputMillis = now;
        } else if (who.getPlayerNumber() == b.getPlayerNumber()) {
            bRequested = d;
            bLastInputMillis = now;
        }
    }

    /** Called by LobbyManager when it receives a rematch vote from a client. */
    public void onClientRematchVote(ClientSession who, boolean yes) {
        if (!waitingForRematchVotes) return;
        rematchVotes.vote(who, yes);
        if (yes) {
            sendToBoth(Protocol.S_REMATCH_STATUS + "|result=yes|by=" + who.getPlayerNumber());
        }
    }

    public void onClientDisconnected(ClientSession who) {
        running.set(false);
        boolean duringRematchVote = waitingForRematchVotes;
        waitingForRematchVotes = false;
        rematchVotes.end();

        ClientSession survivor = (who == a) ? b : a;
        String summary;
        if (survivor != null && survivor.isConnected()) {
            if (duringRematchVote) {
                survivor.sendLine(Protocol.S_REMATCH_STATUS + "|result=disconnect");
                sleepMillis(MatchRules.RETURN_TO_LOBBY_DELAY_MS);
                survivor.sendLine(Protocol.S_RETURN_TO_LOBBY + "|reason=disconnect");
                summary = "Opponent disconnected during rematch vote";
            } else {
                summary = "Opponent disconnected. You win the match by forfeit.";
                survivor.sendLine(Protocol.S_ROUND_END
                        + "|box=" + boxId
                        + "|event=FORFEIT"
                        + "|yourResult=WIN"
                        + "|winnerSide=" + ((survivor == a) ? "A" : "B")
                        + "|matchOver=1"
                        + "|summary=" + summary);
                sleepMillis(MatchRules.MATCH_CONCLUSION_FREEZE_MS);
                survivor.sendLine(Protocol.S_RETURN_TO_LOBBY + "|reason=disconnect");
            }
        } else {
            summary = "Both players disconnected";
        }

        if (onEnded != null) {
            onEnded.onMatchEnded(boxId, new MatchResult(a, b, summary));
        }
    }

    private void runLoop() {
        // Send an explicit match start line so the client can switch out of lobby/practice mode.
        sendToBoth(Protocol.S_MATCH_START
                + "|box=" + boxId
                + "|mode=pvp"
                + "|bestOf=3"
                + "|roundsToWin=" + MatchRules.ROUNDS_TO_WIN_MATCH);

        resetForNewMatch();
        runCountdown();

        long nextTickNanos = System.nanoTime();
        long nanosPerTick = 1_000_000_000L / MatchRules.TICK_RATE;

        while (running.get()) {
            // Liveness / disconnect checks
            if (!a.isConnected() || !b.isConnected()) {
                endMatch("Opponent disconnected");
                return;
            }
            long nowMs = System.currentTimeMillis();
            if (nowMs - a.getLastHeardMillis() > MatchRules.HEARTBEAT_TIMEOUT_MS) {
                endMatch("Player " + pad2(a.getPlayerNumber()) + " timed out");
                return;
            }
            if (nowMs - b.getLastHeardMillis() > MatchRules.HEARTBEAT_TIMEOUT_MS) {
                endMatch("Player " + pad2(b.getPlayerNumber()) + " timed out");
                return;
            }

            // Rematch voting phase
            if (waitingForRematchVotes) {
                if (rematchVotes.bothVotedYes()) {
                    logger.info(String.format("[Box %d] Rematch accepted by both players.", boxId));
                    waitingForRematchVotes = false;
                    rematchVotes.end();

                    sendToBoth(Protocol.S_MATCH_START
                            + "|box=" + boxId
                            + "|mode=pvp"
                            + "|bestOf=3"
                            + "|roundsToWin=" + MatchRules.ROUNDS_TO_WIN_MATCH
                            + "|rematch=1");

                    resetForNewMatch();
                    runCountdown();
                } else if (rematchVotes.anyVotedNo() || rematchVotes.timedOut(nowMs)) {
                    sendToBoth(Protocol.S_REMATCH_STATUS + "|result=no");
                    sleepMillis(MatchRules.RETURN_TO_LOBBY_DELAY_MS);
                    sendToBoth(Protocol.S_RETURN_TO_LOBBY + "|reason=no");
                    endMatch("Match concluded");
                    return;
                }

                // During rematch vote we still tick a little so clients don't think we froze
                sendSnapshot();
                sleepUntilNextTick(nanosPerTick);
                continue;
            }


// Optional idle-kick (only during active match play, not lobby).
if (config != null && config.idleKickSeconds > 0) {
    long idleLimitMs = config.idleKickSeconds * 1000L;
    boolean aIdle = (nowMs - aLastInputMillis) > idleLimitMs;
    boolean bIdle = (nowMs - bLastInputMillis) > idleLimitMs;

    if (aIdle && bIdle) {
        endMatch("Both players idle");
        return;
    } else if (aIdle) {
        endMatch(String.format("Player %s idle (forfeit)", pad2(a.getPlayerNumber())));
        return;
    } else if (bIdle) {
        endMatch(String.format("Player %s idle (forfeit)", pad2(b.getPlayerNumber())));
        return;
    }
}

            // Authoritative tick
            tick++;

            // Movement is applied using per-player budgets so we can support slow/accelerate behavior.
            // Baseline: 1 grid step every MatchRules.TICKS_PER_MOVE ticks.
            processDirectionalRequests();

            // Effective speed for this tick (reverse => slow, same-dir => accelerate).
            float basePerTick = 1.0f / (float) MatchRules.TICKS_PER_MOVE;

            float aEffective = getEffectiveSpeedMultiplier(true);
            float bEffective = getEffectiveSpeedMultiplier(false);

            aMoveBudget += basePerTick * aEffective;
            bMoveBudget += basePerTick * bEffective;

            // Perform as many simultaneous sub-steps as budgets allow.
            boolean roundTransitionHandled = false;
            while (aMoveBudget >= 1.0f || bMoveBudget >= 1.0f) {
                int[] aDelta = (aMoveBudget >= 1.0f) ? dirToDelta(aDir) : new int[]{0, 0};
                int[] bDelta = (bMoveBudget >= 1.0f) ? dirToDelta(bDir) : new int[]{0, 0};

                MatchState.StepResult res = state.stepBoth(aDelta[0], aDelta[1], bDelta[0], bDelta[1], nowMs);

                if (aMoveBudget >= 1.0f) aMoveBudget -= 1.0f;
                if (bMoveBudget >= 1.0f) bMoveBudget -= 1.0f;

                if (res.anyCollision()) {
                    handleRoundEnd(res);
                    roundTransitionHandled = true;
                    break;
                }
            }

            // Clear per-tick speed modifier requests (clients resend while the key is held)
            aAccelRequested = false;
            bAccelRequested = false;
            aSlowRequested = false;
            bSlowRequested = false;

            if (roundTransitionHandled) {
                continue;
            }

            sendSnapshot();
            sleepUntilNextTick(nanosPerTick);
        }
    }

    private void sleepUntilNextTick(long nanosPerTick) {
        long next = System.nanoTime() + nanosPerTick;
        while (true) {
            long remaining = next - System.nanoTime();
            if (remaining <= 0) break;
            try {
                // sleep in millis, then spin a tiny bit
                long ms = remaining / 1_000_000L;
                if (ms > 0) Thread.sleep(ms);
                else Thread.yield();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleRoundEnd(MatchState.StepResult res) {
        // Determine winner (if any)
        boolean aHit = res.a != MatchState.CollisionResult.NONE;
        boolean bHit = res.b != MatchState.CollisionResult.NONE;

        String roundSummary;
        boolean wasDraw;

        if (aHit && !bHit) {
            wasDraw = false;

            // B wins round
            bWins++;
            roundSummary = String.format("Round %d: %s %s derezzed %s %s. Score: %d-%d",
                    roundNumber,
                    b.getDisplayColor(), pad2(b.getPlayerNumber()),
                    a.getDisplayColor(), pad2(a.getPlayerNumber()),
                    aWins, bWins);
        } else if (bHit && !aHit) {
            wasDraw = false;
            // A wins round
            aWins++;
            roundSummary = String.format("Round %d: %s %s derezzed %s %s. Score: %d-%d",
                    roundNumber,
                    a.getDisplayColor(), pad2(a.getPlayerNumber()),
                    b.getDisplayColor(), pad2(b.getPlayerNumber()),
                    aWins, bWins);
        } else {
            wasDraw = true;
            // draw (both collided)
            roundSummary = String.format("Round %d: draw (simultaneous collision). Score: %d-%d",
                    roundNumber, aWins, bWins);
        }

        boolean matchOver = aWins >= MatchRules.ROUNDS_TO_WIN_MATCH || bWins >= MatchRules.ROUNDS_TO_WIN_MATCH;
        String winnerSide = "NONE";
        if (aHit && !bHit) {
            winnerSide = "B";
        } else if (bHit && !aHit) {
            winnerSide = "A";
        }

        a.sendLine(Protocol.S_ROUND_END + "|box=" + boxId
                + "|event=COLLISION"
                + "|yourResult=" + perspectiveResult(winnerSide, "A")
                + "|winnerSide=" + winnerSide
                + "|matchOver=" + (matchOver ? 1 : 0)
                + "|summary=" + roundSummary);
        b.sendLine(Protocol.S_ROUND_END + "|box=" + boxId
                + "|event=COLLISION"
                + "|yourResult=" + perspectiveResult(winnerSide, "B")
                + "|winnerSide=" + winnerSide
                + "|matchOver=" + (matchOver ? 1 : 0)
                + "|summary=" + roundSummary);
        sendSnapshot();

        if (logger != null) {
            logger.info("[Box " + boxId + "] " + roundSummary);
        }

        if (wasDraw) consecutiveDraws++; else consecutiveDraws = 0;

        // Match ended?
        if (matchOver) {
            String winner;
            if (aWins > bWins) winner = a.getDisplayColor() + " " + pad2(a.getPlayerNumber());
            else winner = b.getDisplayColor() + " " + pad2(b.getPlayerNumber());

            String matchSummary = String.format("%s won the match. Final Score: %d-%d", winner, aWins, bWins);

            // Emit a machine-readable match end while preserving legacy position of the human summary.
            // Format:
            //   S_MATCH_END|box=<id>|<summary>|winner=<playerNum>|result=WIN/LOSE|score=a-b
            int winnerNum = (aWins > bWins) ? a.getPlayerNumber() : b.getPlayerNumber();
            String score = aWins + "-" + bWins;

            sleepMillis(MatchRules.MATCH_CONCLUSION_FREEZE_MS);

            a.sendLine(Protocol.S_MATCH_END + "|box=" + boxId + "|" + matchSummary
                    + "|winner=" + pad2(winnerNum)
                    + "|result=" + ((winnerNum == a.getPlayerNumber()) ? "WIN" : "LOSE")
                    + "|score=" + score);

            b.sendLine(Protocol.S_MATCH_END + "|box=" + boxId + "|" + matchSummary
                    + "|winner=" + pad2(winnerNum)
                    + "|result=" + ((winnerNum == b.getPlayerNumber()) ? "WIN" : "LOSE")
                    + "|score=" + score);

            if (logger != null) {
                logger.info("[Box " + boxId + "] MATCH END: " + matchSummary);
            }

            ClientSession winnerSession = (winnerNum == a.getPlayerNumber()) ? a : b;
            ClientSession loserSession = (winnerSession == a) ? b : a;
            statsManager.recordMatchResult(winnerSession, loserSession);


            // Enter rematch vote window
            waitingForRematchVotes = true;
            rematchVotes.start(System.currentTimeMillis(), MatchRules.REMATCH_VOTE_TIMEOUT_MS);
            sendToBoth(Protocol.S_REMATCH_PROMPT + "|box=" + boxId + "|timeoutMs=" + MatchRules.REMATCH_VOTE_TIMEOUT_MS
                    + "|hint=Send " + Protocol.C_REMATCH_VOTE + "|YES or |NO");

            return;
        }

// Optional draw breaker: after N consecutive draws, re-seed spawns to avoid infinite head-on loops.
if (wasDraw && config != null && config.maxConsecutiveDraws > 0 && consecutiveDraws >= config.maxConsecutiveDraws) {
    consecutiveDraws = 0;

    int gridW = Protocol.ARENA_COLS;
    int safeMargin = 4;
    int midX = gridW / 2;

    // Pick offsets so A and B are not perfectly aligned.
    int offsetA = java.util.concurrent.ThreadLocalRandom.current().nextInt(-6, 7);
    int offsetB = java.util.concurrent.ThreadLocalRandom.current().nextInt(-6, 7);

    // Ensure offsets are not identical (reduce chance of another head-on).
    if (offsetA == offsetB) offsetB += (offsetB >= 0) ? 2 : -2;

    nextSpawnAx = clamp(midX + offsetA, safeMargin, gridW - 1 - safeMargin);
    nextSpawnBx = clamp(midX + offsetB, safeMargin, gridW - 1 - safeMargin);

    nextSpawnAy = 10;
    nextSpawnBy = Protocol.ARENA_ROWS - 11;

    // Break perfect vertical symmetry so idle mirrored players do not still hit their
    // opposite walls on the same tick after only an x-offset reseed.
    if (java.util.concurrent.ThreadLocalRandom.current().nextBoolean()) {
        nextSpawnAy += 1;
    } else {
        nextSpawnBy -= 1;
    }

    useCustomSpawnsNextRound = true;

    if (logger != null) {
        logger.info(String.format("[Box %d] Draw breaker engaged after %d consecutive draws. Next spawns: A(%d,%d) B(%d,%d)",
                boxId, config.maxConsecutiveDraws, nextSpawnAx, nextSpawnAy, nextSpawnBx, nextSpawnBy));
    }
}

        sleepMillis(MatchRules.ROUND_COLLISION_FREEZE_MS);

        // Next round
        roundNumber++;
        resetForNextRound();
        runCountdown();
    }

    private void resetForNewMatch() {
        aWins = 0;
        bWins = 0;
        roundNumber = 1;
        tick = 0;
        consecutiveDraws = 0;
        long now = System.currentTimeMillis();
        aLastInputMillis = now;
        bLastInputMillis = now;
        resetForNextRound();
    }

    private void resetForNextRound() {
        if (useCustomSpawnsNextRound) {
            state.resetWithSpawns(nextSpawnAx, nextSpawnAy, nextSpawnBx, nextSpawnBy);
            useCustomSpawnsNextRound = false;
        } else {
            state.reset();
        }

        // Start directions
        aDir = 'D';
        bDir = 'U';
        aRequested = '\0';
        bRequested = '\0';

        // Reset speed/budget state
        aMoveBudget = 0.0f;
        bMoveBudget = 0.0f;
        aAccelRequested = false;
        bAccelRequested = false;
        aSlowRequested = false;
        bSlowRequested = false;

        // Notify clients of reset state via one snapshot immediately
        sendSnapshot();
    }
    
    private void runCountdown() {
        // Send the countdown while gameplay is frozen. A dedicated start cue packet is
        // emitted 2067ms before the round resumes.
        sendToBoth(Protocol.S_ROUND_END + "|box=" + boxId + "|COUNTDOWN:3");
        sendSnapshot();
        sleepMillis(933);
        sendToBoth(Protocol.S_ROUND_END + "|box=" + boxId + "|COUNTDOWN:CYCLESTART");
        sleepMillis(67);
        sendToBoth(Protocol.S_ROUND_END + "|box=" + boxId + "|COUNTDOWN:2");
        sendSnapshot();
        sleepMillis(1000);
        sendToBoth(Protocol.S_ROUND_END + "|box=" + boxId + "|COUNTDOWN:1");
        sendSnapshot();
        sleepMillis(1000);
        sendToBoth(Protocol.S_ROUND_END + "|box=" + boxId + "|COUNTDOWN:GO");
        sendSnapshot();
    }

	/**
	 * Applies queued directional input requests to authoritative direction state.
	 *
	 * Amendment rules:
	 *  - Opposite direction: do NOT reverse; apply a one-tick slow multiplier.
	 *  - Same direction: request a one-tick acceleration multiplier (client resends while held).
	 *  - Orthogonal direction: turn normally.
	 */
	private void processDirectionalRequests() {
		// Player A
		char reqA = aRequested;
		if (isValidDir(reqA)) {
			if (isOpposite(aDir, reqA)) {
				aSlowRequested = true;
			} else if (reqA == aDir) {
				aAccelRequested = true;
			} else {
				aDir = reqA;
			}
		}

		// Player B
		char reqB = bRequested;
		if (isValidDir(reqB)) {
			if (isOpposite(bDir, reqB)) {
				bSlowRequested = true;
			} else if (reqB == bDir) {
				bAccelRequested = true;
			} else {
				bDir = reqB;
			}
		}

		aRequested = '\0';
		bRequested = '\0';
	}

	/**
	 * Returns the effective multiplier for this tick.
	 *
	 * Slow is time-based (ticks). Accel is a one-tick request.
	 */
	private float getEffectiveSpeedMultiplier(boolean isPlayerA) {
		if (isPlayerA) {
			if (aSlowRequested) return SLOW_MULTIPLIER;
			if (aAccelRequested) return ACCEL_MULTIPLIER;
			return 1.0f;
		}

		if (bSlowRequested) return SLOW_MULTIPLIER;
		if (bAccelRequested) return ACCEL_MULTIPLIER;
		return 1.0f;
	}

	private static boolean isValidDir(char dir) {
		return dir == 'U' || dir == 'D' || dir == 'L' || dir == 'R';
	}

	private static boolean isOpposite(char currentDir, char requestedDir) {
		return (currentDir == 'U' && requestedDir == 'D')
				|| (currentDir == 'D' && requestedDir == 'U')
				|| (currentDir == 'L' && requestedDir == 'R')
				|| (currentDir == 'R' && requestedDir == 'L');
	}

    private static void sleepMillis(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private void sendSnapshot() {
        Snapshot s = new Snapshot(
                boxId,
                tick,
                state.ax, state.ay,
                state.bx, state.by,
                aDir, bDir,
                aWins, bWins,
                roundNumber
        );
        String line = SnapshotSerializer.toSnapshotLine(s);
        sendToBoth(line);
    }

    private void sendToBoth(String line) {
        if (a != null && a.isConnected()) a.sendLine(line);
        if (b != null && b.isConnected()) b.sendLine(line);
    }

    private void endMatch(String summary) {
        running.set(false);

        // If we got here during rematch voting, don't leave it dangling.
        waitingForRematchVotes = false;
        rematchVotes.end();

        if (onEnded != null) {
            onEnded.onMatchEnded(boxId, new MatchResult(a, b, summary));
        }
    }

    
private static int clamp(int v, int min, int max) {
    if (v < min) return min;
    if (v > max) return max;
    return v;
}

private static String pad2(int n) {
        return (n < 10) ? ("0" + n) : Integer.toString(n);
    }

    private static String perspectiveResult(String winnerSide, String side) {
        if ("NONE".equals(winnerSide)) {
            return "DRAW";
        }
        return winnerSide.equals(side) ? "WIN" : "LOSE";
    }

    private static int[] dirToDelta(char dir) {
        switch (dir) {
            case 'U': return new int[]{0, -1};
            case 'D': return new int[]{0, 1};
            case 'L': return new int[]{-1, 0};
            case 'R': return new int[]{1, 0};
            default:  return new int[]{0, 0};
        }
    }

}
