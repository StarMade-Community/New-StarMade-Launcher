package smlauncher.downloader;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

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
	private Thread downloadThread;
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
		(downloadThread = new Thread(() -> {
			try {
				download();
				unzip();
				//If on Linux or Mac, mark the Java executable as executable
				if(currentOS == OperatingSystem.LINUX || currentOS == OperatingSystem.MAC) (new File(getJreFolderName() + "/bin/java")).setExecutable(true);
				else if(currentOS == OperatingSystem.WINDOWS) (new File(getJreFolderName() + "/bin/java.exe")).setExecutable(true);
				else throw new IOException("Downloaded Java, but failed to mark it as executable due to unknown OS: " + currentOS);
				finished = true;
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		})).start();
	}

	public void download() throws IOException {
		String url = getJavaURL();
		if(url == null) return;

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
		File zipFile = new File(zipFilename);

		// Extract the file
		UnArchiver unzipper;
		if("zip".equals(currentOS.zipExtension)) {
			unzipper = new ZipUnArchiver(zipFile);
		} else {
			unzipper = new TarGZipUnArchiver(zipFile);
		}
		unzipper.setDestDirectory(new File("./"));
		unzipper.extract();

		cleanupZip(); // Delete the zip file
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
		FileUtils.moveDirectory(extractedFolder, jreFolder);
	}

	// Helper Methods

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
//		File zipFile = new File(getZipFilename());
//		if(zipFile.exists()) zipFile.delete();
	}

	void cleanupFolder() {
		File jreFolder = new File(getJreFolderName());
		try {
			if(jreFolder.exists()) {
				FileUtils.cleanDirectory(jreFolder);
				FileUtils.deleteDirectory(jreFolder);
			}
		} catch(IOException ignored) {
		}
	}

	public void forceStopThread() {
		if(downloadThread != null) downloadThread.interrupt();
	}

	public boolean isDownloaded() {
		return doesJreFolderExist() && finished;
	}
}
