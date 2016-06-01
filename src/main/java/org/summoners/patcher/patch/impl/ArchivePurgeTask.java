package org.summoners.patcher.patch.impl;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

import org.summoners.cache.*;
import org.summoners.cache.util.*;
import org.summoners.patcher.patch.*;
import org.summoners.patcher.worker.*;
import org.summoners.util.*;

/**
 * The archive purge task to clean unnecessary and duplicate data from the caches.
 * @author Xuplup
 * @author Brittan Thomas
 */
public class ArchivePurgeTask extends PatchTask {

	/**
	 * Instantiates a new archive purge task.
	 *
	 * @param branch
	 *            the branch being patched
	 * @param project
	 *            the type of project being patched
	 * @param version
	 *            the target version being patched
	 * @param type
	 *            the type of data being patched
	 */
	public ArchivePurgeTask(String branch, String project, String version, String type) {
		this.branch = branch;
		this.project = project;
		this.version = version;
		this.type = type;
		setName("Summoners-" + project + "-Purger-Task");
	}
	
	/**
	 * The branch being patched.
	 */
	private final String branch;
	
	/**
	 * Gets the branch being patched.
	 *
	 * @return the branch being patched
	 */
	public String getBranch() {
		return branch;
	}
	
	/**
	 * The type of project being patched.
	 */
	private final String project;
	
	/**
	 * Gets the type of project being patched.
	 *
	 * @return the type of project being patched
	 */
	public String getProject() {
		return project;
	}
	
	/**
	 * The target version being patched.
	 */
	private final String version;
	
	/**
	 * Gets the target version being patched.
	 *
	 * @return the target version being patched
	 */
	public String getVersion() {
		return version;
	}
	
	/**
	 * The type of data being patched.
	 */
	private final String type;
	
	/**
	 * Gets the type of data being patched.
	 *
	 * @return the type of data being patched
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * The amount of archives being purged.
	 */
	private int archiveCount;
	
	/**
	 * The percentage completeion of all purge tasks.
	 */
	private float globalPercentage;

	/**
	 * Gets the percentage completeion of all purge tasks.
	 *
	 * @return the percentage completeion of all purge tasks
	 */
	public float getGlobalPercentage() {
		return globalPercentage;
	}

	/**
	 * Sets the percentage completeion of all purge tasks.
	 *
	 * @param globalPercentage
	 *            the new percentage completeion of all purge tasks
	 */
	public void setGlobalPercentage(float globalPercentage) {
		this.globalPercentage = globalPercentage;
	}
	
	/**
	 * The percentage completion of the most recent archive.
	 */
	private float archivePercentage;

	/**
	 * Gets the percentage completion of the most recent archive.
	 *
	 * @return the percentage completion of the most recent archive
	 */
	public float getArchivePercentage() {
		return archivePercentage;
	}

	/**
	 * Sets the percentage completion of the most recent archive.
	 *
	 * @param archivePercentage
	 *            the new percentage completion of the most recent archive
	 */
	public void setArchivePercentage(float archivePercentage) {
		this.archivePercentage = archivePercentage;
	}

	/* (non-Javadoc)
	 * @see org.summoners.patcher.PatchTask#patch()
	 */
	@Override
	public void patch() throws MalformedURLException, IOException, NoSuchAlgorithmException {
		status = "Reading manifest";
		
		RiotReleaseManifestFile releaseManifest = RiotReleaseManifestFile.download(branch, type, project, version);
		
		LinkedList<VersionedArchive<RiotFileManifest>> pendingArchives = new LinkedList<>();
		
		LinkedList<RiotFileManifest> files = new LinkedList<>();
		Collections.addAll(files, releaseManifest.getFiles());
		files.sort((f1, f2) -> Integer.compare(f1.getReleaseInt(), f2.getReleaseInt()));
		
		VersionedArchive<RiotFileManifest> archive = null;
		for (RiotFileManifest manifest : files) {
			if (manifest.getFileType().isArchive()) {
				if (archive == null || !archive.getVersion().equals(manifest.getRelease())) {
					archive = new VersionedArchive<>(manifest.getRelease());
					pendingArchives.add(archive);
				}
				archive.getFiles().add(manifest);
			}
		}
		
		archiveCount = pendingArchives.size();
		for (int i = 0; i != archiveCount; ++i) {
			setGlobalPercentage(((float) i / archiveCount));
			purgeArchive(pendingArchives.pop());
			if (finished)
				return;
		}
		
		finished = true;
		setGlobalPercentage(1F);
		setArchivePercentage(0F);
	}
    
