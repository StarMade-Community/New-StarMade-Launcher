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
			//Look for ./StarMade Launcher.jar
			String launcherPath = "./StarMade-Launcher.jar";
			File launcher = new File(launcherPath);
			if(launcher.exists()) {
				if(!launcher.canExecute()) launcher.setExecutable(true);
				File smFolder = new File("./StarMade");
				if(!smFolder.exists()) smFolder.mkdirs();
				
				String javaPath = "./jre23/bin/java";
				if(OperatingSystem.getCurrent() == OperatingSystem.WINDOWS) javaPath += ".exe";
				else if(OperatingSystem.getCurrent() == OperatingSystem.MAC) javaPath = "./jre23/Contents/Home/bin/java";
				
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
