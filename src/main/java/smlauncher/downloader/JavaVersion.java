package smlauncher.downloader;

/**
 * A version of the Java Runtime Environment (JRE).
 *
 * @author SlavSquatSuperstar
 */
public enum JavaVersion {

	JAVA_8(8, "jre8"),
	JAVA_23(23, "jre23");

	public final int number; // Version number
	public final String fileStart; // JDK folder header

	JavaVersion(int number, String fileStart) {
		this.number = number;
		this.fileStart = fileStart;
	}

	public static JavaVersion getWithNumber(int number) {
		if(number == 8) return JAVA_8;
		else if(number == 23) return JAVA_23;
		else return null;
	}

	@Override
	public String toString() {
		return "Java " + number;
	}
}
