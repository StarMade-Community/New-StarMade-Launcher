package smlauncher.downloader;

/**
 * A version of the Java Runtime Environment (JRE).
 *
 * @author SlavSquatSuperstar
 */
public enum JavaVersion {

//	JAVA_8(8, "jdk8", "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u392-b08/OpenJDK8U-jre_x64_%s_hotspot_8u392b08.%s"),
//	JAVA_18(18, "jdk-18", "https://github.com/adoptium/temurin18-binaries/releases/download/jdk-18.0.2.1%%2B1/OpenJDK18U-jre_x64_%s_hotspot_18.0.2.1_1.%s");
	JAVA_21(21, "jdk-21", "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%%2B11/OpenJDK21U-jdk_x64_%s_hotspot_21.0.5_11.%s");
	
	public final int number; // Version number
	public final String fileStart; // JDK folder header
	public final String fmtURL; // Base download URL

	JavaVersion(int number, String fileStart, String fmtURL) {
		this.number = number;
		this.fileStart = fileStart;
		this.fmtURL = fmtURL;
	}
	
	@Override
	public String toString() {
		return "Java " + number;
	}
}
