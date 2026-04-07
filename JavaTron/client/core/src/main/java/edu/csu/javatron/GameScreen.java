/*
 * AI Tools Use Transparency Disclosure:
 * Primary prior GitHub handling credit: Bhawna Gogna.
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import edu.csu.javatron.client.bot.BotController;

import java.util.HashSet;
import java.util.Set;

/** Game screen for rendering the match and handling input. */
public class GameScreen extends ScreenAdapter {
    private static final boolean ENABLE_SCOREBOARD = false;

    private final JavaTronGame game;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private SpriteBatch spriteBatch;
    private Texture gridTexture;
    private ShapeRenderer shapeRenderer;
    private Set<Integer> aTrails; // encoded as x*100 + y
    private Set<Integer> bTrails;
    private BitmapFont font;
    private int prevAx, prevAy, prevBx, prevBy;
    private int prevRoundNumber = -1; // track round changes to clear trails
    private float goDisplayTimer = 0f; // shows GO for 1.5 seconds then clears
    private Music gameMusic;
    private Music cycleHum;
    private Sound turnSound;
    private Sound crashSound;
    private Sound winSound;
    private Sound loseSound;
    private Sound winAltSound;
    private Sound gameOverSound;
    private Sound cycleStartSound;
    private Texture playerCycleTexture;
    private Texture opponentCycleTexture;
    private Texture explosionTexture1;
    private Texture explosionTexture2;
    private String lastCountdownMessage = null;
    private long scheduledMusicStartAtMs = -1L;
    private int handledRoundEventId = 0;
    private boolean exitPromptVisible = false;
    private boolean hideCycleA = false;
    private boolean hideCycleB = false;
    private float explosionAx = 0f;
    private float explosionAy = 0f;
    private float explosionBx = 0f;
    private float explosionBy = 0f;
    private long explosionAStartMs = -1L;
    private long explosionBStartMs = -1L;
    private final BotController botController = new BotController();
    private float practiceTickAccumulator = 0f;
    private float practiceRoundResetDelay = 0f;
    private float practiceMatchEndTransitionDelay = 0f;
    private String practicePendingTurnDir = null;
    private float practiceCountdownTimer = 0f;
    private int practiceCountdownStep = 0;

    // Arena constants — must match server Protocol.java
    private static final int ARENA_COLS = 48;
    private static final int ARENA_ROWS = 80;
    private static final int CELL = 10; // pixels per cell
    private static final float GAMEPLAY_SPRITE_SCALE = 2f;
    private static final long EXPLOSION_SWAP_MS = 250L;
    private static final float PRACTICE_TICK_SECONDS = 1f / 12f;
    private static final float PRACTICE_ROUND_RESET_SECONDS = 5.616f;
    private static final float PRACTICE_COUNTDOWN_THREE_SECONDS = 0.933f;
    private static final float PRACTICE_COUNTDOWN_CYCLESTART_SECONDS = 0.067f;
    private static final float PRACTICE_COUNTDOWN_STANDARD_SECONDS = 1.0f;
    private static final float PRACTICE_MATCH_END_SCREEN_DELAY_SECONDS = 6.5f;
    private static final int ROUNDS_TO_WIN = 2;

    public GameScreen(JavaTronGame game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT, camera);
        viewport.apply(true);

        // Set input processor to null to allow raw key input
        Gdx.input.setInputProcessor(null);

        spriteBatch = new SpriteBatch();
        gridTexture = new Texture(Gdx.files.internal("gfx/tile/bgtile_indigo.png"));
        gridTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        playerCycleTexture = loadCycleTexture(resolvePlayerColor());
        opponentCycleTexture = loadCycleTexture(resolveOpponentColor());
        explosionTexture1 = new Texture(Gdx.files.internal("gfx/expl1.png"));
        explosionTexture2 = new Texture(Gdx.files.internal("gfx/expl2.png"));
        shapeRenderer = new ShapeRenderer();
        aTrails = new HashSet<>();
        bTrails = new HashSet<>();
        font = new BitmapFont();

        // Load gameplay music and sound effects
        try {
            gameMusic = Gdx.audio.newMusic(Gdx.files.internal("snd/mus_gameplay.mp3"));
            gameMusic.setLooping(true);
        } catch (Exception e) {
            System.out.println("Could not load game music: " + e.getMessage());
        }

