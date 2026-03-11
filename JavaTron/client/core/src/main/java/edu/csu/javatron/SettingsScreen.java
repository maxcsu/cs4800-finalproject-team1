/*
 * AI Tools Use Transparency Disclosure:
 * Primary prior GitHub handling credit: Bhawna Gogna.
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends ScreenAdapter {
    private static final float CYCLE_DRIVE_IN_DURATION = 1.2f;
    private static final Color BINDING_LABEL_IDLE_COLOR = Color.valueOf("c8bb67");

    private final JavaTronGame game;
    private Stage stage;
    private Skin skin;
    private TextButtonStyle defaultButtonStyle;
    private TextButtonStyle yellowButtonStyle;
    private com.badlogic.gdx.scenes.scene2d.ui.SelectBox<String> colorBox;
    private boolean isBinding = false;
    private String bindingAction = "";
    private Label bindingLabel = null;

    private com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch;
    private com.badlogic.gdx.graphics.Texture bgTexture;
    private com.badlogic.gdx.graphics.Texture logoTexture;
    private com.badlogic.gdx.graphics.Texture cycleTexture;
    private com.badlogic.gdx.graphics.Texture trailTexture;
    private ShapeRenderer shapeRenderer;
    private float trailAnchorOffsetX = 0f;
    private float trailWidthScale = 12f;
    private float bgScrollX = 0f;
    private float bgScrollY = 0f;
    private float cycleEntranceTime = 0f;
    private float pulseTime = 0;
    private final List<Actor> menuActors = new ArrayList<>();
    private int selectedIndex = -1;

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

                    game.saveInputBindings();
                    bindingLabel.setText(com.badlogic.gdx.Input.Keys.toString(keycode));
                    bindingLabel.setColor(Color.YELLOW);
                    isBinding = false;
                    bindingLabel = null;
                    bindingAction = "";
                    return true;
                }
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
                    game.playMenuBackSound();
                    game.showMainMenu();
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
        trailTexture = new com.badlogic.gdx.graphics.Texture(pixmap);
        skin.add("white", trailTexture);
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
        selectBoxStyle.fontColor = CycleColors.get(game.playerColor, Color.WHITE);
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
        Table colorTable = new Table();
        colorTable.add(new Label("Cycle Color: ", skin)).padRight(12);
        colorBox = new ColorSelectBox(skin);
        colorBox.setItems(COLORS.toArray(new String[0]));
        colorBox.setSelected(game.playerColor);
        applySelectedColorText();
        colorBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.playerColor = colorBox.getSelected();
                game.savePlayerIdentity();
                applySelectedColorText();
                loadCycleTexture();
                game.playMenuConfirmSound();
            }
        });
        colorTable.add(colorBox).width(200);
        registerSelectable(colorBox);
        rootTable.add(colorTable).colspan(2).padBottom(50).row();

        // Rebind Actions
        Table controlTable = new Table();
        addControlRow(controlTable, "Up: ", game.upKey, "UP");
        addControlRow(controlTable, "Left: ", game.leftKey, "LEFT");
        addControlRow(controlTable, "Down: ", game.downKey, "DOWN");
        addControlRow(controlTable, "Right: ", game.rightKey, "RIGHT");
        rootTable.add(controlTable).colspan(2).padBottom(30).row();

        addToggleRow(rootTable, "Music", game.isMusicEnabled(),
                enabled -> {
                    game.setMusicEnabled(enabled);
                    game.playMenuConfirmSound();
                });
        addToggleRow(rootTable, "Menu SFX", game.isMenuSoundEffectsEnabled(),
                enabled -> {
                    game.setMenuSoundEffectsEnabled(enabled);
                    if (enabled) {
                        game.playMenuConfirmSound();
                    }
                });
        addToggleRow(rootTable, "In-Game SFX", game.isGameSoundEffectsEnabled(),
                enabled -> {
                    game.setGameSoundEffectsEnabled(enabled);
                    game.playMenuConfirmSound();
                });

        TextButton backBtn = new TextButton("BACK TO MENU", skin);
        backBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                game.playMenuBackSound();
                game.showMainMenu();
            }
        });
        registerSelectable(backBtn);
        rootTable.add(backBtn).width(280).height(60).colspan(2).padTop(-12f);
    }

    private void addControlRow(Table table, String label, int key, String action) {
        table.add(new Label(label, skin)).left().padRight(10);
        final Label keyLabel = new Label(com.badlogic.gdx.Input.Keys.toString(key), skin);
        keyLabel.setColor(BINDING_LABEL_IDLE_COLOR);
        keyLabel.setUserObject(action);
        TextButton rebind = new TextButton("REBIND", skin);
        rebind.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                startBinding(action, keyLabel);
            }
        });
        keyLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startBinding(action, keyLabel);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                setSelectedIndex(menuActors.indexOf(keyLabel), false);
            }
        });
        registerSelectable(keyLabel);
        rebind.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                rebind.setStyle(yellowButtonStyle);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                rebind.setStyle(defaultButtonStyle);
            }
        });
        table.add(keyLabel).width(80).left();
        table.add(rebind).width(110).height(40).padBottom(5).row();
    }

    private void addToggleRow(Table rootTable, String labelText, boolean enabled, java.util.function.Consumer<Boolean> onToggle) {
        final Label toggleLabel = new Label(labelText + ": " + (enabled ? "ON" : "OFF"), skin);
        toggleLabel.setColor(enabled ? Color.GREEN : Color.RED);
        rootTable.add(toggleLabel).colspan(2).padBottom(10).row();

        Table toggleTable = new Table();
        TextButton onButton = new TextButton("ON", skin);
        onButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onToggle.accept(true);
                toggleLabel.setText(labelText + ": ON");
                toggleLabel.setColor(Color.GREEN);
            }
        });
        TextButton offButton = new TextButton("OFF", skin);
        offButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onToggle.accept(false);
                toggleLabel.setText(labelText + ": OFF");
                toggleLabel.setColor(Color.RED);
            }
        });
        registerSelectable(onButton);
        registerSelectable(offButton);
        toggleTable.add(onButton).width(80).height(45).padRight(10);
        toggleTable.add(offButton).width(80).height(45);
        rootTable.add(toggleTable).colspan(2).padBottom(18).row();
    }

    private void startBinding(String action, Label keyLabel) {
        game.playMenuConfirmSound();
        isBinding = true;
        bindingAction = action;
        bindingLabel = keyLabel;
        keyLabel.setText("?");
        keyLabel.setColor(Color.RED);
    }

    private void loadCycleTexture() {
        if (cycleTexture != null)
            cycleTexture.dispose();
        String normalizedColor = normalizeCycleColor(game.playerColor);
        com.badlogic.gdx.files.FileHandle file =
                Gdx.files.internal("gfx/lightcycle_" + normalizedColor.toLowerCase() + ".png");
        cycleTexture = new com.badlogic.gdx.graphics.Texture(file);
        updateTrailAnchorMetrics(file);
    }

    private String normalizeCycleColor(String colorName) {
        if (colorName == null || colorName.isBlank()) {
            return "white";
        }
        String normalized = colorName.trim().toLowerCase();
        for (String supported : COLORS) {
            if (supported.equalsIgnoreCase(normalized)) {
                return supported.toLowerCase();
            }
        }
        return "white";
    }

    private void updateTrailAnchorMetrics(com.badlogic.gdx.files.FileHandle file) {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(file);
        try {
            int bottomRow = -1;
            int minX = pixmap.getWidth();
            int maxX = -1;

            for (int y = 0; y < pixmap.getHeight(); y++) {
                int rowMin = pixmap.getWidth();
                int rowMax = -1;
                for (int x = 0; x < pixmap.getWidth(); x++) {
                    int pixel = pixmap.getPixel(x, y);
                    int alpha = pixel & 0x000000ff;
                    if (alpha != 0) {
                        if (rowMin > x) rowMin = x;
                        if (rowMax < x) rowMax = x;
                    }
                }
                if (rowMax >= rowMin) {
                    bottomRow = y;
                    minX = rowMin;
                    maxX = rowMax;
                }
            }

            if (bottomRow >= 0 && maxX >= minX) {
                float centerX = (minX + maxX + 1) * 0.5f;
                trailAnchorOffsetX = centerX;
                trailWidthScale = maxX - minX + 1;
            } else {
                trailAnchorOffsetX = pixmap.getWidth() * 0.5f;
                trailWidthScale = 6f;
            }
        } finally {
            pixmap.dispose();
        }
    }

    @Override
    public void show() {
        game.playMenuMusic();
        bgScrollX = game.sharedMenuScrollX;
        bgScrollY = game.sharedMenuScrollY;
        cycleEntranceTime = 0f;
        setSelectedIndex(0, false);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        pulseTime += delta;
        cycleEntranceTime = Math.min(CYCLE_DRIVE_IN_DURATION, cycleEntranceTime + delta);
        bgScrollY += delta * MenuVisuals.DOWNWARD_SCROLL_SPEED;
        game.sharedMenuScrollX = bgScrollX;
        game.sharedMenuScrollY = bgScrollY;
        float pulse = 0.7f + 0.3f * (float) Math.sin(pulseTime * 2.5f);

        spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();

        float uRepeat = MenuVisuals.backgroundURepeat(worldWidth, bgTexture.getWidth());
        float vRepeat = MenuVisuals.backgroundVRepeat(worldHeight, bgTexture.getHeight());
        spriteBatch.draw(bgTexture, 0, 0, worldWidth, worldHeight, 0f, bgScrollY, uRepeat, bgScrollY + vRepeat);

        float logoWidth = 350;
        float logoHeight = logoWidth * ((float) logoTexture.getHeight() / logoTexture.getWidth());
        spriteBatch.draw(logoTexture, worldWidth / 2 - logoWidth / 2, worldHeight - logoHeight - 40, logoWidth,
                logoHeight);

        if (cycleTexture != null) {
            float targetCycleX = worldWidth - 110;
            float cycleWidth = cycleTexture.getWidth() * 2f;
            float cycleHeight = cycleTexture.getHeight() * 2f;
            float targetCycleY = worldHeight / 2 - 40;
            float startCycleY = -cycleHeight;
            float t = cycleEntranceTime / CYCLE_DRIVE_IN_DURATION;
            t = 1f - (1f - t) * (1f - t) * (1f - t);
            float cycleX = targetCycleX;
            float cycleY = startCycleY + (targetCycleY - startCycleY) * t;
            Color tint = CycleColors.get(game.playerColor, Color.WHITE);

            float trailWidth = trailWidthScale * 2f;
            float trailX = cycleX + trailAnchorOffsetX * 2f - trailWidth * 0.5f;
            float trailTopY = Math.max(0f, cycleY);
            if (trailTopY > 0f) {
                spriteBatch.setColor(tint);
                spriteBatch.draw(trailTexture, trailX, 0f, trailWidth, trailTopY, 0f, 0f, 1f, 1f);
                spriteBatch.setColor(Color.WHITE);
            }
            spriteBatch.draw(cycleTexture, cycleX, cycleY, cycleWidth, cycleHeight);
            spriteBatch.setColor(Color.WHITE);
        }
        spriteBatch.end();

        // Pulsing Grid
        if (MenuVisuals.ENABLE_GLOW) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            shapeRenderer.setProjectionMatrix(stage.getViewport().getCamera().combined);
            shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);
            int gridSpacing = 64;
            for (int pass = 0; pass < 4; pass++) {
                float alpha = (0.2f / (pass + 1)) * pulse;
                for (int i = 0; i < (worldWidth / gridSpacing) + 1; i++) {
                    float x = i * gridSpacing;
                    float glowT = (float) i / (float) (worldWidth / gridSpacing);
                    shapeRenderer.setColor(0.1f * (1 - glowT) + 1f * glowT, 0.8f * (1 - glowT) + 0.1f * glowT,
                            1f * (1 - glowT) + 0.8f * glowT, alpha);
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
        }

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
        spriteBatch.dispose();
        bgTexture.dispose();
        logoTexture.dispose();
        if (cycleTexture != null)
            cycleTexture.dispose();
        shapeRenderer.dispose();
    }

    private void registerSelectable(Actor actor) {
        menuActors.add(actor);
        if (actor instanceof TextButton button) {
            button.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    setSelectedIndex(menuActors.indexOf(button), true);
                }
            });
        } else if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.SelectBox<?> select) {
            select.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    setSelectedIndex(menuActors.indexOf(select), true);
                }
            });
        }
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
            } else if (actor instanceof Label label && label != bindingLabel) {
                if (label.getUserObject() instanceof String) {
                    label.setColor(selected ? Color.YELLOW : BINDING_LABEL_IDLE_COLOR);
                } else {
                    label.setColor(selected ? Color.YELLOW : Color.WHITE);
                }
            } else if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.SelectBox<?> && colorBox != null) {
                applySelectedColorText();
            }
        }
        if (bindingLabel != null) {
            bindingLabel.setColor(Color.RED);
        }
        if (playSound) {
            game.playMenuNavigateSound();
        }
    }

    private void applySelectedColorText() {
        if (colorBox != null) {
            colorBox.getStyle().fontColor = CycleColors.get(colorBox.getSelected(), Color.WHITE);
        }
    }

    private final class ColorSelectBox extends com.badlogic.gdx.scenes.scene2d.ui.SelectBox<String> {
        private ColorSelectBox(Skin skin) {
            super(skin);
        }

        @Override
        protected GlyphLayout drawItem(Batch batch, BitmapFont font, String item, float x, float y, float width) {
            Color original = new Color(font.getColor());
            Color itemColor = CycleColors.get(item, Color.WHITE);
            font.setColor(itemColor.r, itemColor.g, itemColor.b, original.a);
            GlyphLayout layout = super.drawItem(batch, font, item, x, y, width);
            font.setColor(original);
            return layout;
        }

        @Override
        protected SelectBoxScrollPane<String> newScrollPane() {
            return new ColorSelectBoxScrollPane(this);
        }
    }

    private final class ColorSelectBoxScrollPane extends com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxScrollPane<String> {
        private ColorSelectBoxScrollPane(ColorSelectBox selectBox) {
            super(selectBox);
        }

        @Override
        protected com.badlogic.gdx.scenes.scene2d.ui.List<String> newList() {
            return new com.badlogic.gdx.scenes.scene2d.ui.List<String>(getSelectBox().getStyle().listStyle) {
                @Override
                protected GlyphLayout drawItem(Batch batch, BitmapFont font, int index, String item, float x, float y,
                        float width) {
                    Color original = new Color(font.getColor());
                    Color itemColor = CycleColors.get(item, Color.WHITE);
                    font.setColor(itemColor.r, itemColor.g, itemColor.b, original.a);
                    GlyphLayout layout = super.drawItem(batch, font, index, item, x, y, width);
                    font.setColor(original);
                    return layout;
                }

                @Override
                public String toString(String obj) {
                    return obj;
                }
            };
        }
    }

    private void activateSelection() {
        if (selectedIndex < 0 || selectedIndex >= menuActors.size()) return;
        Actor actor = menuActors.get(selectedIndex);
        if (actor == colorBox) {
            int next = (colorBox.getSelectedIndex() + 1) % COLORS.size();
            colorBox.setSelectedIndex(next);
            return;
        }
        if (actor instanceof Label label) {
            Object action = label.getUserObject();
            if (action instanceof String bindingActionName) {
                startBinding(bindingActionName, label);
            }
            return;
        }
        actor.fire(new ChangeListener.ChangeEvent());
    }
}
