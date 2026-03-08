package edu.csu.javatron.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StatsManager {

    private static class PlayerStats {
        AtomicInteger wins = new AtomicInteger(0);
        AtomicInteger losses = new AtomicInteger(0);
    }

    private final ConcurrentHashMap<Integer, PlayerStats> stats = new ConcurrentHashMap<>();

    private PlayerStats getOrCreate(int playerNumber) {
        return stats.computeIfAbsent(playerNumber, k -> new PlayerStats());
    }

    public void recordWin(int playerNumber) {
        getOrCreate(playerNumber).wins.incrementAndGet();
    }

    public void recordLoss(int playerNumber) {
        getOrCreate(playerNumber).losses.incrementAndGet();
    }

    public String getStats(int playerNumber) {
        PlayerStats ps = getOrCreate(playerNumber);
        int w = ps.wins.get();
        int l = ps.losses.get();
        int total = w + l;

        double ratio = total == 0 ? 0.0 : ((double) w / total) * 100.0;

        return "wins=" + w + "|losses=" + l + "|winRate=" + String.format("%.2f", ratio);
    }
}