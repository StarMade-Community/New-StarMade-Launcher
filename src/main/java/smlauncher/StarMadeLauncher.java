package smlauncher;

import com.formdev.flatlaf.FlatDarkLaf;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import smlauncher.community.LauncherCommunityPanel;
import smlauncher.fileio.TextFileUtil;
import smlauncher.news.LauncherNewsPanel;
import smlauncher.starmade.*;
import smlauncher.util.OperatingSystem;
import smlauncher.util.Palette;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Main class for the StarMade Launcher.
 *
 * @author TheDerpGamer
 * @author SlavSquatSuperstar
 */
public class StarMadeLauncher extends JFrame {

	public static final String BUG_REPORT_URL = "https://github.com/garretreichenbach/New-StarMade-Launcher/issues";
	private static final String J23ARGS = "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED";
	private static IndexFileEntry gameVersion;
	private static GameBranch lastUsedBranch = GameBranch.RELEASE;
	private static boolean debugMode;
	private static String selectedVersion;
	private static boolean serverMode;
	private static int port;
	private static OperatingSystem currentOS;
	private static JTextField portField;
	private final VersionRegistry versionRegistry;
	private final DownloadStatus dlStatus = new DownloadStatus();
	private static UpdaterThread updaterThread;
	private int mouseX;
	private int mouseY;
	private JButton updateButton;
	private JPanel mainPanel;
	private JPanel centerPanel;
	private JPanel footerPanel;
	private JPanel versionPanel;
	private JPanel playPanel;
	private JPanel serverPanel;
	private JPanel playPanelButtons;
	private JScrollPane centerScrollPane;
	private LauncherNewsPanel newsPanel;
	private LauncherCommunityPanel communityPanel;
	private JavaDownloader downloader;

	public StarMadeLauncher() {
		// Set window properties
		super("StarMade Launcher");
		Thread.currentThread().setUncaughtExceptionHandler((t, e) -> LogManager.logFatal("Encountered an unexpected error \"" + e.getClass().getSimpleName() + "\"", e));

		setBounds(100, 100, 800, 550);
		setMinimumSize(new Dimension(800, 550));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		// Set window icon
		try {
			URL resource = StarMadeLauncher.class.getResource("/sprites/icon.png");
			if(resource != null) setIconImage(Toolkit.getDefaultToolkit().getImage(resource));
			else setIconImage(Toolkit.getDefaultToolkit().getImage("icon.png"));
		} catch(Exception exception) {
			LogManager.logException("Failed to set window icon", exception);
		}

		// Fetch game versions
		versionRegistry = new VersionRegistry();
		try {
			versionRegistry.createRegistry();
		} catch(Exception exception) {
			LogManager.logException("Failed to fetch version list! Check your internet connection!", exception);
			JOptionPane.showMessageDialog(this, "Failed to fetch version list! Check your internet connection!", "Warning", JOptionPane.WARNING_MESSAGE);
		}

		// Read launch settings
		LaunchSettings.readSettings();
		LogManager.initialize();

		// Read game version and branch
		
		gameVersion = getLastUsedVersion();
		setGameVersion(gameVersion);
		setBranch(gameVersion.branch);

		LaunchSettings.saveSettings();

		// Get the current OS
		currentOS = OperatingSystem.getCurrent();

		// Create launcher UI
		createMainPanel();
		createNewsPanel();
		dispose();
		setUndecorated(true);
		setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
		setResizable(false);
		getRootPane().setDoubleBuffered(true);
		setVisible(true);
	}

	public static void main(String[] args) {
		Thread.currentThread().setUncaughtExceptionHandler((t, e) -> LogManager.logFatal("Encountered an unexpected error \"" + e.getClass().getSimpleName() + "\"", e));
		boolean headless = false;
		int backupMode = GameUpdater.BACK_DB;
		boolean selectVersion = false;
		boolean autoUpdate = true;

		if(args == null || args.length == 0) startup();
		else {
			GameBranch buildBranch = GameBranch.RELEASE;
			List<String> argList = new ArrayList<>(Arrays.asList(args));
			if(argList.contains("-debug_mode")) debugMode = true;
			if(argList.contains("-no_update")) autoUpdate = false;
			if(argList.contains("-version")) {
				selectVersion = true;
				if(argList.contains("-dev")) buildBranch = GameBranch.DEV;
				else if(argList.contains("-pre")) buildBranch = GameBranch.PRE;
				else buildBranch = GameBranch.RELEASE;
			} else if(argList.contains("-no_gui") || argList.contains("-nogui")) {
				displayHelp();
				headless = true;
			} else if(argList.contains("-backup")) {
				backupMode = GameUpdater.BACK_ALL;
			} else if(argList.contains("-no_backup")) {
				backupMode = GameUpdater.BACK_NONE;
			} else if(argList.contains("-help")) {
				displayHelp();
				return;
			} else if(argList.contains("-headless")) {
				headless = true;
			} else if(argList.contains("-server")) {
				boolean hasPort = false;
				if(argList.contains("-port:")) {
					try {
						port = Integer.parseInt(argList.get(argList.indexOf("-port:") + 1).trim());
						hasPort = true;
					} catch(NumberFormatException ignored) {
					}
				}
				if(!hasPort) {
					displayHelp();
					System.out.println("Please specify a port for the server to run on");
					return;
				}
				headless = true;
				serverMode = true;
			}
			LaunchSettings.readSettings();
			if(autoUpdate) {
				if(headless) GameUpdater.withoutGUI(true, LaunchSettings.getInstallDir(), buildBranch, backupMode, selectVersion);
			}

			if(headless) {
				System.out.println("Running in headless mode");
				gameVersion = new VersionRegistry().getLatestVersion(buildBranch);
				if(gameVersion == null) {
					System.err.println("Could not get latest game version, defaulting to Java 8");
					//Get last used version from config
					try {
						gameVersion = new VersionRegistry().getLatestVersion(GameBranch.values()[LaunchSettings.getLastUsedBranch()]);
					} catch(Exception exception) {
						LogManager.logException("Failed to get last used game version", exception);
						gameVersion = new VersionRegistry().getLatestVersion(GameBranch.RELEASE);
					}
					setGameVersion(gameVersion);
				}
				setGameVersion(gameVersion);
				if(serverMode) startServerHeadless();
				else startGameHeadless();
			} else startup();
		}
	}

	public static void emergencyStop() {
		if(updaterThread != null) updaterThread.interrupt();
	}

	private static void startGameHeadless() {
		ArrayList<String> commandComponents = getCommandComponents(false);
		ProcessBuilder process = new ProcessBuilder(commandComponents);
		process.directory(new File(LaunchSettings.getInstallDir()));
		process.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		process.redirectError(ProcessBuilder.Redirect.INHERIT);
		try {
			process.start();
		} catch(Exception exception) {
			LogManager.logFatal("Failed to start game in headless mode", exception);
		}
	}

