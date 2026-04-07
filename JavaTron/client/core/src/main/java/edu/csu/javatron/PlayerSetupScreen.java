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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Player setup screen for choosing name and color. */
public class PlayerSetupScreen extends ScreenAdapter {
    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private TextField nameField;
    private String selectedColor = "Blue";
    private Label colorLabel;
    private com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private com.badlogic.gdx.graphics.Texture bgTexture;
    private com.badlogic.gdx.graphics.Texture logoTexture;

    public PlayerSetupScreen(JavaTronGame game) {
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
        buttonStyle.fontColor = Color.CYAN;
        buttonStyle.downFontColor = Color.YELLOW;
        skin.add("default", buttonStyle);

        TextFieldStyle fieldStyle = new TextFieldStyle();
        fieldStyle.font = font;
        fieldStyle.fontColor = Color.WHITE;
        fieldStyle.focusedFontColor = Color.YELLOW;
        skin.add("default", fieldStyle);

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

        // Title
        Label titleLabel = new Label("Player Setup", skin);
        titleLabel.setFontScale(1.5f);
        table.add(titleLabel).padBottom(50).row();

        // Name field
        Label nameLabel = new Label("Enter Your Name:", skin);
        table.add(nameLabel).padBottom(10).row();
        
        nameField = new TextField("Player", skin);
        nameField.setMessageText("Your name");
        nameField.setMaxLength(20);
        table.add(nameField).width(250).height(50).padBottom(40).row();

        // Color selection
        Label colorSelectLabel = new Label("Select Your Color:", skin);
        table.add(colorSelectLabel).padBottom(15).row();

        colorLabel = new Label("Selected: Blue", skin);
        colorLabel.setColor(Color.BLUE);
        colorLabel.setFontScale(1.2f);
        table.add(colorLabel).padBottom(20).row();

        // Color buttons
        Table colorTable = new Table();
        
        TextButton blueButton = new TextButton("Blue", skin);
        blueButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedColor = "Blue";
                colorLabel.setText("Selected: Blue");
                colorLabel.setColor(Color.BLUE);
            }
        });
        colorTable.add(blueButton).width(80).height(50).padRight(10);

        TextButton redButton = new TextButton("Red", skin);
        redButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedColor = "Red";
                colorLabel.setText("Selected: Red");
                colorLabel.setColor(Color.RED);
            }
        });
        colorTable.add(redButton).width(80).height(50).padRight(10);

        TextButton greenButton = new TextButton("Green", skin);
        greenButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedColor = "Green";
                colorLabel.setText("Selected: Green");
                colorLabel.setColor(Color.GREEN);
            }
        });
        colorTable.add(greenButton).width(80).height(50).padRight(10);

        TextButton yellowButton = new TextButton("Yellow", skin);
        yellowButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedColor = "Yellow";
                colorLabel.setText("Selected: Yellow");
                colorLabel.setColor(Color.YELLOW);
            }
        });
        colorTable.add(yellowButton).width(80).height(50);

        table.add(colorTable).padBottom(50).row();

        // Done button
        TextButton doneButton = new TextButton("Done", skin);
        doneButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String playerName = nameField.getText();
                if (playerName == null || playerName.isEmpty()) {
                    playerName = "Player";
                }
                game.playerName = playerName;
                game.playerColor = selectedColor;
                game.savePlayerIdentity();
                game.getNetworkClient().send("C_HELLO|" + selectedColor + "|" + playerName + "|" + game.playerId);
                game.showLobbyScreen();
            }
        });
        table.add(doneButton).width(250).height(60).padBottom(20).row();

        // Back button
        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getNetworkClient().disconnect();
                game.showMainMenu();
            }
        });
        table.add(backButton).width(250).height(60).row();
    }

    @Override
    public void show() {
        game.playMenuMusic();
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
        float uRepeat = MenuVisuals.backgroundURepeat(worldWidth, bgTexture.getWidth());
        float vRepeat = MenuVisuals.backgroundVRepeat(worldHeight, bgTexture.getHeight());
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
        if (WindowAspectEnforcer.enforce(width, height)) {
            return;
        }
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
