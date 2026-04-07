/*
 * AI Tools Use Transparency Disclosure:
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron.server.net;

import edu.csu.javatron.server.player.ColorResolver;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * One connected client. Owns its socket streams and a tiny amount of per-client state.
 */
public final class ClientSession {

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;

    private final Object outLock = new Object();

    private final int playerNumber;
    private volatile String requestedColor = "Green";
    private volatile String activeColor = null; // non-null only while in a match
    private volatile String playerName = "Player";
    private volatile String persistentPlayerId;
    private volatile boolean connected = true;

    // Heartbeat / liveness
    private volatile long lastHeardMillis = System.currentTimeMillis();

    // Latest requested direction input from the client (U/D/L/R). MatchRoom validates.
    private volatile char pendingTurn = '\0';

    public ClientSession(Socket socket, int playerNumber) throws IOException {
        this.socket = socket;
        this.playerNumber = playerNumber;

        // Safer defaults for a real-time-ish TCP session.
        this.socket.setTcpNoDelay(true);
        this.socket.setKeepAlive(true);

        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public String getIp() {
        return socket.getInetAddress().getHostAddress();
    }

    public boolean isConnected() {
        return connected && !socket.isClosed();
    }

    public long getLastHeardMillis() {
        return lastHeardMillis;
    }

    public void markHeard() {
        lastHeardMillis = System.currentTimeMillis();
    }

    public String getRequestedColor() {
        return requestedColor;
    }

    public void setRequestedColor(String requestedColor) {
        this.requestedColor = ColorResolver.normalizeColor(requestedColor);
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            this.playerName = playerName.trim();
        }
    }

    public String getPersistentPlayerId() {
        return persistentPlayerId;
    }

    public void setPersistentPlayerId(String persistentPlayerId) {
        if (persistentPlayerId == null || persistentPlayerId.isBlank()) {
            return;
        }
        this.persistentPlayerId = persistentPlayerId.trim();
    }

    public String getActiveColor() {
        return activeColor;
    }

    public void setActiveColor(String activeColor) {
        if (activeColor == null || activeColor.isBlank()) {
            this.activeColor = null;
        } else {
            this.activeColor = ColorResolver.normalizeColor(activeColor);
        }
    }

    public void clearActiveColor() {
        this.activeColor = null;
    }

    /** Color to display/use in match-related messages. */
    public String getDisplayColor() {
        return (activeColor != null && !activeColor.isBlank()) ? activeColor : requestedColor;
    }

    /** Accept a client direction input (U/D/L/R). */
    public void setPendingTurn(char dir) {
        char d = Character.toUpperCase(dir);
        if (d == 'U' || d == 'D' || d == 'L' || d == 'R') {
            pendingTurn = d;
        }
    }

    /** Returns the latest pending turn without clearing it. */
    public char peekPendingTurn() {
        return pendingTurn;
    }

    public void sendLine(String line) {
        if (!isConnected()) return;

        try {
            synchronized (outLock) {
                out.write(line);
                out.write("\n");
                out.flush();
            }
        } catch (IOException io) {
            // Treat send failure as a disconnect.
            connected = false;
            close();
        }
    }

    public String readLine() throws IOException {
        if (!isConnected()) return null;

        try {
            String line = in.readLine();
            if (line != null) {
                markHeard();
            }
            return line;
        } catch (IOException io) {
            connected = false;
            throw io;
        }
    }

    public void close() {
        connected = false;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
