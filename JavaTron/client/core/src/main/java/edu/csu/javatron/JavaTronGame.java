package edu.csu.javatron;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class JavaTronGame extends com.badlogic.gdx.Game {
	
	public static final float VIRTUAL_WIDTH = 480f; // Define width
	public static final float VIRTUAL_HEIGHT = 800f; // Define height
	
    @Override
    public void create() {
        setScreen(new FirstScreen(this)); // Spawn the game window
    }
}