package com.mineshaftersquared;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.creatifcubed.simpleapi.ISimpleSettings;
import com.creatifcubed.simpleapi.Platform;
import com.creatifcubed.simpleapi.SimpleRequest;
import com.creatifcubed.simpleapi.SimpleVersion;
import com.creatifcubed.simpleapi.SimpleXMLSettings;
import com.creatifcubed.simpleapi.SimpleUtils;
import com.mineshaftersquared.gui.panels.FeedbackTabPanel;
import com.mineshaftersquared.gui.panels.IndexTabPanel;
import com.mineshaftersquared.gui.panels.InfoTabPanel;
import com.mineshaftersquared.gui.panels.SettingsTabPanel;
import com.mineshaftersquared.proxy.MineProxy;
import com.mineshaftersquared.resources.MS2Frame;
import com.mineshaftersquared.resources.Utils;

public class MineshafterSquaredGUI implements Runnable {
	
	private String[] args;
	public ISimpleSettings settings;
	public JTextField username;
	public JLabel sessionId;
	public int proxyPort;
	public IndexTabPanel indexPane;
	
	private static final int DEFAULT_WIDTH = 854;
	private static final int DEFAULT_HEIGHT = 480;
	public static final String MC_DOWNLOAD = "http://s3.amazonaws.com/MinecraftDownload/minecraft.jar";
	public static final SimpleVersion VERSION = new SimpleVersion("4.0.0");
	
	public static void main(String[] args) {
		(new MineshafterSquaredGUI(args)).run();
	}
	
	public MineshafterSquaredGUI(String[] args) {
		this.args = args;
		new File("ms2-resources").mkdir();
		this.settings = new SimpleXMLSettings("ms2-resources/settings.xml");
		this.username = null;
		this.sessionId = null;
		this.proxyPort = 0;
	}
	
	public void run() {
		JFrame frame = new JFrame("Mineshafter Squared " + VERSION.toString());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel contentPane = new JPanel(new BorderLayout());
		
		JTabbedPane tabbedPane = new JTabbedPane();
		this.indexPane = new IndexTabPanel(this);
		tabbedPane.add("Main", this.indexPane);
		//tabbedPane.add("Versions", buildVersionsPage());
		tabbedPane.add("Settings", new SettingsTabPanel(this));
		//tabbedPane.add("News", buildNewsPage());
		tabbedPane.add("Info", new InfoTabPanel());
		tabbedPane.add("Feedback", new FeedbackTabPanel());
		contentPane.add(tabbedPane);
		
		frame.setContentPane(contentPane);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setVisible(true);
		
		System.out.println(SimpleUtils.getRam());
		
		this.startProxy();
		
		if (clientHasUpdate()) {
			JOptionPane.showMessageDialog(null, "There is an update at ms2.creatifcubed.com");
		}
	}
	
	public static boolean clientHasUpdate() {
		try {
			String result = new String(new SimpleRequest(new URL("http://ms2.creatifcubed.com/resources/latestversion.php")).doGet());
			SimpleVersion latest = new SimpleVersion(result);
			System.out.println("Latest version: " + latest.toString());
			return VERSION.shouldUpdateTo(latest);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Unable to check update. Please go manually to ms2.creatifcubed.com");
		}
		return false;
	}
	
	public void startProxy() {
		MineProxy proxy = new MineProxy(VERSION, ""); // create proxy
		MineProxy.authServer = "alpha.mineshaftersquared.com";
		proxy.start(); // launch proxy
		this.proxyPort = proxy.getPort();
		

		System.setProperty("http.proxyHost", "127.0.0.1");
		System.setProperty("http.proxyPort", Integer.toString(this.proxyPort));
		System.setProperty("java.net.preferIPv4Stack", "true");
		
		this.indexPane.updateInfo(this.proxyPort);
		
	}
	
	private static File[] listAllMinecraftsIn(File dir) {
		return dir.listFiles(new FilenameFilter() {
			public boolean accept(File f, String name) {
				return f.isFile() && Pattern.compile("[a-zA-Z0-9_.-]*.jar").matcher(name).matches();
			}
		});
	}
	
