/*
 * AI Assistance Disclosure:
 * Portions of this file were drafted with assistance from OpenAI ChatGPT.
 * All architecture, design, and final review were performed by Maxwell Nield.
 */

package edu.csu.javatron.server;

import edu.csu.javatron.server.lobby.LobbyManager;
import edu.csu.javatron.server.net.ServerTransport;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ServerMain {

    private static final SimpleDateFormat TS = new SimpleDateFormat("M-d-yyyy | HH:mm:ss");
    private static final SimpleDateFormat LOG_TS = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static void main(String[] args) {
        boolean noGui = false;
        for (String arg : args) {
            if (arg == null) continue;
            String trimmedArg = arg.trim();
            if (trimmedArg.equalsIgnoreCase("--nogui") || trimmedArg.equalsIgnoreCase("-nogui")) {
                noGui = true;
            }
        }

        System.out.println("[Server] JavaTron server starting...");

        AtomicBoolean running = new AtomicBoolean(true);

        ServerGui gui = null;
        ServerConfig.Logger baseLogger;

        if (!noGui) {
            gui = new ServerGui();
            ServerGui finalGui = gui;
            baseLogger = new ServerConfig.Logger() {
                @Override public void info(String message) {
                    String line = formatLine(message);
                    finalGui.appendLog(line);
                    System.out.println(line);
                }
                @Override public void warn(String message) {
                    String line = formatLine("[WARN] " + message);
                    finalGui.appendLog(line);
                    System.out.println(line);
                }
                @Override public void error(String message, Throwable error) {
                    String line = formatLine("[ERROR] " + message);
                    finalGui.appendLog(line);
                    System.err.println(line);

                    if (error != null) {
                        String errLine = formatLine("        " + error.toString());
                        finalGui.appendLog(errLine);
                        error.printStackTrace(System.err);
                    }
                }
            };
        } else {
            baseLogger = new ServerConfig.Logger() {
                @Override public void info(String message) { System.out.println(formatLine(message)); }
                @Override public void warn(String message) { System.out.println(formatLine("[WARN] " + message)); }
                @Override public void error(String message, Throwable error) {
                    System.err.println(formatLine("[ERROR] " + message));
                    if (error != null) error.printStackTrace(System.err);
                }
            };
        }

        ServerTransport transport = null;
        LobbyManager lobbyManager = null;
        StatsManager statsManager = null;

        LogFileSink logFileSink = null;
        DiscordWebhookSink webhookSink = null;

        try {
            Path configPath = Path.of("server.cfg");
            ServerConfig config = ServerConfig.loadOrCreate(configPath, baseLogger);

            // Create sinks ONCE into final locals so the logger can capture them.
            final LogFileSink finalLogFileSink =
                    (config.logToFile) ? LogFileSink.create(config.logOneFile, baseLogger) : null;

            final DiscordWebhookSink finalWebhookSink =
                    (config.logToWebhook) ? DiscordWebhookSink.create(config.logWebhookUrl, baseLogger) : null;

            // Store sinks for shutdown paths outside this try block.
            logFileSink = finalLogFileSink;
            webhookSink = finalWebhookSink;

            // Wrap base logger so every line mirrors to file/webhook if enabled.
            final ServerConfig.Logger logger = new ServerConfig.Logger() {
                @Override public void info(String message) {
                    baseLogger.info(message);
                    if (finalLogFileSink != null) finalLogFileSink.write(formatLine(message));
                    if (finalWebhookSink != null) finalWebhookSink.enqueue(formatLine(message));
                }
                @Override public void warn(String message) {
                    String text = "[WARN] " + message;
                    baseLogger.warn(message);
                    if (finalLogFileSink != null) finalLogFileSink.write(formatLine(text));
                    if (finalWebhookSink != null) finalWebhookSink.enqueue(formatLine(text));
                }
                @Override public void error(String message, Throwable error) {
                    String text = "[ERROR] " + message;
                    baseLogger.error(message, error);
                    if (finalLogFileSink != null) finalLogFileSink.write(formatLine(text));
                    if (finalWebhookSink != null) finalWebhookSink.enqueue(formatLine(text));
                    if (error != null) {
                        String errText = "        " + error.toString();
                        if (finalLogFileSink != null) finalLogFileSink.write(formatLine(errText));
                        if (finalWebhookSink != null) finalWebhookSink.enqueue(formatLine(errText));
                    }
                }
            };
            
         // Webhook status visibility + one-time test line
            if (config.logToWebhook) {
                if (finalWebhookSink == null) {
                    logger.warn("Webhook logging requested but DISABLED (invalid/empty URL after config parsing).");
                    logger.warn("Parsed logwebhookurl (masked): " + DiscordWebhookSink.maskWebhookUrl(config.logWebhookUrl));
                } else {
                    logger.info("Webhook logging enabled. Parsed logwebhookurl (masked): "
                            + DiscordWebhookSink.maskWebhookUrl(config.logWebhookUrl));
                    finalWebhookSink.enqueue(formatLine("JavaTron server started (webhook test line)."));
                }
            }

            statsManager = new StatsManager(logger);
            lobbyManager = new LobbyManager(config, logger, gui, statsManager);
            transport = new ServerTransport(config, logger, lobbyManager);

            ServerTransport finalTransport = transport;
            LobbyManager finalLobby = lobbyManager;

            if (gui != null) {
                ServerGui finalGui = gui;
                StatsManager finalStatsManager = statsManager;
                finalGui.setOnClose(() -> {
                    running.set(false);
                    logger.info("Shutdown requested (GUI close).");
                    finalTransport.shutdown();
                    finalLobby.shutdown();
                    if (finalStatsManager != null) {
                        try { finalStatsManager.close(); } catch (Exception ignored) {}
                    }
                    if (finalWebhookSink != null) finalWebhookSink.shutdown();
                    if (finalLogFileSink != null) finalLogFileSink.close();
                });
            }

            logger.info("Listening on " + config.serverIp + ":" + config.serverPort + " (TCP)");
            logger.info("MOTD: " + config.serverMotd);
            logger.info("max-clients=" + config.maxClients + ", max-matches=" + config.maxMatches);
            logger.info("logtofile=" + config.logToFile + ", logonefile=" + config.logOneFile
                    + ", logtowebhook=" + config.logToWebhook);

            transport.start(config.serverPort);
            logger.info("[Lobby] Server socket opened.");

        } catch (Exception ex) {
            System.err.println("[Server] Fatal startup error: " + ex);
            ex.printStackTrace(System.err);
            baseLogger.error("Fatal startup error. Server did not start.", ex);
            running.set(false);
        }

        while (running.get()) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        try {
            if (transport != null) transport.shutdown();
        } catch (Exception ignored) {}

        try {
            if (lobbyManager != null) lobbyManager.shutdown();
        } catch (Exception ignored) {}

        try {
            if (statsManager != null) statsManager.close();
        } catch (Exception ignored) {}

        try {
            if (webhookSink != null) webhookSink.shutdown();
        } catch (Exception ignored) {}

        try {
            if (logFileSink != null) logFileSink.close();
        } catch (Exception ignored) {}

        baseLogger.info("Server stopped.");
    }

    private static String formatLine(String msg) {
        return "[" + TS.format(new Date()) + "] " + msg;
    }

    // ------------------------------------------------------------
    // File logging sink
    // ------------------------------------------------------------
    private static final class LogFileSink {
        private final BufferedWriter writer;
        private final Object lock = new Object();

        private LogFileSink(BufferedWriter writer) {
            this.writer = writer;
        }

        public static LogFileSink create(boolean oneFile, ServerConfig.Logger logger) {
            try {
                if (oneFile) {
                    Path filePath = Path.of("javatron.log");
                    BufferedWriter w = Files.newBufferedWriter(
                            filePath,
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    );
                    if (logger != null) logger.info("File logging enabled: " + filePath.toAbsolutePath());
                    return new LogFileSink(w);
                } else {
                    Path logsDir = Path.of("logs");
                    Files.createDirectories(logsDir);
                    String stamp = LOG_TS.format(new Date());
                    Path filePath = logsDir.resolve("javatron_" + stamp + ".log");
                    BufferedWriter w = Files.newBufferedWriter(
                            filePath,
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE
                    );
                    if (logger != null) logger.info("File logging enabled: " + filePath.toAbsolutePath());
                    return new LogFileSink(w);
                }
            } catch (IOException ex) {
                if (logger != null) logger.error("Failed to open log file; continuing without file logging.", ex);
                return null;
            }
        }

        public void write(String line) {
            synchronized (lock) {
                try {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                } catch (IOException ignored) {
                    // Don't crash the server if log writing fails.
                }
            }
        }

        public void close() {
            synchronized (lock) {
                try { writer.flush(); } catch (IOException ignored) {}
                try { writer.close(); } catch (IOException ignored) {}
            }
        }
    }

    // ------------------------------------------------------------
    // Discord webhook sink (queued sender)
    // ------------------------------------------------------------
    private static final class DiscordWebhookSink {
        private final String url;
        private final ServerConfig.Logger logger;

        private final Queue<String> queue = new ArrayDeque<>();
        private final Object lock = new Object();
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final Thread worker;

        // Rate-limit internal error logs
        private volatile long lastErrorLogMillis = 0L;

        private DiscordWebhookSink(String url, ServerConfig.Logger logger) {
            this.url = url;
            this.logger = logger;
            this.worker = new Thread(this::runLoop, "WebhookLogger");
            this.worker.setDaemon(true);
            this.worker.start();
        }

        public static DiscordWebhookSink create(String url, ServerConfig.Logger logger) {
            if (url == null) url = "";
            String trimmed = url.trim();

            if (trimmed.isEmpty()) {
                if (logger != null) logger.warn("logtowebhook=true but logwebhookurl is empty; webhook logging disabled.");
                return null;
            }

            if (!looksLikeDiscordWebhook(trimmed)) {
                if (logger != null) logger.warn("Webhook URL does not look like a Discord webhook. Webhook logging disabled.");
                return null;
            }

            if (logger != null) logger.info("Webhook logging enabled.");
            return new DiscordWebhookSink(trimmed, logger);
        }

        private static boolean looksLikeDiscordWebhook(String u) {
            // Accept:
            // https://discord.com/api/webhooks/<id>/<token>
            // https://discord.com/api/v10/webhooks/<id>/<token>
            // plus ptb./canary. and discordapp.com
            return u.matches("(?i)^https?://(ptb\\.|canary\\.)?discord(app)?\\.com/api/(v\\d+/)?webhooks/\\d+/.+");
        }
        
        public static String maskWebhookUrl(String url) {
            if (url == null) return "(null)";
            String u = url.trim();

            int lastSlash = u.lastIndexOf('/');
            if (lastSlash <= 0) return u;

            return u.substring(0, lastSlash + 1) + "****";
        }

        public void enqueue(String line) {
            if (!running.get()) return;
            synchronized (lock) {
                if (queue.size() > 500) queue.poll();
                queue.add(line);
                lock.notifyAll();
            }
        }

        private void runLoop() {
            while (running.get()) {
                String next = null;
                synchronized (lock) {
                    if (queue.isEmpty()) {
                        try {
                            lock.wait(500);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    next = queue.poll();
                }

                if (next == null) continue;

                String content = next;
                if (content.length() > 1900) content = content.substring(0, 1900) + "...";

                try {
                    postDiscordWebhook(url, content);
                    Thread.sleep(250);
                } catch (Exception ex) {
                    logWebhookErrorRateLimited(ex);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        private void logWebhookErrorRateLimited(Exception ex) {
            long now = System.currentTimeMillis();
            if (now - lastErrorLogMillis < 10_000L) return;
            lastErrorLogMillis = now;

            String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            if (logger != null) logger.warn("Discord webhook logging failed: " + msg);
            else System.err.println("[Server] Discord webhook logging failed: " + msg);
        }

        public void shutdown() {
            running.set(false);
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        private static void postDiscordWebhook(String webhookUrl, String content) throws IOException {
            String trimmed = (content == null) ? "" : content.trim();
            if (trimmed.isEmpty()) {
                throw new IOException("Discord webhook refused: content empty/whitespace");
            }

            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(7000);
            conn.setDoOutput(true);

            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "JavaTronServer/1.0");

            String json = "{\"content\":\"" + escapeJson(trimmed) + "\"}";
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);

            conn.setFixedLengthStreamingMode(payload.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
                os.flush();
            }

            int code = conn.getResponseCode();

            String responseBody = "";
            try (java.io.InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
                if (is != null) {
                    byte[] buf = is.readAllBytes();
                    responseBody = new String(buf, StandardCharsets.UTF_8);
                    if (responseBody.length() > 800) responseBody = responseBody.substring(0, 800) + "...";
                }
            } catch (Exception ignored) {}

            conn.disconnect();

            if (code < 200 || code >= 300) {
                throw new IOException("Discord webhook HTTP " + code + (responseBody.isEmpty() ? "" : (" | " + responseBody)));
            }
        }

        private static String escapeJson(String s) {
            StringBuilder sb = new StringBuilder(s.length() + 32);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '\\': sb.append("\\\\"); break;
                    case '"':  sb.append("\\\""); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    case '\b': sb.append("\\b"); break;
                    case '\f': sb.append("\\f"); break;
                    default:
                        // Escape other control chars (0x00 - 0x1F) as \XXXX (NOTE: no "" in comments!)
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                        break;
                }
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------------
    // GUI
    // ------------------------------------------------------------
    public static final class ServerGui {
        private final JFrame frame;
        private final DefaultListModel<String> playerListModel;
        private final JTextArea logArea;
        private Runnable onClose = null;

        public ServerGui() {
            playerListModel = new DefaultListModel<>();
            logArea = new JTextArea();
            logArea.setEditable(false);
            logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            JList<String> playerList = new JList<>(playerListModel);
            JScrollPane leftScroll = new JScrollPane(playerList);
            JScrollPane rightScroll = new JScrollPane(logArea);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
            split.setDividerLocation(260);

            frame = new JFrame("JavaTron Server");
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent e) {
                    if (onClose != null) onClose.run();
                    frame.dispose();
                }
            });

            frame.getContentPane().add(split);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);

            SwingUtilities.invokeLater(() -> frame.setVisible(true));
        }

        public void setOnClose(Runnable onClose) {
            this.onClose = onClose;
        }

        public void appendLog(String line) {
            SwingUtilities.invokeLater(() -> {
                logArea.append(line);
                logArea.append("\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }

        public void setPlayers(java.util.List<String> lines) {
            SwingUtilities.invokeLater(() -> {
                playerListModel.clear();
                for (String line : lines) playerListModel.addElement(line);
            });
        }
    }
}
