package edu.csu.javatron;

import java.util.Locale;

public final class LeaderboardEntry {
    public final int playerNumber;
    public final String playerId;
    public final String color;
    public final int wins;
    public final int losses;
    public final double winRate;
    public final int derezzes;

    public LeaderboardEntry(int playerNumber, String playerId, String color, int wins, int losses, double winRate,
                            int derezzes) {
        this.playerNumber = playerNumber;
        this.playerId = playerId;
        this.color = color;
        this.wins = wins;
        this.losses = losses;
        this.winRate = winRate;
        this.derezzes = derezzes;
    }

    public String getDisplayId() {
        if (playerNumber > 0) {
            return String.format("%02d", playerNumber);
        }
        if (playerId == null || playerId.isBlank()) {
            return "??";
        }
        String compact = playerId.replace("-", "");
        return compact.length() <= 2 ? compact.toUpperCase() : compact.substring(0, 2).toUpperCase();
    }

    public String getPlayerDisplay() {
        return color + " " + getDisplayId();
    }

    public String getWinsDisplay() {
        return wins + "W";
    }

    public String getLossesDisplay() {
        return losses + "L";
    }

    public String getLoseRateDisplay() {
        int totalMatches = wins + losses;
        double loseRate = totalMatches <= 0 ? 0.0 : (losses * 100.0) / totalMatches;
        return String.format(Locale.US, "%.2f%%", loseRate);
    }

    public String getRateDisplay() {
        return String.format(Locale.US, "%.2f%%", winRate);
    }

    public String getDerezzesDisplay() {
        return Integer.toString(derezzes);
    }
}