	private static void startServerHeadless() {
		ArrayList<String> commandComponents = getCommandComponents(true);
		ProcessBuilder process = new ProcessBuilder(commandComponents);
		process.directory(new File(LaunchSettings.getInstallDir()));
		process.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		process.redirectError(ProcessBuilder.Redirect.INHERIT);
		try {
			System.out.println("Command: " + String.join(" ", commandComponents));
			process.start();
		} catch(Exception exception) {
			LogManager.logFatal("Failed to start server in headless mode", exception);
		}
	}

	private static void startup() {
		EventQueue.invokeLater(() -> {
			try {
				FlatDarkLaf.setup();
				startLauncherFrame();
			} catch(Exception exception) {
				LogManager.logException("Failed to start launcher", exception);
			}
		});
	}

	private static void startLauncherFrame() {
		JFrame frame = new StarMadeLauncher();
		(new Thread(() -> {
			//For steam: keep it repainting so the damn overlays go away
			try {
				Thread.sleep(1200);
			} catch(InterruptedException e) {
				LogManager.logWarning("Failed to sleep thread", e);
			}
			while(frame.isVisible()) {
				try {
					Thread.sleep(500);
				} catch(InterruptedException exception) {
					LogManager.logException("Failed to sleep thread", exception);
				}
				EventQueue.invokeLater(frame::repaint);
			}
		})).start();
	}

	public static ImageIcon getIcon(String s) {
		try {
			return new ImageIcon(ImageIO.read(Objects.requireNonNull(StarMadeLauncher.class.getResource("/" + s))));
		} catch(IOException exception) {
			return new ImageIcon();
		}
	}

	public static ImageIcon getIcon(String s, int width, int height) {
		try {
			return new ImageIcon(ImageIO.read(Objects.requireNonNull(StarMadeLauncher.class.getResource("/" + s))).getScaledInstance(width, height, Image.SCALE_SMOOTH));
		} catch(IOException exception) {
			LogManager.logException("Failed to get icon", exception);
			return new ImageIcon();
		}
	}

	public static void displayHelp() {
		System.out.println("StarMade Launcher Help:");
		System.out.println("-version : Version selection prompt");
		System.out.println("-no_gui : Don't start gui (needed for linux dedicated servers)");
		System.out.println("-no_backup : Don't create backup (default backup is server database only)");
		System.out.println("-backup_all : Create backup of everything (default backup is server database only)");
		System.out.println("-pre : Use pre branch (default is release)");
		System.out.println("-dev : Use dev branch (default is release)");
		System.out.println("-server -port: <port> : Start in server mode");
	}

	private static String getCurrentUser() {
		try {
			return StarMadeCredentials.read().getUser();
		} catch(Exception exception) {
			LogManager.logException("Failed to get current user", exception);
			return "";
		}
	}

	private static void removeCurrentUser() {
		try {
			StarMadeCredentials.removeFile();
		} catch(IOException exception) {
			LogManager.logException("Failed to remove current user", exception);
		}
	}

	private static void setGameVersion(IndexFileEntry gameVersion) {
		if(gameVersion != null) {
			LaunchSettings.setLastUsedVersion(gameVersion.version);
			if(usingOldVersion()) LaunchSettings.setJvmArgs("--illegal-access=permit");
			else LaunchSettings.setJvmArgs("");
		} else {
			LaunchSettings.setLastUsedVersion("NONE");
			LaunchSettings.setJvmArgs("");
		}
	}

	private static void setBranch(GameBranch branch) {
		lastUsedBranch = branch;
		LaunchSettings.setLastUsedBranch(lastUsedBranch.index);
	}

	private static boolean usingOldVersion() {
		return gameVersion.version.startsWith("0.2") || gameVersion.version.startsWith("0.1");
	}

	private static void clearPanel(JPanel panel) {
		if(panel != null) {
			panel.removeAll();
			panel.revalidate();
			panel.repaint();
		}
	}

	private static JTextField createPortField() {
		JTextField portField = new JTextField("4242");
		portField.setDoubleBuffered(true);
		portField.setOpaque(true);
		portField.setBackground(Palette.paneColor);
		portField.setForeground(Palette.textColor);
		portField.setFont(new Font("Roboto", Font.PLAIN, 12));
		portField.setMinimumSize(new Dimension(50, 20));
		portField.setPreferredSize(new Dimension(50, 20));
		portField.setMaximumSize(new Dimension(50, 20));
		portField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				portField.setToolTipText("Port");
			}

