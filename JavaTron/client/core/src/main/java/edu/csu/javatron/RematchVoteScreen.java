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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
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

public class RematchVoteScreen extends ScreenAdapter {
    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private float timer = 30.0f;
    private boolean voted = false;
    private Label timerLabel;
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
    private float pulseTime = 0;

    public RematchVoteScreen(JavaTronGame game) {
        this.game = game;
        stage = new Stage(new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);
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

        Label resLabel = new Label(
                game.winnerName != null && !game.winnerName.isEmpty() ? game.winnerName : "MATCH END", skin);
        resLabel.setFontScale(1.2f);
        table.add(resLabel).padBottom(30).row();

        // Scores
        Table scoreT = new Table();
        Label p1L = new Label(game.isPlayerA ? "You (A)" : game.oppName + " (A)", skin);
        scoreT.add(p1L).padRight(10);
        Label s1L = new Label(": " + game.aWins, skin);
        s1L.setColor(CycleColors.get(resolveColorForSideA(), Color.WHITE));
        scoreT.add(s1L).row();

        Label p2L = new Label(game.isPlayerA ? game.oppName + " (B)" : "You (B)", skin);
        scoreT.add(p2L).padRight(10);
        Label s2L = new Label(": " + game.bWins, skin);
        s2L.setColor(CycleColors.get(resolveColorForSideB(), Color.WHITE));
        scoreT.add(s2L).row();
        table.add(scoreT).padBottom(30).row();

        TextButton yesBtn = new TextButton("YES (Rematch)", skin, "yellow");
        yesBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                if (!voted) {
                    voted = true;
                    game.rematchVoteYesPending = true;
                    game.playMenuConfirmSound();
                    game.getNetworkClient().send("C_REMATCH_VOTE|YES");
                }
            }
        });
        registerMenuButton(yesBtn);
        table.add(yesBtn).width(300).height(60).padBottom(15).row();

        TextButton noBtn = new TextButton("NO (Quit)", skin);
        noBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                if (!voted) {
                    voted = true;
                    game.rematchVoteYesPending = false;
                    game.playMenuConfirmSound();
                    game.getNetworkClient().send("C_REMATCH_VOTE|NO");
                    game.getNetworkClient().disconnect();
                    game.showMainMenu();
                }
            }
        });
        registerMenuButton(noBtn);
        table.add(noBtn).width(300).height(60).padBottom(20).row();

        timerLabel = new Label("30s...", skin);
        table.add(timerLabel);

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
                if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                    if (!voted && menuButtons.size() > 1) {
                        setSelectedIndex(1, false);
                        activateSelection();
                    }
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

        if (!voted) {
            timer -= delta;
            timerLabel.setText(Math.max(0, (int) Math.ceil(timer)) + "s...");
            if (timer <= 0) {
                voted = true;
                game.rematchVoteYesPending = false;
                game.getNetworkClient().send("C_REMATCH_VOTE|NO");
                game.getNetworkClient().disconnect();
                game.showMainMenu();
                return;
            }
        } else {
            timerLabel.setText("Waiting for server...");
        }

        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float wW = stage.getViewport().getWorldWidth();
        float wH = stage.getViewport().getWorldHeight();
        float uRepeat = MenuVisuals.backgroundURepeat(wW, bgTexture.getWidth());
        float vRepeat = MenuVisuals.backgroundVRepeat(wH, bgTexture.getHeight());
        spriteBatch.draw(bgTexture, 0, 0, wW, wH, bgScrollX, bgScrollY, bgScrollX + uRepeat, bgScrollY + vRepeat);
        float logoW = 380;
        float logoH = logoW * ((float) logoTexture.getHeight() / logoTexture.getWidth());
        spriteBatch.draw(logoTexture, wW / 2 - logoW / 2, wH - logoH - 50, logoW, logoH);
        spriteBatch.end();

        if (MenuVisuals.ENABLE_GLOW) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            shapeRenderer.setProjectionMatrix(stage.getViewport().getCamera().combined);
            shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);
            int gridSpacing = 64;
            for (int pass = 0; pass < 4; pass++) {
                float alpha = (0.2f / (pass + 1)) * pulse;
                for (int i = 0; i < (wW / gridSpacing) + 1; i++) {
                    float x = i * gridSpacing;
                    float t = (float) i / (float) (wW / gridSpacing);
                    shapeRenderer.setColor(0.1f * (1 - t) + 1f * t, 0.8f * (1 - t) + 0.1f * t,
                            1f * (1 - t) + 0.8f * t, alpha);
                    shapeRenderer.line(x, 0, x, wH);
                }
                for (int i = 0; i < (wH / gridSpacing) + 2; i++) {
                    float y = i * gridSpacing;
                    shapeRenderer.setColor(1.0f, 0.1f, 0.7f, alpha);
                    shapeRenderer.line(0, y, wW, y);
                }
            }
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
        if (WindowAspectEnforcer.enforce(w, h)) {
            return;
        }
        stage.getViewport().update(w, h, true);
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
