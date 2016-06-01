package org.summoners.patcher.worker.impl;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.logging.*;
import java.util.zip.*;

import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.impl.client.*;
import org.summoners.cache.*;
import org.summoners.patcher.patch.impl.*;
import org.summoners.patcher.worker.*;
import org.summoners.util.*;

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
	 * @param builder
	 *            the pre-set URI builder
	 * @param client
	 *            the http client doing the downloading
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws URISyntaxException 
	 * 			   the uri syntax exception
	 */
	private void download(RiotFileManifest manifest, URIBuilder builder, CloseableHttpClient client) throws MalformedURLException, IOException, NoSuchAlgorithmException, URISyntaxException {
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
		URI uri = builder.setPath("/releases/" + patcher.getBranch() + "/projects/" + patcher.getProject() + "/releases/" + manifest.getRelease() 
					+ "/files/" + manifest.getPath().replaceAll(" ", "%20") + manifest.getName().replaceAll(" ", "%20")
					+ (manifest.getFileType().ordinal() > 0 ? ".compressed" : "")).build();
		
		long total = 0;
		try (CloseableHttpResponse response = client.execute(HttpUtil.getRequest(uri, "l3cdn.riotgames.com"))) {
			Validate.require(response.getStatusLine().getStatusCode() == 200, "Http responded with invalid code." + response.getStatusLine().getStatusCode(), IOException.class);
			try (InputStream stream = response.getEntity().getContent()) {
				try (InputStream input = manifest.getFileType().ordinal() > 0 ? new InflaterInputStream(stream) : stream) {
					try (OutputStream output = new BufferedOutputStream(new FileOutputStream(target))) {
						int read; byte[] buffer = new byte[4096];
						while ((read = input.read(buffer)) != -1) {
							output.write(buffer, 0, read);
							record(read);
							total += read;
							progress = (float) total / manifest.getSizeCompressed();
							if (patcher.isFinished())
								return;
						}
					}
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
			URIBuilder builder = new URIBuilder().setScheme("http").setHost("l3cdn.riotgames.com");
			try (CloseableHttpClient client = HttpUtil.getDefaultClient()) {
				RiotFileManifest task;
				while (true) {
					synchronized (patcher.getPendingFiles()) {
						if (patcher.getPendingFiles().isEmpty() || patcher.isFinished() || patcher.getError() != null)
							break;
						
						task = patcher.getPendingFiles().pop();
					}
					
					startTime = System.currentTimeMillis();
					status = task.getName();
					download(task, builder, client);
					startTime = -1;
				}
			}
		} catch (IOException | NoSuchAlgorithmException | URISyntaxException ex) {
			Logger.getLogger(FileWorker.class.getName()).log(Level.SEVERE, null, ex);
			if (patcher.getError() == null)
				patcher.error(ex);
		}
	}
}
