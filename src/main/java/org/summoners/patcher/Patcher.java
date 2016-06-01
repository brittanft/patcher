package org.summoners.patcher;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.*;

import org.summoners.patcher.patch.*;
import org.summoners.util.*;

public class Patcher {
	
	public static void main(String[] args) {
		System.out.println(-1654783749L & 0xFFFFFFFFL);
	}
	
	public Patcher() {
		Versions.initVersions(server, language);
	}
	
	private static LinkedList<PatchTask> activePatcherTasks = new LinkedList<>();
	
	public static LinkedList<PatchTask> getActivePatcherTasks() {
		return activePatcherTasks;
	}
	
	public static void setActivePatcherTasks(LinkedList<PatchTask> activePatcherTasks) {
		Patcher.activePatcherTasks = activePatcherTasks;
	}
	
	private static String server;
	
	public static String getServer() {
		return server;
	}
	
	public static void setServer(String server) {
		Patcher.server = server;
	}
	
	private static String language;
	
	public static String getLanguage() {
		return language;
	}
	
	public static void setLanguage(String language) {
		Patcher.language = language;
	}
	
	private static boolean ignoreS_OK = false;
	
	public static boolean shouldIgnoreS_OK() {
		return ignoreS_OK;
	}
	
	public static void setIgnoreS_OK(boolean ignoreS_OK) {
		Patcher.ignoreS_OK = ignoreS_OK;
	}
	
	private static boolean forced = false;
	
	public static boolean isForced() {
		return forced;
	}
	
	public static void setForced(boolean forced) {
		Patcher.forced = forced;
	}
	
}
