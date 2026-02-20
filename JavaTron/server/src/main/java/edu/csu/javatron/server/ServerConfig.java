/*
 * AI Assistance Disclosure:
 * Portions of this file were drafted with assistance from OpenAI ChatGPT.
 * All architecture, design, and final review were performed by Maxwell Nield.
 */

package edu.csu.javatron.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads/creates and provides access to server.cfg.
 *
 * Format:
 * key=value
 * // comments allowed (whole-line or trailing after value)
 */
public final class ServerConfig {

    public interface Logger {
        void info(String message);
        void warn(String message);
        void error(String message, Throwable error);
    }

    public String serverIp = "0.0.0.0";
    public int serverPort = 7777;
    public String serverMotd = "JavaTron Server";
    public int maxClients = 20;
    public int maxMatches = 10;
    public boolean allowBotGames = true;
    public int trailTimeSeconds = -1; // -1 permanent
    public boolean enforceUniqueColors = false;

    // Optional safety valves
    public int maxConsecutiveDraws = -1; // -1 disables
    public int idleKickSeconds = -1;     // -1 disables

    // New logging-related config
    public boolean logToFile = true;
    public boolean logOneFile = false;
    public boolean logToWebhook = false;
    public String logWebhookUrl = "";

    public static ServerConfig loadOrCreate(Path configPath, Logger logger) {
        ServerConfig config = new ServerConfig();

        try {
            if (!Files.exists(configPath)) {
                Path parentDir = configPath.getParent();
                if (parentDir != null) {
                    Files.createDirectories(parentDir);
                }

                writeDefault(configPath, config);
                safeInfo(logger, "Created default server.cfg at " + configPath.toAbsolutePath());
                return config;
            }

            Map<String, String> map = parseKeyValueFile(configPath);

            // Load with defaults
            config.serverIp = map.getOrDefault("server-ip", config.serverIp);
            config.serverPort = parseInt(map.get("server-port"), config.serverPort);
            config.serverMotd = map.getOrDefault("server-motd", config.serverMotd);

            // Spec requires 2+ users.
            config.maxClients = clamp(parseInt(map.get("max-clients"), config.maxClients), 2, 20);
            config.maxMatches = clamp(parseInt(map.get("max-matches"), config.maxMatches), 1, 10);
            
            config.allowBotGames = parseBool(map.get("allow-botgames"), config.allowBotGames);
            config.trailTimeSeconds = parseInt(map.get("trail-time"), config.trailTimeSeconds);
            config.enforceUniqueColors = parseBool(map.get("enforce-uniquecolors"), config.enforceUniqueColors);

            config.maxConsecutiveDraws = parseInt(map.get("max-consecutive-draws"), config.maxConsecutiveDraws);
            config.idleKickSeconds = parseInt(map.get("idle-kick-seconds"), config.idleKickSeconds);

            // New logging fields
            config.logToFile = parseBool(map.get("logtofile"), config.logToFile);
            config.logOneFile = parseBool(map.get("logonefile"), config.logOneFile);
            config.logToWebhook = parseBool(map.get("logtowebhook"), config.logToWebhook);
            config.logWebhookUrl = map.getOrDefault("logwebhookurl", config.logWebhookUrl);

            // Append any missing keys to config file (required by your request)
            appendMissingKeys(configPath, map, config);

            safeInfo(logger, "Loaded server.cfg from " + configPath.toAbsolutePath());
            return config;

        } catch (IOException io) {
            safeError(logger, "Failed reading server.cfg. Using defaults.", io);
            return config;

        } catch (RuntimeException ex) {
            safeError(logger, "Config error while reading/creating server.cfg. Using defaults.", ex);
            return config;
        }
    }

