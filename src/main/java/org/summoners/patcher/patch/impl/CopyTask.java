package org.summoners.patcher.patch.impl;

import java.io.*;
import java.net.*;
import java.security.*;

import org.summoners.patcher.patch.*;
import org.summoners.patcher.worker.*;

/**
 * A task to copy two files.
 * @author Xupwup
 * @author Brittan Thomas
 */
public class CopyTask extends PatchTask {
	
	/**
	 * Instantiates a new copy task.
	 *
	 * @param source
	 *            the file being copied
	 * @param destination
	 *            the destination to copy the file to
	 */
	public CopyTask(File source, File destination) {
		this(source, destination, true);
	}
	
	/**
	 * Instantiates a new copy task.
	 *
	 * @param source
	 *            the file being copied
	 * @param destination
	 *            the destination to copy the file to
	 * @param merge
	 *            if the files should merge
	 */
	public CopyTask(File source, File destination, boolean merge) {
		this.source = source;
		this.destination = destination;
		this.merge = merge;
		setName("Summoners-Copy-Task");
	}
	
	/**
	 * The file being copied.
	 */
	protected File source;
	
	/**
	 * Gets the file being copied.
	 *
	 * @return the file being copied
	 */
	public File getSource() {
		return source;
	}
	
	/**
	 * The destination location to copy.
	 */
	protected File destination;
	
	/**
	 * Gets the destination location to copy.
	 *
	 * @return the destination location to copy
	 */
	public File getDestination() {
		return destination;
	}
	
	/**
	 * If this task should merge the folders.
	 */
	protected boolean merge;
	
	/**
	 * Determines if this task should merge the folders.
	 *
	 * @return true, if this task should merge the folders.
	 */
	public boolean shouldMerge() {
		return merge;
	}
	
	/**
	 * Copies the source file into the destination folder.
	 *
	 * @param source
	 *            the source file or folder to be copied
	 * @param destination
	 *            the destination to be copied to
	 * @throws FileNotFoundException
	 *             the file not found exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void copy(File source, File destination) throws FileNotFoundException, IOException {
		File file = new File(destination, source.getName());
		if (source.isDirectory()) {
			file.mkdir();
			for (String name : source.list()) {
				status = name;
				copy(new File(source, name), new File(destination, source.getName()));
			}
			return;
		}
		
		file.createNewFile();
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(source))) {
			int read; byte[] buffer = new byte[4096];
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
				while ((read = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, read);
					if (finished)
						return;
					
					Worker.record(read);
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.summoners.patcher.PatchTask#patch()
	 */
	@Override
	public void patch() throws MalformedURLException, IOException, NoSuchAlgorithmException {
		destination.mkdirs();
		merge &= source.isDirectory();
		
		percentage = 0F;
		if (!merge) {
			copy(source, destination);
		} else {
			for (String fileName : source.list()) {
				status = fileName;
				copy(new File(source, fileName), destination);
			}
		}
		
		percentage = 100F;
		finished = true;
	}
	
}
