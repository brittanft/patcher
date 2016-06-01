package org.summoners.patcher.worker.impl;

import java.io.*;
import java.util.*;

import org.summoners.cache.*;
import org.summoners.patcher.patch.impl.*;
import org.summoners.patcher.worker.*;

/**
 * The difference calculator worker class.
 * @author Xupwup
 */
public class DifferenceWorker extends Worker {

	/**
	 * Instantiates a new difference calculator.
	 *
	 * @param patcher
	 *            the patcher instance currently running
	 * @param current
	 *            the up to date release manifest
	 * @param old
	 *            the outdated release manifest for comparison
	 * @param filter
	 *            the filename filter
	 * @param offset
	 *            the offset for parallel calculation
	 * @param length
	 *            the extent of which this calculator runs
	 */
	public DifferenceWorker(ArchivePatcher patcher, RiotReleaseManifestFile current,
						RiotReleaseManifestFile old, FilenameFilter filter, int offset, int length) {
		this.patcher = patcher;
		this.current = current;
		this.old = old;
		this.filter = filter;
		this.offset = offset;
		this.length = length;
		setName("Summoners-Difference-Worker");
	}
	
	/**
	 * The patcher instance currently running.
	 */
	private ArchivePatcher patcher;
	
	/**
	 * The up to date release manifest.
	 */
	private RiotReleaseManifestFile current;
	
	/**
	 * The outdated release manifest for comparison.
	 */
	private RiotReleaseManifestFile old;
	
	/**
	 * The filename filter.
	 */
	private FilenameFilter filter;
	
	/**
	 * The offset for parallel calculation.
	 */
	private int offset;
	
	/**
	 * The extent of which this calculator runs.
	 */
	private int length;
	
	/**
	 * The files needing patches.
	 */
	private LinkedList<RiotFileManifest> result = new LinkedList<>();
	
	/**
	 * Gets the files needing patches.
	 *
	 * @return the files needing patches
	 */
	public LinkedList<RiotFileManifest> getResult() {
		return result;
	}
	
	/**
	 * Determines if the archive needs patching.
	 *
	 * @param file
	 *            the manifest file being comparisoned.
	 * @return true, if successful
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private boolean needsPatch(RiotFileManifest file) throws IOException {
		if (file.getFileType().isArchive())
			return patcher.getArchive(file.getRelease()).get(file.getPath() + file.getName()) == null;
		
		if (old != null) {
			RiotFileManifest oldFile = current.getFile(file.getPath() + file.getName());
			if (oldFile != null && Arrays.equals(oldFile.getChecksum(), file.getChecksum()) && new File(patcher.getFileDirectory(file), file.getName()).exists())
				return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try {
			progress = 0;
			for (int index = 0; index != length; ++index) {
				progress = (float) index / length;
				RiotFileManifest file = current.getFiles()[offset + index];
				status = file.getName();
				if (filter.accept(null, file.getName()) && needsPatch(file))
					result.add(file);
			}
			
			progress = 1;
			result.sort((o1, o2) -> o1.compareTo(o2));
		} catch (IOException ex) {
			patcher.error(ex);
			ex.printStackTrace();
		}
	}
    
	/**
	 * Merges the parallel results in a fashion I do not know of.
	 *
	 * @param files
	 *            the list of results
	 * @return the merged, sorted results
	 */
	public static LinkedList<RiotFileManifest> mergeLists(LinkedList<LinkedList<RiotFileManifest>> files) {
		LinkedList<RiotFileManifest> total = new LinkedList<>();
		int[] indices = new int[files.size()];
		RiotFileManifest smallest;
		do {
			int smallestIndex = -1;
			smallest = null;
			for (int i = 0; i < files.size(); i++) {
				if (indices[i] < files.get(i).size()) {
					RiotFileManifest f = files.get(i).get(indices[i]);
					if (smallest == null || f.getReleaseInt() < smallest.getReleaseInt()) {
						smallestIndex = i;
						smallest = f;
					}
				}
			}
			if (smallest != null) {
				indices[smallestIndex]++;
				total.add(smallest);
			}
		} while (smallest != null);
		return total;
	}
}
