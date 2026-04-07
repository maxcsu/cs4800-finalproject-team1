package edu.csu.javatron.client.bot;

import java.util.function.Predicate;

/** Practice-mode bot controller. */
public class BotController {
    public char chooseDirection(char currentDir, int botX, int botY, Predicate<Character> isSafeDirection) {
        char[] candidates = preferenceOrder(currentDir);
        for (char candidate : candidates) {
            if (isSafeDirection.test(candidate)) {
                return candidate;
            }
        }
        return currentDir;
    }

    private char[] preferenceOrder(char currentDir) {
        return switch (currentDir) {
            case 'U' -> new char[] {'U', 'L', 'R'};
            case 'D' -> new char[] {'D', 'R', 'L'};
            case 'L' -> new char[] {'L', 'D', 'U'};
            case 'R' -> new char[] {'R', 'U', 'D'};
            default -> new char[] {'U', 'L', 'R', 'D'};
        };
    }
}
