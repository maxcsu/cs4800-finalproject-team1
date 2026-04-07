/*
 * AI Tools Use Transparency Disclosure:
 * Primary prior GitHub handling credit: Bhawna Gogna.
 * This file was handled by Maxwell Nield using Codex.
 */

package edu.csu.javatron;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import edu.csu.javatron.client.net.NetworkClient;

import java.util.Collections;
import java.util.List;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all
 * platforms.
 */
public class JavaTronGame extends com.badlogic.gdx.Game {

	public static final float VIRTUAL_WIDTH = 480f; // Define width
	public static final float VIRTUAL_HEIGHT = 800f; // Define height

	private NetworkClient networkClient;
	private ClientSettings settings;
	private Music menuMusic;
	private Sound menuConfirmSound;
	private Sound menuBackSound;
	private Sound menuNavigateSound;
	private Sound disconnectSound;
	private Sound startSound;
	private Sound newGameSound;
	private Sound votingStartSound;
	public float sharedMenuScrollX = 0f;
	public float sharedMenuScrollY = 0f;

	// Player info
	public String playerName = "Player";
	public String playerColor = "Blue";
	public String playerId = "";
	public String oppName = "Opponent";
	public String oppColor = "Red";
	public String serverMotd = "";
	public String matchPlayerColor = null;
	public String matchOppColor = null;
	public int lobbyPlayerCount = 0;
	public boolean practiceMode = false;
	public volatile boolean matchFoundPending = false;
	public volatile String lobbyNoticeText = "";
	public volatile String roundResultText = null;
	public volatile boolean finalMatchResult = false;
	public String connectStatusMessage = "";
	public boolean connectStatusIsError = false;
	public volatile List<LeaderboardEntry> leaderboardEntries = Collections.emptyList();
	public volatile int leaderboardVersion = 0;
	public volatile boolean leaderboardLoading = false;
	public volatile String leaderboardStatusMessage = "";

	// --- Game state: written by network thread, read by render thread ---
	// Using volatile ensures the render thread always sees latest values (Java
	// memory model)
	public volatile int ax = 24, ay = 20, bx = 24, by = 60;
	public volatile char aDir = 'D', bDir = 'U';
	public volatile int aWins = 0, bWins = 0;
	public volatile int roundNumber = 1;
	public volatile String countdownMessage = null;
	public volatile boolean countdownActive = false; // true while countdown is showing
	public volatile String latestRoundResult = null;
	public volatile int latestRoundEventId = 0;
	public volatile String latestRoundEventType = null;
	public volatile String latestWinnerSide = null;

	public boolean isPlayerA = true; // Which side this client is on
	public String winnerName = ""; // Name of the match winner

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

	public void updateSnapshot(int ax, int ay, int bx, int by, char aDir, char bDir, int aWins, int bWins,
			int roundNumber) {
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
		settings = new ClientSettings();
		playerName = settings.getPlayerName();
		playerColor = settings.getPlayerColor();
		playerId = settings.getOrCreatePlayerId();
		upKey = settings.getUpKey();
		downKey = settings.getDownKey();
		leftKey = settings.getLeftKey();
		rightKey = settings.getRightKey();
		setScreen(new FirstScreen(this)); // Start with splash screen
	}

	public ClientSettings getSettings() {
		return settings;
	}

	public boolean isAudioEnabled() {
		return isMusicEnabled();
	}

	public void setAudioEnabled(boolean enabled) {
		setMusicEnabled(enabled);
	}

	public boolean isMusicEnabled() {
		return settings != null && settings.isMusicEnabled();
	}

	public void setMusicEnabled(boolean enabled) {
		settings.setMusicEnabled(enabled);
		settings.flush();
		applyAudioSettings();
	}

	public boolean isMenuSoundEffectsEnabled() {
		return settings != null && settings.isMenuSoundEffectsEnabled();
	}

	public void setMenuSoundEffectsEnabled(boolean enabled) {
		settings.setMenuSoundEffectsEnabled(enabled);
		settings.flush();
	}

	public boolean isGameSoundEffectsEnabled() {
		return settings != null && settings.isGameSoundEffectsEnabled();
	}

