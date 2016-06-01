package org.summoners.patcher.patch.impl;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;

import org.summoners.cache.*;
import org.summoners.patcher.*;
import org.summoners.patcher.patch.*;

/**
 * The patcher task to load all relevant configuration.
 * @author Brittan Thomas
 */
public class ConfigurationTask extends PatchTask {
	
	/**
	 * Instantiates a new configuration task.
	 */
	public ConfigurationTask() {
		setName("Summoners-Configuration-Task");
	}

	/* (non-Javadoc)
	 * @see org.summoners.patcher.patch.PatchTask#patch()
	 */
	@Override
	public void patch() throws MalformedURLException, IOException, NoSuchAlgorithmException {
		percentage = 0F;
		//TODO setup client settings saves
		if (!RiotFileUtil.getCustomFile("settings.txt").exists()) {
			
		} else {
			Properties properties = new Properties();
			try (FileReader reader = new FileReader(RiotFileUtil.getCustomFile("settings.txt"))) {
				properties.load(reader);
			}
			
			try {
				Versions.downloadSolutionManifest(Versions.getGameSLNVersion(), Versions.getBranch());
				dumpConfig();
			} catch (IOException ex) {
				Logger.getLogger(ConfigurationTask.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			Patcher.setActivePatcherTasks(getPatcherTasks());
		}
		percentage = 100F;
	}
	
	/**
	 * Dumps and generates necessary configuration files.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void dumpConfig() throws IOException {
		File configManifest = RiotFileUtil.getRADSFile("solutions/lol_game_client_sln/releases/" + Versions.getGameSLNVersion() + "/configurationmanifest");
		configManifest.createNewFile();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(configManifest))) {
			writer.write("RADS Configuration Manifest\r\n" +
						"1.0.0.0\r\n" + Patcher.getLanguage() + "\r\n" + 
						"2\r\n" + "lol_game_client\r\n" + 
						"lol_game_client_" + Patcher.getLanguage() + "\r\n");
		}
		
		File configDefaults = RiotFileUtil.getRADSFile("solutions/lol_game_client_sln/releases/" + Versions.getGameSLNVersion() + "/deploy/DATA/cfg/defaults/");
		configDefaults.mkdirs();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(configDefaults, "locale.cfg")))) {
			writer.write("[General]\r\n" + "LanguageLocaleRegion=" + Patcher.getLanguage().split("_")[0] + "_" + Patcher.getLanguage().split("_")[1].toUpperCase());
		}
	}
	
	/**
	 * Populates a list of tasks to patch the entire client.
	 *
	 * @return the list of patcher tasks
	 */
	public LinkedList<PatchTask> getPatcherTasks() {
		LinkedList<PatchTask> patchers = new LinkedList<>();
		patchers.add(new ArchivePatcher(Versions.getAirVersion(), "lol_air_client", Versions.getBranch(), Patcher.shouldIgnoreS_OK(), Patcher.isForced()));
		patchers.add(new ArchivePatcher(Versions.getAirConfigVersion(), "lol_air_client_config" + (!Patcher.getServer().equals("PBE") ? "_" + Patcher.getServer().toLowerCase() : ""), 
													Versions.getBranch(), Patcher.shouldIgnoreS_OK(), Patcher.isForced()));
		patchers.add(new ArchivePatcher(Versions.getGameVersion(), "lol_game_client", Versions.getBranch(), Patcher.shouldIgnoreS_OK(), Patcher.isForced()));
		patchers.add(new ArchivePatcher(Versions.getGameLanguageVersion(), "lol_game_client_" + Patcher.getLanguage(), Versions.getBranch(), Patcher.shouldIgnoreS_OK(), Patcher.isForced()));
		patchers.add(new CopyTask(RiotFileUtil.getRADSFile("projects/" + "lol_air_client_config" + (!Patcher.getServer().equals("PBE") ? "_" + Patcher.getServer().toLowerCase() : "")
																	+ "/releases/" + Versions.getAirConfigVersion() + "/deploy/"),
								RiotFileUtil.getRADSFile("projects/lol_air_client/releases/" + Versions.getAirVersion() + "/deploy/"), true));
		patchers.add(new SLNPatchTask(Versions.getGameVersion(), Versions.getGameSLNVersion(), Patcher.shouldIgnoreS_OK()));
		patchers.add(new RunTask(() -> {
			try {
				File file = RiotFileUtil.getRADSFile("projects/lol_air_client/releases/" + Versions.getAirVersion() + "/deploy/locale.properties");
				file.createNewFile();
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
					writer.write("locale=" + Patcher.getLanguage().split("_")[0] + "_" + Patcher.getLanguage().split("_")[1].toUpperCase());
				}
			} catch (IOException ex) {
				Logger.getLogger(ConfigurationTask.class.getName()).log(Level.SEVERE, null, ex);
			}
		}, "Locale config"));
		
		finished = true;
		return patchers;
	}
}