        try {
            cycleHum = Gdx.audio.newMusic(Gdx.files.internal("snd/snd_cyclehum.mp3"));
            cycleHum.setLooping(true);
        } catch (Exception e) {
            System.out.println("Could not load cycle hum: " + e.getMessage());
        }

        try {
            turnSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_cycleturn.mp3"));
        } catch (Exception e) {
            System.out.println("Could not load turn sound: " + e.getMessage());
        }

        try {
            crashSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_crash.mp3"));
            winSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_win.mp3"));
            loseSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_lose.mp3"));
            winAltSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_win_alt.mp3"));
            gameOverSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_gameover.mp3"));
            cycleStartSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_cyclestart.mp3"));
        } catch (Exception e) {
            System.out.println("Could not load one or more gameplay sounds: " + e.getMessage());
        }
    }

    @Override
    public void show() {
        game.stopMenuMusic();
        applyAudioSettings();
        game.clearPendingNetworkSnapshots();
        lastCountdownMessage = null;
        scheduledMusicStartAtMs = -1L;
        handledRoundEventId = game.latestRoundEventId;
        practiceTickAccumulator = 0f;
        practiceRoundResetDelay = 0f;
        practiceMatchEndTransitionDelay = 0f;
        practicePendingTurnDir = null;
        practiceCountdownTimer = 0f;
        practiceCountdownStep = 0;
        if (game.practiceMode && game.roundResultText == null) {
            beginPracticeCountdown();
        }
    }

    public void applyAudioSettings() {
        if (!game.isMusicEnabled()) {
            if (gameMusic != null && gameMusic.isPlaying()) {
                gameMusic.stop();
            }
        }
        if (!game.isGameSoundEffectsEnabled()) {
            if (cycleHum != null && cycleHum.isPlaying()) {
                cycleHum.stop();
            }
        }
    }

    public void updateTrails() {
        if (!game.practiceMode) {
            updateNetworkTrails();
            return;
        }

        // Clear trails when a new round starts (positions jump back to spawn)
        if (game.roundNumber != prevRoundNumber) {
            aTrails.clear();
            bTrails.clear();
            resetCycleEffects();
            game.roundResultText = null;
            game.finalMatchResult = false;
            game.latestRoundEventType = null;
            game.latestWinnerSide = null;
            prevRoundNumber = game.roundNumber;
            prevAx = game.ax;
            prevAy = game.ay;
            prevBx = game.bx;
            prevBy = game.by;
            return;
        }
        // Only add to trail when the cycle actually moved
        if (prevAx != game.ax || prevAy != game.ay) {
            aTrails.add(prevAx * 100 + prevAy);
        }
        if (prevBx != game.bx || prevBy != game.by) {
            bTrails.add(prevBx * 100 + prevBy);
        }
        prevAx = game.ax;
        prevAy = game.ay;
        prevBx = game.bx;
        prevBy = game.by;
    }

    private void updateNetworkTrails() {
        JavaTronGame.SnapshotFrame snapshot;
        boolean consumedSnapshot = false;
        while ((snapshot = game.pollPendingNetworkSnapshot()) != null) {
            consumedSnapshot = true;
            if (snapshot.roundNumber != prevRoundNumber) {
                aTrails.clear();
                bTrails.clear();
                resetCycleEffects();
                game.roundResultText = null;
                game.finalMatchResult = false;
                game.latestRoundEventType = null;
                game.latestWinnerSide = null;
                prevRoundNumber = snapshot.roundNumber;
                prevAx = snapshot.ax;
                prevAy = snapshot.ay;
                prevBx = snapshot.bx;
                prevBy = snapshot.by;
                continue;
            }

            addTrailSegment(aTrails, prevAx, prevAy, snapshot.ax, snapshot.ay);
            addTrailSegment(bTrails, prevBx, prevBy, snapshot.bx, snapshot.by);

            prevAx = snapshot.ax;
            prevAy = snapshot.ay;
            prevBx = snapshot.bx;
            prevBy = snapshot.by;
        }

        if (!consumedSnapshot && game.roundNumber != prevRoundNumber) {
            aTrails.clear();
            bTrails.clear();
            resetCycleEffects();
            game.roundResultText = null;
            game.finalMatchResult = false;
            game.latestRoundEventType = null;
            game.latestWinnerSide = null;
            prevRoundNumber = game.roundNumber;
            prevAx = game.ax;
            prevAy = game.ay;
            prevBx = game.bx;
            prevBy = game.by;
        }
    }

    private void addTrailSegment(Set<Integer> trailSet, int fromX, int fromY, int toX, int toY) {
        if (fromX == toX && fromY == toY) {
            return;
        }

        int dx = Integer.compare(toX, fromX);
        int dy = Integer.compare(toY, fromY);
        int x = fromX;
        int y = fromY;

        while (x != toX || y != toY) {
            trailSet.add(x * 100 + y);
            x += dx;
            y += dy;
        }
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            if (exitPromptVisible) {
                confirmExitPrompt();
            } else {
                exitPromptVisible = true;
            }
        }
        if (exitPromptVisible) {
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.Y)) {
                confirmExitPrompt();
            } else if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.N)) {
                exitPromptVisible = false;
            }
        }

        if (game.practiceMode && !exitPromptVisible) {
            updatePracticeMatch(delta);
        }

        if (!exitPromptVisible || !game.practiceMode) {
            updateTrails();
        }

        handleRoundAudioState();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        spriteBatch.setProjectionMatrix(camera.combined);

        // Draw grid across full world
        spriteBatch.begin();
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        float uRepeat = MenuVisuals.backgroundURepeat(worldWidth, gridTexture.getWidth());
        float vRepeat = MenuVisuals.backgroundVRepeat(worldHeight, gridTexture.getHeight());
        spriteBatch.draw(gridTexture, 0, 0, worldWidth, worldHeight, 0, 0, uRepeat, vRepeat);
        spriteBatch.end();

        // Helper method to convert String color to LibGDX Color
        Color colorA = CycleColors.get(resolveColorForSideA(), Color.valueOf("99d9ea"));
        Color colorB = CycleColors.get(resolveColorForSideB(), Color.valueOf("ff4a68"));

        // Draw trails
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(colorA);
        for (int trail : aTrails) {
            int x = trail / 100;
            int y = trail % 100;
            shapeRenderer.rect(x * CELL, y * CELL, CELL, CELL);
        }

        shapeRenderer.setColor(colorB);
        for (int trail : bTrails) {
            int x = trail / 100;
            int y = trail % 100;
            shapeRenderer.rect(x * CELL, y * CELL, CELL, CELL);
        }

        shapeRenderer.end();

        // Draw arena border walls (white outline) — this is the deadly edge!
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(0, 0, ARENA_COLS * CELL, ARENA_ROWS * CELL);
        shapeRenderer.end();

        spriteBatch.begin();
        if (ENABLE_SCOREBOARD) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0.15f, 0.78f);
            float boxX = 5f, boxH = 120f, boxW = 180f;
            float boxY = worldHeight - boxH - 5f;
            shapeRenderer.rect(boxX, boxY, boxW, boxH);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            float tx = boxX + 8f;
            float ty = boxY + boxH - 10f;

            font.setColor(Color.CYAN);
            font.draw(spriteBatch, "Round " + game.roundNumber, tx, ty);
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, "Score:", tx, ty - 22);
            font.setColor(colorA);
            font.draw(spriteBatch, (game.isPlayerA ? "You" : game.oppName) + ": " + game.aWins, tx, ty - 44);
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, "vs", tx, ty - 66);
            font.setColor(colorB);
            font.draw(spriteBatch, (game.isPlayerA ? game.oppName : "You") + ": " + game.bWins, tx, ty - 88);
        }

        drawCycleOrExplosion(game.ax, game.ay, game.aDir, textureForSideA(), hideCycleA, explosionAx, explosionAy,
                explosionAStartMs);
        drawCycleOrExplosion(game.bx, game.by, game.bDir, textureForSideB(), hideCycleB, explosionBx, explosionBy,
                explosionBStartMs);

        // Large countdown in center of screen
        String msg = game.countdownMessage;
        if (msg != null && !"CYCLESTART".equals(msg)) {
            if ("GO".equals(msg)) {
                // Start or continue the GO timer
                if (goDisplayTimer <= 0f)
                    goDisplayTimer = 1.5f;
                font.setColor(Color.GREEN);
            } else {
                goDisplayTimer = 0f; // reset GO timer for numeric counts
                font.setColor(Color.YELLOW);
            }
            font.getData().setScale(4f);
            font.draw(spriteBatch, msg, worldWidth / 2f - 25, worldHeight / 2f + 30);
            font.getData().setScale(1f);
        }

        // Tick the GO display timer and auto-clear when expired
        if (goDisplayTimer > 0f && (!exitPromptVisible || !game.practiceMode)) {
            goDisplayTimer -= delta;
            if (goDisplayTimer <= 0f) {
                game.countdownMessage = null;
                game.countdownActive = false;
            }
        }

        if (exitPromptVisible) {
            font.setColor(Color.WHITE);
            font.getData().setScale(2f);
            font.draw(spriteBatch,
                    game.getNetworkClient().isConnected() && !game.practiceMode
                            ? "Disconnect from server?"
                            : game.getNetworkClient().isConnected() ? "Return to Lobby?" : "Return to Menu?",
                    worldWidth / 2f - 125f, worldHeight / 2f + 40f);
            font.getData().setScale(1.4f);
            font.draw(spriteBatch, "Y/N", worldWidth / 2f - 22f, worldHeight / 2f);
            font.getData().setScale(1f);
        } else if (game.roundResultText != null && !game.roundResultText.isBlank()) {
            font.setColor(Color.WHITE);
            font.getData().setScale(game.finalMatchResult ? 1.6f : 1.2f);
            font.draw(spriteBatch, game.roundResultText, 30f, worldHeight / 2f + 20f, worldWidth - 60f,
                    com.badlogic.gdx.utils.Align.center, true);
            font.getData().setScale(1f);
        }
        spriteBatch.end();

        if (exitPromptVisible) {
            return;
        }

        // === INPUT: custom bind keys + Arrow key fallback ===
        // IMPORTANT: Server direction convention (libGDX y-up):
        // 'D' = +y = UP on screen 'U' = -y = DOWN on screen 'L'/'R' = left/right
        boolean upPressed = Gdx.input.isKeyPressed(game.upKey)
                || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.UP);
        boolean downPressed = Gdx.input.isKeyPressed(game.downKey)
                || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.DOWN);
        boolean leftPressed = Gdx.input.isKeyPressed(game.leftKey)
                || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT);
        boolean rightPressed = Gdx.input.isKeyPressed(game.rightKey)
                || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT);
        boolean upJustPressed = Gdx.input.isKeyJustPressed(game.upKey)
                || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP);
        boolean downJustPressed = Gdx.input.isKeyJustPressed(game.downKey)
                || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN);
        boolean leftJustPressed = Gdx.input.isKeyJustPressed(game.leftKey)
                || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.LEFT);
        boolean rightJustPressed = Gdx.input.isKeyJustPressed(game.rightKey)
                || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.RIGHT);

        String turnDir = null;
        if (upPressed)
            turnDir = "D"; // W/↑ → send 'D' which is +y = up on screen
        else if (downPressed)
            turnDir = "U"; // S/↓ → send 'U' which is -y = down on screen
        else if (leftPressed)
            turnDir = "L";
        else if (rightPressed)
            turnDir = "R";

        if (turnDir != null) {
            if (game.practiceMode) {
                practicePendingTurnDir = turnDir;
            } else {
                game.getNetworkClient().send("C_TURN|" + turnDir);
            }
            boolean movementSoundAllowed = game.roundResultText == null
                    && (!game.countdownActive || "GO".equals(game.countdownMessage));
            if (turnSound != null && game.isGameSoundEffectsEnabled()
                    && movementSoundAllowed
                    && (upJustPressed || downJustPressed || leftJustPressed || rightJustPressed))
                turnSound.play(0.5f);
        }
    }

    private void updatePracticeMatch(float delta) {
        if (practiceMatchEndTransitionDelay > 0f) {
            practiceMatchEndTransitionDelay = Math.max(0f, practiceMatchEndTransitionDelay - delta);
            if (practiceMatchEndTransitionDelay == 0f) {
                stopGameplayLoopSounds();
                game.showPracticeRematchScreen();
            }
            return;
        }
        if (practiceCountdownStep > 0) {
            practiceCountdownTimer = Math.max(0f, practiceCountdownTimer - delta);
            if (practiceCountdownTimer == 0f) {
                advancePracticeCountdown();
            }
            return;
        }
        if (practiceRoundResetDelay > 0f) {
            practiceRoundResetDelay = Math.max(0f, practiceRoundResetDelay - delta);
            if (practiceRoundResetDelay == 0f) {
                startPracticeRound();
            }
            return;
        }
        if (game.roundResultText != null) {
            return;
        }
        practiceTickAccumulator += delta;
        while (practiceTickAccumulator >= PRACTICE_TICK_SECONDS) {
            practiceTickAccumulator -= PRACTICE_TICK_SECONDS;
            runPracticeTick();
            if (game.roundResultText != null) {
                break;
            }
        }
    }

    private void runPracticeTick() {
        char nextADir = applyPendingTurn(game.aDir, practicePendingTurnDir);
        practicePendingTurnDir = null;
        char nextBDir = botController.chooseDirection(game.bDir, game.bx, game.by, candidate ->
                isPracticeDirectionSafe(game.bx, game.by, candidate, game.ax, game.ay),
                candidate -> practiceDirectionScore(game.bx, game.by, candidate, game.ax, game.ay));

        int nextAx = game.ax + directionDeltaX(nextADir);
        int nextAy = game.ay + directionDeltaY(nextADir);
        int nextBx = game.bx + directionDeltaX(nextBDir);
        int nextBy = game.by + directionDeltaY(nextBDir);

        boolean aCrash = isWallCollision(nextAx, nextAy)
                || isTrailCollision(nextAx, nextAy)
                || (nextAx == game.bx && nextAy == game.by);
        boolean bCrash = isWallCollision(nextBx, nextBy)
                || isTrailCollision(nextBx, nextBy)
                || (nextBx == game.ax && nextBy == game.ay);

        if (nextAx == nextBx && nextAy == nextBy) {
            aCrash = true;
            bCrash = true;
        } else if (nextAx == game.bx && nextAy == game.by && nextBx == game.ax && nextBy == game.ay) {
            aCrash = true;
            bCrash = true;
        }

        if (aCrash || bCrash) {
            handlePracticeRoundEnd(aCrash, bCrash);
            return;
        }

        game.aDir = nextADir;
        game.bDir = nextBDir;
        game.ax = nextAx;
        game.ay = nextAy;
        game.bx = nextBx;
        game.by = nextBy;
    }

    private void handlePracticeRoundEnd(boolean aCrash, boolean bCrash) {
        String winnerSide;
        if (aCrash && bCrash) {
            winnerSide = "NONE";
            game.roundResultText = "Tie round!";
        } else if (aCrash) {
            winnerSide = "B";
            game.bWins++;
            game.roundResultText = "Bot won round " + game.roundNumber + ".";
        } else {
            winnerSide = "A";
            game.aWins++;
            game.roundResultText = "You won round " + game.roundNumber + ".";
        }

        boolean matchOver = game.aWins >= ROUNDS_TO_WIN || game.bWins >= ROUNDS_TO_WIN;
        if (matchOver) {
            String matchSummary = (game.aWins > game.bWins ? "You" : "Bot")
                    + " won the match. Final Score: " + game.aWins + "-" + game.bWins;
            game.finalMatchResult = true;
            game.winnerName = matchSummary;
            game.roundResultText = matchSummary;
            practiceRoundResetDelay = 0f;
            practiceMatchEndTransitionDelay = PRACTICE_MATCH_END_SCREEN_DELAY_SECONDS;
        } else {
            game.finalMatchResult = false;
            practiceRoundResetDelay = PRACTICE_ROUND_RESET_SECONDS;
        }

        game.latestRoundEventType = "COLLISION";
        game.latestWinnerSide = winnerSide;
        game.latestRoundResult = "A".equals(winnerSide) ? "WIN" : "B".equals(winnerSide) ? "LOSE" : "TIE";
        game.latestRoundEventId++;
    }

    private void startPracticeRound() {
        game.roundNumber++;
        game.ax = 24;
        game.ay = 20;
        game.bx = 24;
        game.by = 60;
        game.aDir = 'D';
        game.bDir = 'U';
        game.roundResultText = null;
        game.latestRoundEventType = null;
        game.latestWinnerSide = null;
        game.countdownMessage = null;
        game.countdownActive = false;
        practiceTickAccumulator = 0f;
        practiceMatchEndTransitionDelay = 0f;
        practicePendingTurnDir = null;
        beginPracticeCountdown();
    }

    private boolean isPracticeDirectionSafe(int fromX, int fromY, char dir, int otherX, int otherY) {
        if (isReverseDir(dir, game.bDir)) {
            return false;
        }
        int nx = fromX + directionDeltaX(dir);
        int ny = fromY + directionDeltaY(dir);
        if (isWallCollision(nx, ny) || isTrailCollision(nx, ny)) {
            return false;
        }
        return nx != otherX || ny != otherY;
    }

    private int practiceDirectionScore(int fromX, int fromY, char dir, int otherX, int otherY) {
        if (!isPracticeDirectionSafe(fromX, fromY, dir, otherX, otherY)) {
            return Integer.MIN_VALUE;
        }

        int dx = directionDeltaX(dir);
        int dy = directionDeltaY(dir);
        int x = fromX;
        int y = fromY;
        int openSteps = 0;

        while (true) {
            x += dx;
            y += dy;
            if (isWallCollision(x, y) || isTrailCollision(x, y) || (x == otherX && y == otherY)) {
                break;
            }
            openSteps++;
        }

        int centerBias = -Math.abs(x - (ARENA_COLS / 2));
        return (openSteps * 100) + centerBias;
    }

    private boolean isWallCollision(int x, int y) {
        return x < 0 || x >= ARENA_COLS || y < 0 || y >= ARENA_ROWS;
    }

    private boolean isTrailCollision(int x, int y) {
        int encoded = x * 100 + y;
        return aTrails.contains(encoded) || bTrails.contains(encoded);
    }

    private char applyPendingTurn(char currentDir, String turnDir) {
        if (turnDir == null || turnDir.isBlank()) {
            return currentDir;
        }
        char candidate = turnDir.charAt(0);
        return isReverseDir(currentDir, candidate) ? currentDir : candidate;
    }

    private boolean isReverseDir(char currentDir, char candidateDir) {
        return (currentDir == 'U' && candidateDir == 'D')
                || (currentDir == 'D' && candidateDir == 'U')
                || (currentDir == 'L' && candidateDir == 'R')
                || (currentDir == 'R' && candidateDir == 'L');
    }

    private void beginPracticeCountdown() {
        practiceCountdownStep = 1;
        practiceCountdownTimer = PRACTICE_COUNTDOWN_THREE_SECONDS;
        game.countdownMessage = "3";
        game.countdownActive = true;
        lastCountdownMessage = null;
        handleRoundAudioState();
    }

    private void advancePracticeCountdown() {
        switch (practiceCountdownStep) {
            case 1 -> {
                practiceCountdownStep = 2;
                practiceCountdownTimer = PRACTICE_COUNTDOWN_CYCLESTART_SECONDS;
                game.countdownMessage = "CYCLESTART";
            }
            case 2 -> {
                practiceCountdownStep = 3;
                practiceCountdownTimer = PRACTICE_COUNTDOWN_STANDARD_SECONDS;
                game.countdownMessage = "2";
            }
            case 3 -> {
                practiceCountdownStep = 4;
                practiceCountdownTimer = PRACTICE_COUNTDOWN_STANDARD_SECONDS;
                game.countdownMessage = "1";
            }
            case 4 -> {
                practiceCountdownStep = 0;
                practiceCountdownTimer = 0f;
                game.countdownMessage = "GO";
            }
            default -> {
                practiceCountdownStep = 0;
                practiceCountdownTimer = 0f;
                game.countdownMessage = null;
                game.countdownActive = false;
            }
        }
        if (practiceCountdownStep == 0 && !"GO".equals(game.countdownMessage)) {
            game.countdownActive = false;
        } else {
            game.countdownActive = true;
        }
        lastCountdownMessage = null;
        handleRoundAudioState();
    }

    @Override
    public void resize(int width, int height) {
        if (WindowAspectEnforcer.enforce(width, height)) {
            return;
        }
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        spriteBatch.dispose();
        gridTexture.dispose();
        shapeRenderer.dispose();
        font.dispose();
        if (gameMusic != null) {
            gameMusic.stop();
            gameMusic.dispose();
        }
        if (cycleHum != null) {
            cycleHum.stop();
            cycleHum.dispose();
        }
        if (turnSound != null) {
            turnSound.dispose();
        }
        if (playerCycleTexture != null) playerCycleTexture.dispose();
        if (opponentCycleTexture != null) opponentCycleTexture.dispose();
        if (explosionTexture1 != null) explosionTexture1.dispose();
        if (explosionTexture2 != null) explosionTexture2.dispose();
        if (crashSound != null) crashSound.dispose();
        if (winSound != null) winSound.dispose();
        if (loseSound != null) loseSound.dispose();
        if (winAltSound != null) winAltSound.dispose();
        if (gameOverSound != null) gameOverSound.dispose();
        if (cycleStartSound != null) cycleStartSound.dispose();
    }

    private void confirmExitPrompt() {
        exitPromptVisible = false;
        stopGameplayLoopSounds();
        game.playMenuBackSound();
        if (game.getNetworkClient().isConnected()) {
            if (game.practiceMode) {
                game.matchPlayerColor = null;
                game.matchOppColor = null;
                game.showLobbyScreen();
            } else {
                game.playDisconnectSound();
                game.getNetworkClient().disconnect();
                game.showMainMenu();
            }
        } else {
            game.practiceMode = false;
            game.showMainMenu();
        }
    }

    private void handleRoundAudioState() {
        String currentCountdown = game.countdownMessage;
        if (currentCountdown != null && !currentCountdown.equals(lastCountdownMessage)) {
            if ("CYCLESTART".equals(currentCountdown)) {
                if (cycleStartSound != null && game.isGameSoundEffectsEnabled()) {
                    cycleStartSound.play(0.6f);
                }
            } else if ("GO".equals(currentCountdown)) {
                scheduledMusicStartAtMs = System.currentTimeMillis() + 500L;
                startCycleHum();
            } else if ("3".equals(currentCountdown) || "2".equals(currentCountdown) || "1".equals(currentCountdown)) {
                game.roundResultText = null;
                game.finalMatchResult = false;
                stopGameplayLoopSounds();
            }
            lastCountdownMessage = currentCountdown;
        }

        if (scheduledMusicStartAtMs > 0L && System.currentTimeMillis() >= scheduledMusicStartAtMs) {
            scheduledMusicStartAtMs = -1L;
            if (gameMusic != null && game.isMusicEnabled() && !gameMusic.isPlaying()) {
                gameMusic.play();
            }
        }

        if (game.latestRoundEventId != handledRoundEventId) {
            handledRoundEventId = game.latestRoundEventId;
            scheduledMusicStartAtMs = -1L;
            stopGameplayLoopSounds();
            updateExplosionState();
            if (crashSound != null && game.isGameSoundEffectsEnabled()) {
                crashSound.play(0.7f);
            }
            if (game.isGameSoundEffectsEnabled()) {
                if (game.finalMatchResult) {
                    if ("WIN".equalsIgnoreCase(game.latestRoundResult) && winAltSound != null) {
                        winAltSound.play(0.7f);
                    } else if ("LOSE".equalsIgnoreCase(game.latestRoundResult) && gameOverSound != null) {
                        gameOverSound.play(0.7f);
                    } else if ("TIE".equalsIgnoreCase(game.latestRoundResult) && loseSound != null) {
                        loseSound.play(0.7f);
                    }
                } else {
                    if ("WIN".equalsIgnoreCase(game.latestRoundResult) && winSound != null) {
                        winSound.play(0.7f);
                    } else if ("LOSE".equalsIgnoreCase(game.latestRoundResult) && loseSound != null) {
                        loseSound.play(0.7f);
                    } else if ("TIE".equalsIgnoreCase(game.latestRoundResult) && loseSound != null) {
                        loseSound.play(0.7f);
                    }
                }
            }
        }
    }

    private void startCycleHum() {
        if (cycleHum != null && game.isGameSoundEffectsEnabled() && !cycleHum.isPlaying()) {
            cycleHum.play();
        }
    }

    private void stopGameplayLoopSounds() {
        if (gameMusic != null && gameMusic.isPlaying()) {
            gameMusic.stop();
        }
        if (cycleHum != null && cycleHum.isPlaying()) {
            cycleHum.stop();
        }
    }

    private Texture loadCycleTexture(String colorName) {
        return new Texture(Gdx.files.internal("gfx/lightcycle_" + normalizeCycleColor(colorName) + ".png"));
    }

    private float cycleDrawSize() {
        return playerCycleTexture != null ? playerCycleTexture.getWidth() * GAMEPLAY_SPRITE_SCALE : 32f * GAMEPLAY_SPRITE_SCALE;
    }

    private String resolvePlayerColor() {
        return game.matchPlayerColor != null ? game.matchPlayerColor : game.playerColor;
    }

    private String resolveOpponentColor() {
        return game.matchOppColor != null ? game.matchOppColor : game.oppColor;
    }

    private String resolveColorForSideA() {
        return game.isPlayerA ? resolvePlayerColor() : resolveOpponentColor();
    }

    private String resolveColorForSideB() {
        return game.isPlayerA ? resolveOpponentColor() : resolvePlayerColor();
    }

    private Texture textureForSideA() {
        return game.isPlayerA ? playerCycleTexture : opponentCycleTexture;
    }

    private Texture textureForSideB() {
        return game.isPlayerA ? opponentCycleTexture : playerCycleTexture;
    }

    private String normalizeCycleColor(String colorName) {
        if (colorName == null || colorName.isBlank()) {
            return "white";
        }
        String normalized = colorName.trim().toLowerCase();
        return switch (normalized) {
            case "green", "blue", "orange", "red", "yellow", "purple", "cyan", "pink", "white", "black" -> normalized;
            default -> "white";
        };
    }

    private void drawCycleOrExplosion(int gridX, int gridY, char dir, Texture cycleTexture, boolean hidden,
            float explosionX, float explosionY, long explosionStartMs) {
        float drawSize = cycleDrawSize();
        float drawX = gridX * CELL + (CELL * 0.5f) - (drawSize * 0.5f);
        float drawY = gridY * CELL + (CELL * 0.5f) - (drawSize * 0.5f);
        if (!hidden && cycleTexture != null) {
            spriteBatch.draw(cycleTexture, drawX, drawY, drawSize * 0.5f, drawSize * 0.5f,
                    drawSize, drawSize, 1f, 1f, directionRotation(dir), 0, 0,
                    cycleTexture.getWidth(), cycleTexture.getHeight(), false, false);
            return;
        }
        if (explosionStartMs > 0L) {
            Texture explosionTexture = System.currentTimeMillis() - explosionStartMs < EXPLOSION_SWAP_MS
                    ? explosionTexture1
                    : explosionTexture2;
            spriteBatch.draw(explosionTexture, explosionX, explosionY, drawSize, drawSize);
        }
    }

    private float directionRotation(char dir) {
        return switch (dir) {
            case 'R' -> -90f;
            case 'L' -> 90f;
            case 'U' -> 180f;
            default -> 0f;
        };
    }

    private void updateExplosionState() {
        if (!"COLLISION".equalsIgnoreCase(game.latestRoundEventType)) {
            return;
        }
        long now = System.currentTimeMillis();
        String winnerSide = game.latestWinnerSide == null ? "NONE" : game.latestWinnerSide;
        if ("A".equalsIgnoreCase(winnerSide)) {
            hideCycleB = true;
            setExplosionForB(now);
        } else if ("B".equalsIgnoreCase(winnerSide)) {
            hideCycleA = true;
            setExplosionForA(now);
        } else {
            hideCycleA = true;
            hideCycleB = true;
            setExplosionForA(now);
            setExplosionForB(now);
        }
    }

    private void setExplosionForA(long now) {
        explosionAStartMs = now;
        explosionAx = collisionDrawX(game.ax, game.aDir);
        explosionAy = collisionDrawY(game.ay, game.aDir);
    }

    private void setExplosionForB(long now) {
        explosionBStartMs = now;
        explosionBx = collisionDrawX(game.bx, game.bDir);
        explosionBy = collisionDrawY(game.by, game.bDir);
    }

    private float collisionDrawX(int gridX, char dir) {
        return (gridX + directionDeltaX(dir)) * CELL + (CELL * 0.5f) - (cycleDrawSize() * 0.5f);
    }

    private float collisionDrawY(int gridY, char dir) {
        return (gridY + directionDeltaY(dir)) * CELL + (CELL * 0.5f) - (cycleDrawSize() * 0.5f);
    }

    private int directionDeltaX(char dir) {
        return switch (dir) {
            case 'L' -> -1;
            case 'R' -> 1;
            default -> 0;
        };
    }

    private int directionDeltaY(char dir) {
        return switch (dir) {
            case 'D' -> 1;
            case 'U' -> -1;
            default -> 0;
        };
    }

    private void resetCycleEffects() {
        hideCycleA = false;
        hideCycleB = false;
        explosionAStartMs = -1L;
        explosionBStartMs = -1L;
    }
}
