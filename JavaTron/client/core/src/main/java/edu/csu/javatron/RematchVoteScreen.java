package edu.csu.javatron;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;

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

        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.downFontColor = Color.YELLOW;
        skin.add("default", buttonStyle);

        TextButtonStyle yellowButtonStyle = new TextButtonStyle();
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
        s1L.setColor(getColorFromString(game.isPlayerA ? game.playerColor : game.oppColor));
        scoreT.add(s1L).row();

        Label p2L = new Label(game.isPlayerA ? game.oppName + " (B)" : "You (B)", skin);
        scoreT.add(p2L).padRight(10);
        Label s2L = new Label(": " + game.bWins, skin);
        s2L.setColor(getColorFromString(game.isPlayerA ? game.oppColor : game.playerColor));
        scoreT.add(s2L).row();
        table.add(scoreT).padBottom(30).row();

        TextButton yesBtn = new TextButton("YES (Rematch)", skin, "yellow");
        yesBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                if (!voted) {
                    voted = true;
                    game.getNetworkClient().send("C_REMATCH_VOTE|YES");
                    game.showLobbyScreen();
                }
            }
        });
        table.add(yesBtn).width(300).height(60).padBottom(15).row();

        TextButton noBtn = new TextButton("NO (Quit)", skin);
        noBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                if (!voted) {
                    voted = true;
                    game.getNetworkClient().send("C_REMATCH_VOTE|NO");
                    game.showMainMenu();
                }
            }
        });
        table.add(noBtn).width(300).height(60).padBottom(20).row();

        timerLabel = new Label("30s...", skin);
        table.add(timerLabel);
    }

    private Color getColorFromString(String col) {
        if (col == null)
            return Color.WHITE;
        switch (col.toUpperCase()) {
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
                return Color.WHITE;
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        pulseTime += delta;
        float pulse = 0.7f + 0.3f * (float) Math.sin(pulseTime * 2.5f);

        if (!voted) {
            timer -= delta;
            timerLabel.setText(Math.max(0, (int) Math.ceil(timer)) + "s...");
            if (timer <= 0) {
                voted = true;
                game.getNetworkClient().send("C_REMATCH_VOTE|NO");
                game.showMainMenu();
            }
        }

        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float wW = stage.getViewport().getWorldWidth();
        float wH = stage.getViewport().getWorldHeight();
        spriteBatch.draw(bgTexture, 0, 0, wW, wH, 0, 0, wW / bgTexture.getWidth(), wH / bgTexture.getHeight());
        float logoW = 380;
        float logoH = logoW * ((float) logoTexture.getHeight() / logoTexture.getWidth());
        spriteBatch.draw(logoTexture, wW / 2 - logoW / 2, wH - logoH - 50, logoW, logoH);
        spriteBatch.end();

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
                shapeRenderer.setColor(0.1f * (1 - t) + 1f * t, 0.8f * (1 - t) + 0.1f * t, 1f * (1 - t) + 0.8f * t,
                        alpha);
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

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
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
}