	/**
	 * Purges the specified archive by deleting unnecessary or duplicate data.
	 *
	 * @param archive
	 *            the archive being purged
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void purgeArchive(VersionedArchive<RiotFileManifest> archive) throws IOException {
		File folder = new File(RiotFileUtil.getRADSPath() + type + "/" + project + "/filearchives/" + archive.getVersion() + "/");
		Validate.require(folder.exists(), "Invalid installation. Run quick repair first.", IOException.class);
		String[] archives = folder.list((dir, name) -> name.matches("Archive_[0-9]+\\.raf"));
		Validate.require(archives.length == 1, () -> "Invalid installation. Expected one archive, found " + archives.length + ".", IOException.class);
		Validate.require(new File(folder, archives[0] + ".dat").exists(), "Invalid installation. Missing .raf.dat file in " + folder.getCanonicalPath() + ".", IOException.class);
		
		File raf = new File(folder, archives[0]), data = new File(folder, archives[0] + ".dat"); int fileCount = 0;
		try (RiotArchiveFile source = new RiotArchiveFile(raf.toPath(), data.toPath())) {
			File tmpDir = new File(folder, "temp");
			if (tmpDir.exists())
				RiotFileUtil.deleteDir(tmpDir);
			
			status = archive.getVersion();
			long sum = source.getFiles().stream().mapToInt(f -> f.getSize()).sum();
			if (source.getDataPath().toFile().length() == sum && source.getFiles().size() == archive.getFiles().size())
				return; // only purge if file has gaps or contains unnecessary files
			
			tmpDir.mkdir();
			
			status = "Loading " + archive.getVersion();
			try (RiotArchiveFile target = new RiotArchiveFile(folder.getAbsolutePath() + "/temp/Archive_1.raf")) {
				for (int index = 0; index < archive.getFiles().size(); index++) {
					RiotFileManifest manifest = archive.getFiles().get(index);
					setArchivePercentage((float) index / archive.getFiles().size());
					status = manifest.getName(); fileCount++;
					try (InputStream in = source.get(manifest.getPath() + manifest.getName()).getInputStream()) {
						try (OutputStream os = target.write(manifest.getPath() + manifest.getName(), manifest)) {
							int read; byte[] buffer = new byte[1024];
							while ((read = in.read(buffer)) != -1) {
								Worker.record(read);
								if (finished) {
									System.out.println("exited archive purge task");
									return;
								}
								os.write(buffer, 0, read);
							}
						}
					}
					int sourceSize = source.get(manifest.getPath() + manifest.getName()).getSize(),
						targetSize = target.get(manifest.getPath() + manifest.getName()).getSize();
					Validate.require(targetSize == sourceSize, () -> "Size mismatch: " + sourceSize + " != " + targetSize, IOException.class);
				}
			}
		}
		
		if (fileCount == 0)
			RiotFileUtil.deleteDir(folder);
		else {
			Validate.require(raf.delete(), "Delete failed for " + folder.getAbsolutePath() + archives[0], IOException.class);
			Validate.require(data.delete(), "Delete failed for " + folder.getAbsolutePath() + archives[0], IOException.class);
			Validate.require(new File(folder.getAbsolutePath() + "/temp/Archive_1.raf").renameTo(new File(folder, "Archive_1.raf")), 
					() -> "Move failed for " + folder.getAbsolutePath() + "temp/Archive_1.raf", IOException.class);
			Validate.require(new File(folder.getAbsolutePath() + "/temp/Archive_1.raf.dat").renameTo(new File(folder, "Archive_1.raf.dat")), 
					() -> "Move failed for " + folder.getAbsolutePath() + "temp/Archive_1.raf.dat", IOException.class);
			new File(folder.getAbsolutePath() + "/temp/").delete();
		}
	}

    /* (non-Javadoc)
     * @see org.summoners.patcher.PatchTask#getPercentage()
     */
    @Override
    public float getPercentage() {
        return 100F * (globalPercentage + archivePercentage / archiveCount);
    }
}
