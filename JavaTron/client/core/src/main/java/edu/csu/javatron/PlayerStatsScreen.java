package edu.csu.javatron;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.List;

public class PlayerStatsScreen extends ScreenAdapter {
    private static final float PLAYER_COL_WIDTH = 148f;
    private static final float LOSE_RATE_COL_WIDTH = 102f;
    private static final float WIN_RATE_COL_WIDTH = 94f;
    private static final float DEREZZES_COL_WIDTH = 88f;

    private final JavaTronGame game;
    private final Stage stage;
    private final Skin skin;
    private final SpriteBatch spriteBatch;
    private final Texture bgTexture;
    private final Texture logoTexture;
    private final ShapeRenderer shapeRenderer;
    private final TextButtonStyle defaultButtonStyle;
    private final TextButtonStyle yellowButtonStyle;
    private final List<TextButton> menuButtons = new ArrayList<>();
    private final Table rowsTable;
    private final Label statusLabel;
    private int selectedIndex = -1;
    private int renderedLeaderboardVersion = -1;
    private float bgScrollX = 0f;
    private float bgScrollY = 0f;
    private float pulseTime = 0f;

    public PlayerStatsScreen(JavaTronGame game) {
        this.game = game;
        this.stage = new Stage(new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT));
        this.skin = new Skin();
        this.shapeRenderer = new ShapeRenderer();
        this.spriteBatch = new SpriteBatch();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.35f);
        skin.add("default-font", font);
        skin.add("default", new Label.LabelStyle(font, Color.WHITE));

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

        bgTexture = new Texture(Gdx.files.internal("gfx/tile/bgtile_indigo.png"));
        bgTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        logoTexture = new Texture(Gdx.files.internal("gfx/menu/title.png"));

        Table root = new Table();
        root.setFillParent(true);
        root.padTop(128);
        root.padLeft(12);
        root.padRight(12);
        stage.addActor(root);

        Label titleLabel = new Label("More Player Stats", skin);
        titleLabel.setFontScale(1.6f);
        titleLabel.setAlignment(Align.center);
        root.add(titleLabel).padBottom(16).row();

        statusLabel = new Label("", skin);
        statusLabel.setAlignment(Align.center);
        root.add(statusLabel).padBottom(14).row();

        rowsTable = new Table();
        rowsTable.defaults().padBottom(8).padRight(12).left();
        root.add(rowsTable).expandY().top().row();

        TextButton backBtn = new TextButton("Back", skin);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.playMenuBackSound();
                game.showLeaderboardScreen();
            }
        });
        registerMenuButton(backBtn);
        root.add(backBtn).width(220).height(60).padTop(10).padBottom(12).row();

        TextButton returnBtn = new TextButton("Return to Lobby", skin, "yellow");
        returnBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.playMenuBackSound();
                game.showLobbyScreen();
            }
        });
        registerMenuButton(returnBtn);
        root.add(returnBtn).width(280).height(60).padTop(18);

        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                    game.playMenuBackSound();
                    game.showLeaderboardScreen();
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
        renderedLeaderboardVersion = -1;
        setSelectedIndex(0, false);
        if (!game.leaderboardLoading && game.leaderboardEntries.isEmpty()) {
            game.requestLeaderboardRefresh();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        bgScrollX += delta * MenuVisuals.PARALLAX_SCROLL_X_SPEED;
        bgScrollY += delta * MenuVisuals.PARALLAX_SCROLL_Y_SPEED;
        game.sharedMenuScrollX = bgScrollX;
        game.sharedMenuScrollY = bgScrollY;
        pulseTime += delta;

        if (renderedLeaderboardVersion != game.leaderboardVersion) {
            rebuildRows();
            renderedLeaderboardVersion = game.leaderboardVersion;
        }

        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();
        float uRepeat = MenuVisuals.backgroundURepeat(worldWidth, bgTexture.getWidth());
        float vRepeat = MenuVisuals.backgroundVRepeat(worldHeight, bgTexture.getHeight());
        spriteBatch.draw(bgTexture, 0, 0, worldWidth, worldHeight, bgScrollX, bgScrollY,
                bgScrollX + uRepeat, bgScrollY + vRepeat);
        float logoW = 360;
        float logoH = logoW * ((float) logoTexture.getHeight() / logoTexture.getWidth());
        spriteBatch.draw(logoTexture, worldWidth / 2 - logoW / 2, worldHeight - logoH - 48, logoW, logoH);
        spriteBatch.end();

        if (MenuVisuals.ENABLE_GLOW) {
            float pulse = 0.7f + 0.3f * (float) Math.sin(pulseTime * 2.5f);
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

    private void rebuildRows() {
        rowsTable.clearChildren();
        statusLabel.setText(game.leaderboardLoading ? "Loading leaderboard..." : game.leaderboardStatusMessage);

        List<LeaderboardEntry> entries = game.leaderboardEntries;
        if (entries.isEmpty()) {
            return;
        }

        rowsTable.add(buildHeaderCell("Player", PLAYER_COL_WIDTH)).left();
        rowsTable.add(buildHeaderCell("Loss Rate", LOSE_RATE_COL_WIDTH)).left();
        rowsTable.add(buildHeaderCell("Win Rate", WIN_RATE_COL_WIDTH)).left();
        rowsTable.add(buildHeaderCell("Derezzes", DEREZZES_COL_WIDTH)).left();
        rowsTable.row();

        for (LeaderboardEntry entry : entries) {
            rowsTable.add(buildValueCell(entry.getPlayerDisplay(), PLAYER_COL_WIDTH)).left();
            rowsTable.add(buildValueCell(entry.getLoseRateDisplay(), LOSE_RATE_COL_WIDTH)).left();
            rowsTable.add(buildValueCell(entry.getRateDisplay(), WIN_RATE_COL_WIDTH)).left();
            rowsTable.add(buildValueCell(entry.getDerezzesDisplay(), DEREZZES_COL_WIDTH)).left();
            rowsTable.row();
        }
    }

    private Label buildHeaderCell(String text, float width) {
        Label label = new Label(text, skin);
        label.setColor(Color.WHITE);
        label.setAlignment(Align.left);
        label.setWidth(width);
        return label;
    }

    private Label buildValueCell(String text, float width) {
        Label label = new Label(text, skin);
        label.setColor(Color.YELLOW);
        label.setAlignment(Align.left);
        label.setWidth(width);
        return label;
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

    private void setSelectedIndex(int index, boolean playSound) {
        if (index < 0 || index >= menuButtons.size()) {
            return;
        }
        if (selectedIndex == index) {
            return;
        }
        selectedIndex = index;
        for (int i = 0; i < menuButtons.size(); i++) {
            menuButtons.get(i).setStyle(i == selectedIndex ? yellowButtonStyle : defaultButtonStyle);
        }
        if (playSound) {
            game.playMenuNavigateSound();
        }
    }

    private void activateSelection() {
        if (selectedIndex < 0 || selectedIndex >= menuButtons.size()) {
            return;
        }
        menuButtons.get(selectedIndex).fire(new ChangeListener.ChangeEvent());
    }
}
