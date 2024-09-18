package smlauncher;

import org.json.JSONObject;
import smlauncher.fileio.TextFileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Stores launch settings and manages the settings file.
 *
 * @author SlavSquatSuperstar
 */
public final class LaunchSettings {

	// TODO store more properties

	private static final String SETTINGS_FILENAME = "launch-settings.json";
	private static JSONObject launchSettings;

//	static {
//		// Make sure this isn't null on startup
//		launchSettings = getDefaultLaunchSettings();
//	}

	private LaunchSettings() {
	}

	// Settings File Methods

	public static void readSettings() {
		File jsonFile = new File(SETTINGS_FILENAME);
		JSONObject defaultSettings = getDefaultLaunchSettings();

		// Create file if not present
		if(!jsonFile.exists()) {
			launchSettings = defaultSettings;
			saveSettings();
		} else {
			// Read the settings file
			try {
				launchSettings = new JSONObject(TextFileUtil.readText(jsonFile));
			} catch(IOException e) {
				System.out.println("Could not read launch settings from file");
			}
		}
	}

	public static void saveSettings() {
		File settingsFile = new File(SETTINGS_FILENAME);
		try {
			TextFileUtil.writeText(settingsFile, launchSettings.toString());
		} catch(IOException exception) {
			System.out.println("Could not save launch settings to file");
		}
	}

	private static JSONObject getDefaultLaunchSettings() {
		JSONObject settings = new JSONObject();
		settings.put("installDir", "./StarMade");
		settings.put("jvm_args", "");
		settings.put("lastUsedBranch", 0); // Release
		settings.put("lastUsedVersion", "NONE");
		settings.put("launchArgs", "");
		settings.put("memory", 4096);
		return settings;
	}

	// Settings Getters and Setters

	public static String getInstallDir() {
		return launchSettings.getString("installDir");
	}

	public static void setInstallDir(String installDir) {
		launchSettings.put("installDir", installDir);
	}

	public static String getJvmArgs() {
		return launchSettings.getString("jvm_args");
	}

	public static void setJvmArgs(String jvmArgs) {
		launchSettings.put("jvm_args", jvmArgs);
	}

	public static String getLaunchArgs() {
		return launchSettings.getString("launchArgs");
	}

	public static void setLaunchArgs(String launchArgs) {
		launchSettings.put("launchArgs", launchArgs);
	}

	public static int getLastUsedBranch() {
		return launchSettings.getInt("lastUsedBranch");
	}

	public static void setLastUsedBranch(int lastUsedBranch) {
		launchSettings.put("lastUsedBranch", lastUsedBranch);
	}

	public static String getLastUsedVersion() {
		return launchSettings.getString("lastUsedVersion");
	}

	public static void setLastUsedVersion(String lastUsedVersion) {
		launchSettings.put("lastUsedVersion", lastUsedVersion);
	}

	public static int getMemory() {
		return launchSettings.getInt("memory");
	}

	public static void setMemory(int memory) {
		launchSettings.put("memory", memory);
	}

}
