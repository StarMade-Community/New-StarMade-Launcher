package smlauncher;

import java.io.File;

/**
 * Used to start the launcher. This needs to be separate, as GraalVM doesn't cooperate with Java Swing, and therefore the launcher needs to be started in a separate process.
 *
 * @author TheDerpGamer
 */
public class StarMadeStarter {
	
	public static void main(String[] args) {
		try {
			//Look for ./lib/StarMade Launcher.jar
			String launcherPath = "./lib/StarMade-Launcher.jar";
			File launcher = new File(launcherPath);
			if(launcher.exists()) {
				if(!launcher.canExecute()) {
					//Make the file executable
					launcher.setExecutable(true);
				}
				//Start the launcher
				ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", launcherPath, String.join(" ", args));
				processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
				processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
				processBuilder.start();
			} else System.err.println("Launcher not found at: " + launcherPath);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
}
