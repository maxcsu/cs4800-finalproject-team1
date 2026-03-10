package edu.csu.javatron.client.net;

import java.io.*;
import java.net.Socket;
import edu.csu.javatron.JavaTronGame;
import com.badlogic.gdx.Gdx;

/** Client networking wrapper. */
public class NetworkClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;
    private JavaTronGame game;

    public NetworkClient(JavaTronGame game) {
        this.game = game;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        listenerThread = new Thread(this::listen);
        listenerThread.start();
    }

    public void disconnect() {
        try {
            if (socket != null)
                socket.close();
            if (listenerThread != null)
                listenerThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
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
                handleMessage(line);
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + e.getMessage());
        }
    }

    private void handleMessage(String message) {
        System.out.println("Received: " + message);
        if (message.startsWith(Protocol.S_SNAPSHOT)) {
            parseSnapshot(message);
        } else if (message.startsWith(Protocol.S_MATCH_FOUND)) {
            String[] parts = message.split("\\|");
            for (String part : parts) {
                if (part.startsWith("oppName="))
                    game.oppName = part.substring(8);
                else if (part.startsWith("oppColor="))
                    game.oppColor = part.substring(9);
                else if (part.startsWith("yourSide=")) {
                    game.isPlayerA = "A".equalsIgnoreCase(part.substring(9));
                }
            }
            Gdx.app.postRunnable(() -> game.showGameScreen());
        } else if (message.startsWith(Protocol.S_MATCH_START)) {
            // New match or rematch started by server - reset countdown state and transition
            // to game
            game.countdownMessage = null;
            game.countdownActive = false;
            Gdx.app.postRunnable(() -> game.showGameScreen());
        } else if (message.startsWith(Protocol.S_MATCH_END)) {
            // S_MATCH_END|box=1|BLUE 01 won the match. Final Score:
            // 2-0|winner=01|result=WIN|score=2-0
            String[] parts = message.split("\\|");
            if (parts.length >= 3) {
                game.winnerName = parts[2];
            }
            Gdx.app.postRunnable(() -> game.showRematchVoteScreen());
        } else if (message.startsWith(Protocol.S_LOBBY_STATUS)) {
            String[] parts = message.split("\\|");
            if (parts.length > 1 && parts[1].startsWith("players=")) {
                try {
                    game.lobbyPlayerCount = Integer.parseInt(parts[1].substring(8));
                } catch (NumberFormatException ignored) {
                }
            }
        } else if (message.startsWith(Protocol.S_ROUND_END)) {
            String[] parts = message.split("\\|");
            for (String part : parts) {
                if (part.startsWith("COUNTDOWN:")) {
                    game.countdownMessage = part.substring(10);
                    game.countdownActive = true;
                }
            }
        }
        // TODO: Handle other messages
    }

    private void parseSnapshot(String message) {
        // S_SNAPSHOT|box=1|tick=1234|ax=..|ay=..|bx=..|by=..|adir=R|bdir=L|score=1-0|round=2
        String[] parts = message.split("\\|");
        int ax = 5, ay = 5, bx = 43, by = 75;
        char aDir = 'R', bDir = 'L';
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
}
