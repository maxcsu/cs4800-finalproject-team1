/*
 * AI Assistance Disclosure:
 * Portions of this file were drafted with assistance from OpenAI ChatGPT.
 * All architecture, design, and final review were performed by Maxwell Nield.
 */

package edu.csu.javatron.server.lobby;

import edu.csu.javatron.server.ServerConfig;
import edu.csu.javatron.server.ServerMain;
import edu.csu.javatron.server.match.MatchRoom;
import edu.csu.javatron.server.net.ClientSession;
import edu.csu.javatron.server.net.Protocol;
import edu.csu.javatron.server.player.ColorResolver;
import edu.csu.javatron.server.StatsManager;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class LobbyManager {

    private final ServerConfig config;
    private final ServerConfig.Logger logger;
    private final ServerMain.ServerGui guiOrNull;

    private final AtomicInteger nextPlayerNumber = new AtomicInteger(1);

    private final Map<Integer, ClientSession> clientsByNumber = new ConcurrentHashMap<>();
    private final Queue<ClientSession> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Set<Integer> queuedPlayerNumbers = ConcurrentHashMap.newKeySet();

    // Box index -> MatchRoom
    private final MatchRoom[] activeMatchesByBox;

    // PlayerNumber -> BoxId (only when actively in a match)
    private final Map<Integer, Integer> playerToBox = new ConcurrentHashMap<>();
    
 // Stats tracking for win/loss ratio feature
    private final StatsManager statsManager = new StatsManager();
    public LobbyManager(ServerConfig config, ServerConfig.Logger logger, ServerMain.ServerGui guiOrNull) {
        this.config = config;
        this.logger = logger;
        this.guiOrNull = guiOrNull;
        this.activeMatchesByBox = new MatchRoom[config.maxMatches + 1]; // 1..maxMatches
    }

    public void shutdown() {
        for (ClientSession session : clientsByNumber.values()) {
            session.close();
        }
        for (int box = 1; box < activeMatchesByBox.length; box++) {
            MatchRoom room = activeMatchesByBox[box];
            if (room != null) room.shutdown();
        }
    }

    public void onClientConnected(Socket socket) {
        if (clientsByNumber.size() >= config.maxClients) {
            try { socket.close(); } catch (IOException ignored) {}
            logger.warn("[Lobby] Connection refused (server full).");
            return;
        }

        int playerNum = nextPlayerNumber.getAndIncrement();
        ClientSession session;
        try {
            session = new ClientSession(socket, playerNum);
        } catch (IOException io) {
            logger.error("Failed creating session.", io);
            return;
        }

        clientsByNumber.put(playerNum, session);

        // Default color until HELLO arrives.
        session.setRequestedColor("Green");

        logger.info(String.format("[Lobby] Player %s %02d (IP %s) connected.",
                session.getRequestedColor(), session.getPlayerNumber(), session.getIp()));

        // Minimal wire format for now: S_WELCOME|playerNumber|allowBotGames
        // allowBotGames == "client may offer practice while waiting"
        session.sendLine(Protocol.S_WELCOME + "|" + playerNum + "|" + config.allowBotGames);

        updateGuiPlayers();
        sendLobbyStatusToAll();

        startClientReadLoop(session);
    }

    private void startClientReadLoop(ClientSession session) {
        Thread t = new Thread(() -> runClientReadLoop(session), "ClientReader-" + session.getPlayerNumber());
        t.setDaemon(true);
        t.start();
    }

    private void runClientReadLoop(ClientSession session) {
        try {
            while (session.isConnected()) {
                String line = session.readLine();
                if (line == null) {
                    onClientDisconnected(session, "EOF");
                    return;
                }
                line = line.trim();
                if (line.isEmpty()) continue;

                handleClientLine(session, line);
            }
        } catch (IOException io) {
            onClientDisconnected(session, "IO: " + io.getClass().getSimpleName());
        } catch (RuntimeException ex) {
            logger.error("[Lobby] Client handler crashed for player " + session.getPlayerNumber(), ex);
            onClientDisconnected(session, "Handler crash");
        }
    }

    private void handleClientLine(ClientSession session, String line) {
        String[] parts = line.split("\\|", -1);
        String cmd = parts[0].trim();

        if (cmd.equalsIgnoreCase(Protocol.C_HELLO)) {
            // C_HELLO|DesiredColor
            if (parts.length >= 2) {
                session.setRequestedColor(parts[1]);
            }
            logger.info(String.format("[Lobby] Player %02d says HELLO. RequestedColor=%s",
                    session.getPlayerNumber(), session.getRequestedColor()));

            session.sendLine(Protocol.S_WELCOME + "|" + session.getPlayerNumber() + "|" + config.allowBotGames);
            sendLobbyStatusToAll();
            updateGuiPlayers();
            return;
        }

        if (cmd.equalsIgnoreCase(Protocol.C_PING)) {
            // C_PING|clientTimeMillis
            String clientMs = (parts.length >= 2) ? parts[1].trim() : "";
            long serverMs = System.currentTimeMillis();
            session.sendLine(Protocol.S_PONG + "|" + clientMs + "|" + serverMs);
            return;
        }

        if (cmd.equalsIgnoreCase(Protocol.C_FIND_MATCH)) {
            // IMPORTANT: Bot games are CLIENT-SIDE ONLY per your spec.
            // allow-botgames only controls whether the client UI may offer "Practice While Waiting".
            enqueueForHumanMatch(session);
            tryStartMatches();
            return;
        }

        // Gameplay input (only meaningful while in a match)
        if (cmd.equalsIgnoreCase(Protocol.C_TURN)) {
            // C_TURN|U/D/L/R
            if (parts.length >= 2 && parts[1] != null && !parts[1].isBlank()) {
                char d = parts[1].trim().toUpperCase().charAt(0);
                session.setPendingTurn(d);

                Integer box = playerToBox.get(session.getPlayerNumber());
                if (box != null) {
                    MatchRoom room = activeMatchesByBox[box];
                    if (room != null) {
                        room.onClientTurn(session, d);
                    }
                }
            }
            return;
        }

        if (cmd.equalsIgnoreCase(Protocol.C_REMATCH_VOTE)) {
            // C_REMATCH_VOTE|YES/NO  (legacy: C_REMATCH_VOTE means YES)
            boolean yes = true;
            if (parts.length >= 2) {
                String v = parts[1].trim().toLowerCase();
                yes = v.equals("yes") || v.equals("y") || v.equals("true") || v.equals("1");
            }
            Integer box = playerToBox.get(session.getPlayerNumber());
            if (box != null) {
                MatchRoom room = activeMatchesByBox[box];
                if (room != null) {
                    room.onClientRematchVote(session, yes);
                }
            }
            return;
        }
     // Handle stats request
        if (cmd.equalsIgnoreCase(Protocol.C_GET_STATS)) {
            int playerNum = session.getPlayerNumber();
            String stats = statsManager.getStats(playerNum);

            session.sendLine(Protocol.S_STATS + "|" + stats);
            return;
        }
        session.sendLine(Protocol.S_ERROR + "|UNKNOWN_COMMAND|" + cmd);
    }

    private void enqueueForHumanMatch(ClientSession session) {
        int num = session.getPlayerNumber();

        if (playerToBox.containsKey(num)) {
            session.sendLine(Protocol.S_ERROR + "|ALREADY_IN_MATCH");
            return;
        }

        if (queuedPlayerNumbers.add(num)) {
            waitingQueue.add(session);
            logger.info(String.format("[Lobby] Player %s %02d entered matchmaking queue.",
                    session.getRequestedColor(), session.getPlayerNumber()));
            sendLobbyStatusToAll();
            updateGuiPlayers();
        } else {
            session.sendLine(Protocol.S_LOBBY_STATUS + "|ALREADY_QUEUED");
        }
    }

    private void updateGuiPlayers() {
        if (guiOrNull == null) return;

        List<String> lines = new ArrayList<>();
        List<Integer> nums = new ArrayList<>(clientsByNumber.keySet());
        nums.sort(Integer::compareTo);

        for (Integer num : nums) {
            ClientSession s = clientsByNumber.get(num);
            if (s == null) continue;

            String status;
            if (playerToBox.containsKey(num)) status = "IN_MATCH";
            else if (queuedPlayerNumbers.contains(num)) status = "QUEUED";
            else status = "LOBBY";

            String colorShown = playerToBox.containsKey(num) ? s.getDisplayColor() : s.getRequestedColor();
            lines.add(String.format("%s %02d  %s  %s", colorShown, s.getPlayerNumber(), s.getIp(), status));
        }

        guiOrNull.setPlayers(lines);
    }

    private void sendLobbyStatusToAll() {
        int players = clientsByNumber.size();
        int queued = queuedPlayerNumbers.size();
        int matches = 0;
        for (int i = 1; i < activeMatchesByBox.length; i++) {
            if (activeMatchesByBox[i] != null) matches++;
        }

        String msg = Protocol.S_LOBBY_STATUS + "|players=" + players
                + "|queued=" + queued
                + "|matches=" + matches
                + "|allowBotGames=" + config.allowBotGames;

        for (ClientSession s : clientsByNumber.values()) {
            s.sendLine(msg);
        }
    }

    private void tryStartMatches() {
        while (true) {
            int freeBox = findFreeBox();
            if (freeBox == -1) return;

            ClientSession p1 = pollConnectedQueued();
            if (p1 == null) return;

            ClientSession p2 = pollConnectedQueued();
            if (p2 == null) {
                // Put p1 back at the end of the queue and stop.
                requeue(p1);
                logger.info(String.format("[Lobby] No opponent available for player %s %02d. Waiting in queue.",
                        p1.getRequestedColor(), p1.getPlayerNumber()));
                sendLobbyStatusToAll();
                updateGuiPlayers();
                return;
            }

            startMatchPvp(freeBox, p1, p2);
        }
    }

    private ClientSession pollConnectedQueued() {
        while (true) {
            ClientSession s = waitingQueue.poll();
            if (s == null) return null;

            if (!s.isConnected() || !clientsByNumber.containsKey(s.getPlayerNumber())) {
                queuedPlayerNumbers.remove(s.getPlayerNumber());
                continue;
            }

            queuedPlayerNumbers.remove(s.getPlayerNumber());
            return s;
        }
    }

    private void requeue(ClientSession session) {
        if (session == null) return;
        int num = session.getPlayerNumber();
        if (!session.isConnected() || !clientsByNumber.containsKey(num)) return;

        if (queuedPlayerNumbers.add(num)) {
            waitingQueue.add(session);
        }
    }

    private int findFreeBox() {
        for (int i = 1; i < activeMatchesByBox.length; i++) {
            if (activeMatchesByBox[i] == null) return i;
        }
        return -1;
    }

    private void startMatchPvp(int box, ClientSession a, ClientSession b) {
        Set<String> used = new HashSet<>();
        String colorA = ColorResolver.resolveColor(a.getRequestedColor(), config.enforceUniqueColors, used);
        used.add(colorA);
        String colorB = ColorResolver.resolveColor(b.getRequestedColor(), config.enforceUniqueColors, used);

        a.setActiveColor(colorA);
        b.setActiveColor(colorB);

        // Log temporary in-match color reassignment when enforcing unique colors.
        if (config.enforceUniqueColors) {
            String reqA = a.getRequestedColor();
            if (reqA != null && !reqA.isBlank() && !colorA.equalsIgnoreCase(reqA)) {
                logger.info(String.format("[Box %d] Color reassignment: player %02d requested %s but is temporarily assigned %s for this match.",
                        box, a.getPlayerNumber(), reqA, colorA));
            }
            String reqB = b.getRequestedColor();
            if (reqB != null && !reqB.isBlank() && !colorB.equalsIgnoreCase(reqB)) {
                logger.info(String.format("[Box %d] Color reassignment: player %02d requested %s but is temporarily assigned %s for this match.",
                        box, b.getPlayerNumber(), reqB, colorB));
            }
        }

        playerToBox.put(a.getPlayerNumber(), box);
        playerToBox.put(b.getPlayerNumber(), box);

        MatchRoom room = new MatchRoom(config, logger, box, a, b, null, this::onMatchEnded);
        activeMatchesByBox[box] = room;

        logger.info(String.format("[Box %d] Player %s %02d assigned to Box %d, versus player %s %02d.",
                box, a.getDisplayColor(), a.getPlayerNumber(), box, b.getDisplayColor(), b.getPlayerNumber()));
        
        // Client should interrupt practice mode on receipt.
        a.sendLine(Protocol.S_MATCH_FOUND + "|box=" + box + "|opponent=" + b.getPlayerNumber()
        	+ "|yourColor=" + a.getDisplayColor() + "|oppColor=" + b.getDisplayColor());

        b.sendLine(Protocol.S_MATCH_FOUND + "|box=" + box + "|opponent=" + a.getPlayerNumber()
        	+ "|yourColor=" + b.getDisplayColor() + "|oppColor=" + a.getDisplayColor());
        
        sendLobbyStatusToAll();
        updateGuiPlayers();

        room.start();
    }

    private void onMatchEnded(int box, MatchRoom.MatchResult result) {
        activeMatchesByBox[box] = null;

        if (result.playerA != null) playerToBox.remove(result.playerA.getPlayerNumber());
        if (result.playerB != null) playerToBox.remove(result.playerB.getPlayerNumber());

        logger.info(String.format("[Box %d] Match ended. %s", box, result.summaryLine));
     // Update win/loss stats when a full match concludes
        if (result.summaryLine != null && result.summaryLine.contains("won the match")) {

            int playerANum = result.playerA.getPlayerNumber();
            int playerBNum = result.playerB.getPlayerNumber();

            // If summary contains Player A's number, A won
            if (result.summaryLine.contains(String.format("%02d", playerANum))) {
                statsManager.recordWin(playerANum);
                statsManager.recordLoss(playerBNum);
            } else {
                statsManager.recordWin(playerBNum);
                statsManager.recordLoss(playerANum);
            }
        }

        // IMPORTANT: Do NOT auto-requeue.
        // MatchRoom is responsible for emitting S_MATCH_END (and S_REMATCH_PROMPT, if applicable).

        if (result.playerA != null) result.playerA.clearActiveColor();
        if (result.playerB != null) result.playerB.clearActiveColor();

        sendLobbyStatusToAll();
        updateGuiPlayers();
    }

    private void onClientDisconnected(ClientSession session, String reason) {
        int num = session.getPlayerNumber();

        clientsByNumber.remove(num);

        queuedPlayerNumbers.remove(num);
        rebuildWaitingQueueWithout(num);

        Integer box = playerToBox.remove(num);
        if (box != null) {
            MatchRoom room = activeMatchesByBox[box];
            activeMatchesByBox[box] = null;

            if (room != null) {
                room.shutdown();
            }

            // Remove anyone else still mapped to this box (return them to lobby)
            List<Integer> alsoInBox = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : playerToBox.entrySet()) {
                if (e.getValue() != null && e.getValue() == box) {
                    alsoInBox.add(e.getKey());
                }
            }
            for (Integer n : alsoInBox) {
                playerToBox.remove(n);
            }

            for (Integer n : alsoInBox) {
                ClientSession other = clientsByNumber.get(n);
                if (other != null && other.isConnected()) {
                    other.sendLine(Protocol.S_MATCH_END + "|Opponent disconnected");
                }
            }
        }

        session.close();

        logger.info(String.format("[Lobby] Player %02d disconnected (%s).", num, reason));
        updateGuiPlayers();
        sendLobbyStatusToAll();
    }

    private void rebuildWaitingQueueWithout(int playerNumber) {
        if (waitingQueue.isEmpty()) return;

        Queue<ClientSession> rebuilt = new ConcurrentLinkedQueue<>();
        ClientSession s;
        while ((s = waitingQueue.poll()) != null) {
            if (s.getPlayerNumber() == playerNumber) continue;
            rebuilt.add(s);
        }

        waitingQueue.addAll(rebuilt);
    }
}