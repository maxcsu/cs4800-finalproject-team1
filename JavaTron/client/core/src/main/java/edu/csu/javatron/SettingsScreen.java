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

public class SettingsScreen extends ScreenAdapter {
    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private static boolean audioEnabled = true;

    // Track if user is currently binding a key
    private boolean isBinding = false;
    private String bindingAction = ""; // "UP", "DOWN", "LEFT", "RIGHT"
    private Label bindingLabel = null; 
    
    private com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private com.badlogic.gdx.graphics.Texture bgTexture;
    private com.badlogic.gdx.graphics.Texture logoTexture;
    private com.badlogic.gdx.graphics.Texture cycleTexture;

    public SettingsScreen(JavaTronGame game) {
        this.game = game;
        stage = new Stage(new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT));
        
        // Custom input multiplexer to grab key rebinds before standard stage clicks
        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (isBinding && bindingLabel != null) {
                    if (bindingAction.equals("UP")) game.upKey = keycode;
                    else if (bindingAction.equals("DOWN")) game.downKey = keycode;
                    else if (bindingAction.equals("LEFT")) game.leftKey = keycode;
                    else if (bindingAction.equals("RIGHT")) game.rightKey = keycode;
                    
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

        try {
            cycleTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("gfx/lightcycle_" + game.playerColor.toLowerCase() + ".png"));
        } catch (Exception e) {
            System.out.println("Could not load cycle texture for color " + game.playerColor);
        }

        Table table = new Table();
        table.setFillParent(true);
        table.padTop(100);
        stage.addActor(table);

        // Title
        Label titleLabel = new Label("Settings:", skin);
        table.add(titleLabel).padBottom(20).row();

        // Color binding
        Table colorTable = new Table();
        Label colorLabelLabel = new Label("Color: ", skin);
        Label colorValLabel = new Label(game.playerColor, skin);
        if (game.playerColor.equalsIgnoreCase("Red")) colorValLabel.setColor(Color.RED);
        else if (game.playerColor.equalsIgnoreCase("Blue")) colorValLabel.setColor(Color.BLUE);
        else if (game.playerColor.equalsIgnoreCase("Green")) colorValLabel.setColor(Color.GREEN);
        else if (game.playerColor.equalsIgnoreCase("Yellow")) colorValLabel.setColor(Color.YELLOW);
        colorTable.add(colorLabelLabel);
        colorTable.add(colorValLabel).padRight(15);

