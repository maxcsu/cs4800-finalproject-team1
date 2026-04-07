/*
 * AI Tools Use Transparency Disclosure:
 * Primary prior GitHub handling credit: Bhawna Gogna.
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron.client.net;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import edu.csu.javatron.JavaTronGame;
import edu.csu.javatron.LeaderboardEntry;
import com.badlogic.gdx.Gdx;

/** Client networking wrapper. */
public class NetworkClient {
    private static final long SERVER_TIMEOUT_MS = 12_000L;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;
    private JavaTronGame game;
    private volatile boolean connected = false;
    private volatile boolean intentionalDisconnect = false;
    private volatile boolean lossHandled = false;
    private volatile long lastServerHeardMillis = 0L;

    public NetworkClient(JavaTronGame game) {
        this.game = game;
    }

    public void connect(String host, int port) throws IOException {
        intentionalDisconnect = false;
        lossHandled = false;
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        connected = true;
        lastServerHeardMillis = System.currentTimeMillis();
        listenerThread = new Thread(this::listen);
        listenerThread.start();
    }

    public void disconnect() {
        disconnect(true);
    }

    public void disconnect(boolean intentional) {
        try {
            intentionalDisconnect = intentional;
            connected = false;
            if (socket != null)
                socket.close();
            if (listenerThread != null)
                listenerThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket = null;
            in = null;
            out = null;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void checkConnectionTimeout() {
        if (!connected || intentionalDisconnect || lossHandled) {
            return;
        }
        if (System.currentTimeMillis() - lastServerHeardMillis > SERVER_TIMEOUT_MS) {
            handleConnectionLost();
        }
    }

    public void send(String line) {
        if (out != null) {
            out.println(line);
        }
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                lastServerHeardMillis = System.currentTimeMillis();
                handleMessage(line);
            }
            if (!intentionalDisconnect) {
                handleConnectionLost();
            }
        } catch (IOException e) {
            if (!intentionalDisconnect) {
                System.out.println("Connection lost: " + e.getMessage());
                handleConnectionLost();
            }
        } finally {
            connected = false;
        }
    }

    private void handleMessage(String message) {
        System.out.println("Received: " + message);
        if (message.startsWith(Protocol.S_SNAPSHOT)) {
            parseSnapshot(message);
        } else if (message.startsWith(Protocol.S_WELCOME)) {
            String[] parts = message.split("\\|");
            for (String part : parts) {
                if (part.startsWith("motd=")) {
                    game.serverMotd = part.substring(5);
                }
            }
        } else if (message.startsWith(Protocol.S_MATCH_FOUND)) {
            String[] parts = message.split("\\|");
            for (String part : parts) {
                if (part.startsWith("yourColor=")) {
                    game.matchPlayerColor = part.substring(10);
                }
                if (part.startsWith("oppName="))
                    game.oppName = part.substring(8);
                else if (part.startsWith("oppColor="))
                    game.matchOppColor = part.substring(9);
                else if (part.startsWith("yourSide=")) {
                    game.isPlayerA = "A".equalsIgnoreCase(part.substring(9));
                }
            }
            boolean wasPracticeMode = game.practiceMode;
            game.practiceMode = false;
            Gdx.app.postRunnable(() -> {
                game.onMatchFoundNotice();
                if (wasPracticeMode && game.getScreen() instanceof edu.csu.javatron.GameScreen) {
                    game.showLobbyScreen();
                }
            });
        } else if (message.startsWith(Protocol.S_MATCH_START)) {
            // New match or rematch started by server - reset countdown state and transition
            // to game
            game.countdownMessage = null;
            game.countdownActive = false;
            game.roundResultText = null;
            game.finalMatchResult = false;
            game.latestRoundEventType = null;
            game.latestWinnerSide = null;
            game.practiceMode = false;
            Gdx.app.postRunnable(() -> game.showGameScreen());
        } else if (message.startsWith(Protocol.S_MATCH_END)) {
            // S_MATCH_END|box=1|BLUE 01 won the match. Final Score:
            // 2-0|winner=01|result=WIN|score=2-0
            String[] parts = message.split("\\|");
            if (parts.length >= 3) {
                game.winnerName = parts[2];
            }
            Gdx.app.postRunnable(() -> game.showRematchVoteScreen());
        } else if (message.startsWith(Protocol.S_REMATCH_STATUS)) {
            String[] parts = message.split("\\|");
            String result = "";
            for (String part : parts) {
                if (part.startsWith("result=")) {
                    result = part.substring(7);
                }
            }
            String finalResult = result;
            Gdx.app.postRunnable(() -> {
                if ("yes".equalsIgnoreCase(finalResult)) {
                    game.playNewGameSound();
                } else if ("no".equalsIgnoreCase(finalResult) || "disconnect".equalsIgnoreCase(finalResult)) {
                    game.playDisconnectSound();
                }
            });
        } else if (message.startsWith(Protocol.S_RETURN_TO_LOBBY)) {
            game.roundResultText = null;
            game.finalMatchResult = false;
            Gdx.app.postRunnable(() -> {
                game.clearMatchFoundNotice();
                game.matchPlayerColor = null;
                game.matchOppColor = null;
                game.showLobbyScreen();
            });
        } else if (message.startsWith(Protocol.S_LOBBY_STATUS)) {
            String[] parts = message.split("\\|");
            if (parts.length > 1 && parts[1].startsWith("players=")) {
                try {
                    game.lobbyPlayerCount = Integer.parseInt(parts[1].substring(8));
                } catch (NumberFormatException ignored) {
                }
            }
        } else if (message.startsWith(Protocol.S_PONG)) {
            lastServerHeardMillis = System.currentTimeMillis();
        } else if (message.startsWith(Protocol.S_LEADERBOARD)) {
            parseLeaderboard(message);
        } else if (message.startsWith(Protocol.S_ROUND_END)) {
            String[] parts = message.split("\\|");
            boolean collision = false;
            String eventType = null;
            String yourResult = "";
            String summary = null;
            boolean matchOver = false;
            String winnerSide = null;
            for (String part : parts) {
                if (part.startsWith("COUNTDOWN:")) {
                    game.countdownMessage = part.substring(10);
                    game.countdownActive = true;
                } else if (part.startsWith("event=")) {
                    eventType = part.substring(6);
                    collision = true;
                } else if (part.startsWith("yourResult=")) {
                    yourResult = part.substring(11);
                } else if (part.startsWith("winnerSide=")) {
                    winnerSide = part.substring(11);
                } else if (part.startsWith("summary=")) {
                    summary = part.substring(8);
                } else if (part.startsWith("matchOver=")) {
                    matchOver = "1".equals(part.substring(10));
                }
            }
            if (collision) {
                game.roundResultText = summary;
                game.latestRoundResult = yourResult;
                game.finalMatchResult = matchOver;
                game.latestRoundEventType = eventType;
                game.latestWinnerSide = winnerSide;
                game.latestRoundEventId++;
            }
        }
        // TODO: Handle other messages
    }

