/*
 * AI Tools Use Transparency Disclosure:
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.List;

public class PracticeRematchScreen extends ScreenAdapter {
    private final JavaTronGame game;
    private final Stage stage;
    private final Skin skin;
    private final ShapeRenderer shapeRenderer;
    private final TextButtonStyle defaultButtonStyle;
    private final TextButtonStyle yellowButtonStyle;
    private final List<TextButton> menuButtons = new ArrayList<>();
    private int selectedIndex = -1;
    private float bgScrollX = 0f;
    private float bgScrollY = 0f;
    private float pulseTime = 0f;
    private final com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private final com.badlogic.gdx.graphics.Texture bgTexture;
    private final com.badlogic.gdx.graphics.Texture logoTexture;

    public PracticeRematchScreen(JavaTronGame game) {
        this.game = game;
        stage = new Stage(new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT));
        shapeRenderer = new ShapeRenderer();

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);
        skin.add("default-font", font);

        defaultButtonStyle = new TextButtonStyle();
        defaultButtonStyle.font = font;
        defaultButtonStyle.fontColor = Color.WHITE;
        defaultButtonStyle.downFontColor = Color.YELLOW;
        skin.add("default", defaultButtonStyle);

        yellowButtonStyle = new TextButtonStyle();
        yellowButtonStyle.font = font;
        yellowButtonStyle.fontColor = Color.YELLOW;
        yellowButtonStyle.downFontColor = Color.CYAN;
        skin.add("yellow", yellowButtonStyle);

        skin.add("default", new LabelStyle(font, Color.WHITE));

        spriteBatch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
        bgTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/tile/bgtile_indigo.png"));
        bgTexture.setWrap(com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat,
                com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat);
        logoTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/menu/title.png"));

        Table table = new Table();
        table.setFillParent(true);
        table.padTop(120);
        stage.addActor(table);

        Label resultLabel = new Label(game.winnerName != null && !game.winnerName.isBlank()
                ? game.winnerName : "PRACTICE MATCH END", skin);
        resultLabel.setFontScale(1.2f);
        table.add(resultLabel).padBottom(30).row();

        Label detailLabel = new Label("Final score versus bot", skin);
        table.add(detailLabel).padBottom(20).row();

        Table scoreTable = new Table();
        Label playerLabel = new Label(game.playerName + ":", skin);
        scoreTable.add(playerLabel).padRight(10);
        Label playerScore = new Label(String.valueOf(game.aWins), skin);
        playerScore.setColor(CycleColors.get(game.playerColor, Color.WHITE));
        scoreTable.add(playerScore).row();

        Label botLabel = new Label("Bot:", skin);
        scoreTable.add(botLabel).padRight(10);
        Label botScore = new Label(String.valueOf(game.bWins), skin);
        botScore.setColor(CycleColors.get(game.oppColor, Color.WHITE));
        scoreTable.add(botScore).row();
        table.add(scoreTable).padBottom(30).row();

        TextButton yesBtn = new TextButton("YES (Rematch)", skin, "yellow");
        yesBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.playMenuConfirmSound();
                game.playNewGameSound();
                game.restartPracticeGame();
            }
        });
        registerMenuButton(yesBtn);
        table.add(yesBtn).width(300).height(60).padBottom(15).row();

        String exitLabel = game.practiceStartedFromLobby ? "NO (Return to Lobby)" : "NO (Main Menu)";
        TextButton noBtn = new TextButton(exitLabel, skin);
        noBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.playMenuConfirmSound();
                game.practiceMode = false;
                if (game.practiceStartedFromLobby) {
                    game.showLobbyScreen();
                } else {
                    game.showMainMenu();
                }
            }
        });
        registerMenuButton(noBtn);
        table.add(noBtn).width(300).height(60).padBottom(20).row();

        Label hintLabel = new Label("Press Y or N", skin);
        table.add(hintLabel);

        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == com.badlogic.gdx.Input.Keys.Y) {
                    setSelectedIndex(0, false);
                    activateSelection();
                    return true;
                }
                if (keycode == com.badlogic.gdx.Input.Keys.N || keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                    setSelectedIndex(1, false);
                    activateSelection();
                    return true;
                }
                if (isMenuUp(keycode)) {
                    moveSelection(-1);
                    return true;
                }
                if (isMenuDown(keycode)) {
                    moveSelection(1);
                    return true;
                }
                return false;
            }
        });
        multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void show() {
        game.playMenuMusic();
        game.playVotingStartSound();
        setSelectedIndex(0, false);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        bgScrollX += delta * MenuVisuals.PARALLAX_SCROLL_X_SPEED;
        bgScrollY += delta * MenuVisuals.PARALLAX_SCROLL_Y_SPEED;
        pulseTime += delta;
        float pulse = 0.7f + 0.3f * (float) Math.sin(pulseTime * 2.5f);

        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();
        float uRepeat = MenuVisuals.backgroundURepeat(worldWidth, bgTexture.getWidth());
        float vRepeat = MenuVisuals.backgroundVRepeat(worldHeight, bgTexture.getHeight());
        spriteBatch.draw(bgTexture, 0, 0, worldWidth, worldHeight,
                bgScrollX, bgScrollY, bgScrollX + uRepeat, bgScrollY + vRepeat);
        float logoWidth = 380f;
        float logoHeight = logoWidth * ((float) logoTexture.getHeight() / logoTexture.getWidth());
        spriteBatch.draw(logoTexture, worldWidth / 2f - logoWidth / 2f, worldHeight - logoHeight - 50f,
                logoWidth, logoHeight);
        spriteBatch.end();

        if (MenuVisuals.ENABLE_GLOW) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            shapeRenderer.setProjectionMatrix(stage.getViewport().getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            int gridSpacing = 64;
            for (int pass = 0; pass < 4; pass++) {
                float alpha = (0.2f / (pass + 1)) * pulse;
                for (int i = 0; i < (worldWidth / gridSpacing) + 1; i++) {
                    float x = i * gridSpacing;
                    float t = (float) i / (float) (worldWidth / gridSpacing);
                    shapeRenderer.setColor(0.1f * (1 - t) + 1f * t, 0.8f * (1 - t) + 0.1f * t,
                            1f * (1 - t) + 0.8f * t, alpha);
                    shapeRenderer.line(x, 0, x, worldHeight);
                }
                for (int i = 0; i < (worldHeight / gridSpacing) + 2; i++) {
                    float y = i * gridSpacing;
                    shapeRenderer.setColor(1.0f, 0.1f, 0.7f, alpha);
                    shapeRenderer.line(0, y, worldWidth, y);
                }
            }
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (WindowAspectEnforcer.enforce(width, height)) {
            return;
        }
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        spriteBatch.dispose();
        bgTexture.dispose();
        logoTexture.dispose();
        shapeRenderer.dispose();
    }

    private void registerMenuButton(TextButton button) {
        final int index = menuButtons.size();
        menuButtons.add(button);
        button.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                setSelectedIndex(index, true);
            }
        });
    }

    private boolean isMenuUp(int keycode) {
        return keycode == com.badlogic.gdx.Input.Keys.UP || keycode == game.upKey;
    }

    private boolean isMenuDown(int keycode) {
        return keycode == com.badlogic.gdx.Input.Keys.DOWN || keycode == game.downKey;
    }

    private void moveSelection(int delta) {
        if (menuButtons.isEmpty()) return;
        int next = selectedIndex < 0 ? 0 : (selectedIndex + delta + menuButtons.size()) % menuButtons.size();
        setSelectedIndex(next, true);
    }

    private void setSelectedIndex(int index, boolean playSound) {
        if (index < 0 || index >= menuButtons.size()) return;
        if (selectedIndex == index) return;
        selectedIndex = index;
        for (int i = 0; i < menuButtons.size(); i++) {
            menuButtons.get(i).setStyle(i == selectedIndex ? yellowButtonStyle : defaultButtonStyle);
        }
        if (playSound) {
            game.playMenuNavigateSound();
        }
    }

    private void activateSelection() {
        if (selectedIndex < 0 || selectedIndex >= menuButtons.size()) return;
        menuButtons.get(selectedIndex).fire(new ChangeListener.ChangeEvent());
    }
}
