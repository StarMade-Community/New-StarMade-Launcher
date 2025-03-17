package smlauncher;

import smlauncher.downloader.OperatingSystem;
import java.io.File;

/**
 * Used to start the launcher. This needs to be separate, as GraalVM doesn't cooperate with Java Swing, and therefore the launcher needs to be started in a separate process.
 *
 * @author TheDerpGamer
 */
public class StarMadeStarter {
	
	public static void main(String[] args) {
		try {
			String launcherPath = "./StarMade-Launcher.jar";
			File launcher = new File(launcherPath);
			if(launcher.exists()) {
				if(!launcher.canExecute()) launcher.setExecutable(true);
				String javaPath = "./StarMade/jre23/bin/java";
				if(OperatingSystem.getCurrent() == OperatingSystem.WINDOWS) javaPath += ".exe";
				else if(OperatingSystem.getCurrent() == OperatingSystem.MAC) javaPath = "./StarMade/jre23/Contents/Home/bin/java";
				ProcessBuilder processBuilder = new ProcessBuilder(javaPath, "-jar", launcherPath, String.join(" ", args));
				processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
				processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
				processBuilder.start();
			} else System.err.println("Launcher not found at: " + launcherPath);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
}
