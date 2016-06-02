package org.summoners.patcher.patch.impl;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

import org.summoners.cache.*;
import org.summoners.cache.data.model.*;
import org.summoners.cache.structure.*;
import org.summoners.patcher.patch.*;
import org.summoners.patcher.worker.*;
import org.summoners.patcher.worker.impl.*;

/**
 * A patcher to update League of Legends' assets.
 * @author Brittan Thomas
 */
public class ArchivePatcher extends PatchTask {
	
	/**
	 * Instantiates a new patcher.
	 *
	 * @param branch
	 *            the branch being patched
	 * @param project
	 *            the project being patched
	 * @param version
	 *            the target version being patched
	 * @param ignoreOk
	 *            if the patch ignores S_OK files
	 * @param forced
	 *            if the patch is forced
	 */
	public ArchivePatcher(String branch, String project, String version, boolean ignoreOk, boolean forced) {
		this(branch, project, version, ignoreOk, forced, (f, n) -> true);
	}
	
	/**
	 * Instantiates a new patcher.
	 *
	 * @param branch
	 *            the branch being patched
	 * @param project
	 *            the project being patched
	 * @param version
	 *            the target version being patched
	 * @param ignoreOk
	 *            if the patch ignores S_OK files
	 * @param forced
	 *            if the patch is forced
	 * @param filter
	 *            the file name filter instance
	 */
	public ArchivePatcher(String branch, String project, String version, boolean ignoreOk, boolean forced, FilenameFilter filter) {
		this.branch = branch;
		this.project = project;
		this.version = version;
		this.ignoreOk = ignoreOk;
		this.forced = forced;
		this.filter = filter;
		setName("Summoners-" + project + "-Patcher-Task");
	}
	
	/**
	 * The branch being patched.
	 */
	private String branch;
	
	/**
	 * Gets the branch being patched.
	 *
	 * @return the branch being patched
	 */
	public String getBranch() {
		return branch;
	}
	
	/**
	 * The target version being patched.
	 */
	private String version;
	
	/**
	 * Gets the target version being patched.
	 *
	 * @return the target version being patched
	 */
	public String getTargetVersion() {
		return version;
	}
	
	/**
	 * The type of project being patched.
	 */
	private String project;
	
	/**
	 * Gets the type of project being patched.
	 *
	 * @return the type of project being patched
	 */
	public String getProject() {
		return project;
	}
	
	/**
	 * The map of versions to RAF's.
	 */
	private LinkedHashMap<String, RiotArchiveFile> archives = new LinkedHashMap<>();
	
	/**
	 * Gets the map of versions to RAF's.
	 *
	 * @return the map of versions to RAF's
	 */
	public LinkedHashMap<String, RiotArchiveFile> getArchives() {
		return archives;
	}
	
	/**
	 * Synchronizes and saves all the archives.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void syncAllArchives() throws IOException {
		for (RiotArchiveFile archive : archives.values())
			archive.sync();
	}
	    
	/**
	 * The array of active worker instances.
	 */
	private Worker[] activeWorkers;

	/**
	 * Gets the array of active worker instances.
	 *
	 * @return the array of active worker instances
	 */
	public Worker[] getActiveWorkers() {
		return activeWorkers;
	}
	
	/**
	 * If we should ignore the S_OK files.
	 */
	private final boolean ignoreOk;
	
	/**
	 * If we are forced to patch.
	 */
	private final boolean forced;

	/**
	 * Checks if we are forced to patch.
	 *
	 * @return if we are forced to patch
	 */
	public boolean isForced() {
		return forced;
	}
	
	/**
	 * If we are forced to patch single files.
	 */
	private boolean forceSingleFiles;

	/**
	 * Checks if we are forced to patch single files.
	 *
	 * @return if we are forced to patch single files
	 */
	public boolean isForceSingleFiles() {
		return forceSingleFiles;
	}
	
	/**
	 * The filename filter to omit files from being patched.
	 */
	private FilenameFilter filter;
	
	/**
	 * Gets the filename filter to omit files from being patched.
	 *
	 * @return the filename filter to omit files from being patched
	 */
	public FilenameFilter getFilter() {
		return filter;
	}
	
	private int fileCount;
	
	private int archiveCount;
	
	/**
	 * The percentage of files contained in an archive.
	 */
	private float percentageInArchive;
	
	/**
	 * Gets the percentage of files contained in an archive.
	 *
	 * @return the percentage of files contained in an archive
	 */
	public float getPercentageInArchive() {
		return percentageInArchive;
	}
	
	/**
	 * Sets the percentage of files contained in an archive.
	 *
	 * @param percentageInArchive
	 *            the new percentage of files contained in an archive
	 */
	public void setPercentageInArchive(float percentageInArchive) {
		this.percentageInArchive = percentageInArchive;
	}
	
