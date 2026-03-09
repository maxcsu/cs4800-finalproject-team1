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
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Lobby screen while waiting for a match. */
public class LobbyScreen extends ScreenAdapter {
    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private Music music;
    private Label playerCountLabel;
    private com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private com.badlogic.gdx.graphics.Texture bgTexture;
    private com.badlogic.gdx.graphics.Texture logoTexture;

    public LobbyScreen(JavaTronGame game) {
        this.game = game;
        stage = new Stage(new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        // Matchmaking joined via ConnectScreen

        // Create a simple skin programmatically
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

        LabelStyle labelStyle = new LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        spriteBatch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
        bgTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/tile/bgtile_indigo.png"));
        bgTexture.setWrap(com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat,
                com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat);
        logoTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/menu/title.png"));

        Table table = new Table();
        table.setFillParent(true);
        table.padTop(100);
        stage.addActor(table);

        // Title
        Label titleLabel = new Label("You are in the lobby.", skin);
        table.add(titleLabel).padBottom(10).row();

        Label waitingLabel = new Label("Waiting for players...", skin);
        table.add(waitingLabel).padBottom(40).row();

        // Player count label
        playerCountLabel = new Label("There are " + Math.max(1, game.lobbyPlayerCount) + " user(s)", skin);
        table.add(playerCountLabel).padBottom(10).row();
        Label waitingToPlayLabel = new Label("waiting to play.", skin);
        table.add(waitingToPlayLabel).padBottom(50).row();

        // Music
        try {
            music = Gdx.audio.newMusic(Gdx.files.internal("snd/mus_menu.mp3"));
            music.setLooping(true);
            if (SettingsScreen.isAudioEnabled()) {
                music.play();
            }
        } catch (Exception e) {
            System.out.println("Could not load menu music: " + e.getMessage());
        }

        // Bot Practice button
        TextButton practiceButton = new TextButton("Bot Practice", skin);
        practiceButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showGameScreen();
            }
        });
        table.add(practiceButton).width(250).height(60).padBottom(20).row();

        // Quit button
        TextButton quitButton = new TextButton("Quit to Menu", skin, "yellow");
        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getNetworkClient().disconnect();
                game.showMainMenu();
            }
        });
        table.add(quitButton).width(250).height(60).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        playerCountLabel.setText("There are " + Math.max(1, game.lobbyPlayerCount) + " user(s)");

        // Draw background and logo
        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();
        float uRepeat = worldWidth / bgTexture.getWidth();
        float vRepeat = worldHeight / bgTexture.getHeight();
        spriteBatch.draw(bgTexture, 0, 0, worldWidth, worldHeight, 0, 0, uRepeat, vRepeat);

        float logoWidth = 400; // approximate target width
        float logoHeight = logoWidth * ((float) logoTexture.getHeight() / logoTexture.getWidth());
        spriteBatch.draw(logoTexture, worldWidth / 2 - logoWidth / 2, worldHeight - logoHeight - 60, logoWidth,
                logoHeight);
        spriteBatch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (music != null) {
            music.stop();
            music.dispose();
        }
        stage.dispose();
        skin.dispose();
        if (spriteBatch != null)
            spriteBatch.dispose();
        if (bgTexture != null)
            bgTexture.dispose();
        if (logoTexture != null)
            logoTexture.dispose();
    }
}