	public void goLauncher() {
		String max = this.settings.getString("runtime.ram.max");
		String min = this.settings.getString("runtime.ram.min");
		System.out.println("max is null " + (max == null ?  "true" : max));
		System.out.println("min is null " + (min == null ?  "true" : min));
		
		//MinecraftLauncher launcher = new MinecraftLauncher(/*args*/ new String[] { "Adrian", "a"}, new Dimension(854, 500), false, false);
		//launcher.run();
		
		try {
			String str = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			List<String> arr = new ArrayList<String>();
			arr.add("java");
			if (max != null) {
				arr.add("-Xmx" + max + "m");
			}
			//arr.add("-Dsun.java2d.noddraw=true");
			//arr.add("-Dsun.java2d.d3d=false");
			//arr.add("-Dsun.java2d.opengl=false");
			//arr.add("-Dsun.java2d.pmoffscreen=false");
			
			arr.add("-Dhttp.proxyHost=127.0.0.1");
			arr.add("-Dhttp.proxyPort=" + this.proxyPort);

			arr.add("-classpath");
	        arr.add(str);
	        arr.add("com.mineshaftersquared.resources.GameEntry");
	        arr.add(this.username.getText());
	        arr.add(this.sessionId.getText());
	        int pathFind = this.settings.getInt("gui.launch.pathfind", MinecraftLauncher.PATH_AUTODETECT);
	        arr.add(String.valueOf(pathFind));
	        
	        
	        ProcessBuilder builder = new ProcessBuilder(arr);
	        System.out.println("here");
	        //builder.redirectOutput();
	        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
	        builder.redirectErrorStream(true);
	        builder.start();
	        for (String a : arr) {
	        	System.out.print(a + " ");
	        }
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void goServer() {
		String max = this.settings.getString("runtime.ram.max");
		String min = this.settings.getString("runtime.ram.min");
		System.out.println("max is " + (max == null ?  "null" : max));
		System.out.println("min is " + (min == null ?  "null" : min));
		
		//MinecraftLauncher launcher = new MinecraftLauncher(/*args*/ new String[] { "Adrian", "a"}, new Dimension(854, 500), false, false);
		//launcher.run();
		
		try {
			//String str = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			List<String> arr = new ArrayList<String>();
			arr.add("java");
			if (max != null) {
				arr.add("-Xmx" + max + "m");
				//arr.add("-Xms" + max + "m");
			}
			if (min != null) {
				arr.add("-Xms" + min + "m");
			}
			

			arr.add("-Dhttp.proxyHost=127.0.0.1");
			arr.add("-Dhttp.proxyPort=" + this.proxyPort);

			arr.add("-jar");
	        String server = this.settings.getString("server.name");
	        if (server == null) {
	        	server = "minecraft_server";
	        }
	        server += ".jar";
	        if (!(new File(server).exists())) {
	        	JOptionPane.showMessageDialog(null, "Server file {" + server + "} does not exist");
	        }
	        arr.add(server);
	        ProcessBuilder builder = new ProcessBuilder(arr);
	        builder.redirectOutput();
	        builder.redirectError();
	        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
	        builder.start();
	        for (String a : arr) {
	        	System.out.print(a + " ");
	        }
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void doDownload() {
		/*
		int pathfind = this.settings.getInt("gui.launch.pathfind", MinecraftLauncher.PATH_LOCAL);
		if (pathfind == MinecraftLauncher.PATH_AUTODETECT) {
			pathfind = MinecraftLauncher.PATH_LOCAL;
		}
		
		String cwd = System.getProperty("user.dir");
		if (pathfind != MinecraftLauncher.PATH_LOCAL) {
			cwd = Utils.getDefaultMCPath();
		}
		System.out.println(cwd);
		System.out.println(System.getProperty("user.dir"));
		File bin = new File(cwd, "bin");
		if (!bin.exists()) {
			bin.mkdir();
		}
		
		System.out.println(bin.getAbsolutePath());
		try {
			URL location = new URL(MC_DOWNLOAD);
			ReadableByteChannel rbc = Channels.newChannel(location.openStream());
			FileOutputStream fos = new FileOutputStream(new File(bin, "minecraft.jar"));
			fos.getChannel().transferFrom(rbc, 0, 1 << 24);
			System.out.println("done");
		} catch (Exception ex) {
			ex.printStackTrace();
		}*/
		MinecraftLauncher m = new MinecraftLauncher(null);
		m.setPathfind(MinecraftLauncher.PATH_LOCAL);
		GameUpdaterProxy proxy = new GameUpdaterProxy(m.getPath());
		proxy.update();
	}
}
