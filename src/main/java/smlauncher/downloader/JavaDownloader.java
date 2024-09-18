package smlauncher.downloader;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import smlauncher.LaunchSettings;
import smlauncher.util.OperatingSystem;

import javax.swing.*;
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

	public JavaDownloader(JavaVersion version) {
		this(OperatingSystem.getCurrent(), version);
	}

	// Set OS for testing only
	JavaDownloader(OperatingSystem currentOS, JavaVersion version) {
		this.currentOS = currentOS;
		this.version = version;
	}

	public void downloadAndUnzip(JDialog dialog) throws IOException {
		// Don't unzip if the folder already exists
		if(doesJreFolderExist()) return;
		new Thread(() -> {
			try {
				download();
				unzip();
				dialog.setVisible(false);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}).start();
		dialog.setVisible(true);
		while(dialog.isVisible()) {
			try {
				Thread.sleep(100);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
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

	private String getJreFolderName() {
		return LaunchSettings.getInstallDir() + "/jre" + version.number;
	}

	void cleanupZip() {
		File zipFile = new File(getZipFilename());
		if(zipFile.exists()) zipFile.delete();
	}

	void cleanupFolder() {
		String folderName = getJreFolderName();
		System.out.println(folderName);
		File jreFolder = new File(getJreFolderName());
		try {
			if(jreFolder.exists()) {
				FileUtils.cleanDirectory(jreFolder);
				FileUtils.deleteDirectory(jreFolder);
			}
		} catch(IOException ignored) {
		}
	}

}
