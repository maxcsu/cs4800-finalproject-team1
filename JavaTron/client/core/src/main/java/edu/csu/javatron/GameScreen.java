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

import java.util.HashSet;
import java.util.Set;

/** Game screen for rendering the match and handling input. */
public class GameScreen extends ScreenAdapter {
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
    private Sound turnSound;

    // Arena constants — must match server Protocol.java
    private static final int ARENA_COLS = 48;
    private static final int ARENA_ROWS = 80;
    private static final int CELL = 10; // pixels per cell

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
        shapeRenderer = new ShapeRenderer();
        aTrails = new HashSet<>();
        bTrails = new HashSet<>();
        font = new BitmapFont();

        // Load gameplay music and sound effects
        try {
            gameMusic = Gdx.audio.newMusic(Gdx.files.internal("snd/mus_gameplay.mp3"));
            gameMusic.setLooping(true);
            if (SettingsScreen.isAudioEnabled()) {
                gameMusic.play();
            }
        } catch (Exception e) {
            System.out.println("Could not load game music: " + e.getMessage());
        }

        try {
            turnSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_cycleturn.mp3"));
        } catch (Exception e) {
            System.out.println("Could not load turn sound: " + e.getMessage());
        }
    }

    public void updateTrails() {
        // Clear trails when a new round starts (positions jump back to spawn)
        if (game.roundNumber != prevRoundNumber) {
            aTrails.clear();
            bTrails.clear();
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

    @Override
    public void render(float delta) {

        updateTrails();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        spriteBatch.setProjectionMatrix(camera.combined);

        // Draw grid across full world
        spriteBatch.begin();
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        float uRepeat = worldWidth / gridTexture.getWidth();
        float vRepeat = worldHeight / gridTexture.getHeight();
        spriteBatch.draw(gridTexture, 0, 0, worldWidth, worldHeight, 0, 0, uRepeat, vRepeat);
        spriteBatch.end();

        // Helper method to convert String color to LibGDX Color
        Color colorA = getColorFromString(game.isPlayerA ? game.playerColor : game.oppColor, Color.BLUE);
        Color colorB = getColorFromString(game.isPlayerA ? game.oppColor : game.playerColor, Color.RED);

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

        // Draw cycles
        shapeRenderer.setColor(colorA);
        shapeRenderer.rect(game.ax * CELL, game.ay * CELL, CELL, CELL);
        shapeRenderer.setColor(colorB);
        shapeRenderer.rect(game.bx * CELL, game.by * CELL, CELL, CELL);
        shapeRenderer.end();

        // Draw arena border walls (white outline) — this is the deadly edge!
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(0, 0, ARENA_COLS * CELL, ARENA_ROWS * CELL);
        shapeRenderer.end();

        // === Compact scoreboard in top-left corner ===
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

        spriteBatch.begin();
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

        // Large countdown in center of screen
        String msg = game.countdownMessage;
        if (msg != null) {
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
        if (goDisplayTimer > 0f) {
            goDisplayTimer -= delta;
            if (goDisplayTimer <= 0f) {
                game.countdownMessage = null;
                game.countdownActive = false;
            }
        }
        spriteBatch.end();

        // === INPUT: custom bind keys + Arrow key fallback ===
        // IMPORTANT: Server direction convention (libGDX y-up):
        // 'D' = +y = UP on screen 'U' = -y = DOWN on screen 'L'/'R' = left/right
        boolean upPressed = Gdx.input.isKeyJustPressed(game.upKey)
                || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP);
        boolean downPressed = Gdx.input.isKeyJustPressed(game.downKey)
                || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN);
        boolean leftPressed = Gdx.input.isKeyJustPressed(game.leftKey)
                || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.LEFT);
        boolean rightPressed = Gdx.input.isKeyJustPressed(game.rightKey)
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
            game.getNetworkClient().send("C_TURN|" + turnDir);
            if (turnSound != null && SettingsScreen.isAudioEnabled())
                turnSound.play(0.5f);
        }
    }

    private Color getColorFromString(String colorStr, Color defaultColor) {
        if (colorStr == null)
            return defaultColor;
        switch (colorStr.toUpperCase()) {
            case "GREEN":
                return Color.GREEN;
            case "BLUE":
                return Color.BLUE;
            case "ORANGE":
                return Color.ORANGE;
            case "RED":
                return Color.RED;
            case "YELLOW":
                return Color.YELLOW;
            case "PURPLE":
                return Color.PURPLE;
            case "CYAN":
                return Color.CYAN;
            case "PINK":
                return Color.PINK;
            case "WHITE":
                return Color.WHITE;
            case "BLACK":
                return Color.BLACK;
            default:
                return defaultColor;
        }
    }

    @Override
    public void resize(int width, int height) {
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
        if (turnSound != null) {
            turnSound.dispose();
        }
    }
}