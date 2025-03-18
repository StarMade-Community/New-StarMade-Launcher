package smlauncher;

/**
 * A version of the Java Runtime Environment (JRE).
 *
 * @author SlavSquatSuperstar
 */
public enum JavaVersion {

	JAVA_8(8, "jdk8", "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u392-b08/OpenJDK8U-jre_x64_%s_hotspot_8u392b08.%s"),
	JAVA_23(23, "jdk-23", "https://github.com/adoptium/temurin23-binaries/releases/download/jdk-23.0.2|2B7/OpenJDK23U-jdk_x64_%s_hotspot_23.0.2_7.%s");

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