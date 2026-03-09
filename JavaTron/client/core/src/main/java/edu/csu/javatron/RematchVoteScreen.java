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
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Rematch vote screen after match ends. */
public class RematchVoteScreen extends ScreenAdapter {
    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private float timer = 30.0f; // 30 seconds to vote
    private boolean voted = false;
    private Label timerLabel;
    private com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private com.badlogic.gdx.graphics.Texture bgTexture;
    private com.badlogic.gdx.graphics.Texture logoTexture;

    public RematchVoteScreen(JavaTronGame game) {
        this.game = game;
        stage = new Stage(new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);

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
        bgTexture.setWrap(com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat, com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat);
        logoTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/menu/title.png"));

        Table table = new Table();
        table.setFillParent(true);
        table.padTop(100);
        stage.addActor(table);

        // Result label
        Label resultLabel = new Label("Play again?", skin);
        resultLabel.setFontScale(1.2f);
        table.add(resultLabel).padBottom(40).row();

        // Player Labels
        Table p1Table = new Table();
        p1Table.add(new Label("Player ", skin)).padRight(5);
        Label oneLabel = new Label("One", skin);
        oneLabel.setColor(Color.BLUE);
        p1Table.add(oneLabel).padRight(5);
        p1Table.add(new Label(": ", skin));
        Label p1Score = new Label(game.aWins + "", skin);
        p1Score.setColor(Color.GREEN);
        p1Table.add(p1Score);
        table.add(p1Table).padBottom(10).row();

        Table p2Table = new Table();
        p2Table.add(new Label("Player ", skin)).padRight(5);
        Label twoLabel = new Label("Two", skin);
        twoLabel.setColor(Color.ORANGE);
        p2Table.add(twoLabel).padRight(5);
        p2Table.add(new Label(": ", skin));
        Label p2Score = new Label(game.bWins + "", skin);
        p2Score.setColor(Color.YELLOW);
        p2Table.add(p2Score);
        table.add(p2Table).padBottom(40).row();

        // Vote label
        Label voteLabel = new Label("Vote:", skin);
        table.add(voteLabel).padBottom(10).row();

        // Yes button
        TextButton yesButton = new TextButton("Yes (Rematch)", skin, "yellow");
        yesButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!voted) {
                    voted = true;
                    game.getNetworkClient().send("C_REMATCH_VOTE|YES");
                    game.showLobbyScreen(); // Back to lobby
                }
            }
        });
        table.add(yesButton).width(350).height(60).padBottom(10).row();

        // No button
        TextButton noButton = new TextButton("No (Exit to Menu)", skin);
        noButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!voted) {
                    voted = true;
                    game.getNetworkClient().send("C_REMATCH_VOTE|NO");
                    game.showMainMenu(); // Back to menu
                }
            }
        });
        table.add(noButton).width(350).height(60).padBottom(30).row();

        // Timer label
        timerLabel = new Label("30s...", skin);
        table.add(timerLabel).padBottom(20).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        if (!voted) {
            timer -= delta;
            int timeLeft = (int) Math.ceil(timer);
            timerLabel.setText(Math.max(0, timeLeft) + "s...");
            
            if (timer <= 0) {
                voted = true;
                game.getNetworkClient().send("C_REMATCH_VOTE|NO");
                game.showMainMenu();
                return;
            }
        }
        
        // Draw background and logo
        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();
        float uRepeat = worldWidth / bgTexture.getWidth();
        float vRepeat = worldHeight / bgTexture.getHeight();
        spriteBatch.draw(bgTexture, 0, 0, worldWidth, worldHeight, 0, 0, uRepeat, vRepeat);
        
        float logoWidth = 400; // approximate target width
        float logoHeight = logoWidth * ((float)logoTexture.getHeight() / logoTexture.getWidth());
        spriteBatch.draw(logoTexture, worldWidth / 2 - logoWidth / 2, worldHeight - logoHeight - 60, logoWidth, logoHeight);
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
        stage.dispose();
        skin.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (bgTexture != null) bgTexture.dispose();
        if (logoTexture != null) logoTexture.dispose();
    }
}