/*
 * AI Tools Use Transparency Disclosure:
 * Primary prior GitHub handling credit: Bhawna Gogna.
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron.client.net;

/** Shared protocol constants. */
public final class Protocol {
	private Protocol() {}
	
	// Client to Server
	public static final String C_HELLO = "C_HELLO"; 				// C_HELLO|DesiredColor
	public static final String C_FIND_MATCH = "C_FIND_MATCH"; 		// C_FIND_MATCH
	public static final String C_TURN = "C_TURN";					// C_TURN|U/D/L/R
	public static final String C_REMATCH_VOTE = "C_REMATCH_VOTE";	// C_REMATCH_VOTE
	public static final String C_PING = "C_PING";					// C_PING|clientTimeMillis
	
	// Server to Client
	public static final String S_WELCOME = "S_WELCOME";				// S_WELCOME
	public static final String S_LOBBY_STATUS = "S_LOBBY_STATUS";
	public static final String S_MATCH_FOUND = "S_MATCH_FOUND";
	public static final String S_MATCH_START = "S_MATCH_START";
	public static final String S_SNAPSHOT = "S_SNAPSHOT";
	public static final String S_ROUND_END = "S_ROUND_END";
	public static final String S_MATCH_END = "S_MATCH_END";
	public static final String S_REMATCH_PROMPT = "S_REMATCH_PROMPT";
	public static final String S_REMATCH_STATUS = "S_REMATCH_STATUS";
	public static final String S_RETURN_TO_LOBBY = "S_RETURN_TO_LOBBY";
	public static final String S_PONG = "S_PONG";
	public static final String S_ERROR = "S_ERROR";
	
	// Shared gameplay constants for gameplay between client and server
	public static final int VIRTUAL_WIDTH = 480;
	public static final int VIRTUAL_HEIGHT = 800;
	public static final int CELL_SIZE_PX = 10;
	public static final int ARENA_COLS = (VIRTUAL_WIDTH / CELL_SIZE_PX); // 48
	public static final int ARENA_ROWS = (VIRTUAL_HEIGHT / CELL_SIZE_PX); // 80
	public static final int TICK_RATE = 60;
	public static final int MOVE_CELLS_PER_SEC = 12; // May need to be changed later
	public static final int TICKS_PER_MOVE = (TICK_RATE / MOVE_CELLS_PER_SEC); // 5
}