	/**
	 * The list of files pending their patches.
	 */
	public LinkedList<RiotFileManifest> pendingFiles;

	/**
	 * Gets the list of files pending their patches.
	 *
	 * @return the list of files pending their patches
	 */
	public LinkedList<RiotFileManifest> getPendingFiles() {
		return pendingFiles;
	}
	
	/**
	 * The list of archives pending their patches.
	 */
	public LinkedList<VersionedArchive<RiotFileManifest>> pendingArchives;

	/**
	 * Gets the list of archives pending their patches.
	 *
	 * @return the list of archives pending their patches
	 */
	public LinkedList<VersionedArchive<RiotFileManifest>> getPendingArchives() {
		return pendingArchives;
	}
	
	/**
	 * The progress percentage of an active download.
	 */
	private float downloadProgress = 0F;
	
	/**
	 * Gets the progress percentage of an active download.
	 *
	 * @return the progress percentage of an active download
	 */
	public float getDownloadProgress() {
		return downloadProgress;
	}

	/**
	 * Sets the progress percentage of an active download.
	 *
	 * @param downloadProgress
	 *            the new progress percentage of an active download
	 */
	public void setDownloadProgress(float downloadProgress) {
		this.downloadProgress = downloadProgress;
	}
	
	/* (non-Javadoc)
	 * @see org.summoners.patcher.PatchTask#patch()
	 */
	@Override
	public void patch() throws IOException {
		boolean okExists = RiotFileUtil.getRADSFile("projects/" + project + "/releases/" + version + "/S_OK").exists();
		if (okExists && !ignoreOk) {
			finished = true;
			return;
		}
		
		RiotReleaseManifestFile old = null;
		File target = RiotFileUtil.getRADSFile("projects/" + project + "/releases/");
		if (target.exists()) {
			String prev = RiotFileUtil.getNewestVersionInDirectory(target);
			if (prev != null) {
				File oldDir = new File(target, prev), newDir = new File(target, version);
				if (oldDir.renameTo(newDir)) {
					System.out.println(oldDir);
					if (new File(newDir, "S_OK").exists()) {
						if (!ignoreOk || !prev.equals(version))
							new File(newDir, "S_OK").delete();
						
						old = new RiotReleaseManifestFile(new File(newDir, "RiotReleaseManifestFile").toPath());
					} else
						forceSingleFiles = true;
				} else
					throw new IOException("New release version already exists! Rename failed.");
			}
		}
		
		status = "Reading manifest";
		
		RiotReleaseManifestFile current = RiotReleaseManifestFile.download(branch, "projects", project, version);
		
		status = "Calculating differences";
		
		LinkedList<RiotFileManifest> files = new LinkedList<>();
		if (forced || forceSingleFiles) {
			for (RiotFileManifest file : current.getFiles()) {
				if (!filter.accept(null, file.getName()))
					continue;
				else if (forced && file.getFileType().isArchive())
					files.add(file);
				else 
					files.add(file);
			}
		}
		
		if (error != null)
			return;
		
		LinkedList<RiotFileManifest> culledFiles = cullFiles(current, old);
		if (files.isEmpty()) {
			files = culledFiles;
			if (culledFiles.size() > 0) {
				try {
					status = "Downloading packages";
					PackageWorker pkgWorker = new PackageWorker(branch, project, version, culledFiles);
					pkgWorker.start();
					try {
						pkgWorker.join();
					} catch (InterruptedException ex) {
						Logger.getLogger(ArchivePatcher.class.getName()).log(Level.SEVERE, null, ex);
					}
					files.removeAll(pkgWorker.downloadRanges(this));
					downloadProgress = 0;
				} catch (IOException | URISyntaxException e) {
					e.printStackTrace();
				}
			}
		}
		
		files.sort((f1, f2) -> f1.compareTo(f2));
		status = "Organizing files";
		
		archiveCount = (int) files.stream().filter(f -> f.getFileType().isArchive()).count();
		fileCount = (int) files.stream().count() - archiveCount;
		percentageInArchive = (float) archiveCount / (archiveCount + fileCount);
		
		ArrayList<VersionedArchive<RiotFileManifest>> atp = new ArrayList<>();
		pendingFiles = new LinkedList<>();
		
		VersionedArchive<RiotFileManifest> lastArchive = null;
		for (RiotFileManifest f : files) {
			if (f.getFileType().isArchive()) {
				if (lastArchive == null || !lastArchive.getVersion().equals(f.getRelease())) {
					lastArchive = new VersionedArchive<>(f.getRelease());
					atp.add(lastArchive);
				}
				lastArchive.getFiles().add(f);
			} else
				pendingFiles.add(f);
		}
		
		atp.sort((a1, a2) -> -Integer.compare(a1.getFiles().size(), a2.getFiles().size()));
		
		pendingArchives = new LinkedList<>();
		pendingArchives.addAll(atp);
		
		status = "Patching separate files";
		
		final int WORKER_COUNT = 6;
		Worker[] workers = new FileWorker[WORKER_COUNT];
		for (int i = 0; i < WORKER_COUNT; i++) {
			workers[i] = new FileWorker(this);
			workers[i].start();
		}
		activeWorkers = workers;
		
		for (Worker worker : activeWorkers) {
			try {
				worker.join();
			} catch (InterruptedException ex) {
				Logger.getLogger(ArchivePatcher.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		status = "Patching archives";
		
		workers = new ArchiveWorker[WORKER_COUNT];
		for (int i = 0; i < WORKER_COUNT; i++) {
			workers[i] = new ArchiveWorker(this);
			workers[i].start();
		}
		activeWorkers = workers;
		
		for (Worker w : activeWorkers) {
			try {
				w.join();
			} catch (InterruptedException ex) {
				Logger.getLogger(ArchivePatcher.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		for (RiotArchiveFile raf : archives.values())
			raf.close();
		archives.clear();
		
		cleanupManagedFiles(current);
		if (!finished && error == null) {
			RiotFileUtil.getRADSFile("projects/" + project + "/releases/" + version + "/S_OK").createNewFile();
			finished = true;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.summoners.patcher.PatchTask#getPercentage()
	 */
	@Override
	public float getPercentage() {
		if (pendingArchives == null)
			return downloadProgress;
		
		float finished = fileCount - pendingFiles.size();
		if (activeWorkers != null && activeWorkers instanceof FileWorker[])
			for (Worker worker : activeWorkers)
				if (worker != null)
					finished -= (1 - worker.getProgress());
	
		float finishedArchives = archiveCount - pendingArchives.size();
		if (activeWorkers != null && activeWorkers instanceof ArchiveWorker[])
			for (Worker worker : activeWorkers)
				if (worker != null)
					finishedArchives -= (1 - worker.getProgress());
		
		float file = finished / fileCount, archive = finishedArchives / archiveCount;
		return (((fileCount == 0 ? 0 : file) * (1F - getPercentageInArchive())) + ((archiveCount == 0 ? 0 : archive) * getPercentageInArchive())) * 100F + downloadProgress;
	}
	
	/**
	 * Culls all files which are outdated and in need of patching.
	 *
	 * @param current
	 *            the up-to-date release manifest
	 * @param old
	 *            the out-dated release manifest
	 * @return the outdated files to be patched
	 */
	private LinkedList<RiotFileManifest> cullFiles(RiotReleaseManifestFile current, RiotReleaseManifestFile old) {
		int cores = Runtime.getRuntime().availableProcessors();
		
		LinkedList<DifferenceWorker> calculators = new LinkedList<>();
		
		int slices = 1 + current.getFiles().length / cores;
		for (int calc = 0; calc != cores; ++calc) {
			int offset = calc * slices, length = Math.max(0, Math.min(current.getFiles().length - offset, slices));
			DifferenceWorker calculator = new DifferenceWorker(this, current, old, filter, offset, length);
			calculator.start();
			calculators.add(calculator);
		}
		
		for (DifferenceWorker calc : calculators) {
			try {
				calc.join();
			} catch (InterruptedException ex) {
				Logger.getLogger(ArchivePatcher.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		LinkedList<LinkedList<RiotFileManifest>> files = new LinkedList<>();
		for (int calc = 0; calc != cores; ++calc)
			files.add(calculators.get(calc).getResult());
		
		return DifferenceWorker.mergeLists(files);
	}

	/**
	 * Cleans up the managed files branch.
	 *
	 * @param manifest
	 *            the release manifest file
	 */
	private void cleanupManagedFiles(RiotReleaseManifestFile manifest) {
		File managedFileDir = RiotFileUtil.getRADSFile("projects/" + project + "/managedfiles/");
		if (managedFileDir.exists()) {
			String[] versions = managedFileDir.list();
			for (String v : versions) {
				boolean found = false;
				for (RiotFileManifest fileManifest : manifest.getFiles()) {
					if (fileManifest.getFileType() == RiotFileType.MANAGED && fileManifest.getRelease().equals(v)) {
						found = true;
						break;
					}
				}
				
				if (!found)
					RiotFileUtil.deleteDir(new File(managedFileDir, v));
			}
		}
	}
	
	/**
	 * Gets the RAF with the specified name.
	 *
	 * @param version
	 *            the name of the archive being queried for
	 * @return the RAF instance
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public RiotArchiveFile getArchive(String version) throws IOException {
		RiotArchiveFile raf = archives.get(version);
		if (raf == null) {
			String folder = "RADS/projects/" + project + "/filearchives/" + version + "/";
			File file = new File(folder);
			file.mkdirs();
			String fileName = "Archive_1.raf";
			String[] files = file.list((dir, dirName) -> dirName.matches("Archive_[0-9]+\\.raf"));
			if (files.length > 0) {
				raf = new RiotArchiveFile(Paths.get(folder, files[0]), Paths.get(folder, files[0] + ".dat"));
				synchronized (archives) {
					archives.put(version, raf);
				}
				return raf;
			}
			try {
				raf = new RiotArchiveFile(folder + fileName);
				synchronized (archives) {
					archives.put(version, raf);
				}
				return raf;
			} catch (IOException ex) {
				Logger.getLogger(ArchivePatcher.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return raf;
	}
	
	/**
	 * Gets the current version from the Riot games site.
	 *
	 * @param type
	 *            the type (ex: downloads, projects, solutions)
	 * @param project
	 *            the project (ex: lol_air_client, lol_game_client)
	 * @param server
	 *            the server (ex: EUW, NA, etc.)
	 * @return the current version
	 */
	public static String getVersion(String type, String project, String server) {
		try {
			URL url = new URL("http://l3cdn.riotgames.com/releases/" + (server.equals("PBE") ? "pbe" : "live") + "/" + type + "/" 
									+ project + "/releases/releaselisting_" + server);
			try (BufferedReader rd = new BufferedReader(new InputStreamReader(url.openStream()))) {
				return rd.readLine();
			} catch (IOException ex) {
				Logger.getLogger(ArchivePatcher.class.getName()).log(Level.SEVERE, null, ex);
			}
		} catch (MalformedURLException ex) {
			Logger.getLogger(ArchivePatcher.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	/**
	 * Gets the relevant file directory from the specified manifest.
	 *
	 * @param manifest
	 *            the manifest detailing the file
	 * @return the file directory
	 */
	public String getFileDirectory(RiotFileManifest manifest) {
        return RiotFileUtil.getRADSPath() + "projects/" + project + (manifest.getFileType().ordinal() == 5 ? "/managedfiles/" : "/releases/")
                + (manifest.getFileType().ordinal() == 5 ? manifest.getRelease() : version)
                + (manifest.getFileType().ordinal() == 5 ? "/" : "/deploy/") + manifest.getPath();
	}
    
	public static void main(String[] args) throws IOException {
		System.out.println((2142495409L & 0xFFFFFFFFL));
		String version = RiotFileUtil.getReleaseName(316);
		System.out.println("Version: " + version);
		
		//RiotReleaseManifestFile file = RiotReleaseManifestFile.download("live", "projects", "lol_game_client", version);
		//for (RiotFileManifest manifest : file.getFiles())
		//	System.out.println(manifest.getPath() + "/" + manifest.getName());
		
		try (RiotArchiveFile rafArchive = new RiotArchiveFile(Paths.get("Archive_114251952.raf"), Paths.get("Archive_114251952.raf.dat"))) {
			
			for (RiotFile file : rafArchive.getFiles())
				if (file.getName().contains(".list") || file.getName().contains(".anm") || file.getName().contains(".skl") || file.getName().contains(".skn"))
					System.out.println(file);
			
			System.out.println(rafArchive.getDictionary().size());
			RiotFile file = rafArchive.get("DATA/Characters/Xerath/Animations.list");
			AnimationListDefinition def = new AnimationListDefinition(0, file);
			def.getClass();
			System.out.println(AnimationListDefinition.getAnimations());
			
			RiotFile anim = rafArchive.get("DATA/Characters/Xerath/animations/Xerath_joke.anm");
			AnimationDefinition animationDef = new AnimationDefinition(anim.getRealData());
			System.out.println(animationDef.getId() + ", " + animationDef.getBoneCount() + ", " + animationDef.getBones().size() + ", " + animationDef.getPlaybackFps());
			
			RiotFile skeleton = rafArchive.get("DATA/Characters/Xerath/Xerath.skl");
			SkeletonDefinition skelDef = new SkeletonDefinition(skeleton.getRealData());
			System.out.println(skelDef.getId() + ", " + skelDef.getBoneCount() + ", " + skelDef.getBones().size());
			
			RiotFile skin = rafArchive.get("DATA/Characters/Xerath/Xerath.skn");
			SkinDefinition skinDef = new SkinDefinition(skin.getRealData());
			System.out.println(skinDef.getMagic() + ", " + skinDef.getIndexCount() + ", " + skinDef.getIndices().size());
		}
	}
	
}