    private void parseLeaderboard(String message) {
        String[] parts = message.split("\\|");
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (String part : parts) {
            if (!part.startsWith("entry=")) {
                continue;
            }

            String[] entryParts = part.substring(6).split("~", -1);
            if (entryParts.length < 7) {
                continue;
            }

            try {
                entries.add(new LeaderboardEntry(
                        Integer.parseInt(entryParts[0]),
                        entryParts[1],
                        entryParts[2],
                        Integer.parseInt(entryParts[3]),
                        Integer.parseInt(entryParts[4]),
                        Double.parseDouble(entryParts[5]),
                        Integer.parseInt(entryParts[6])));
            } catch (NumberFormatException ignored) {
            }
        }
        game.setLeaderboardEntries(entries);
    }

    private void parseSnapshot(String message) {
        // S_SNAPSHOT|box=1|tick=1234|ax=..|ay=..|bx=..|by=..|adir=R|bdir=L|score=1-0|round=2
        String[] parts = message.split("\\|");
        int ax = 24, ay = 20, bx = 24, by = 60;
        char aDir = 'D', bDir = 'U';
        int aWins = 0, bWins = 0, roundNumber = 1;
        long tick = 0;
        for (String part : parts) {
            if (part.startsWith("tick="))
                tick = Long.parseLong(part.substring(5));
            else if (part.startsWith("ax="))
                ax = Integer.parseInt(part.substring(3));
            else if (part.startsWith("ay="))
                ay = Integer.parseInt(part.substring(3));
            else if (part.startsWith("bx="))
                bx = Integer.parseInt(part.substring(3));
            else if (part.startsWith("by="))
                by = Integer.parseInt(part.substring(3));
            else if (part.startsWith("adir="))
                aDir = part.charAt(5);
            else if (part.startsWith("bdir="))
                bDir = part.charAt(5);
            else if (part.startsWith("score=")) {
                String[] scores = part.substring(6).split("-");
                aWins = Integer.parseInt(scores[0]);
                bWins = Integer.parseInt(scores[1]);
            } else if (part.startsWith("round="))
                roundNumber = Integer.parseInt(part.substring(6));
        }
        if (tick > 0 && !"GO".equals(game.countdownMessage)) {
            // Any real game tick that isn't the GO message clears the countdown
            game.countdownMessage = null;
            game.countdownActive = false;
        }
        game.updateSnapshot(ax, ay, bx, by, aDir, bDir, aWins, bWins, roundNumber);
    }

    private void handleConnectionLost() {
        if (lossHandled || intentionalDisconnect) {
            return;
        }
        lossHandled = true;
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        } finally {
            socket = null;
            in = null;
            out = null;
        }
        Gdx.app.postRunnable(game::handleServerConnectionLost);
    }
}
