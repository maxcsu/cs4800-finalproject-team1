package edu.csu.javatron.server;

import edu.csu.javatron.server.net.ClientSession;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class StatsManager implements AutoCloseable {

    public static final class LeaderboardEntry {
        public final String playerId;
        public final int lastPlayerNumber;
        public final String playerName;
        public final String color;
        public final int wins;
        public final int losses;
        public final double winRate;
        public final int derezzes;

        public LeaderboardEntry(String playerId, int lastPlayerNumber, String playerName, String color,
                                int wins, int losses, double winRate, int derezzes) {
            this.playerId = playerId;
            this.lastPlayerNumber = lastPlayerNumber;
            this.playerName = playerName;
            this.color = color;
            this.wins = wins;
            this.losses = losses;
            this.winRate = winRate;
            this.derezzes = derezzes;
        }
    }

    private final ServerConfig.Logger logger;
    private final String dbUrl;

    public StatsManager(ServerConfig.Logger logger) {
        this(logger, Path.of("leaderboard.db"));
    }

    StatsManager(ServerConfig.Logger logger, Path databasePath) {
        this.logger = logger;
        this.dbUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
        initialize();
    }

    private void initialize() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS leaderboard (
                        player_id TEXT PRIMARY KEY,
                        last_player_number INTEGER NOT NULL,
                        player_name TEXT NOT NULL,
                        color TEXT NOT NULL,
                        wins INTEGER NOT NULL DEFAULT 0,
                        losses INTEGER NOT NULL DEFAULT 0,
                        win_rate REAL NOT NULL DEFAULT 0.0,
                        derezzes INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_derezzes (
                        winner_player_id TEXT NOT NULL,
                        defeated_player_id TEXT NOT NULL,
                        PRIMARY KEY (winner_player_id, defeated_player_id)
                    )
                    """);
            migrateWinRateColumn(connection, statement);
            migrateDerezzesColumns(connection, statement);
            migrateLegacyPlayerIds(connection);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to initialize leaderboard database.", ex);
        }
    }

    public synchronized void registerPlayerSession(ClientSession session) {
        if (session == null) {
            return;
        }
        String playerId = resolvePlayerId(session);
        try (Connection connection = openConnection()) {
            upsertPlayerIdentity(connection, session, playerId);
        } catch (SQLException ex) {
            logError("Failed to register leaderboard player " + playerId, ex);
        }
    }

    public synchronized void recordMatchResult(ClientSession winner, ClientSession loser) {
        if (winner == null || loser == null) {
            return;
        }

        String winnerId = resolvePlayerId(winner);
        String loserId = resolvePlayerId(loser);

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            upsertPlayerIdentity(connection, winner, winnerId);
            upsertPlayerIdentity(connection, loser, loserId);
            incrementWin(connection, winnerId);
            incrementLoss(connection, loserId);
            recordUniqueDerezz(connection, winnerId, loserId);

            connection.commit();
        } catch (SQLException ex) {
            logError("Failed to record leaderboard match result.", ex);
        }
    }

    public synchronized List<LeaderboardEntry> getTopLeaderboardEntries(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        String sql = """
                SELECT player_id, last_player_number, player_name, color, wins, losses, win_rate, derezzes
                FROM leaderboard
                ORDER BY wins DESC, win_rate DESC, losses ASC, player_id ASC
                LIMIT ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new LeaderboardEntry(
                            resultSet.getString("player_id"),
                            resultSet.getInt("last_player_number"),
                            resultSet.getString("player_name"),
                            resultSet.getString("color"),
                            resultSet.getInt("wins"),
                            resultSet.getInt("losses"),
                            readWinRate(resultSet),
                            resultSet.getInt("derezzes")));
                }
            }
        } catch (SQLException ex) {
            logError("Failed to load leaderboard entries.", ex);
        }

        return entries;
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void migrateLegacyPlayerIds(Connection connection) throws SQLException {
        List<LeaderboardEntry> legacyEntries = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT player_id, last_player_number, player_name, color, wins, losses, win_rate, derezzes
                FROM leaderboard
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String playerId = resultSet.getString("player_id");
                if (!isLegacyPlayerId(playerId)) {
                    continue;
                }

                legacyEntries.add(new LeaderboardEntry(
                        playerId,
                        resultSet.getInt("last_player_number"),
                        resultSet.getString("player_name"),
                        resultSet.getString("color"),
                        resultSet.getInt("wins"),
                        resultSet.getInt("losses"),
                        readWinRate(resultSet),
                        resultSet.getInt("derezzes")));
            }
        }

        for (LeaderboardEntry legacyEntry : legacyEntries) {
            if (legacyEntry.lastPlayerNumber <= 0) {
                continue;
            }

            String normalizedPlayerId = formatPlayerNumber(legacyEntry.lastPlayerNumber);
            mergeLegacyEntry(connection, legacyEntry, normalizedPlayerId);
        }
    }

    private void mergeLegacyEntry(Connection connection, LeaderboardEntry legacyEntry, String normalizedPlayerId)
            throws SQLException {
        if (normalizedPlayerId.equals(legacyEntry.playerId)) {
            return;
        }

        connection.setAutoCommit(false);
        try {
            Integer existingWins = null;
            Integer existingLosses = null;
            Integer existingDerezzes = null;

            try (PreparedStatement selectStatement = connection.prepareStatement("""
                    SELECT wins, losses, derezzes
                    FROM leaderboard
                    WHERE player_id = ?
                    """)) {
                selectStatement.setString(1, normalizedPlayerId);
                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    if (resultSet.next()) {
                        existingWins = resultSet.getInt("wins");
                        existingLosses = resultSet.getInt("losses");
                        existingDerezzes = resultSet.getInt("derezzes");
                    }
                }
            }

            if (existingWins == null) {
                try (PreparedStatement updateIdStatement = connection.prepareStatement("""
                        UPDATE leaderboard
                        SET player_id = ?
                        WHERE player_id = ?
                        """)) {
                    updateIdStatement.setString(1, normalizedPlayerId);
                    updateIdStatement.setString(2, legacyEntry.playerId);
                    updateIdStatement.executeUpdate();
                }
                migrateDerezzReferences(connection, legacyEntry.playerId, normalizedPlayerId);
            } else {
                try (PreparedStatement mergeStatement = connection.prepareStatement("""
                        UPDATE leaderboard
                        SET wins = ?,
                            losses = ?,
                            win_rate = ?,
                            derezzes = ?,
                            player_name = ?,
                            color = ?,
                            last_player_number = ?
                        WHERE player_id = ?
                        """)) {
                    int mergedWins = existingWins + legacyEntry.wins;
                    int mergedLosses = existingLosses + legacyEntry.losses;
                    mergeStatement.setInt(1, mergedWins);
                    mergeStatement.setInt(2, mergedLosses);
                    mergeStatement.setDouble(3, calculateWinRate(mergedWins, mergedLosses));
                    mergeStatement.setInt(4, (existingDerezzes == null ? 0 : existingDerezzes) + legacyEntry.derezzes);
                    mergeStatement.setString(5, legacyEntry.playerName);
                    mergeStatement.setString(6, legacyEntry.color);
                    mergeStatement.setInt(7, legacyEntry.lastPlayerNumber);
                    mergeStatement.setString(8, normalizedPlayerId);
                    mergeStatement.executeUpdate();
                }

                migrateDerezzReferences(connection, legacyEntry.playerId, normalizedPlayerId);

                try (PreparedStatement deleteLegacyStatement = connection.prepareStatement("""
                        DELETE FROM leaderboard
                        WHERE player_id = ?
                        """)) {
                    deleteLegacyStatement.setString(1, legacyEntry.playerId);
                    deleteLegacyStatement.executeUpdate();
                }
            }

            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void upsertPlayerIdentity(Connection connection, ClientSession session, String playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO leaderboard (player_id, last_player_number, player_name, color, wins, losses, win_rate, derezzes)
                VALUES (?, ?, ?, ?, 0, 0, 0.0, 0)
                ON CONFLICT(player_id) DO UPDATE SET
                    last_player_number = excluded.last_player_number,
                    player_name = excluded.player_name,
                    color = excluded.color
                """)) {
            statement.setString(1, playerId);
            statement.setInt(2, session.getPlayerNumber());
            statement.setString(3, session.getPlayerName());
            statement.setString(4, session.getRequestedColor());
            statement.executeUpdate();
        }
    }

    private void recordUniqueDerezz(Connection connection, String winnerId, String loserId) throws SQLException {
        if (winnerId == null || winnerId.isBlank() || loserId == null || loserId.isBlank()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO player_derezzes (winner_player_id, defeated_player_id)
                VALUES (?, ?)
                """)) {
            statement.setString(1, winnerId);
            statement.setString(2, loserId);
            int inserted = statement.executeUpdate();
            if (inserted > 0) {
                try (PreparedStatement updateWinner = connection.prepareStatement("""
                        UPDATE leaderboard
                        SET derezzes = derezzes + 1
                        WHERE player_id = ?
                        """)) {
                    updateWinner.setString(1, winnerId);
                    updateWinner.executeUpdate();
                }
            }
        }
    }

    private void incrementWin(Connection connection, String playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE leaderboard
                SET wins = wins + 1,
                    win_rate = CASE
                        WHEN (wins + losses + 1) = 0 THEN 0.0
                        ELSE ROUND(((wins + 1) * 100.0) / (wins + losses + 1), 2)
                    END
                WHERE player_id = ?
                """)) {
            statement.setString(1, playerId);
            statement.executeUpdate();
        }
    }

    private void incrementLoss(Connection connection, String playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE leaderboard
                SET losses = losses + 1,
                    win_rate = CASE
                        WHEN (wins + losses + 1) = 0 THEN 0.0
                        ELSE ROUND((wins * 100.0) / (wins + losses + 1), 2)
                    END
                WHERE player_id = ?
                """)) {
            statement.setString(1, playerId);
            statement.executeUpdate();
        }
    }

    private String resolvePlayerId(ClientSession session) {
        return formatPlayerNumber(session.getPlayerNumber());
    }

    private boolean isLegacyPlayerId(String playerId) {
        return playerId == null || !playerId.matches("\\d+");
    }

    private void migrateWinRateColumn(Connection connection, Statement statement) throws SQLException {
        boolean hasWinRate = false;
        boolean hasOverallScore = false;

        try (ResultSet columns = statement.executeQuery("PRAGMA table_info(leaderboard)")) {
            while (columns.next()) {
                String columnName = columns.getString("name");
                if ("win_rate".equalsIgnoreCase(columnName)) {
                    hasWinRate = true;
                }
                if ("overall_score".equalsIgnoreCase(columnName)) {
                    hasOverallScore = true;
                }
            }
        }

        if (!hasWinRate) {
            statement.executeUpdate("ALTER TABLE leaderboard ADD COLUMN win_rate REAL NOT NULL DEFAULT 0.0");
        }

        if (!hasWinRate || hasOverallScore) {
            try (PreparedStatement updateStatement = connection.prepareStatement("""
                    UPDATE leaderboard
                    SET win_rate = CASE
                        WHEN (wins + losses) = 0 THEN 0.0
                        ELSE ROUND((wins * 100.0) / (wins + losses), 2)
                    END
                    """)) {
                updateStatement.executeUpdate();
            }
        }
    }

    private void migrateDerezzesColumns(Connection connection, Statement statement) throws SQLException {
        boolean hasDerezzes = false;

        try (ResultSet columns = statement.executeQuery("PRAGMA table_info(leaderboard)")) {
            while (columns.next()) {
                String columnName = columns.getString("name");
                if ("derezzes".equalsIgnoreCase(columnName)) {
                    hasDerezzes = true;
                }
            }
        }

        if (!hasDerezzes) {
            statement.executeUpdate("ALTER TABLE leaderboard ADD COLUMN derezzes INTEGER NOT NULL DEFAULT 0");
        }

        try (PreparedStatement rebuildStatement = connection.prepareStatement("""
                UPDATE leaderboard
                SET derezzes = (
                    SELECT COUNT(*)
                    FROM player_derezzes d
                    WHERE d.winner_player_id = leaderboard.player_id
                )
                """)) {
            rebuildStatement.executeUpdate();
        }
    }

    private void migrateDerezzReferences(Connection connection, String oldPlayerId, String newPlayerId) throws SQLException {
        if (oldPlayerId == null || newPlayerId == null || oldPlayerId.equals(newPlayerId)) {
            return;
        }

        try (PreparedStatement winnerRows = connection.prepareStatement("""
                SELECT defeated_player_id
                FROM player_derezzes
                WHERE winner_player_id = ?
                """)) {
            winnerRows.setString(1, oldPlayerId);
            try (ResultSet resultSet = winnerRows.executeQuery()) {
                while (resultSet.next()) {
                    try (PreparedStatement insert = connection.prepareStatement("""
                            INSERT OR IGNORE INTO player_derezzes (winner_player_id, defeated_player_id)
                            VALUES (?, ?)
                            """)) {
                        insert.setString(1, newPlayerId);
                        insert.setString(2, resultSet.getString("defeated_player_id"));
                        insert.executeUpdate();
                    }
                }
            }
        }

        try (PreparedStatement defeatedRows = connection.prepareStatement("""
                SELECT winner_player_id
                FROM player_derezzes
                WHERE defeated_player_id = ?
                """)) {
            defeatedRows.setString(1, oldPlayerId);
            try (ResultSet resultSet = defeatedRows.executeQuery()) {
                while (resultSet.next()) {
                    try (PreparedStatement insert = connection.prepareStatement("""
                            INSERT OR IGNORE INTO player_derezzes (winner_player_id, defeated_player_id)
                            VALUES (?, ?)
                            """)) {
                        insert.setString(1, resultSet.getString("winner_player_id"));
                        insert.setString(2, newPlayerId);
                        insert.executeUpdate();
                    }
                }
            }
        }

        try (PreparedStatement delete = connection.prepareStatement("""
                DELETE FROM player_derezzes
                WHERE winner_player_id = ? OR defeated_player_id = ?
                """)) {
            delete.setString(1, oldPlayerId);
            delete.setString(2, oldPlayerId);
            delete.executeUpdate();
        }
    }

    private double calculateWinRate(int wins, int losses) {
        int totalGames = wins + losses;
        if (totalGames <= 0) {
            return 0.0;
        }
        double rawRate = (wins * 100.0) / totalGames;
        return Math.round(rawRate * 100.0) / 100.0;
    }

    private double readWinRate(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if ("win_rate".equalsIgnoreCase(metaData.getColumnName(i))) {
                return resultSet.getDouble("win_rate");
            }
        }
        return calculateWinRate(resultSet.getInt("wins"), resultSet.getInt("losses"));
    }

    private String formatPlayerNumber(int playerNumber) {
        return (playerNumber < 10) ? ("0" + playerNumber) : Integer.toString(playerNumber);
    }

    private void logError(String message, SQLException ex) {
        if (logger != null) {
            logger.error(message, ex);
        }
    }

    @Override
    public void close() {
        // Connections are short-lived, so there is nothing to release here.
    }
    
 // Determine rank tier based on win rate percentage
    public String getRankTier(double winRate) {
        if (winRate <= 50.0) return "BRONZE";
        if (winRate <= 75.0) return "SILVER";
        return "GOLD";
    }
}