        TextButton blueBtn = new TextButton("B", skin);
        blueBtn.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { game.playerColor="Blue"; colorValLabel.setText("Blue"); colorValLabel.setColor(Color.BLUE); }});
        colorTable.add(blueBtn).width(40).height(40).padRight(5);

        TextButton redBtn = new TextButton("R", skin);
        redBtn.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { game.playerColor="Red"; colorValLabel.setText("Red"); colorValLabel.setColor(Color.RED); }});
        colorTable.add(redBtn).width(40).height(40).padRight(5);

        TextButton greenBtn = new TextButton("G", skin);
        greenBtn.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { game.playerColor="Green"; colorValLabel.setText("Green"); colorValLabel.setColor(Color.GREEN); }});
        colorTable.add(greenBtn).width(40).height(40).padRight(5);

        TextButton yellowBtn = new TextButton("Y", skin);
        yellowBtn.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { game.playerColor="Yellow"; colorValLabel.setText("Yellow"); colorValLabel.setColor(Color.YELLOW); }});
        colorTable.add(yellowBtn).width(40).height(40);

        table.add(colorTable).padBottom(10).left().padLeft(60).row();

        // Control bindings
        Table controlTable = new Table();
        
        controlTable.add(new Label("Up: ", skin)).left();
        Label wLabel = new Label(com.badlogic.gdx.Input.Keys.toString(game.upKey), skin);
        wLabel.setColor(Color.YELLOW);
        TextButton upBtn = new TextButton("Rebind", skin);
        upBtn.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { isBinding = true; bindingAction = "UP"; bindingLabel = wLabel; wLabel.setText("?"); wLabel.setColor(Color.RED); }});
        controlTable.add(wLabel).width(40).left();
        controlTable.add(upBtn).width(80).height(40).padBottom(5).row();

        controlTable.add(new Label("Left: ", skin)).left();
        Label aLabel = new Label(com.badlogic.gdx.Input.Keys.toString(game.leftKey), skin);
        aLabel.setColor(Color.YELLOW);
        TextButton leftBtn = new TextButton("Rebind", skin);
        leftBtn.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { isBinding = true; bindingAction = "LEFT"; bindingLabel = aLabel; aLabel.setText("?"); aLabel.setColor(Color.RED); }});
        controlTable.add(aLabel).width(40).left();
        controlTable.add(leftBtn).width(80).height(40).padBottom(5).row();

        controlTable.add(new Label("Right: ", skin)).left();
        Label dLabel = new Label(com.badlogic.gdx.Input.Keys.toString(game.rightKey), skin);
        dLabel.setColor(Color.YELLOW);
        TextButton rightBtn = new TextButton("Rebind", skin);
        rightBtn.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { isBinding = true; bindingAction = "RIGHT"; bindingLabel = dLabel; dLabel.setText("?"); dLabel.setColor(Color.RED); }});
        controlTable.add(dLabel).width(40).left();
        controlTable.add(rightBtn).width(80).height(40).padBottom(5).row();

        controlTable.add(new Label("Down: ", skin)).left();
        Label sLabel = new Label(com.badlogic.gdx.Input.Keys.toString(game.downKey), skin);
        sLabel.setColor(Color.YELLOW);
        TextButton downBtn = new TextButton("Rebind", skin);
        downBtn.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { isBinding = true; bindingAction = "DOWN"; bindingLabel = sLabel; sLabel.setText("?"); sLabel.setColor(Color.RED); }});
        controlTable.add(sLabel).width(40).left();
        controlTable.add(downBtn).width(80).height(40).padBottom(5).row();

        table.add(controlTable).padBottom(30).left().padLeft(60).row();

        // Audio toggle (Keeping requested feature but making it fit the UI)
        Label audioLabel = new Label("Audio: " + (audioEnabled ? "ON" : "OFF"), skin);
        audioLabel.setColor(audioEnabled ? Color.GREEN : Color.RED);
        table.add(audioLabel).padBottom(10).row();

        Table audioTable = new Table();
        
        TextButton audioOnButton = new TextButton("ON", skin);
        audioOnButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                audioEnabled = true;
                audioLabel.setText("Audio: ON");
                audioLabel.setColor(Color.GREEN);
            }
        });
        audioTable.add(audioOnButton).width(80).height(50).padRight(10);

        TextButton audioOffButton = new TextButton("OFF", skin);
        audioOffButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                audioEnabled = false;
                audioLabel.setText("Audio: OFF");
                audioLabel.setColor(Color.RED);
            }
        });
        audioTable.add(audioOffButton).width(80).height(50);

        table.add(audioTable).padBottom(30).row();

        // Back button (Exit in mockup)
        TextButton backButton = new TextButton("Exit", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showMainMenu();
            }
        });
        table.add(backButton).width(250).height(60).row();
    }

    public static boolean isAudioEnabled() {
        return audioEnabled;
    }

    public static void setAudioEnabled(boolean enabled) {
        audioEnabled = enabled;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
        
        // Draw cycle texture on the right side if successfully loaded
        if (cycleTexture != null) {
            float cycleX = worldWidth - 120;
            float cycleY = worldHeight / 2 - 50; 
            spriteBatch.draw(cycleTexture, cycleX, cycleY, cycleTexture.getWidth() * 2, cycleTexture.getHeight() * 2);
            // Draw a trail for aesthetic
            com.badlogic.gdx.graphics.Texture pixel = new com.badlogic.gdx.graphics.Texture(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            com.badlogic.gdx.graphics.Pixmap pmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            if (game.playerColor.equalsIgnoreCase("Red")) pmap.setColor(Color.RED);
            else if (game.playerColor.equalsIgnoreCase("Blue")) pmap.setColor(Color.BLUE);
            else if (game.playerColor.equalsIgnoreCase("Green")) pmap.setColor(Color.GREEN);
            else if (game.playerColor.equalsIgnoreCase("Yellow")) pmap.setColor(Color.YELLOW);
            else pmap.setColor(Color.WHITE);
            pmap.fill();
            pixel.draw(pmap, 0, 0);
            spriteBatch.draw(pixel, cycleX + cycleTexture.getWidth() - 2, 0, 6, cycleY + cycleTexture.getHeight());
            pmap.dispose();
            pixel.dispose();
        }

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
        if (cycleTexture != null) cycleTexture.dispose();
    }
}