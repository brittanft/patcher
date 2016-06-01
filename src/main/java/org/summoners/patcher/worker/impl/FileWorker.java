package org.summoners.patcher.worker.impl;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.logging.*;
import java.util.zip.*;

import org.summoners.cache.*;
import org.summoners.http.*;
import org.summoners.patcher.patch.impl.*;
import org.summoners.patcher.worker.*;

/**
 * The file download worker.
 * @author Xupwup
 */
public class FileWorker extends Worker {
	
	/**
	 * Instantiates a new file download worker.
	 *
	 * @param patcher
	 *            the active patcher instance
	 */
	public FileWorker(ArchivePatcher patcher) {
		this.patcher = patcher;
		setName("Summoners-Download-Worker");
	}
	
	/**
	 * The active patcher instance.
	 */
	private ArchivePatcher patcher;
	
	/**
	 * Downloads the specified file using the given http client.
	 *
	 * @param manifest
	 *            the file manifest being downloaded
	 * @param client
	 *            the http client doing the downloading
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 */
	private void download(RiotFileManifest manifest, HttpClient client) throws MalformedURLException, IOException, NoSuchAlgorithmException {
		progress = 0F;
		alternative = false;
		File targetDir = new File(patcher.getFileDirectory(manifest));
		File target = new File(targetDir.getPath() + "/" + manifest.getName());
		targetDir.mkdirs();
		
		if (!target.createNewFile() && (patcher.isForced() || patcher.isForceSingleFiles())) {
			alternative = true;
			if (checkHash(new BufferedInputStream(new FileInputStream(target)), patcher, manifest))
				return;
		}
		
		progress = 0F;
		String url = "/releases/" + patcher.getBranch() + "/projects/" + patcher.getProject() + "/releases/" + manifest.getRelease() 
					+ "/files/" + manifest.getPath().replaceAll(" ", "%20") + manifest.getName().replaceAll(" ", "%20")
					+ (manifest.getFileType().ordinal() > 0 ? ".compressed" : "");
		
		HttpResult result = client.get(url);
		InputStream fileStream = result.getInputStream();
		
		long total = 0L;
		try (InputStream input = (manifest.getFileType().ordinal() > 0 ? new InflaterInputStream(fileStream) : fileStream)) {
			try (OutputStream output = new BufferedOutputStream(new FileOutputStream(target))) {
				int read; byte[] buffer = new byte[4096];
				while((read = input.read(buffer)) != -1) {
					output.write(buffer, 0, read);
					record(read);
					total += read;
					progress = (float) total / manifest.getSizeCompressed();
					if (patcher.isFinished())
						return;
				}
			}
		}
		
		progress = 1F;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try {
			try (HttpClient client = new HttpClient("l3cdn.riotgames.com")) {
				client.throwExceptionWhenNot200 = true;
				client.setErrorHandler(HTTP_ERROR_HANDLER);
				
				RiotFileManifest task;
				while (true) {
					synchronized (patcher.getPendingFiles()) {
						if (patcher.getPendingFiles().isEmpty() || patcher.isFinished() || patcher.getError() != null)
							break;
						
						task = patcher.getPendingFiles().pop();
					}
					
					startTime = System.currentTimeMillis();
					status = task.getName();
					download(task, client);
					startTime = -1;
				}
			}
		} catch (IOException | NoSuchAlgorithmException ex) {
			Logger.getLogger(FileWorker.class.getName()).log(Level.SEVERE, null, ex);
			if (patcher.getError() == null)
				patcher.error(ex);
		}
	}
}