	public void setGameSoundEffectsEnabled(boolean enabled) {
		settings.setGameSoundEffectsEnabled(enabled);
		settings.flush();
		applyAudioSettings();
	}

	public void saveInputBindings() {
		settings.setUpKey(upKey);
		settings.setDownKey(downKey);
		settings.setLeftKey(leftKey);
		settings.setRightKey(rightKey);
		settings.flush();
	}

	public void savePlayerIdentity() {
		settings.setPlayerName(playerName);
		settings.setPlayerColor(playerColor);
		settings.flush();
	}

	public void playMenuMusic() {
		if (menuMusic == null) {
			try {
				menuMusic = Gdx.audio.newMusic(Gdx.files.internal("snd/mus_menu.mp3"));
				menuMusic.setLooping(true);
			} catch (Exception e) {
				System.out.println("Could not load menu music: " + e.getMessage());
				return;
			}
		}
		if (isMusicEnabled() && !menuMusic.isPlaying()) {
			menuMusic.play();
		}
	}

	public void stopMenuMusic() {
		if (menuMusic != null && menuMusic.isPlaying()) {
			menuMusic.stop();
		}
	}

	public void applyAudioSettings() {
		if (getScreen() instanceof GameScreen gameScreen) {
			gameScreen.applyAudioSettings();
			return;
		}
		if (isMusicEnabled()) {
			playMenuMusic();
		} else {
			stopMenuMusic();
		}
	}

	public void playMenuConfirmSound() {
		if (!isMenuSoundEffectsEnabled()) return;
		if (menuConfirmSound == null) {
			try {
				menuConfirmSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_menu_confirm.mp3"));
			} catch (Exception e) {
				System.out.println("Could not load menu confirm sound: " + e.getMessage());
				return;
			}
		}
		menuConfirmSound.play(0.6f);
	}

	public void playMenuBackSound() {
		if (!isMenuSoundEffectsEnabled()) return;
		if (menuBackSound == null) {
			try {
				menuBackSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_menu_back.mp3"));
			} catch (Exception e) {
				System.out.println("Could not load menu back sound: " + e.getMessage());
				return;
			}
		}
		menuBackSound.play(0.6f);
	}

	public void playMenuNavigateSound() {
		if (!isMenuSoundEffectsEnabled()) return;
		if (menuNavigateSound == null) {
			try {
				menuNavigateSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_cycleturn.mp3"));
			} catch (Exception e) {
				System.out.println("Could not load menu navigate sound: " + e.getMessage());
				return;
			}
		}
		menuNavigateSound.play(0.35f);
	}

	public void playDisconnectSound() {
		if (!isMenuSoundEffectsEnabled()) return;
		if (disconnectSound == null) {
			try {
				disconnectSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_disconnect.mp3"));
			} catch (Exception e) {
				System.out.println("Could not load disconnect sound: " + e.getMessage());
				return;
			}
		}
		disconnectSound.play(0.6f);
	}

	public void playStartSound() {
		if (!isMenuSoundEffectsEnabled()) return;
		if (startSound == null) {
			try {
				startSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_start.mp3"));
			} catch (Exception e) {
				System.out.println("Could not load start sound: " + e.getMessage());
				return;
			}
		}
		startSound.play(0.6f);
	}

	public void playNewGameSound() {
		if (!isMenuSoundEffectsEnabled()) return;
		if (newGameSound == null) {
			try {
				newGameSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_newgame.mp3"));
			} catch (Exception e) {
				System.out.println("Could not load new game sound: " + e.getMessage());
				return;
			}
		}
		newGameSound.play(0.6f);
	}

	public void playVotingStartSound() {
		if (!isMenuSoundEffectsEnabled()) return;
		if (votingStartSound == null) {
			try {
				votingStartSound = Gdx.audio.newSound(Gdx.files.internal("snd/snd_votingstart.mp3"));
			} catch (Exception e) {
				System.out.println("Could not load voting start sound: " + e.getMessage());
				return;
			}
		}
		votingStartSound.play(0.6f);
	}

