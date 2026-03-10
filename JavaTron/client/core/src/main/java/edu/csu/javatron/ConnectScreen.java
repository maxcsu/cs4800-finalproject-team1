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

/** Connect screen to input server IP/port and connect. */
public class ConnectScreen extends ScreenAdapter {
    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private TextField nameField;
    private TextField ipField;
    private TextField portField;
    private Label statusLabel;
    private boolean connecting = false;
    private com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private com.badlogic.gdx.graphics.Texture bgTexture;
    private com.badlogic.gdx.graphics.Texture logoTexture;

    public ConnectScreen(JavaTronGame game) {
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
        Label titleLabel = new Label("Enter server IP:", skin);
        table.add(titleLabel).padBottom(30).row();

        // Name field
        nameField = new TextField(game.playerName != null ? game.playerName : "Player", skin);
        nameField.setMessageText("Name");
        table.add(nameField).width(250).height(40).padBottom(10).row();

        // IP field
        ipField = new TextField("localhost", skin);
        ipField.setMessageText("Server IP");
        table.add(ipField).width(250).height(40).padBottom(10).row();

        // Port field
        portField = new TextField("7777", skin);
        portField.setMessageText("Port");
        table.add(portField).width(250).height(40).padBottom(30).row();

        // Status label
        statusLabel = new Label("", skin);
        statusLabel.setColor(Color.YELLOW);
        table.add(statusLabel).padBottom(20).row();

        // Connect button
        TextButton connectButton = new TextButton("Connect", skin, "yellow");
        connectButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!connecting) {
                    connecting = true;
                    statusLabel.setText("Connecting...");
                    statusLabel.setColor(Color.YELLOW);
                    
                    new Thread(() -> {
                        try {
                            String ip = ipField.getText();
                            String portStr = portField.getText();
                            int port = Integer.parseInt(portStr);
                            
                            System.out.println("Connecting to " + ip + ":" + port);
                            game.getNetworkClient().connect(ip, port);
                            System.out.println("Connected successfully!");
                            
                            Gdx.app.postRunnable(() -> {
                                statusLabel.setText("Connected! Sent Handshake.");
                                statusLabel.setColor(Color.GREEN);
                                
                                String pName = nameField.getText() == null || nameField.getText().isEmpty() ? "Player" : nameField.getText();
                                game.playerName = pName;
                                game.getNetworkClient().send("C_HELLO|" + game.playerColor + "|" + pName);

                                // The spec requests C_FIND_MATCH to be sent here, skipping the old setup screen.
                                game.getNetworkClient().send("C_FIND_MATCH");
                                
                                Gdx.app.postRunnable(() -> game.showLobbyScreen());
                            });
                        } catch (Exception e) {
                            System.out.println("Connection error: " + e);
                            e.printStackTrace();
                            Gdx.app.postRunnable(() -> {
                                statusLabel.setText("Failed: " + e.getClass().getSimpleName());
                                statusLabel.setColor(Color.RED);
                                connecting = false;
                            });
                        }
                    }).start();
                }
            }
        });
        table.add(connectButton).width(250).height(60).padBottom(20).row();

        // Back button
        TextButton backButton = new TextButton("Return to Menu", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                connecting = false;
                game.showMainMenu();
            }
        });
        table.add(backButton).width(250).height(60).row();
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