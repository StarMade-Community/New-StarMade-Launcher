package smlauncher.starmade;

import smlauncher.LogManager;
import smlauncher.StarMadeLauncher;
import smlauncher.community.LauncherCommunityPanel;
import smlauncher.util.OperatingSystem;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class ErrorDialog extends JDialog {

	private static final int STACKTRACE_LIMIT = 30;

	public ErrorDialog(String error, String description, Throwable exception, boolean exitAfterConfirmation) {
		StarMadeLauncher.emergencyStop();
		setTitle("Error");
		setSize(750, 500);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		setResizable(true);
		setAlwaysOnTop(true);

		JPanel errorPanel = new JPanel();
		errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.Y_AXIS));
		errorPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JTextArea errorLabel = new JTextArea(error);
		errorLabel.setEditable(false);
		errorLabel.setFont(new Font("Arial", Font.BOLD, 16));
		errorPanel.add(errorLabel, BorderLayout.NORTH);

		//scroll pane

		JPanel descriptionPanel = new JPanel();
		add(descriptionPanel, BorderLayout.CENTER);
		
		StringBuilder descriptionBuilder = new StringBuilder();
		descriptionBuilder.append("Description:\n");
		descriptionBuilder.append("\t").append(description).append("\n");
		descriptionBuilder.append("Stack Trace:\n");
		int i = 0;
		for(StackTraceElement element : exception.getStackTrace()) {
			if(i > STACKTRACE_LIMIT) {
				descriptionBuilder.append("\t...").append("\n");
				break;
			}
			descriptionBuilder.append("\t").append(element.toString()).append("\n");
			i ++;
		}

		JTextArea stackTraceLabel = new JTextArea(descriptionBuilder.toString());
		stackTraceLabel.setFont(new Font("Arial", Font.BOLD, 12));
		descriptionPanel.add(stackTraceLabel);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(buttonPanel, BorderLayout.SOUTH);

		JButton okButton = new JButton("Ok");
		okButton.addActionListener(e -> {
			dispose();
			if(exitAfterConfirmation) System.exit(-1);
		});
		buttonPanel.add(okButton);
		buttonPanel.add(Box.createHorizontalGlue());

		JButton reportButton = new JButton("Report");
		reportButton.addActionListener(e -> {
			dispose();
			File reportFile = LogManager.createErrorReport(error, description, exception);
			if(reportFile != null) JOptionPane.showMessageDialog(null, "Error report saved to: " + reportFile.getAbsolutePath(), "\nPlease create a new bug report on the GitHub issues page and upload this file!", JOptionPane.INFORMATION_MESSAGE);
			else JOptionPane.showMessageDialog(null, "Failed to save error report", "Error Report", JOptionPane.ERROR_MESSAGE);
			try {
				Desktop.getDesktop().browse(new URI(StarMadeLauncher.BUG_REPORT_URL));
			} catch(IOException | URISyntaxException exception1) {
				exception1.printStackTrace();
				throw new RuntimeException(exception1);
			}
			if(exitAfterConfirmation) System.exit(-1);
		});
		buttonPanel.add(reportButton);
		buttonPanel.add(Box.createHorizontalGlue());

		JButton starmadeDiscordButton = new JButton("StarMade Discord");
		starmadeDiscordButton.addActionListener(e -> {
			if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				try {
					Desktop.getDesktop().browse(new URI(LauncherCommunityPanel.MAIN_DISCORD_URL));
				} catch(IOException | URISyntaxException exception1) {
					exception1.printStackTrace();
					throw new RuntimeException(exception1);
				}
			}
		});
		buttonPanel.add(starmadeDiscordButton);
		buttonPanel.add(Box.createHorizontalGlue());

		JButton supportDiscordButton = new JButton("Support Discord");
		supportDiscordButton.addActionListener(e -> {
			if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				try {
					Desktop.getDesktop().browse(new URI(LauncherCommunityPanel.STARLOADER_DISCORD_URL));
				} catch(IOException | URISyntaxException exception1) {
					exception1.printStackTrace();
					throw new RuntimeException(exception1);
				}
			}
		});
		buttonPanel.add(supportDiscordButton);
	}
}
