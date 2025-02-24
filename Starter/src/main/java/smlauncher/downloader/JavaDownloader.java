package smlauncher.downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

/**
 * Downloads a JDK from the web and unzips it.
 *
 * @author SlavSquatSuperstar
 */
public class JavaDownloader {

	private final OperatingSystem currentOS;
	private final JavaVersion version;
	private boolean finished;

	public JavaDownloader(JavaVersion version) {
		this(OperatingSystem.getCurrent(), version);
	}

	// Set OS for testing only
	JavaDownloader(OperatingSystem currentOS, JavaVersion version) {
		this.currentOS = currentOS;
		this.version = version;
	}

	public void downloadAndUnzip() throws IOException {
		// Don't unzip if the folder already exists
		if(doesJreFolderExist()) return;
		(new Thread(() -> {
			try {
				download();
				unzip();
				//If on Linux or Mac, mark the Java executable as executable
				if(currentOS == OperatingSystem.LINUX || currentOS == OperatingSystem.MAC) (new File(getJreFolderName() + "/bin/java")).setExecutable(true);
				else if(currentOS == OperatingSystem.WINDOWS) (new File(getJreFolderName() + "/bin/java.exe")).setExecutable(true);
				else throw new IOException("Downloaded Java, but failed to mark it as executable due to unknown OS: " + currentOS);
				cleanupZip();
				finished = true;
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		})).start();
	}

	public void download() throws IOException {
		String url = getJavaURL();
		URL website = new URL(url);
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		String destination = getZipFilename();
		FileOutputStream fos = new FileOutputStream(destination);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
		System.out.println("Downloaded " + destination);
	}

	public void unzip() throws IOException {
		String zipFilename = getZipFilename();
		//Extract the file
		//We cant use UnArchiver, because GraalVM won't include it even though its in the fucking jar
		//We have to do this the hard way using OS specific commands
		ProcessBuilder processBuilder;
		Process process = null;
		switch(OperatingSystem.getCurrent()) {
			case WINDOWS:
				if(zipFilename.endsWith(".tar.gz")) throw new IOException("Cannot extract .tar.gz files on Windows"); //Why was it downloaded then?
				else processBuilder = new ProcessBuilder("powershell.exe", "Expand-Archive", "-Path", zipFilename, "-DestinationPath", ".");
				processBuilder.redirectErrorStream(true);
				process = processBuilder.start();
				break;
			case LINUX:
			case SOLARIS:
			case MAC:
				if(zipFilename.endsWith(".tar.gz")) processBuilder = new ProcessBuilder("tar", "-xzf", zipFilename);
				else processBuilder = new ProcessBuilder("unzip", zipFilename);
				processBuilder.redirectErrorStream(true);
				process = processBuilder.start();
				break;
			default:
				throw new IOException("Unknown OS: " + OperatingSystem.getCurrent());
		}
		if(process != null) {
			try {
				process.waitFor();
			} catch(InterruptedException exception) {
				exception.printStackTrace();
			}
		}
		
		moveExtractedFolder();
		System.out.println("Unzipped " + zipFilename);
		
		//Mark as executable
		if(currentOS == OperatingSystem.LINUX || currentOS == OperatingSystem.SOLARIS || currentOS == OperatingSystem.MAC) (new File(getJreFolderName() + "/bin/java")).setExecutable(true);
		else if(currentOS == OperatingSystem.WINDOWS) (new File(getJreFolderName() + "/bin/java.exe")).setExecutable(true);
		else throw new IOException("Downloaded Java, but failed to mark it as executable due to unknown OS: " + currentOS);
	}

	private void moveExtractedFolder() throws IOException {
		File jreFolder = new File(getJreFolderName());
		File extractedFolder = null;

		// Find the extracted folder
		for(File file : Objects.requireNonNull(new File("./").listFiles())) {
			if(file.getName().startsWith(version.fileStart)) {
				extractedFolder = file;
				break;
			}
		}
		if(extractedFolder == null) throw new IOException("Could not find extracted folder");

		// Rename the extracted folder to jre<#>/
		extractedFolder.renameTo(jreFolder);
	}
	
	private boolean doesJreFolderExist() {
		File jreFolder = new File(getJreFolderName());
		return jreFolder.isDirectory();
	}

	private String getJavaURL() {
		return String.format(version.fmtURL, currentOS.toString(), currentOS.zipExtension);
	}

	private String getZipFilename() {
		return String.format("jre%d.%s", version.number, currentOS.zipExtension);
	}

	public String getJreFolderName() {
		return "./StarMade/jre" + version.number;
	}

	void cleanupZip() {
		for(File file : Objects.requireNonNull(new File("./").listFiles())) {
			if(file.getName().endsWith(currentOS.zipExtension)) file.delete();
		}
	}

	public boolean isDownloaded() {
		return doesJreFolderExist() && finished;
	}
}