    private static void writeDefault(Path configPath, ServerConfig cfg) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                configPath,
                StandardCharsets.UTF_8,
                new OpenOption[]{} // avoids varargs null warnings
        )) {
            writer.write("server-ip=" + cfg.serverIp + "\n");
            writer.write("server-port=" + cfg.serverPort + " // TCP Port\n");
            writer.write("server-motd=" + cfg.serverMotd + " // Displayed to players while in lobby.\n");
            writer.write("max-clients=" + cfg.maxClients + " // Must be 2+ per spec.\n");
            writer.write("max-matches=" + cfg.maxMatches + "\n");
            writer.write("allow-botgames=" + cfg.allowBotGames + " // Allow bot matches while in lobby.\n");
            writer.write("trail-time=" + cfg.trailTimeSeconds + " // -1 = Permanent trails.\n");
            writer.write("enforce-uniquecolors=" + cfg.enforceUniqueColors + "\n");
            writer.write("\n");
            writer.write("// Logging\n");
            writer.write("logtofile=" + cfg.logToFile + " // Write server logs to a file.\n");
            writer.write("logonefile=" + cfg.logOneFile + " // If true: use javatron.log (wiped each start). If false: timestamped log in logs/.\n");
            writer.write("logtowebhook=" + cfg.logToWebhook + " // If true: also send logs to Discord webhook.\n");
            writer.write("logwebhookurl=" + cfg.logWebhookUrl + " // Discord webhook URL (leave blank to disable).\n");
        }
    }

    private static void appendMissingKeys(Path configPath, Map<String, String> existing, ServerConfig defaults) throws IOException {
        // Keep deterministic ordering.
        Set<String> required = new LinkedHashSet<>();
        required.add("server-ip");
        required.add("server-port");
        required.add("server-motd");
        required.add("max-clients");
        required.add("max-matches");
        required.add("allow-botgames");
        required.add("trail-time");
        required.add("enforce-uniquecolors");
        required.add("logtofile");
        required.add("logonefile");
        required.add("logtowebhook");
        required.add("logwebhookurl");

        Set<String> missing = new LinkedHashSet<>();
        for (String key : required) {
            if (!existing.containsKey(key)) {
                missing.add(key);
            }
        }

        if (missing.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("// Added missing keys on startup\n");

        for (String key : missing) {
            sb.append(key).append("=").append(defaultValueForKey(key, defaults)).append("\n");
        }

        Files.writeString(
                configPath,
                sb.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.APPEND
        );
    }

    private static String defaultValueForKey(String key, ServerConfig d) {
        if (key.equals("server-ip")) return d.serverIp;
        if (key.equals("server-port")) return Integer.toString(d.serverPort);
        if (key.equals("server-motd")) return d.serverMotd;
        if (key.equals("max-clients")) return Integer.toString(d.maxClients);
        if (key.equals("max-matches")) return Integer.toString(d.maxMatches);
        if (key.equals("allow-botgames")) return Boolean.toString(d.allowBotGames);
        if (key.equals("trail-time")) return Integer.toString(d.trailTimeSeconds);
        if (key.equals("enforce-uniquecolors")) return Boolean.toString(d.enforceUniqueColors);
        if (key.equals("logtofile")) return Boolean.toString(d.logToFile);
        if (key.equals("logonefile")) return Boolean.toString(d.logOneFile);
        if (key.equals("logtowebhook")) return Boolean.toString(d.logToWebhook);
        if (key.equals("logwebhookurl")) return d.logWebhookUrl == null ? "" : d.logWebhookUrl;
        return "";
    }

    private static Map<String, String> parseKeyValueFile(Path path) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("//")) continue;

            int commentIndex = findCommentStart(line);
            if (commentIndex >= 0) line = line.substring(0, commentIndex).trim();

            int eq = line.indexOf('=');
            if (eq <= 0) continue;

            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            value = stripWrappingQuotes(value);
            map.put(key, value);
        }
        return map;
    }
    
    private static int findCommentStart(String line) {
        int fromIndex = 0;
        while (true) {
            int idx = line.indexOf("//", fromIndex);
            if (idx < 0) return -1;

            if (idx == 0) return 0;

            char before = line.charAt(idx - 1);
            if (Character.isWhitespace(before)) return idx;

            // Likely part of https://, keep searching
            fromIndex = idx + 2;
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean parseBool(String value, boolean fallback) {
        if (value == null) return fallback;
        String v = value.trim().toLowerCase();
        if (v.equals("true")) return true;
        if (v.equals("false")) return false;
        return fallback;
    }
    
    private static String stripWrappingQuotes(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.length() >= 2) {
            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                return v.substring(1, v.length() - 1).trim();
            }
        }
        return v;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void safeInfo(Logger logger, String message) {
        if (logger != null) logger.info(message);
        else System.out.println("[Server] " + message);
    }

    private static void safeError(Logger logger, String message, Throwable error) {
        if (logger != null) logger.error(message, error);
        else {
            System.err.println("[Server] " + message);
            if (error != null) error.printStackTrace(System.err);
        }
    }
}