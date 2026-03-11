/*
 * AI Tools Use Transparency Disclosure:
 * Primary prior GitHub handling credit: Bhawna Gogna.
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

// This file was handled by Max, with help from ChatGPT.
// Rendering game window, tiling grid backdrop, enforcing window constraints.

// This file handles the initial screen and window upon application start.
// It initializes the camera and viewport, with a constrained virtual resolution
// which keeps the game in portrait resolution.


/** First screen of the application. Displayed after the application is created. */
public class FirstScreen extends ScreenAdapter {
	
	private final JavaTronGame game; // Define game for screen function
	
	private OrthographicCamera camera; // Game camera, 2D renderer.
	private Viewport viewport; // Game viewport. Maps game world size to window size.
	
	private SpriteBatch spriteBatch; // Use LibGDX's sprite renderer for 2D textures
	private Texture bgTileTexture; // The Grid. A digital frontier.
	
	private float timer = 0f; // Timer to auto-advance to main menu
	
	// Initial screen
    public FirstScreen(JavaTronGame game) {
    	this.game = game;
	}

	@Override
    public void show() {
		game.playMenuMusic();
		camera = new OrthographicCamera(); // Camera. Orthographic for 2D.
		// Viewport. FitViewport keeps aspect radio for virtual resolution consistent.
		viewport = new FitViewport(JavaTronGame.VIRTUAL_WIDTH, JavaTronGame.VIRTUAL_HEIGHT, camera);
		viewport.apply(true);
		
		// SpriteBatch draws the textures to the screen.
		spriteBatch = new SpriteBatch();
		
		// Path relative to root of assets directory (JavaTron/assets)
		// Select the Grid texture and wrap it to the screen both vertically and horizontally.
		bgTileTexture = new Texture(Gdx.files.internal("gfx/tile/bgtile_indigo.png"));
		bgTileTexture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
    }

	// This function is called whenever the window is resized.
    @Override
    public void resize(int width, int height) {
    	if (WindowAspectEnforcer.enforce(width, height)) return;
    	
    	// Update the viewport with new size.
    	viewport.update(width, height, true);
    }

    // Called each frame. Clears and draws the screen.
    @Override
    public void render(float delta) {
        // Clear screen with every frame
    	Gdx.gl.glClearColor(0f,  0f,  0f,  1f);
    	Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    	
    	// Update camera to match our newly drawn window
    	// and align the SpriteBatch texture renderer with it.
    	camera.update();
    	spriteBatch.setProjectionMatrix(camera.combined);
    	
    	// Define height and width for viewport from world's
    	float worldWidth = viewport.getWorldWidth();
    	float worldHeight = viewport.getWorldHeight();
    	
    	// Repeat counts based on texture pixel size
    	float uRepeat = MenuVisuals.backgroundURepeat(worldWidth, bgTileTexture.getWidth());
    	float vRepeat = MenuVisuals.backgroundVRepeat(worldHeight, bgTileTexture.getHeight());
    	
    	// Draw the grid, originating from (0, 0) across whole viewport.
    	spriteBatch.begin();
    	spriteBatch.draw(
    		bgTileTexture,
    		0f, 0f,
    		worldWidth, worldHeight,
    		0f, 0f,
    		uRepeat, vRepeat
    	);
    	spriteBatch.end();
    	
    	System.out.println("FirstScreen rendering... timer=" + timer);
    	
    	// Auto-advance to main menu after 2 seconds
    	timer += delta;
    	if (timer > 2f) {
    	    System.out.println("Timer reached, advancing to main menu");
    	    game.showMainMenu();
    	}
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    // Cleans up resources to prevent memory leaks.
    @Override
    public void dispose() {
        // Destroy screen's assets here.
    	if (spriteBatch != null) spriteBatch.dispose();
    	if (bgTileTexture != null) bgTileTexture.dispose();
    }
}