	public void onMatchFoundNotice() {
		matchFoundPending = true;
		lobbyNoticeText = "Match found!\nGame is starting...";
		stopMenuMusic();
		playStartSound();
	}

	public void clearMatchFoundNotice() {
		matchFoundPending = false;
		lobbyNoticeText = "";
	}

	public void requestLeaderboardRefresh() {
		leaderboardLoading = true;
		leaderboardStatusMessage = "Loading leaderboard...";
		if (networkClient != null && networkClient.isConnected()) {
			networkClient.send("C_REQUEST_LEADERBOARD");
		} else {
			leaderboardLoading = false;
			leaderboardStatusMessage = "Connect to a server to load leaderboard data.";
		}
	}

	public void setLeaderboardEntries(List<LeaderboardEntry> entries) {
		leaderboardEntries = entries == null ? Collections.emptyList() : List.copyOf(entries);
		leaderboardLoading = false;
		leaderboardStatusMessage = leaderboardEntries.isEmpty() ? "No match data recorded yet." : "";
		leaderboardVersion++;
	}

	// Helper methods to switch screens
	public void showMainMenu() {
		matchPlayerColor = null;
		matchOppColor = null;
		setScreen(new MainMenuScreen(this));
	}

	public void showConnectScreen() {
		setScreen(new ConnectScreen(this));
	}

	public void handleServerConnectionLost() {
		practiceMode = false;
		clearMatchFoundNotice();
		roundResultText = null;
		finalMatchResult = false;
		matchPlayerColor = null;
		matchOppColor = null;
		connectStatusMessage = "Connection to Server Lost.";
		connectStatusIsError = true;
		playDisconnectSound();
		showConnectScreen();
	}

	public void showPlayerSetupScreen() {
		setScreen(new PlayerSetupScreen(this));
	}

	public void showLobbyScreen() {
		setScreen(new LobbyScreen(this));
	}

	public void showLeaderboardScreen() {
		setScreen(new LeaderboardScreen(this));
	}

	public void showGameScreen() {
		clearMatchFoundNotice();
		setScreen(new GameScreen(this));
	}

	public void showRematchVoteScreen() {
		setScreen(new RematchVoteScreen(this));
	}

	public void showSettingsScreen() {
		setScreen(new SettingsScreen(this));
	}

	public void startSingleplayerGame() {
		practiceMode = true;
		networkClient.disconnect();
		resetPracticeMatchState();
		showGameScreen();
	}

	public void startLobbyPracticeGame() {
		practiceMode = true;
		clearMatchFoundNotice();
		resetPracticeMatchState();
		showGameScreen();
	}

	private void resetPracticeMatchState() {
		matchPlayerColor = null;
		matchOppColor = null;
		oppName = "Bot";
		oppColor = "Red";
		lobbyPlayerCount = 0;
		isPlayerA = true;
		winnerName = "";
		countdownMessage = null;
		countdownActive = false;
		aWins = 0;
		bWins = 0;
		roundNumber = 1;
		ax = 24;
		ay = 20;
		bx = 24;
		by = 60;
		aDir = 'D';
		bDir = 'U';
	}

	private float pingTimer = 0f;

	@Override
	public void render() {
		super.render();
		// Heartbeat ping every 5 seconds to prevent server timeout
		pingTimer += com.badlogic.gdx.Gdx.graphics.getDeltaTime();
		if (pingTimer >= 5f) {
			pingTimer = 0f;
			if (networkClient != null) {
				networkClient.send("C_PING|" + System.currentTimeMillis());
			}
		}
		if (networkClient != null) {
			networkClient.checkConnectionTimeout();
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (menuMusic != null) {
			menuMusic.stop();
			menuMusic.dispose();
			menuMusic = null;
		}
		if (menuConfirmSound != null) menuConfirmSound.dispose();
		if (menuBackSound != null) menuBackSound.dispose();
		if (menuNavigateSound != null) menuNavigateSound.dispose();
		if (disconnectSound != null) disconnectSound.dispose();
		if (startSound != null) startSound.dispose();
		if (newGameSound != null) newGameSound.dispose();
		if (votingStartSound != null) votingStartSound.dispose();
	}
}
