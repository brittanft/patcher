package org.summoners.patcher;

import java.io.*;
import java.net.*;
import java.util.*;

import org.summoners.cache.*;
import org.summoners.patcher.patch.impl.*;

public class Versions {
	
	public static void initLanguages(String server) throws IOException {
		downloadSolutionManifest(getGameSLNVersion(), getBranch());
		setLanguages(getLanguages());
	}
	
	public static void initVersions(String server, String language) {
		setBranch(server.equals("PBE") ? "pbe" : "live");
		setAirConfigVersion(ArchivePatcher.getVersion("projects", "lol_air_client_config" + (!server.equals("PBE") ? "_" + server.toLowerCase() : ""), server));
		setGameSLNVersion(ArchivePatcher.getVersion("solutions", "lol_game_client_sln", server));
		setAirVersion(ArchivePatcher.getVersion("projects", "lol_air_client", server));
		setGameVersion(ArchivePatcher.getVersion("projects", "lol_game_client", server));
		setGameLanguageVersion(ArchivePatcher.getVersion("projects", "lol_game_client_" + language, server));
	}
	
	/**
	 * Retrieves the list of possible languages.
	 *
	 * @return the list of possible languages
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static LinkedList<String> retrieveLanguages() throws IOException {
		LinkedList<String> languages = new LinkedList<>();
		File solutionManifest = RiotFileUtil.getRADSFile("solutions/lol_game_client_sln/releases/" + getGameSLNVersion() + "/solutionmanifest");
		try (BufferedReader reader = new BufferedReader(new FileReader(solutionManifest))) {
			while (reader.ready()) {
				String line = reader.readLine();
				if (line.matches("lol_game_client_[a-z]+_[a-z]+")) {
					String lang = line.substring("lol_game_client_".length());
					if (languages.stream().noneMatch(s -> s.equals(lang)))
						languages.add(lang);
				}
			}
		}
		return languages;
	}
	
	/**
	 * Downloads the solution manifest from a URL.
	 *
	 * @param version
	 *            the version being downloaded
	 * @param branch
	 *            the branch being downloaded
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static void downloadSolutionManifest(String version, String branch) throws IOException {
		URL url = new URL("http://l3cdn.riotgames.com/releases/" + branch + "/solutions/lol_game_client_sln/releases/" + version + "/solutionmanifest");
		URLConnection connection = url.openConnection();
		
		File file = RiotFileUtil.getRADSFile("solutions/lol_game_client_sln/releases/" + version + "/solutionmanifest");
		new File(file.getParent()).mkdirs();
		file.createNewFile();
	
		try (InputStream inputStream = connection.getInputStream()) {
			try (OutputStream outputStream = new FileOutputStream(file)) {
				int read; byte[] buffer = new byte[2048];
				while ((read = inputStream.read(buffer)) != -1)
					outputStream.write(buffer, 0, read);
			}
		}
	}
	
	private static String branch;

	public static String getBranch() {
		return branch;
	}
	
	public static void setBranch(String branch) {
		Versions.branch = branch;
	}
	
	private static String airVersion;

	public static String getAirVersion() {
		return airVersion;
	}
	
	public static void setAirVersion(String airVersion) {
		Versions.airVersion = airVersion;
	}
	
	private static String airConfigVersion;
	
	public static String getAirConfigVersion() {
		return airConfigVersion;
	}

	public static void setAirConfigVersion(String airConfigVersion) {
		Versions.airConfigVersion = airConfigVersion;
	}

	private static String gameSLNVersion;
	
	public static String getGameSLNVersion() {
		return gameSLNVersion;
	}

	public static void setGameSLNVersion(String slnVersion) {
		Versions.gameSLNVersion = slnVersion;
	}
	
	private static String gameVersion;
	
	public static String getGameVersion() {
		return gameVersion;
	}

	public static void setGameVersion(String gameVersion) {
		Versions.gameVersion = gameVersion;
	}
	
	private static String gameLanguageVersion;
	
	public static String getGameLanguageVersion() {
		return gameLanguageVersion;
	}

	public static void setGameLanguageVersion(String gameLanguageVersion) {
		Versions.gameLanguageVersion = gameLanguageVersion;
	}
	
	private static LinkedList<String> languages;
	
	public static LinkedList<String> getLanguages() {
		return languages;
	}
	
	public static void setLanguages(LinkedList<String> languages) {
		Versions.languages = languages;
	}
	
}
