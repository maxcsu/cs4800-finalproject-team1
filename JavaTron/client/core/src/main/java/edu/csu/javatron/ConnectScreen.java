/*
 * AI Tools Use Transparency Disclosure:
 * Primary prior GitHub handling credit: Bhawna Gogna.
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
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
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayList;
import java.util.List;

/** Connect screen to input server IP/port and connect. */
public class ConnectScreen extends ScreenAdapter {
    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private TextField ipField;
    private TextField portField;
    private Label statusLabel;
    private boolean connecting = false;
    private TextButtonStyle defaultButtonStyle;
    private TextButtonStyle yellowButtonStyle;
    private com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private com.badlogic.gdx.graphics.Texture bgTexture;
    private com.badlogic.gdx.graphics.Texture logoTexture;
    private final List<Actor> menuActors = new ArrayList<>();
    private int selectedIndex = -1;
    private float bgScrollX = 0f;
    private float bgScrollY = 0f;

    public ConnectScreen(JavaTronGame game) {
        this.game = game;
        stage = new Stage(new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        // Create a simple skin programmatically
        skin = new Skin();
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1,
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new com.badlogic.gdx.graphics.Texture(pixmap));
        pixmap.dispose();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);
        skin.add("default-font", font);

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

        TextFieldStyle fieldStyle = new TextFieldStyle();
        fieldStyle.font = font;
        fieldStyle.fontColor = Color.WHITE;
        fieldStyle.focusedFontColor = Color.YELLOW;
        fieldStyle.messageFont = font;
        fieldStyle.messageFontColor = Color.LIGHT_GRAY;
        fieldStyle.background = skin.newDrawable("white", new Color(0.15f, 0.15f, 0.2f, 1f));
        fieldStyle.focusedBackground = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.26f, 1f));
        fieldStyle.cursor = skin.newDrawable("white", Color.YELLOW);
        fieldStyle.selection = skin.newDrawable("white", new Color(0.35f, 0.35f, 0.16f, 0.85f));
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

        // IP field
        ipField = new TextField("localhost", skin);
        ipField.setMessageText("Server IP");
        registerSelectable(ipField, true);
        table.add(ipField).width(250).height(40).padBottom(10).row();

        // Port field
        portField = new TextField("7777", skin);
        portField.setMessageText("Port");
        registerSelectable(portField, true);
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
                    game.playMenuConfirmSound();
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
                                game.playNewGameSound();
                                statusLabel.setText("Connected! Sent Handshake.");
                                statusLabel.setColor(Color.GREEN);
                                game.getNetworkClient().send("C_HELLO|" + game.playerColor + "|" + game.playerName + "|" + game.playerId);

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
                                game.playDisconnectSound();
                                connecting = false;
                            });
                        }
                    }).start();
                }
            }
        });
        registerSelectable(connectButton, false);
        table.add(connectButton).width(250).height(60).padBottom(20).row();

        // Back button
        TextButton backButton = new TextButton("Return to Menu", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                connecting = false;
                game.playMenuBackSound();
                game.showMainMenu();
            }
        });
        registerSelectable(backButton, true);
        table.add(backButton).width(250).height(60).row();

        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (isMenuUp(keycode)) {
                    moveSelection(-1);
                    return true;
                }
                if (isMenuDown(keycode)) {
                    moveSelection(1);
                    return true;
                }
                if (keycode == com.badlogic.gdx.Input.Keys.ENTER) {
                    activateSelection();
                    return true;
                }
                if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                    connecting = false;
                    game.playMenuBackSound();
                    game.showMainMenu();
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
        if (game.connectStatusMessage != null && !game.connectStatusMessage.isBlank()) {
            statusLabel.setText(game.connectStatusMessage);
            statusLabel.setColor(game.connectStatusIsError ? Color.RED : Color.YELLOW);
            game.connectStatusMessage = "";
            game.connectStatusIsError = false;
        } else {
            statusLabel.setText("");
        }
        connecting = false;
        setSelectedIndex(0, false);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        bgScrollX += delta * MenuVisuals.PARALLAX_SCROLL_X_SPEED;
        bgScrollY += delta * MenuVisuals.PARALLAX_SCROLL_Y_SPEED;
        game.sharedMenuScrollX = bgScrollX;
        game.sharedMenuScrollY = bgScrollY;

        // Draw background and logo
        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();
        float uRepeat = MenuVisuals.backgroundURepeat(worldWidth, bgTexture.getWidth());
        float vRepeat = MenuVisuals.backgroundVRepeat(worldHeight, bgTexture.getHeight());
        spriteBatch.draw(bgTexture, 0, 0, worldWidth, worldHeight, bgScrollX, bgScrollY,
                bgScrollX + uRepeat, bgScrollY + vRepeat);
        
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

    private void registerSelectable(Actor actor, boolean playHoverSound) {
        menuActors.add(actor);
        actor.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                setSelectedIndex(menuActors.indexOf(actor), playHoverSound);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                setSelectedIndex(menuActors.indexOf(actor), false);
                return super.touchDown(event, x, y, pointer, button);
            }
        });
    }

    private boolean isMenuUp(int keycode) {
        return keycode == com.badlogic.gdx.Input.Keys.UP || keycode == game.upKey;
    }

    private boolean isMenuDown(int keycode) {
        return keycode == com.badlogic.gdx.Input.Keys.DOWN || keycode == game.downKey;
    }

    private void moveSelection(int delta) {
        if (menuActors.isEmpty()) return;
        int next = selectedIndex < 0 ? 0 : (selectedIndex + delta + menuActors.size()) % menuActors.size();
        setSelectedIndex(next, true);
    }

    private void setSelectedIndex(int index, boolean playSound) {
        if (index < 0 || index >= menuActors.size()) return;
        if (selectedIndex == index) return;
        selectedIndex = index;
        for (int i = 0; i < menuActors.size(); i++) {
            Actor actor = menuActors.get(i);
            boolean selected = i == selectedIndex;
            if (actor instanceof TextButton button) {
                button.setStyle(selected ? yellowButtonStyle : defaultButtonStyle);
            }
        }
        Actor selectedActor = menuActors.get(selectedIndex);
        stage.setKeyboardFocus(selectedActor instanceof TextField ? selectedActor : null);
        if (playSound) {
            game.playMenuNavigateSound();
        }
    }

    private void activateSelection() {
        if (selectedIndex < 0 || selectedIndex >= menuActors.size()) return;
        Actor actor = menuActors.get(selectedIndex);
        if (actor instanceof TextField field) {
            stage.setKeyboardFocus(field);
            field.setCursorPosition(field.getText().length());
            return;
        }
        actor.fire(new ChangeListener.ChangeEvent());
    }
}
