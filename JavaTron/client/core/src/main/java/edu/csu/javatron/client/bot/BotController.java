package edu.csu.javatron.client.bot;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/** Practice-mode bot controller. */
public class BotController {
    private static final int ARENA_COLS = 48;
    private static final int ARENA_ROWS = 80;

    public char chooseDirection(char currentDir, int botX, int botY, Predicate<Character> isSafeDirection) {
        return chooseDirection(currentDir, botX, botY, isSafeDirection, ignored -> 0);
    }

    public char chooseDirection(char currentDir, int botX, int botY, Predicate<Character> isSafeDirection,
                                ToIntFunction<Character> directionScore) {
        char[] candidates = preferenceOrder(currentDir, botX, botY);
        char bestDirection = currentDir;
        int bestScore = Integer.MIN_VALUE;
        for (char candidate : candidates) {
            if (isSafeDirection.test(candidate)) {
                int score = directionScore.applyAsInt(candidate);
                if (score > bestScore) {
                    bestScore = score;
                    bestDirection = candidate;
                }
            }
        }
        return bestScore == Integer.MIN_VALUE ? currentDir : bestDirection;
    }

    private char[] preferenceOrder(char currentDir, int botX, int botY) {
        char preferredTurn = preferredTurn(currentDir, botX, botY);
        char alternateTurn = alternateTurn(currentDir, preferredTurn);
        return switch (currentDir) {
            case 'U', 'D', 'L', 'R' -> new char[] {currentDir, preferredTurn, alternateTurn};
            default -> new char[] {'U', 'L', 'R', 'D'};
        };
    }

    private char preferredTurn(char currentDir, int botX, int botY) {
        return switch (currentDir) {
            case 'U', 'D' -> horizontalTurn(botX, botY);
            case 'L', 'R' -> verticalTurn(botX, botY);
            default -> 'L';
        };
    }

    private char alternateTurn(char currentDir, char preferredTurn) {
        return switch (currentDir) {
            case 'U', 'D' -> preferredTurn == 'L' ? 'R' : 'L';
            case 'L', 'R' -> preferredTurn == 'U' ? 'D' : 'U';
            default -> 'R';
        };
    }

    private char horizontalTurn(int botX, int botY) {
        int leftSpace = botX;
        int rightSpace = (ARENA_COLS - 1) - botX;
        if (leftSpace == rightSpace) {
            return (botY & 1) == 0 ? 'L' : 'R';
        }
        return leftSpace > rightSpace ? 'L' : 'R';
    }

    private char verticalTurn(int botX, int botY) {
        int downSpace = (ARENA_ROWS - 1) - botY;
        int upSpace = botY;
        if (upSpace == downSpace) {
            return (botX & 1) == 0 ? 'U' : 'D';
        }
        return upSpace > downSpace ? 'U' : 'D';
    }
}
