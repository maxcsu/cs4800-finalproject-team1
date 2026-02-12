package edu.csu.javatron.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import edu.csu.javatron.JavaTronGame;

import org.lwjgl.glfw.GLFW;

//This file was handled by Max, with help from ChatGPT.
//Rendering game window, tiling grid backdrop, enforcing window constraints.

// Desktop entry point for LWJGL3.
// This file controls window size, title, icon and vsync.


/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
    	// Restarts the Java virtual machine if necessary
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    // Defines and spawns our desktop application.
    private static Lwjgl3Application createApplication() {
        Lwjgl3Application app = new Lwjgl3Application(new JavaTronGame(), getDefaultConfiguration());
        
        return app;
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
    	
    	// This file does not directly control aspect-ratio enforcement!
    	// Refer to FirstScreen.java for that. It's under FirstScreen.resize()

    	// Define the window size, scaling from the defined virtual resolution.
    	float scale = 1.25f; // Variable to define how much we're scaling the resolution factor
    	int windowWidth = (int)(JavaTronGame.VIRTUAL_WIDTH * scale); // Inherit width from JavaTronGame
    	int windowHeight = (int)(JavaTronGame.VIRTUAL_HEIGHT * scale); // Inherit height
    	
    	// Prevent window from opening off-screen by clamping the size
    	var displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode(); // Figure out what display mode we're using
    	windowWidth = Math.min(windowWidth,  displayMode.width); // Clamp width and height so it doesn't go off-screen
    	windowHeight = Math.min(windowHeight,  displayMode.height);
    	
    	
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("JavaTron");
        //// Vsync limits the frames per second to what your hardware can display, and helps eliminate
        //// screen tearing. This setting doesn't always work on Linux, so the line after is a safeguard.
        configuration.useVsync(true);
        //// Limits FPS to the refresh rate of the currently active monitor, plus 1 to try to match fractional
        //// refresh rates. The Vsync setting above should limit the actual FPS to match the monitor.
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        //// useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.

        configuration.setWindowedMode(windowWidth, windowHeight);
        configuration.setResizable(true);
        //// You can change these files; they are in lwjgl3/src/main/resources/ .
        //// They can also be loaded from the root of assets/ .
        configuration.setWindowIcon("lightcycle_blue.png");

        //// This could improve compatibility with Windows machines with buggy OpenGL drivers, Macs
        //// with Apple Silicon that have to emulate compatibility with OpenGL anyway, and more.
        //// This uses the dependency `com.badlogicgames.gdx:gdx-lwjgl3-angle` to function.
        //// You would need to add this line to lwjgl3/build.gradle , below the dependency on `gdx-backend-lwjgl3`:
        ////     implementation "com.badlogicgames.gdx:gdx-lwjgl3-angle:$gdxVersion"
        //// You can choose to add the following line and the mentioned dependency if you want; they
        //// are not intended for games that use GL30 (which is compatibility with OpenGL ES 3.0).
        //// Know that it might not work well in some cases.
//        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 0, 0);

        return configuration;
    }
}