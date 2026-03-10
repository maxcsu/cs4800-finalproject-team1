/*
 * AI Assistance Disclosure:
 * Portions of this file were drafted with assistance from OpenAI ChatGPT.
 * All architecture, design, and final review were performed by Maxwell Nield.
 */

package edu.csu.javatron.server.net;

import edu.csu.javatron.server.match.MatchRoom;

/**
 * Encodes match snapshots into a compact line protocol.
 *
 * This is intentionally simple (human-readable) for debugging during the project.
 */
public final class SnapshotSerializer {
    private SnapshotSerializer() {}

    public static String toSnapshotLine(MatchRoom.Snapshot s) {
        // S_SNAPSHOT|box=1|tick=1234|ax=..|ay=..|bx=..|by=..|adir=R|bdir=L|score=1-0|round=2
        return Protocol.S_SNAPSHOT
                + "|box=" + s.boxId
                + "|tick=" + s.tick
                + "|ax=" + s.ax + "|ay=" + s.ay
                + "|bx=" + s.bx + "|by=" + s.by
                + "|adir=" + s.aDir
                + "|bdir=" + s.bDir
                + "|score=" + s.aWins + "-" + s.bWins
                + "|round=" + s.roundNumber;
    }
}
