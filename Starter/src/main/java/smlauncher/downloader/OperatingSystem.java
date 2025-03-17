package smlauncher.downloader;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * A computer's operating system (OS).
 *
 * @author SlavSquatSuperstar
 * @author TheDerpGamer
 */
public enum OperatingSystem {

	LINUX(new String[]{"linux", "unix"}, "tar.gz", "jre23/bin/java"),
	SOLARIS(new String[]{"sunos", "solaris"}, "tar.gz", "jre23/bin/java"),
	MAC(new String[]{"mac"}, "tar.gz", "jre23/Contents/Home/bin/java"),
	WINDOWS(new String[]{"win"}, "zip", "jre23/bin/java.exe"),
	UNKNOWN(new String[0], null, null);

	// Instance
	private static OperatingSystem currentOS;
	public final String zipExtension; // The archive file extension
	public final String javaPath; // Path to the java executable
	// Fields
	private final String[] names; // Names used by Java
//	private String serial;

	OperatingSystem(String[] names, String zipExtension, String javaPath) {
		this.names = names;
		this.zipExtension = zipExtension;
		this.javaPath = javaPath;
	}

	/**
	 * Get the operating system of the current machine.
	 *
	 * @return the current OS
	 */
	public static OperatingSystem getCurrent() {
		if(currentOS != null) return currentOS;

		String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
		for(OperatingSystem os : values()) {
			for(String name : os.names) {
				if(osName.contains(name)) {
					// do some stuff with serial
//					os.serial = "not retrieved";
//					System.err.println("READ SERIAL for " + os.name() + ": " + os.serial);
					currentOS = os;
					return currentOS;
				}
			}
		}
		currentOS = UNKNOWN;
		return currentOS;
	}

	public static File getAppDir() throws IOException {
		return getAppDir("StarMade");
	}

	/**
	 * Get the location of the given folder for the current OS in the user's home directory.
	 *
	 * @param appName the folder filename
	 * @return the application directory
	 * @throws IOException if the directory cannot be created or is a file
	 */
	public static File getAppDir(String appName) throws IOException {
		String homeDir = System.getProperty("user.home", ".");
		File appDir;

		switch(getCurrent()) {
			case WINDOWS:
				String appDataDir = System.getenv("APPDATA");
				if(appDataDir != null) appDir = new File(appDataDir, "." + appName + '/');
				else appDir = new File(homeDir, '.' + appName + '/');
				break;
			case MAC:
				appDir = new File(homeDir, "Library/Application Support/" + appName);
				break;
			case LINUX:
			case SOLARIS:
				appDir = new File(homeDir, '.' + appName + '/');
				break;
			default:
				appDir = new File(homeDir, appName + '/');
				break;
		}

		if(!appDir.exists() && !appDir.mkdirs()) {
			throw new IOException("Error: Failed to create working directory: " + appDir);
		} else if(!appDir.isDirectory()) {
			throw new IOException("Error: File is not a directory: " + appDir);
		} else {
			return appDir;
		}
	}

	//	private String getSerial(){
	//		switch(this){
	//			case LINUX: return Hardware4Nix.getSerialNumber();
	//			case SOLARIS: return Hardware4Nix.getSerialNumber();
	//			case MAC: return Hardware4Mac.getSerialNumber();
	//			case WINDOWS: return Hardware4Win.getSerialNumber();
	//			case UNKNOWN: return Hardware4Win.getSerialNumber();
	//			default: throw new IllegalArgumentException("No operationg system: "+OS);
	//		}
	//	}

}
