package edu.csu.javatron;

import com.badlogic.gdx.Game;
import edu.csu.javatron.client.net.NetworkClient;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class JavaTronGame extends com.badlogic.gdx.Game {
	
	public static final float VIRTUAL_WIDTH = 480f; // Define width
	public static final float VIRTUAL_HEIGHT = 800f; // Define height
	
	private NetworkClient networkClient;
	
	// Player info
	public String playerName = "Player";
	public String playerColor = "Blue";
	public String oppName = "Opponent";
	public String oppColor = "Red";
	public int lobbyPlayerCount = 0;
	
	// --- Game state: written by network thread, read by render thread ---
	// Using volatile ensures the render thread always sees latest values (Java memory model)
	public volatile int ax = 5, ay = 5, bx = 43, by = 75;
	public volatile char aDir = 'R', bDir = 'L';
	public volatile int aWins = 0, bWins = 0;
	public volatile int roundNumber = 1;
	public volatile String countdownMessage = null;
	public volatile boolean countdownActive = false; // true while countdown is showing
	
	// Input keys
	public int upKey = com.badlogic.gdx.Input.Keys.W;
	public int downKey = com.badlogic.gdx.Input.Keys.S;
	public int leftKey = com.badlogic.gdx.Input.Keys.A;
	public int rightKey = com.badlogic.gdx.Input.Keys.D;
	
	public JavaTronGame() {
	    networkClient = new NetworkClient(this);
	}
	
	public NetworkClient getNetworkClient() {
	    return networkClient;
	}
	
	public void updateSnapshot(int ax, int ay, int bx, int by, char aDir, char bDir, int aWins, int bWins, int roundNumber) {
	    this.ax = ax;
	    this.ay = ay;
	    this.bx = bx;
	    this.by = by;
	    this.aDir = aDir;
	    this.bDir = bDir;
	    this.aWins = aWins;
	    this.bWins = bWins;
	    this.roundNumber = roundNumber;
	}
	
    @Override
    public void create() {
        setScreen(new FirstScreen(this)); // Start with splash screen
    }
    
    // Helper methods to switch screens
    public void showMainMenu() {
        setScreen(new MainMenuScreen(this));
    }
    
    public void showConnectScreen() {
        setScreen(new ConnectScreen(this));
    }
    
    public void showPlayerSetupScreen() {
        setScreen(new PlayerSetupScreen(this));
    }
    
    public void showLobbyScreen() {
        setScreen(new LobbyScreen(this));
    }
    
    public void showGameScreen() {
        setScreen(new GameScreen(this));
    }
    
    public void showRematchVoteScreen() {
        setScreen(new RematchVoteScreen(this));
    }
    
    public void showSettingsScreen() {
        setScreen(new SettingsScreen(this));
    }
}