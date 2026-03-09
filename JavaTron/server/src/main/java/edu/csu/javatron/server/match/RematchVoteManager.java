/*
 * AI Assistance Disclosure:
 * Portions of this file were drafted with assistance from OpenAI ChatGPT.
 * All architecture, design, and final review were performed by Maxwell Nield.
 */

package edu.csu.javatron.server.match;

import edu.csu.javatron.server.net.ClientSession;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks rematch votes for two players.
 *
 * Vote window is started by the MatchRoom when the match ends.
 */
public final class RematchVoteManager {

    private final ClientSession a;
    private final ClientSession b;

    private final Map<Integer, Boolean> votesByPlayer = new HashMap<>();
    private long voteStartMillis = 0L;
    private long voteTimeoutMillis = 0L;
    private boolean active = false;

    public RematchVoteManager(ClientSession a, ClientSession b) {
        this.a = a;
        this.b = b;
    }

    public void start(long nowMillis, long timeoutMillis) {
        votesByPlayer.clear();
        voteStartMillis = nowMillis;
        voteTimeoutMillis = timeoutMillis;
        active = true;
    }

    public boolean isActive() {
        return active;
    }

    public long getTimeLeftMillis(long nowMillis) {
        if (!active) return 0L;
        long elapsed = nowMillis - voteStartMillis;
        return Math.max(0L, voteTimeoutMillis - elapsed);
    }

    public void vote(ClientSession who, boolean yes) {
        if (!active || who == null) return;
        votesByPlayer.put(who.getPlayerNumber(), yes);
    }

    public boolean bothVotedYes() {
        if (!active) return false;
        Boolean va = votesByPlayer.get(a.getPlayerNumber());
        Boolean vb = votesByPlayer.get(b.getPlayerNumber());
        return Boolean.TRUE.equals(va) && Boolean.TRUE.equals(vb);
    }

    public boolean anyVotedNo() {
        if (!active) return false;
        for (Boolean v : votesByPlayer.values()) {
            if (Boolean.FALSE.equals(v)) return true;
        }
        return false;
    }

    public boolean timedOut(long nowMillis) {
        return active && getTimeLeftMillis(nowMillis) <= 0L;
    }

    public void end() {
        active = false;
    }
}
