package smlauncher;

import smlauncher.downloader.JavaDownloader;
import smlauncher.downloader.JavaVersion;
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
			//Look for ./lib/StarMade Launcher.jar
			String launcherPath = "./lib/StarMade-Launcher.jar";
			File launcher = new File(launcherPath);
			if(launcher.exists()) {
				if(!launcher.canExecute()) launcher.setExecutable(true);
				File smFolder = new File("./StarMade");
				if(!smFolder.exists()) smFolder.mkdirs();

				//We have to download Java 18 to run the launcher and then execute a command as GraalVM doesn't support Java Swing
				JavaDownloader javaDownloader = new JavaDownloader(JavaVersion.JAVA_21);
				javaDownloader.downloadAndUnzip();
				
				String javaPath = javaDownloader.getJreFolderName() + "/bin/java";
				if(OperatingSystem.getCurrent() == OperatingSystem.WINDOWS) javaPath += ".exe";
				
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
