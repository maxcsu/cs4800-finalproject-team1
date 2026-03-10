package edu.csu.javatron;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Main menu screen with options to connect, settings, or exit. */
public class MainMenuScreen extends ScreenAdapter {
    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private com.badlogic.gdx.graphics.Texture bgTexture;
    private com.badlogic.gdx.graphics.Texture logoTexture;
    private ShapeRenderer shapeRenderer;
    private float bgOffset = 0;
    private float pulseTime = 0;

    public MainMenuScreen(JavaTronGame game) {
        this.game = game;
        stage = new Stage(new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);

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

        TextButton joinServerButton = new TextButton("Join Server", skin, "yellow");
        joinServerButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                game.showConnectScreen();
            }
        });
        table.add(joinServerButton).padBottom(20).row();

        TextButton settingsButton = new TextButton("Settings", skin);
        settingsButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                game.showSettingsScreen();
            }
        });
        table.add(settingsButton).padBottom(20).row();

        TextButton exitButton = new TextButton("Exit", skin);
        exitButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        table.add(exitButton).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        bgOffset = 0; // No scrolling
        pulseTime += delta;
        float pulse = 0.7f + 0.3f * (float) Math.sin(pulseTime * 2.5f);

        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();

        float uRepeat = worldWidth / bgTexture.getWidth();
        float vRepeat = worldHeight / bgTexture.getHeight();
        spriteBatch.draw(bgTexture, 0, 0, worldWidth, worldHeight, 0, 0, uRepeat, vRepeat);
        spriteBatch.end();

        // 2. Draw the "Glow" layers using additive blending
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

        spriteBatch.begin();
        float logoWidth = 450;
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
        stage.dispose();
        skin.dispose();
        spriteBatch.dispose();
        bgTexture.dispose();
        logoTexture.dispose();
        shapeRenderer.dispose();
    }
}
