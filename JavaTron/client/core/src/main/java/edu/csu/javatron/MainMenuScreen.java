/*
 * AI Tools Use Transparency Disclosure:
 * Primary prior GitHub handling credit: Bhawna Gogna.
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayList;
import java.util.List;

/** Main menu screen with options to connect, settings, or exit. */
public class MainMenuScreen extends ScreenAdapter {
    private static final float MENU_FADE_IN_DURATION = 0.4f;

    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private com.badlogic.gdx.graphics.Texture bgTexture;
    private com.badlogic.gdx.graphics.Texture logoTexture;
    private ShapeRenderer shapeRenderer;
    private TextButtonStyle defaultButtonStyle;
    private TextButtonStyle yellowButtonStyle;
    private final List<TextButton> menuButtons = new ArrayList<>();
    private int selectedIndex = -1;
    private float bgScrollX = 0f;
    private float bgScrollY = 0f;
    private float introTime = 0f;
    private float pulseTime = 0;

    public MainMenuScreen(JavaTronGame game) {
        this.game = game;
        stage = new Stage(new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);
        skin.add("default-font", font);

        defaultButtonStyle = new TextButtonStyle();
        defaultButtonStyle.font = font;
        defaultButtonStyle.fontColor = Color.WHITE;
        defaultButtonStyle.downFontColor = Color.YELLOW;
        skin.add("default", defaultButtonStyle);

        this.yellowButtonStyle = new TextButtonStyle();
        this.yellowButtonStyle.font = font;
        this.yellowButtonStyle.fontColor = Color.YELLOW;
        this.yellowButtonStyle.downFontColor = Color.CYAN;
        skin.add("yellow", this.yellowButtonStyle);

        spriteBatch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
        bgTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/tile/bgtile_indigo.png"));
        bgTexture.setWrap(com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat,
                com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat);
        logoTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/menu/title.png"));
        shapeRenderer = new ShapeRenderer();

        Table table = new Table();
        table.setFillParent(true);
        table.padTop(100);
        stage.addActor(table);

        TextButton singleplayerButton = new TextButton("Singleplayer Game", skin, "yellow");
        singleplayerButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                game.playMenuConfirmSound();
                game.startSingleplayerGame();
            }
        });
        registerMenuButton(singleplayerButton);
        table.add(singleplayerButton).padBottom(20).row();

        TextButton joinServerButton = new TextButton("Join Server", skin, "yellow");
        joinServerButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                game.playMenuConfirmSound();
                game.showConnectScreen();
            }
        });
        registerMenuButton(joinServerButton);
        table.add(joinServerButton).padBottom(20).row();

        TextButton settingsButton = new TextButton("Settings", skin);
        settingsButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                game.playMenuConfirmSound();
                game.showSettingsScreen();
            }
        });
        registerMenuButton(settingsButton);
        table.add(settingsButton).padBottom(20).row();

        TextButton exitButton = new TextButton("Exit", skin);
        exitButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                game.playDisconnectSound();
                Gdx.app.exit();
            }
        });
        registerMenuButton(exitButton);
        table.add(exitButton).row();

        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (isMenuUp(keycode)) {
                    moveSelection(-1);
                    return true;
                }
                if (isMenuDown(keycode)) {
                    moveSelection(1);
                    return true;
                }
                if (keycode == com.badlogic.gdx.Input.Keys.ENTER) {
                    activateSelection();
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
        bgScrollX = game.sharedMenuScrollX;
        bgScrollY = game.sharedMenuScrollY;
        introTime = 0f;
        setSelectedIndex(0, false);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        bgScrollX += delta * MenuVisuals.PARALLAX_SCROLL_X_SPEED;
        bgScrollY += delta * MenuVisuals.PARALLAX_SCROLL_Y_SPEED;
        game.sharedMenuScrollX = bgScrollX;
        game.sharedMenuScrollY = bgScrollY;
        introTime = Math.min(MENU_FADE_IN_DURATION, introTime + delta);
        pulseTime += delta;
        float pulse = 0.7f + 0.3f * (float) Math.sin(pulseTime * 2.5f);
        float introAlpha = introTime / MENU_FADE_IN_DURATION;

        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();

        float uRepeat = MenuVisuals.backgroundURepeat(worldWidth, bgTexture.getWidth());
        float vRepeat = MenuVisuals.backgroundVRepeat(worldHeight, bgTexture.getHeight());
        spriteBatch.draw(bgTexture, 0, 0, worldWidth, worldHeight, bgScrollX, bgScrollY,
                bgScrollX + uRepeat, bgScrollY + vRepeat);
        spriteBatch.end();

        // 2. Draw the "Glow" layers using additive blending
        if (MenuVisuals.ENABLE_GLOW) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE); // Additive blending
            shapeRenderer.setProjectionMatrix(stage.getViewport().getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            int gridSpacing = 64;
            int horizontalLines = (int) (worldHeight / gridSpacing) + 2;
            int verticalLines = (int) (worldWidth / gridSpacing) + 1;

            // More passes for a stronger "bloom" effect
            for (int pass = 0; pass < 5; pass++) {
                float alpha = (0.28f / (pass + 1)) * pulse;

                // Vertical lines - Blue to Pink horizontal gradient
                for (int i = 0; i < verticalLines; i++) {
                    float x = i * gridSpacing;
                    float t = (float) i / verticalLines;
                    float r = 0.1f * (1 - t) + 1.0f * t;
                    float g = 0.8f * (1 - t) + 0.1f * t;
                    float b = 1.0f * (1 - t) + 0.8f * t;
                    shapeRenderer.setColor(r, g, b, alpha);

                    for (float off = -pass * 0.8f; off <= pass * 0.8f; off += 1.0f) {
                        shapeRenderer.line(x + off, 0, x + off, worldHeight);
                    }
                }

                // Horizontal lines (Static Pink)
                for (int i = 0; i < horizontalLines; i++) {
                    float y = (i * gridSpacing);
                    shapeRenderer.setColor(1.0f, 0.1f, 0.7f, alpha);
                    if (y >= -20 && y <= worldHeight + 20) {
                        for (float off = -pass * 0.8f; off <= pass * 0.8f; off += 1.0f) {
                            shapeRenderer.line(0, y + off, worldWidth, y + off);
                        }
                    }
                }
            }
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        spriteBatch.begin();
        spriteBatch.setColor(1f, 1f, 1f, introAlpha);
        float logoWidth = 450;
        float logoHeight = logoWidth * ((float) logoTexture.getHeight() / logoTexture.getWidth());
        spriteBatch.draw(logoTexture, worldWidth / 2 - logoWidth / 2, worldHeight - logoHeight - 60, logoWidth,
                logoHeight);
        spriteBatch.setColor(Color.WHITE);
        spriteBatch.end();

        stage.getRoot().getColor().a = introAlpha;
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
