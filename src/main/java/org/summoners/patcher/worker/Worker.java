package org.summoners.patcher.worker;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;

import org.summoners.cache.*;
import org.summoners.patcher.patch.impl.*;

/**
 * Representation of an abstract worker thread.
 * @author Xupwup
 */
public abstract class Worker extends Thread {
	
	/**
	 * The current progression of this worker thread.
	 */
	protected float progress = 1;
	
	/**
	 * Gets the current progression of this worker thread.
	 *
	 * @return the current progression of this worker thread
	 */
	public float getProgress() {
		return progress;
	}
	
	/**
	 * The timestamp when this worker started its work.
	 */
	protected long startTime;
	
	/**
	 * Gets the timestamp when this worker started its work.
	 *
	 * @return the timestamp when this worker started its work
	 */
	public long getStartTime() {
		return startTime;
	}
	
	/**
	 * The most recent status message to be displayed.
	 */
	protected String status;
	
	/**
	 * Gets the most recent status message to be displayed.
	 *
	 * @return the most recent status message to be displayed
	 */
	public String getStatus() {
		return status;
	}
	
	/**
	 * If the current work is an alternative motive.
	 */
	protected boolean alternative;
	
	/**
	 * Checks if the current work is an alternative motive.
	 *
	 * @return if the current work is an alternative motive
	 */
	public boolean isAlternative() {
		return alternative;
	}

	/**
	 * Compares the hash of a downloaded file versus its existing manifest.
	 *
	 * @param input
	 *            the HTTP input stream
	 * @param patcher
	 *            the active patcher instance
	 * @param manifest
	 *            the manifest describing the downloaded file
	 * @return true, if successful
	 */
	protected boolean checkHash(InputStream input, ArchivePatcher patcher, RiotFileManifest manifest) {
		return checkHash(input, patcher, manifest, true);
	}
	
	/**
	 * Compares the hash of a downloaded file versus its existing manifest.
	 *
	 * @param input
	 *            the HTTP input stream
	 * @param patcher
	 *            the active patcher instance
	 * @param manifest
	 *            the manifest describing the downloaded file
	 * @param update
	 *            if progress updates are required
	 * @return true, if successful
	 */
	protected boolean checkHash(InputStream input, ArchivePatcher patcher, RiotFileManifest manifest, boolean update) {
		try {
			long total = 0;
			MessageDigest md = MessageDigest.getInstance("MD5");
			InputStream is = new DigestInputStream(input, md);
			try {
				int read;
				byte[] buffer = new byte[4096];
				while((read = is.read(buffer)) != -1) {
					total += read;
					if (update)
						progress = (float) total / manifest.getSizeCompressed();
					
					record(read);
					if (patcher.isFinished())
						return true;
				}
				is.close();
			} catch (IOException ex) {
				ex.printStackTrace();
				return false;
			} finally {
				is.close();
			}
			byte[] checksum = md.digest();
			return Arrays.equals(checksum, manifest.getChecksum());
		} catch (IOException | NoSuchAlgorithmException ex) {
			Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
		}
		throw new IllegalStateException("MD5 not found.");
	}
	
	/**
	 * The last timestamp of the speed sample.
	 */
	protected static long last = 0L;
	
	/**
	 * The amount of bytes read in this sampling.
	 */
	protected static int sample = 0;
	
	/**
	 * This session's download speed (in kb/s).
	 */
	protected static int speed = 0;
	
	/**
	 * Gets the this session's download speed (in kb/s).
	 *
	 * @return the this session's download speed (in kb/s)
	 */
	public static int getSpeed() {
		return speed;
	}
	
	/**
	 * Records the amount of bytes read in the current sample.
	 *
	 * @param read
	 *            the amount of bytes read in the current sample
	 */
	public static synchronized void record(int read) {
		long now = System.currentTimeMillis();
		sample += read;
		if (now - last > 1000) {
			speed = (int) ((sample / ((now - last) / 1000F)) / 1024);
			sample = 0;
			last = now;
		}
	}
}
