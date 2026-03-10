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
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class LobbyScreen extends ScreenAdapter {
    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private Music music;
    private Label playerCountLabel;
    private com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private com.badlogic.gdx.graphics.Texture bgTexture;
    private com.badlogic.gdx.graphics.Texture logoTexture;
    private ShapeRenderer shapeRenderer;
    private float pulseTime = 0;

    public LobbyScreen(JavaTronGame game) {
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

        table.add(new Label("IN LOBBY", skin)).padBottom(10).row();
        table.add(new Label("Waiting for players...", skin)).padBottom(40).row();

        playerCountLabel = new Label("There are " + Math.max(1, game.lobbyPlayerCount) + " user(s)", skin);
        table.add(playerCountLabel).padBottom(40).row();

        try {
            music = Gdx.audio.newMusic(Gdx.files.internal("snd/mus_menu.mp3"));
            music.setLooping(true);
            if (SettingsScreen.isAudioEnabled())
                music.play();
        } catch (Exception e) {
        }

        TextButton practiceBtn = new TextButton("Bot Practice", skin);
        practiceBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                game.showGameScreen();
            }
        });
        table.add(practiceBtn).width(250).height(60).padBottom(20).row();

        TextButton quitBtn = new TextButton("Quit to Menu", skin, "yellow");
        quitBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                game.getNetworkClient().disconnect();
                game.showMainMenu();
            }
        });
        table.add(quitBtn).width(250).height(60).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        pulseTime += delta;
        float pulse = 0.7f + 0.3f * (float) Math.sin(pulseTime * 2.5f);
        playerCountLabel.setText("There are " + Math.max(1, game.lobbyPlayerCount) + " user(s)");

        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();
        spriteBatch.draw(bgTexture, 0, 0, worldWidth, worldHeight, 0, 0, worldWidth / bgTexture.getWidth(),
                worldHeight / bgTexture.getHeight());
        float logoW = 380;
        float logoH = logoW * ((float) logoTexture.getHeight() / logoTexture.getWidth());
        spriteBatch.draw(logoTexture, worldWidth / 2 - logoW / 2, worldHeight - logoH - 50, logoW, logoH);
        spriteBatch.end();

        // Pulsing Grid
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        shapeRenderer.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);
        int gridSpacing = 64;
        for (int pass = 0; pass < 4; pass++) {
            float alpha = (0.2f / (pass + 1)) * pulse;
            for (int i = 0; i < (worldWidth / gridSpacing) + 1; i++) {
                float x = i * gridSpacing;
                float t = (float) i / (float) (worldWidth / gridSpacing);
                shapeRenderer.setColor(0.1f * (1 - t) + 1f * t, 0.8f * (1 - t) + 0.1f * t, 1f * (1 - t) + 0.8f * t,
                        alpha);
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

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override
    public void dispose() {
        if (music != null) {
            music.stop();
            music.dispose();
        }
        stage.dispose();
        skin.dispose();
        spriteBatch.dispose();
        bgTexture.dispose();
        logoTexture.dispose();
        shapeRenderer.dispose();
    }
}