			@Override
			public void mouseExited(MouseEvent e) {
				portField.setToolTipText("");
			}
		});
		portField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				try {
					int port = Integer.parseInt(portField.getText() + c);
					if(port > 65535 || port < 1 || !Character.isDigit(c)) e.consume();
				} catch(Exception ignored) {
					e.consume();
				}
			}
		});
		return portField;
	}

	private static JComboBox<String> createDropdown() {
		JComboBox<String> dropDown = new JComboBox<>();
		dropDown.setFocusable(false);
		dropDown.setDoubleBuffered(true);
		dropDown.setOpaque(true);
		dropDown.setBackground(Palette.paneColor);
		dropDown.setForeground(Palette.textColor);
		dropDown.setUI(new BasicComboBoxUI() {
			@Override
			protected JButton createArrowButton() {
				JButton button = super.createArrowButton();
				button.setDoubleBuffered(true);
				button.setOpaque(false);
				button.setBackground(Palette.paneColor);
				button.setForeground(Palette.textColor);
				button.setContentAreaFilled(false);
				button.setRolloverEnabled(false);
				button.setBorder(BorderFactory.createEmptyBorder());
				button.setFocusable(false);
				return button;
			}
		});
		dropDown.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(isSelected) setBackground(Palette.selectedColor);
				else setBackground(Palette.deselectedColor);
				return this;
			}
		});
		return dropDown;
	}

	private static void setInitialVersion(JComboBox<String> versionDropdown) {
		String lastUsedVersion = LaunchSettings.getLastUsedVersion();
		if(lastUsedVersion.isEmpty()) lastUsedVersion = "NONE";
		for(int i = 0; i < versionDropdown.getItemCount(); i++) {
			if(versionDropdown.getItemAt(i).equals(lastUsedVersion)) {
				versionDropdown.setSelectedIndex(i);
				break;
			}
		}
	}

	private static String getJavaPath() {
		if(gameVersion.version.startsWith("0.2") || gameVersion.version.startsWith("0.1")) {
			return String.format(currentOS.javaPath, 8);
		} else {
			return String.format(currentOS.javaPath, 23);
		}
	}

	private static JavaVersion getJavaVersion() {
		if(gameVersion.version.startsWith("0.2") || gameVersion.version.startsWith("0.1")) {
			return JavaVersion.JAVA_8;
		} else {
			return JavaVersion.JAVA_23;
		}
	}

	private void downloadJRE() throws Exception {
		if(new File(getJavaPath()).exists()) return;
		downloader = new JavaDownloader(getJavaVersion());
		JDialog dialog = new JDialog();
		dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		dialog.setModal(true);
		dialog.setResizable(false);
		dialog.setTitle("Performing First Time Setup");
		dialog.setSize(500, 100);
		dialog.setLocationRelativeTo(null);
		dialog.setLayout(new BorderLayout());

		JPanel dialogPanel = new JPanel();
		dialogPanel.setDoubleBuffered(true);
		dialogPanel.setOpaque(true);
		dialogPanel.setLayout(new BorderLayout());
		dialog.add(dialogPanel);

		JLabel downloadLabel = new JLabel("Downloading Java " + getJavaVersion().number + "...");
		downloadLabel.setDoubleBuffered(true);
		downloadLabel.setOpaque(true);
		downloadLabel.setFont(new Font("Roboto", Font.BOLD, 16));
		downloadLabel.setHorizontalAlignment(SwingConstants.CENTER);
		dialogPanel.add(downloadLabel, BorderLayout.CENTER);

		downloader.downloadAndUnzip(dialog);
	}

	private static void setClientProperties(JComboBox<String> dropdown, UIDefaults defaults) {
		dropdown.putClientProperty("Nimbus.Overrides", defaults);
		dropdown.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
	}

	// TODO maybe save and don't re-add every time
	private static void updateVersionDropdown(JComboBox<String> versionDropdown, JComboBox<String> branchDropdown, VersionRegistry versionRegistry) {
		GameBranch branch = GameBranch.getForIndex(branchDropdown.getSelectedIndex());
		List<IndexFileEntry> versions = versionRegistry.getVersions(branch);
		if(versions == null) return;

		// Add versions to dropdown
		for(IndexFileEntry version : versions) {
			if(version.equals(versions.getFirst())) versionDropdown.addItem(version.version + " (Latest)");
			else versionDropdown.addItem(version.version);
		}
	}

	public static ArrayList<String> getCommandComponents(boolean server) {
		ArrayList<String> commandComponents = new ArrayList<>();
		commandComponents.add(getJavaPath());
		if(!gameVersion.version.startsWith("0.2") && !gameVersion.version.startsWith("0.1")) {
			commandComponents.add(J23ARGS);
		}

		if(currentOS == OperatingSystem.MAC) {
			// Run OpenGL on main thread on macOS
			// Needs to be added before "-jar"
			commandComponents.add("-XstartOnFirstThread");
		}
//		commandComponents.add("-Dfml.earlyprogresswindow=false");

		if(currentOS == OperatingSystem.LINUX) {
			// Override (meaningless?) default library path
			commandComponents.add("-Djava.library.path=lib:native/linux");
		}

		commandComponents.add("-jar");
		commandComponents.add("StarMade.jar");

		// Memory Arguments
		if(!LaunchSettings.getJvmArgs().isEmpty()) {
			String[] launchArgs = LaunchSettings.getLaunchArgs().split(" ");
			for(String arg : launchArgs) {
				if(arg.startsWith("-Xms") || arg.startsWith("-Xmx")) continue;
				commandComponents.add(arg.trim());
			}
		}
		commandComponents.add("-Xms1024m");
		commandComponents.add("-Xmx" + LaunchSettings.getMemory() + "m");

		// Game arguments
		commandComponents.add("-force");
		if(portField != null) port = Integer.parseInt(portField.getText());
		if(server) {
			commandComponents.add("-server");
			commandComponents.add("-port:" + port);
		}
		return commandComponents;
	}

	private IndexFileEntry getLastUsedVersion() {
		try {
			String version;
			File versionFile = new File(LaunchSettings.getInstallDir(), "version.txt");
			if(versionFile.exists()) {
				version = TextFileUtil.readText(versionFile);
			} else {
				version = LaunchSettings.getLastUsedVersion();
			}
			String shortVersion = version.split("#")[0];
			IndexFileEntry entry = versionRegistry.searchForVersion(e -> shortVersion.equals(e.version));
			if(entry != null) return entry;
		} catch(Exception exception) {
			LogManager.logWarning("Failed to get last used version", exception);
		}
		// Return latest release if nothing found
		return versionRegistry.getLatestVersion(GameBranch.RELEASE);
	}

	// Panel Methods

	private void createMainPanel() {
		mainPanel = new JPanel();
		mainPanel.setDoubleBuffered(true);
		mainPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		setContentPane(mainPanel);
		mainPanel.setLayout(new BorderLayout(0, 0));
		JPanel topPanel = new JPanel();
		topPanel.setDoubleBuffered(true);
		topPanel.setOpaque(false);
		topPanel.setLayout(new StackLayout());
		mainPanel.add(topPanel, BorderLayout.NORTH);
		JLabel topLabel = new JLabel();
		topLabel.setDoubleBuffered(true);
		topLabel.setIcon(getIcon("sprites/header_top.png"));
		topPanel.add(topLabel);
		JPanel topPanelButtons = new JPanel();
		topPanelButtons.setDoubleBuffered(true);
		topPanelButtons.setLayout(new FlowLayout(FlowLayout.RIGHT));
		topPanelButtons.setOpaque(false);
		JButton closeButton = new JButton(null, getIcon("sprites/close_icon.png")); //Todo: Replace these cus they look like shit
		closeButton.setDoubleBuffered(true);
		closeButton.setOpaque(false);
		closeButton.setContentAreaFilled(false);
		closeButton.setBorderPainted(false);
		closeButton.addActionListener(e -> {
			dispose();
			System.exit(0);
		});
		JButton minimizeButton = new JButton(null, getIcon("sprites/minimize_icon.png"));
		minimizeButton.setDoubleBuffered(true);
		minimizeButton.setOpaque(false);
		minimizeButton.setContentAreaFilled(false);
		minimizeButton.setBorderPainted(false);
		minimizeButton.addActionListener(e -> setState(ICONIFIED));
		topPanelButtons.add(minimizeButton);
		topPanelButtons.add(closeButton);
		topLabel.add(topPanelButtons);
		topPanelButtons.setBounds(0, 0, 800, 30);
		//Use top panel to drag the window
		topPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				mouseX = e.getX();
				mouseY = e.getY();
				//If the mouse is on the top panel buttons, don't drag the window
				if(mouseX > 770 || mouseY > 30) {
					mouseX = 0;
					mouseY = 0;
				}
				super.mousePressed(e);
			}
		});
		topPanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if(mouseX != 0 && mouseY != 0) setLocation(getLocation().x + e.getX() - mouseX, getLocation().y + e.getY() - mouseY);
				super.mouseDragged(e);
			}
		});
		JPanel leftPanel = new JPanel();
		leftPanel.setDoubleBuffered(true);
		leftPanel.setOpaque(false);
		leftPanel.setLayout(new StackLayout());
		mainPanel.add(leftPanel, BorderLayout.WEST);
		JLabel leftLabel = new JLabel();
		leftLabel.setDoubleBuffered(true);
		try {
			Image image = ImageIO.read(Objects.requireNonNull(StarMadeLauncher.class.getResource("/sprites/left_panel.png")));
			//Resize the image to the left panel
			image = image.getScaledInstance(150, 500, Image.SCALE_SMOOTH);
			leftLabel.setIcon(new ImageIcon(image));
		} catch(IOException exception) {
			LogManager.logWarning("Failed to load left panel image", exception);
		}
		//Stretch the image to the left panel
		leftPanel.add(leftLabel, StackLayout.BOTTOM);
		JPanel topLeftPanel = new JPanel();
		topLeftPanel.setDoubleBuffered(true);
		topLeftPanel.setOpaque(false);
		topLeftPanel.setLayout(new BorderLayout());
		leftPanel.add(topLeftPanel, StackLayout.TOP);
		//Add list
		JList<JLabel> list = new JList<>();
		list.setDoubleBuffered(true);
		list.setOpaque(false);
		list.setLayoutOrientation(JList.VERTICAL);
		list.setVisibleRowCount(-1);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer((list1, value, index, isSelected, cellHasFocus) -> {
			if(isSelected) {
				value.setForeground(Palette.selectedColor);
				value.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Palette.selectedColor));
			} else {
				value.setForeground(Palette.deselectedColor);
				value.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Palette.deselectedColor));
			}
			return value;
		});
		//Highlight on mouse hover
		list.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int index = list.locationToIndex(e.getPoint());
				if(index != -1) list.setSelectedIndex(index);
				else list.clearSelection();
			}
		});
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 1) {
					int index = list.locationToIndex(e.getPoint());
					if(index != -1) {
						switch(index) {
							case 0:
								createNewsPanel();
								break;
							case 1:
								createForumsPanel();
								break;
							case 2:
								createContentPanel();
								break;
							case 3:
								createCommunityPanel();
								break;
						}
					}
				}
			}
		});
		list.setFixedCellHeight(48);
		DefaultListModel<JLabel> listModel = new DefaultListModel<>();
		listModel.addElement(new JLabel("NEWS"));
		listModel.addElement(new JLabel("FORUMS"));
		listModel.addElement(new JLabel("CONTENT"));
		listModel.addElement(new JLabel("COMMUNITY"));
		for(int i = 0; i < listModel.size(); i++) {
			JLabel label = listModel.get(i);
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setFont(new Font("Roboto", Font.BOLD, 18));
			label.setForeground(Palette.selectedColor);
			label.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Palette.selectedColor));
			label.setDoubleBuffered(true);
			label.setOpaque(false);
		}
		list.setModel(listModel);
		topLeftPanel.add(list);
		JPanel topLeftLogoPanel = new JPanel();
		topLeftLogoPanel.setDoubleBuffered(true);
		topLeftLogoPanel.setOpaque(false);
		topLeftLogoPanel.setLayout(new BorderLayout());
		topLeftPanel.add(topLeftLogoPanel, BorderLayout.NORTH);
		//Add a left inset
		JPanel leftInset = new JPanel();
		leftInset.setDoubleBuffered(true);
		leftInset.setOpaque(false);
		topLeftLogoPanel.add(leftInset, BorderLayout.CENTER);
		//Add logo at top left
		JLabel logo = new JLabel();
		logo.setDoubleBuffered(true);
		logo.setOpaque(false);
		logo.setIcon(getIcon("sprites/logo.png"));
		leftInset.add(logo);
		footerPanel = new JPanel();
		footerPanel.setDoubleBuffered(true);
		footerPanel.setOpaque(false);
		footerPanel.setLayout(new StackLayout());
		mainPanel.add(footerPanel, BorderLayout.SOUTH);
		JLabel footerLabel = new JLabel();
		footerLabel.setDoubleBuffered(true);
		footerLabel.setIcon(getIcon("sprites/footer_normalplay_bg.jpg"));
		footerPanel.add(footerLabel);
		JPanel topRightPanel = new JPanel();
		topRightPanel.setDoubleBuffered(true);
		topRightPanel.setOpaque(false);
		topRightPanel.setLayout(new BorderLayout());
		topPanel.add(topRightPanel, BorderLayout.EAST);
		JLabel logoLabel = new JLabel();
		logoLabel.setDoubleBuffered(true);
		logoLabel.setOpaque(false);
		logoLabel.setIcon(getIcon("sprites/launcher_schine_logo.png"));
		topRightPanel.add(logoLabel, BorderLayout.EAST);
		JButton normalPlayButton = new JButton("Play");
		normalPlayButton.setFont(new Font("Roboto", Font.BOLD, 12));
		normalPlayButton.setDoubleBuffered(true);
		normalPlayButton.setOpaque(false);
		normalPlayButton.setContentAreaFilled(false);
		normalPlayButton.setBorderPainted(false);
		normalPlayButton.setForeground(Palette.textColor);
		JButton dedicatedServerButton = new JButton("Dedicated Server");
		dedicatedServerButton.setFont(new Font("Roboto", Font.BOLD, 12));
		dedicatedServerButton.setDoubleBuffered(true);
		dedicatedServerButton.setOpaque(false);
		dedicatedServerButton.setContentAreaFilled(false);
		dedicatedServerButton.setBorderPainted(false);
		dedicatedServerButton.setForeground(Palette.textColor);
		JPanel footerPanelButtons = new JPanel();
		footerPanelButtons.setDoubleBuffered(true);
		footerPanelButtons.setLayout(new FlowLayout(FlowLayout.LEFT));
		footerPanelButtons.setOpaque(false);
		footerPanelButtons.add(Box.createRigidArea(new Dimension(10, 0)));
		footerPanelButtons.add(normalPlayButton);
		footerPanelButtons.add(Box.createRigidArea(new Dimension(30, 0)));
		footerPanelButtons.add(dedicatedServerButton);
		footerLabel.add(footerPanelButtons);
		footerPanelButtons.setBounds(0, 0, 800, 30);
		if(getLastUsedVersion() == null) selectedVersion = null;
		else selectedVersion = gameVersion.version;
		createPlayPanel(footerPanel);
		createServerPanel(footerPanel);
		JPanel bottomPanel = new JPanel();
		bottomPanel.setDoubleBuffered(true);
		bottomPanel.setOpaque(false);
		bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		footerPanel.add(bottomPanel, BorderLayout.SOUTH);
		JButton launchSettings = new JButton("Launch Settings");
		launchSettings.setIcon(getIcon("sprites/memory_options_gear.png"));
		launchSettings.setFont(new Font("Roboto", Font.BOLD, 12));
		launchSettings.setDoubleBuffered(true);
		launchSettings.setOpaque(false);
		launchSettings.setContentAreaFilled(false);
		launchSettings.setForeground(Palette.textColor);
		bottomPanel.add(launchSettings);
		launchSettings.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				launchSettings.setForeground(Palette.selectedColor);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				launchSettings.setForeground(Palette.textColor);
			}
		});
		launchSettings.addActionListener(e -> {
			JDialog dialog = new JDialog();
			dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			dialog.setModal(true);
			dialog.setResizable(false);
			dialog.setTitle("Launch Settings");
			dialog.setSize(500, 350);
			dialog.setLocationRelativeTo(null);
			dialog.setLayout(new BorderLayout());
			dialog.setAlwaysOnTop(true);
//			dialog.setBackground(Palette.paneColor);
//			dialog.setForeground(Palette.foregroundColor);
			JPanel dialogPanel = new JPanel();
			dialogPanel.setDoubleBuffered(true);
			dialogPanel.setOpaque(true);
//			dialogPanel.setBackground(Palette.paneColor);
//			dialogPanel.setForeground(Palette.foregroundColor);
			dialogPanel.setLayout(new BorderLayout());
			dialog.add(dialogPanel);
			JPanel northPanel = new JPanel();
			northPanel.setDoubleBuffered(true);
			northPanel.setOpaque(true);
			northPanel.setLayout(new BorderLayout());
//			northPanel.setBackground(Palette.paneColor);
//			northPanel.setForeground(Palette.foregroundColor);
			dialogPanel.add(northPanel, BorderLayout.NORTH);
			JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 2048, (int) getSystemMemory(), LaunchSettings.getMemory());
//			slider.setBackground(Palette.paneColor);
			JLabel sliderLabel = new JLabel("Memory: " + slider.getValue() + " MB");
//			sliderLabel.setBackground(Palette.paneColor);
			sliderLabel.setDoubleBuffered(true);
			sliderLabel.setOpaque(true);
			sliderLabel.setFont(new Font("Roboto", Font.BOLD, 12));
			sliderLabel.setHorizontalAlignment(SwingConstants.CENTER);
			northPanel.add(sliderLabel, BorderLayout.NORTH);
			slider.setDoubleBuffered(true);
			slider.setOpaque(true);
			if(getSystemMemory() > 16384) { //Make sure the slider is not too squished for people that have a really epic gamer pc
				slider.setMajorTickSpacing(2048);
				slider.setMajorTickSpacing(1024);
				slider.setLabelTable(slider.createStandardLabels(4096));
			} else if(getSystemMemory() > 8192) {
				slider.setMajorTickSpacing(1024);
				slider.setMinorTickSpacing(512);
				slider.setLabelTable(slider.createStandardLabels(2048));
			} else {
				slider.setMajorTickSpacing(1024);
				slider.setMinorTickSpacing(256);
				slider.setLabelTable(slider.createStandardLabels(1024));
			}
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setSnapToTicks(true);
			northPanel.add(slider, BorderLayout.CENTER);
			slider.addChangeListener(e1 -> sliderLabel.setText("Memory: " + slider.getValue() + " MB"));
			JPanel centerPanel = new JPanel();
			centerPanel.setDoubleBuffered(true);
			centerPanel.setOpaque(false);
			centerPanel.setLayout(new BorderLayout());
//			centerPanel.setBackground(Palette.backgroundColor);
//			centerPanel.setForeground(Palette.foregroundColor);
			dialogPanel.add(centerPanel, BorderLayout.CENTER);
			JTextArea launchArgs = new JTextArea();
//			launchArgs.setBackground(Palette.paneColor);
			launchArgs.setDoubleBuffered(true);
			launchArgs.setOpaque(true);
			launchArgs.setText(LaunchSettings.getLaunchArgs());
			launchArgs.setLineWrap(true);
			launchArgs.setWrapStyleWord(true);
			launchArgs.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			centerPanel.add(launchArgs, BorderLayout.CENTER);
			JLabel launchArgsLabel = new JLabel("JVM Arguments");
//			launchArgsLabel.setBackground(Palette.paneColor);
			launchArgsLabel.setDoubleBuffered(true);
			launchArgsLabel.setOpaque(true);
			launchArgsLabel.setFont(new Font("Roboto", Font.BOLD, 12));
			launchArgsLabel.setHorizontalAlignment(SwingConstants.CENTER);
			centerPanel.add(launchArgsLabel, BorderLayout.NORTH);
			JPanel buttonPanel = new JPanel();
			buttonPanel.setDoubleBuffered(true);
			buttonPanel.setOpaque(true);
			buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			dialogPanel.add(buttonPanel, BorderLayout.SOUTH);
			JButton saveButton = new JButton("Save");
			saveButton.setFont(new Font("Roboto", Font.BOLD, 12));
			saveButton.setDoubleBuffered(true);
			buttonPanel.add(saveButton);
			JButton cancelButton = new JButton("Cancel");
			cancelButton.setFont(new Font("Roboto", Font.BOLD, 12));
			cancelButton.setDoubleBuffered(true);
			buttonPanel.add(cancelButton);
			saveButton.addActionListener(e1 -> {
				LaunchSettings.setMemory(slider.getValue());
				LaunchSettings.setJvmArgs(launchArgs.getText());
				LaunchSettings.saveSettings();
				dialog.dispose();
			});
			cancelButton.addActionListener(e1 -> dialog.dispose());
			dialog.setVisible(true);
		});
		JButton installSettings = new JButton("Installation Settings");
		installSettings.setIcon(getIcon("sprites/launch_options_gear.png"));
		installSettings.setFont(new Font("Roboto", Font.BOLD, 12));
		installSettings.setDoubleBuffered(true);
		installSettings.setOpaque(false);
		installSettings.setContentAreaFilled(false);
		installSettings.setForeground(Palette.textColor);
		bottomPanel.add(installSettings);
		installSettings.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				installSettings.setForeground(Palette.selectedColor);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				installSettings.setForeground(Palette.textColor);
			}
		});
		installSettings.addActionListener(e -> {
			JDialog[] dialog = {new JDialog()};
			dialog[0].setModal(true);
			dialog[0].setResizable(false);
			dialog[0].setTitle("Installation Settings");
			dialog[0].setSize(450, 150);
			dialog[0].setLocationRelativeTo(null);
			dialog[0].setLayout(new BorderLayout());
			dialog[0].setAlwaysOnTop(true);
			dialog[0].setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			JPanel dialogPanel = new JPanel();
			dialogPanel.setDoubleBuffered(true);
			dialogPanel.setOpaque(false);
			dialog[0].add(dialogPanel, BorderLayout.CENTER);
			JPanel installLabelPanel = new JPanel();
			installLabelPanel.setDoubleBuffered(true);
			installLabelPanel.setOpaque(false);
			installLabelPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			dialogPanel.add(installLabelPanel);
			JLabel installLabel = new JLabel("Install Directory: ");
			installLabel.setDoubleBuffered(true);
			installLabel.setOpaque(false);
			installLabel.setFont(new Font("Roboto", Font.BOLD, 12));
			installLabelPanel.add(installLabel);
			JTextField installLabelPath = new JTextField(new File(LaunchSettings.getInstallDir()).getAbsolutePath());
			installLabelPath.setDoubleBuffered(true);
			installLabelPath.setOpaque(false);
			installLabelPath.setFont(new Font("Roboto", Font.PLAIN, 12));
			installLabelPath.setMinimumSize(new Dimension(200, 20));
			installLabelPath.setPreferredSize(new Dimension(200, 20));
			installLabelPath.setMaximumSize(new Dimension(200, 20));
			installLabelPanel.add(installLabelPath);
			installLabelPath.addActionListener(e1 -> {
				String path = installLabelPath.getText();
				if(path == null || path.isEmpty()) return;
				File file = new File(path);
				if(!file.exists()) return;
				installLabelPath.setText(file.getAbsolutePath());
			});

			JButton installButton = new JButton("Change");
			installButton.setIcon(UIManager.getIcon("FileView.directoryIcon"));
			installButton.setDoubleBuffered(true);
			installButton.setOpaque(false);
			installButton.setContentAreaFilled(false);
			installButton.setBorderPainted(false);
			installButton.setFont(new Font("Roboto", Font.BOLD, 12));
			dialogPanel.add(installButton);
			installButton.addActionListener(e1 -> {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(new File(LaunchSettings.getInstallDir()));
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int result = fileChooser.showOpenDialog(dialog[0]);
				if(result == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					installLabelPath.setText(file.getAbsolutePath());
				}
			});

			JButton repairButton = new JButton("Repair");
			repairButton.setDoubleBuffered(true);
			repairButton.setOpaque(false);
			repairButton.setFont(new Font("Roboto", Font.BOLD, 12));
			dialogPanel.add(repairButton);
			repairButton.addActionListener(e1 -> {
				IndexFileEntry version = getLatestVersion(lastUsedBranch);
				if(version != null) {
					if(updaterThread == null || !updaterThread.updating) {
						dialog[0].dispose();
						recreateButtons(playPanel, true);
						updateGame(version);
					}
				} else JOptionPane.showMessageDialog(dialog[0], "The Launcher needs to be online to do this!", "Error", JOptionPane.ERROR_MESSAGE);
			});

			JButton openCurrentInstallButton = new JButton("Open Install Folder");
			openCurrentInstallButton.setDoubleBuffered(true);
			openCurrentInstallButton.setOpaque(false);
			openCurrentInstallButton.setFont(new Font("Roboto", Font.BOLD, 12));
			dialogPanel.add(openCurrentInstallButton);
			openCurrentInstallButton.addActionListener(e1 -> {
				try {
					Desktop.getDesktop().open(new File(LaunchSettings.getInstallDir()));
				} catch(IOException exception) {
					LogManager.logException("Failed to open current install directory", exception);
				}
			});

			JPanel buttonPanel = new JPanel();
			buttonPanel.setDoubleBuffered(true);
			buttonPanel.setOpaque(false);
			buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			dialog[0].add(buttonPanel, BorderLayout.SOUTH);
			JButton saveButton = new JButton("Save");
			saveButton.setFont(new Font("Roboto", Font.BOLD, 12));
			saveButton.setDoubleBuffered(true);
			buttonPanel.add(saveButton);
			JButton cancelButton = new JButton("Cancel");
			cancelButton.setFont(new Font("Roboto", Font.BOLD, 12));
			cancelButton.setDoubleBuffered(true);
			buttonPanel.add(cancelButton);
			saveButton.addActionListener(e1 -> {
				String installDir = installLabelPath.getText();
				if(installDir != null) {
					LaunchSettings.setInstallDir(new File(installDir).getAbsolutePath());
					LaunchSettings.saveSettings();
					recreateButtons(playPanel, false);
				}
				dialog[0].dispose();
			});
			cancelButton.addActionListener(e1 -> dialog[0].dispose());
			dialog[0].setVisible(true);
		});
		if(serverPanel != null) serverPanel.setVisible(false);
		versionPanel.setVisible(true);

		normalPlayButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				normalPlayButton.setForeground(Palette.selectedColor);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				normalPlayButton.setForeground(Palette.textColor);
			}
		});
		normalPlayButton.addActionListener(e -> switchToClientMode(footerLabel));

		dedicatedServerButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				dedicatedServerButton.setForeground(Palette.selectedColor);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				dedicatedServerButton.setForeground(Palette.textColor);
			}
		});
		dedicatedServerButton.addActionListener(e -> {
			if(updaterThread == null || !updaterThread.updating) { //Don't allow this while the game is updating
				switchToServerMode(footerLabel);
			}
		});

		centerPanel = new JPanel();
		centerPanel.setDoubleBuffered(true);
		centerPanel.setOpaque(false);
		centerPanel.setLayout(new BorderLayout());
		mainPanel.add(centerPanel, BorderLayout.CENTER);
		JLabel background = new JLabel();
		background.setDoubleBuffered(true);
		try {
			Image image = ImageIO.read(Objects.requireNonNull(StarMadeLauncher.class.getResource("/sprites/left_panel.png")));
			//Resize the image to the left panel
			image = image.getScaledInstance(800, 500, Image.SCALE_SMOOTH);
			background.setIcon(new ImageIcon(image));
		} catch(IOException exception) {
			LogManager.logException("Failed to load background image", exception);
		}
		centerPanel.add(background, BorderLayout.CENTER);

		switchToClientMode(footerLabel); // make sure right components are visible
	}

	private void switchToClientMode(JLabel footerLabel) {
		footerLabel.setIcon(getIcon("sprites/footer_normalplay_bg.jpg"));
		serverPanel.setVisible(false);
		versionPanel.setVisible(true);
		createPlayPanel(footerPanel);
	}

	private void switchToServerMode(JLabel footerLabel) {
		footerLabel.setIcon(getIcon("sprites/footer_dedicated_bg.jpg"));
		versionPanel.setVisible(false);
		playPanelButtons.removeAll();
		versionPanel.removeAll();
		createServerPanel(footerPanel);
		serverPanel.setVisible(true);
	}

	private long getSystemMemory() {
		try {
			HardwareAbstractionLayer hal = new SystemInfo().getHardware();
			return hal.getMemory().getTotal() / 1024 / 1024;
		} catch(Exception exception) {
			LogManager.logException("Failed to get system memory", exception);
		}
		return 16384; //Just assume 16GB if we can't get the memory
	}

	private void recreateButtons(JPanel playPanel, boolean repair) {
		if(playPanelButtons != null) {
			playPanelButtons.removeAll();
			playPanel.remove(playPanelButtons);
		}
		playPanelButtons = new JPanel();
		playPanelButtons.setDoubleBuffered(true);
		playPanelButtons.setOpaque(false);
		playPanelButtons.setLayout(new BorderLayout());
		playPanel.remove(playPanelButtons);
		playPanel.add(playPanelButtons, BorderLayout.EAST);
		JPanel playPanelButtonsSub = new JPanel();
		playPanelButtonsSub.setDoubleBuffered(true);
		playPanelButtonsSub.setOpaque(false);
		playPanelButtonsSub.setLayout(new FlowLayout(FlowLayout.RIGHT));
		playPanelButtons.add(playPanelButtonsSub, BorderLayout.SOUTH);

		if(checkNeedsUpdate() || repair) {
			updateButton = new JButton(getIcon("sprites/update_btn.png"));
			updateButton.setDoubleBuffered(true);
			updateButton.setOpaque(false);
			updateButton.setContentAreaFilled(false);
			updateButton.setBorderPainted(false);
			updateButton.addActionListener(e -> {
				IndexFileEntry version = versionRegistry.searchForVersion(lastUsedBranch, v -> v.version.equals(selectedVersion));
				System.out.println("selected version " + version);
				if(version != null) {
					if(updaterThread == null || !updaterThread.updating) updateGame(version);
				} else JOptionPane.showMessageDialog(null, "The Launcher needs to be online to do this!", "Error", JOptionPane.ERROR_MESSAGE);
			});
			updateButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					if(updaterThread == null || !updaterThread.updating) updateButton.setIcon(getIcon("sprites/update_roll.png"));
					else updateButton.setToolTipText(dlStatus.toString());
				}

				@Override
				public void mouseExited(MouseEvent e) {
					if(updaterThread == null || !updaterThread.updating) updateButton.setIcon(getIcon("sprites/update_btn.png"));
					else updateButton.setToolTipText(dlStatus.toString());
				}
			});
			playPanelButtonsSub.add(updateButton);
		} else {
			JButton playButton = new JButton(getIcon("sprites/launch_btn.png")); //Todo: Reduce button glow so this doesn't look weird
			playButton.setDoubleBuffered(true);
			playButton.setOpaque(false);
			playButton.setContentAreaFilled(false);
			playButton.setBorderPainted(false);
			playButton.addActionListener(e -> {
				try {
					dispose();
					LaunchSettings.setLastUsedVersion(gameVersion.version);
					LaunchSettings.saveSettings();
					runStarMade(serverMode);
				} catch(Exception exception) {
					LogManager.logFatal("Failed to run StarMade", exception);
				}
			});
			playButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					playButton.setIcon(getIcon("sprites/launch_roll.png"));
				}

				@Override
				public void mouseExited(MouseEvent e) {
					playButton.setIcon(getIcon("sprites/launch_btn.png"));
				}
			});
			playPanelButtonsSub.add(playButton);
		}
		playPanel.revalidate();
		playPanel.repaint();
	}

	private void runStarMade(boolean server) {
		try {
			downloadJRE();
		} catch(Exception exception) {
			LogManager.logFatal("Failed to download JRE", exception);
		}
		ArrayList<String> commandComponents = getCommandComponents(server);
		ProcessBuilder process = new ProcessBuilder(commandComponents);
		process.directory(new File(LaunchSettings.getInstallDir()));
		process.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		process.redirectError(ProcessBuilder.Redirect.INHERIT);
		try {
			process.start();
			System.exit(0);
		} catch(Exception exception) {
			LogManager.logFatal("Failed to start StarMade", exception);
		}
	}
	
	public boolean checkNeedsUpdate() {
		return !gameJarExists(LaunchSettings.getInstallDir()) || gameVersion == null || (!Objects.equals(gameVersion.version, selectedVersion) && selectedVersion != null);
	}

	private void createPlayPanel(JPanel footerPanel) {
		clearPanel(playPanel);
		serverMode = false;
		playPanel = createPanel(footerPanel, false);
	}

	private void createServerPanel(JPanel footerPanel) {
		clearPanel(serverPanel);
		serverMode = true;
		serverPanel = createPanel(footerPanel, true);
	}

	private JPanel createPanel(JPanel footerPanel, boolean serverMode) {
		JPanel panel = new JPanel();
		panel.setDoubleBuffered(true);
		panel.setOpaque(false);
		panel.setLayout(new BorderLayout());
		footerPanel.add(panel);

		versionPanel = createVersionPanel(serverMode);
		footerPanel.add(versionPanel, BorderLayout.WEST);

		recreateButtons(panel, false);

		footerPanel.revalidate();
		footerPanel.repaint();
		return panel;
	}

	private JPanel createVersionPanel(boolean serverMode) {
		JPanel versionPanel = new JPanel();
		versionPanel.setDoubleBuffered(true);
		versionPanel.setOpaque(false);
		versionPanel.setLayout(new BorderLayout());

		JPanel versionSubPanel = new JPanel();
		versionSubPanel.setDoubleBuffered(true);
		versionSubPanel.setOpaque(false);
		versionSubPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		versionPanel.add(versionSubPanel, BorderLayout.SOUTH);

		//Change color of arrow
		UIDefaults defaults = new UIDefaults();
		defaults.put("ComboBox:\"ComboBox.arrowButton\"[Enabled].backgroundPainter", Palette.buttonColor);

		//Version dropdown
		JComboBox<String> versionDropdown = createDropdown();
		setClientProperties(versionDropdown, defaults);

		//Branch dropdown
		JComboBox<String> branchDropdown = createBranchDropdown(versionDropdown, lastUsedBranch.index);
		setClientProperties(branchDropdown, defaults);

		versionDropdown.removeAllItems();
		updateVersionDropdown(versionDropdown, branchDropdown, versionRegistry);
		versionDropdown.addItemListener(e -> onSelectVersion(versionDropdown));
		setInitialVersion(versionDropdown);

		versionSubPanel.add(branchDropdown);
		versionSubPanel.add(versionDropdown);

		//Port field
		if(serverMode) {
			portField = createPortField();
			versionSubPanel.add(portField);
		} else {
			if(portField != null) {
				portField.setVisible(false);
				versionSubPanel.remove(portField);
			}
		}
		return versionPanel;
	}

	private JComboBox<String> createBranchDropdown(JComboBox<String> versionDropdown, int startIndex) {
		JComboBox<String> branchDropdown = createDropdown();
		branchDropdown.addItem("Release");
		branchDropdown.addItem("Dev");
		branchDropdown.addItem("Pre-Release");
		branchDropdown.setSelectedIndex(startIndex);
		branchDropdown.setFocusable(false);
		branchDropdown.addItemListener(e -> onSelectBranch(branchDropdown, versionDropdown));
		return branchDropdown;
	}

	private void onSelectBranch(JComboBox<String> branchDropdown, JComboBox<String> versionDropdown) {
		int branchIndex = branchDropdown.getSelectedIndex();
		setBranch(GameBranch.getForIndex(branchIndex));
		LaunchSettings.saveSettings();
		versionDropdown.removeAllItems();
		updateVersionDropdown(versionDropdown, branchDropdown, versionRegistry);
		recreateButtons(playPanel, false);
	}

	private void onSelectVersion(JComboBox<String> versionDropdown) {
		if(versionDropdown.getSelectedIndex() == -1) return;
		selectedVersion = versionDropdown.getItemAt(versionDropdown.getSelectedIndex()).split(" ")[0];
		LaunchSettings.setLastUsedVersion(selectedVersion);
		LaunchSettings.saveSettings();
		if(playPanel != null) recreateButtons(playPanel, false);
	}

	private void updateGame(IndexFileEntry version) {
		String[] options = {"Backup Database", "Backup Everything", "Don't Backup"};
		int choice = JOptionPane.showOptionDialog(this, "Would you like to backup your database, everything, or nothing?", "Backup", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		int backupMode = UpdaterThread.BACKUP_MODE_NONE;
		if(choice == 0) backupMode = UpdaterThread.BACKUP_MODE_DATABASE;
		else if(choice == 1) backupMode = UpdaterThread.BACKUP_MODE_EVERYTHING;
		ImageIcon updateButtonEmpty = getIcon("sprites/update_load_empty.png");
		ImageIcon updateButtonFilled = getIcon("sprites/update_load_full.png");
		updateButton.setIcon(updateButtonEmpty);
		//Start update process and update progress bar
		(updaterThread = new UpdaterThread(version, backupMode, new File(LaunchSettings.getInstallDir())) {
			@Override
			public void onProgress(float progress, String file, long mbDownloaded, long mbTotal, long mbSpeed) {
				dlStatus.setInstallProgress(progress);
				dlStatus.setDownloadedMb(mbDownloaded);
				dlStatus.setTotalMb(mbTotal);
				dlStatus.setSpeedMb(mbSpeed);
				if(file != null && !"null".equals(file)) dlStatus.setFilename(file);
				int width = updateButtonEmpty.getIconWidth();
				int height = updateButtonEmpty.getIconHeight();
				BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = image.createGraphics();
				g.drawImage(updateButtonEmpty.getImage(), 0, 0, null);
				int filledWidth = (int) (width * progress);
				g.drawImage(updateButtonFilled.getImage(), 0, 0, filledWidth, updateButtonFilled.getIconHeight(), 0, 0, filledWidth, updateButtonFilled.getIconHeight(), null);
				g.dispose();
				updateButton.setIcon(new ImageIcon(image));
				updateButton.setToolTipText(dlStatus.toString());
				updateButton.repaint();
			}

			@Override
			public void onFinished() {
				gameVersion = getLastUsedVersion();
				assert gameVersion != null;
				LaunchSettings.setLastUsedVersion(gameVersion.version);
				selectedVersion = gameVersion.version;
				setBranch(gameVersion.branch);
				LaunchSettings.saveSettings();
				SwingUtilities.invokeLater(() -> {
					try {
						sleep(1);
						recreateButtons(playPanel, false);
					} catch(InterruptedException e) {
						throw new RuntimeException(e);
					}
				});
			}

			@Override
			public void onError(Exception exception) {
				LogManager.logException("Failed to update game", exception);
				updateButton.setIcon(getIcon("sprites/update_btn.png"));
			}
		}).start();
	}

	private void createScroller(JPanel currentPanel) {
		if(centerScrollPane == null) {
			centerScrollPane = new JScrollPane(currentPanel);
			centerScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			centerScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
			centerScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			centerScrollPane.getVerticalScrollBar().setUnitIncrement(16);
			centerPanel.add(centerScrollPane, BorderLayout.CENTER);
		}
		centerScrollPane.setViewportView(currentPanel);
	}

	private void createNewsPanel() {
		if(newsPanel == null) newsPanel = new LauncherNewsPanel();
		createScroller(newsPanel);
		newsPanel.updatePanel();
		SwingUtilities.invokeLater(() -> {
			JScrollBar vertical = centerScrollPane.getVerticalScrollBar();
			vertical.setValue(vertical.getMinimum());
		});
	}

	private void createForumsPanel() {
		if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			String ccURL = "https://starmadedock.net/forums/";
			try {
				Desktop.getDesktop().browse(new URI(ccURL));
			} catch(IOException | URISyntaxException exception) {
				LogManager.logWarning("Failed to open forums", exception);
			}
		}
		/* Todo: Create forums panel
		forumsPanel = new LauncherForumsPanel();
		createScroller(forumsPanel);
		forumsPanel.updatePanel();
		SwingUtilities.invokeLater(() -> {
			JScrollBar vertical = centerScrollPane.getVerticalScrollBar();
			vertical.setValue(vertical.getMinimum());
		});
		 */
	}

	private void createContentPanel() {
		if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			String ccURL = "https://starmadedock.net/content/";
			try {
				Desktop.getDesktop().browse(new URI(ccURL));
			} catch(IOException | URISyntaxException exception) {
				LogManager.logWarning("Failed to open content", exception);
			}
		}
		/* Todo: Create content panel
		contentPanel = new LauncherContentPanel();
		createScroller(contentPanel);
		contentPanel.updatePanel();
		SwingUtilities.invokeLater(() -> {
			JScrollBar vertical = centerScrollPane.getVerticalScrollBar();
			vertical.setValue(vertical.getMinimum());
		});
		 */
	}

	private void createCommunityPanel() {
		communityPanel = new LauncherCommunityPanel();
		createScroller(communityPanel);
		communityPanel.updatePanel();
		SwingUtilities.invokeLater(() -> {
			JScrollBar vertical = centerScrollPane.getVerticalScrollBar();
			vertical.setValue(vertical.getMinimum());
		});
	}

	private IndexFileEntry getLatestVersion(GameBranch branch) {
		IndexFileEntry currentVersion = getLastUsedVersion();
		if(debugMode || (currentVersion != null && !currentVersion.version.startsWith("0.2") && !currentVersion.version.startsWith("0.1"))) {
			return getLastUsedVersion();
		}
		return versionRegistry.getLatestVersion(branch);
	}

	private boolean gameJarExists(String installDir) {
		return (new File(installDir + "/StarMade.jar")).exists();
	}
}