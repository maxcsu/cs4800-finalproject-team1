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
import java.util.List;

public class SettingsScreen extends ScreenAdapter {
    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private static boolean audioEnabled = true;

    private boolean isBinding = false;
    private String bindingAction = "";
    private Label bindingLabel = null;

    private com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private com.badlogic.gdx.graphics.Texture bgTexture;
    private com.badlogic.gdx.graphics.Texture logoTexture;
    private com.badlogic.gdx.graphics.Texture cycleTexture;
    private ShapeRenderer shapeRenderer;
    private float pulseTime = 0;

    private static final List<String> COLORS = List.of(
            "Green", "Blue", "Orange", "Red", "Yellow", "Purple", "Cyan", "Pink", "White", "Black");

    public SettingsScreen(JavaTronGame game) {
        this.game = game;
        stage = new Stage(new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT));
        shapeRenderer = new ShapeRenderer();

        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (isBinding && bindingLabel != null) {
                    if (bindingAction.equals("UP"))
                        game.upKey = keycode;
                    else if (bindingAction.equals("DOWN"))
                        game.downKey = keycode;
                    else if (bindingAction.equals("LEFT"))
                        game.leftKey = keycode;
                    else if (bindingAction.equals("RIGHT"))
                        game.rightKey = keycode;

                    bindingLabel.setText(com.badlogic.gdx.Input.Keys.toString(keycode));
                    bindingLabel.setColor(Color.YELLOW);
                    isBinding = false;
                    bindingLabel = null;
                    bindingAction = "";
                    return true;
                }
                return false;
            }
        });
        multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);

        skin = new Skin();

        // Critical: Add 'white' texture for SelectBox/List backgrounds
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1,
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new com.badlogic.gdx.graphics.Texture(pixmap));
        pixmap.dispose();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);
        skin.add("default-font", font);

        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.downFontColor = Color.YELLOW;
        skin.add("default", buttonStyle);

        LabelStyle labelStyle = new LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // Dropdown Styles
        com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle listStyle = new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle();
        listStyle.font = font;
        listStyle.fontColorSelected = Color.YELLOW;
        listStyle.fontColorUnselected = Color.WHITE;
        listStyle.selection = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.5f, 0.5f));
        skin.add("default", listStyle);

        com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle scrollPaneStyle = new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle();
        scrollPaneStyle.background = skin.newDrawable("white", new Color(0.08f, 0.08f, 0.12f, 1f)); // Solid Opaque
        skin.add("default", scrollPaneStyle);

        com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle selectBoxStyle = new com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle();
        selectBoxStyle.font = font;
        selectBoxStyle.fontColor = Color.CYAN;
        selectBoxStyle.listStyle = listStyle;
        selectBoxStyle.scrollStyle = scrollPaneStyle;
        selectBoxStyle.background = skin.newDrawable("white", new Color(0.15f, 0.15f, 0.2f, 1f)); // Solid
        skin.add("default", selectBoxStyle);

        spriteBatch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
        bgTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/tile/bgtile_indigo.png"));
        bgTexture.setWrap(com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat,
                com.badlogic.gdx.graphics.Texture.TextureWrap.Repeat);
        logoTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/menu/title.png"));
        loadCycleTexture();

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.padTop(120);
        stage.addActor(rootTable);

        rootTable.add(new Label("SETTINGS", skin)).colspan(2).padBottom(40).row();

        // Color Dropdown
        rootTable.add(new Label("Cycle Color: ", skin)).left().padLeft(40);
        final com.badlogic.gdx.scenes.scene2d.ui.SelectBox<String> colorBox = new com.badlogic.gdx.scenes.scene2d.ui.SelectBox<>(
                skin);
        colorBox.setItems(COLORS.toArray(new String[0]));
        colorBox.setSelected(game.playerColor);
        colorBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.playerColor = colorBox.getSelected();
                loadCycleTexture();
            }
        });
        rootTable.add(colorBox).width(200).left().padBottom(50).row(); // More space

        // Rebind Actions
        Table controlTable = new Table();
        addControlRow(controlTable, "Up: ", game.upKey, "UP");
        addControlRow(controlTable, "Left: ", game.leftKey, "LEFT");
        addControlRow(controlTable, "Right: ", game.rightKey, "RIGHT");
        addControlRow(controlTable, "Down: ", game.downKey, "DOWN");
        rootTable.add(controlTable).colspan(2).padBottom(30).row();

        // Audio
        final Label audioLabel = new Label("Audio: " + (audioEnabled ? "ON" : "OFF"), skin);
        audioLabel.setColor(audioEnabled ? Color.GREEN : Color.RED);
        rootTable.add(audioLabel).colspan(2).padBottom(10).row();

        Table audioTable = new Table();
        TextButton audioOn = new TextButton("ON", skin);
        audioOn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                audioEnabled = true;
                audioLabel.setText("Audio: ON");
                audioLabel.setColor(Color.GREEN);
            }
        });
        TextButton audioOff = new TextButton("OFF", skin);
        audioOff.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                audioEnabled = false;
                audioLabel.setText("Audio: OFF");
                audioLabel.setColor(Color.RED);
            }
        });
        audioTable.add(audioOn).width(80).height(45).padRight(10);
        audioTable.add(audioOff).width(80).height(45);
        rootTable.add(audioTable).colspan(2).padBottom(40).row();

        TextButton backBtn = new TextButton("BACK TO MENU", skin);
        backBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                game.showMainMenu();
            }
        });
        rootTable.add(backBtn).width(280).height(60).colspan(2);
    }

    private void addControlRow(Table table, String label, int key, String action) {
        table.add(new Label(label, skin)).left().padRight(10);
        final Label keyLabel = new Label(com.badlogic.gdx.Input.Keys.toString(key), skin);
        keyLabel.setColor(Color.YELLOW);
        TextButton rebind = new TextButton("REBIND", skin);
        rebind.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                isBinding = true;
                bindingAction = action;
                bindingLabel = keyLabel;
                keyLabel.setText("?");
                keyLabel.setColor(Color.RED);
            }
        });
        table.add(keyLabel).width(80).left();
        table.add(rebind).width(110).height(40).padBottom(5).row();
    }

    private void loadCycleTexture() {
        if (cycleTexture != null)
            cycleTexture.dispose();
        String col = game.playerColor.toLowerCase();
        if (Gdx.files.internal("gfx/lightcycle_" + col + ".png").exists()) {
            cycleTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/lightcycle_" + col + ".png"));
        } else {
            cycleTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/lightcycle_white.png"));
        }
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

    public static boolean isAudioEnabled() {
        return audioEnabled;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        pulseTime += delta;
        float pulse = 0.7f + 0.3f * (float) Math.sin(pulseTime * 2.5f);

        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();

        spriteBatch.draw(bgTexture, 0, 0, worldWidth, worldHeight, 0, 0, worldWidth / bgTexture.getWidth(),
                worldHeight / bgTexture.getHeight());

        float logoWidth = 350;
        float logoHeight = logoWidth * ((float) logoTexture.getHeight() / logoTexture.getWidth());
        spriteBatch.draw(logoTexture, worldWidth / 2 - logoWidth / 2, worldHeight - logoHeight - 40, logoWidth,
                logoHeight);

        if (cycleTexture != null) {
            float cycleX = worldWidth - 110;
            float cycleY = worldHeight / 2 - 40;
            Color tint = getColorFromString(game.playerColor);
            if (!Gdx.files.internal("gfx/lightcycle_" + game.playerColor.toLowerCase() + ".png").exists()) {
                spriteBatch.setColor(tint);
            }
            spriteBatch.draw(cycleTexture, cycleX, cycleY, cycleTexture.getWidth() * 2, cycleTexture.getHeight() * 2);
            spriteBatch.setColor(Color.WHITE);
        }
        spriteBatch.end();

        // Pulsing Grid
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        shapeRenderer.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);
        int gridSpacing = 64;
        for (int pass = 0; pass < 4; pass++) {
            float alpha = (0.2f / (pass + 1)) * pulse;
            // Verts
            for (int i = 0; i < (worldWidth / gridSpacing) + 1; i++) {
                float x = i * gridSpacing;
                float t = (float) i / (float) (worldWidth / gridSpacing);
                shapeRenderer.setColor(0.1f * (1 - t) + 1f * t, 0.8f * (1 - t) + 0.1f * t, 1f * (1 - t) + 0.8f * t,
                        alpha);
                shapeRenderer.line(x, 0, x, worldHeight);
            }
            // Horiz
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
        if (cycleTexture != null)
            cycleTexture.dispose();
        shapeRenderer.dispose();
    }
}
