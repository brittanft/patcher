package org.summoners.patcher.worker.impl;

import java.io.*;
import java.net.*;
import java.util.logging.*;
import java.util.zip.*;

import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.impl.client.*;
import org.summoners.cache.*;
import org.summoners.cache.structure.*;
import org.summoners.patcher.patch.impl.*;
import org.summoners.patcher.worker.*;
import org.summoners.util.*;

/**
 * The archive download worker.
 * @author Xupwup
 */
public class ArchiveWorker extends Worker {
	
	/**
	 * Instantiates a new file download worker.
	 *
	 * @param patcher
	 *            the active patcher instance
	 */
	public ArchiveWorker(ArchivePatcher patcher) {
		this.patcher = patcher;
		setName("Summoners-Archive-Worker");
	}
	
	/**
	 * The active patcher instance.
	 */
	private ArchivePatcher patcher;

	/**
	 * Downloads the specified file into the specified archive using an http client.
	 *
	 * @param manifest
	 *            the file manifest being downloaded
	 * @param builder
	 *            the pre-set URI builder
	 * @param client
	 *            the http client doing the downloading
	 * @param archive
	 *            the Riot archive file being written into
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 */
	private void download(RiotFileManifest manifest, URIBuilder builder, CloseableHttpClient client, RiotArchiveFile archive) throws IOException, URISyntaxException {
		URI uri = builder.setPath("/releases/" + patcher.getBranch() + "/projects/" + patcher.getProject() + "/releases/" + manifest.getRelease() 
				+ "/files/" + manifest.getPath().replaceAll(" ", "%20") + manifest.getName().replaceAll(" ", "%20")
				+ (manifest.getFileType().ordinal() > 0 ? ".compressed" : "")).build();
		
		try (CloseableHttpResponse response = client.execute(HttpUtil.getRequest(uri, "l3cdn.riotgames.com"))) {
			Validate.require(response.getStatusLine().getStatusCode() == 200, "Http responded with invalid code." + response.getStatusLine().getStatusCode(), IOException.class);
			try (InputStream fileStream = response.getEntity().getContent()) {
				try (InputStream inputStream = (manifest.getFileType() == RiotFileType.UNCOMPRESSED_ARCHIVE ? new InflaterInputStream(fileStream) : fileStream)) {
					try (OutputStream outputStream = archive.write(manifest.getPath() + manifest.getName(), manifest)) {
						int read; byte[] buffer = new byte[1024];
						while ((read = inputStream.read(buffer)) != -1) {
							record(read);
							if (patcher.isFinished())
								return;
							
							outputStream.write(buffer, 0, read);
						}
					}
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try {
			URIBuilder builder = new URIBuilder().setScheme("http").setHost("l3cdn.riotgames.com");
			try (CloseableHttpClient client = HttpUtil.getDefaultClient()) {
				while (true) {
					VersionedArchive<RiotFileManifest> task;
					synchronized (patcher.getPendingArchives()) {
						if (patcher.getPendingArchives().isEmpty() || patcher.isFinished() || patcher.getError() != null)
							break;
						
						task = patcher.getPendingArchives().pop();
					}
					
					startTime = System.currentTimeMillis();
					progress = 0;
					
					RiotArchiveFile archive = patcher.getArchive(task.getVersion()); //this file is not closed here.
					for (int index = 0; index != task.getFiles().size(); ++index) {
						if (patcher.isFinished() || patcher.getError() != null)
							break;
						
						RiotFileManifest manifest = task.getFiles().get(index);
						status = manifest.getName();
						
						RiotFile file = archive.get(manifest.getPath() + manifest.getName());
						if (file != null) {
							alternative = true;
							
							InputStream inputStream = file.getInputStream();
							if (manifest.getFileType() == RiotFileType.COMPRESSED_ARCHIVE)
								inputStream = new InflaterInputStream(inputStream);
							
							if (checkHash(new BufferedInputStream(inputStream), patcher, manifest, false)) {
								progress = (float) index / task.getFiles().size();
								continue;
							}
							
							Logger.getLogger(ArchiveWorker.class.getName()).log(Level.SEVERE, "Bad File: " + manifest);
							archive.getFiles().remove(file);
							archive.getDictionary().remove(manifest.getPath() + manifest.getName());
						}
						
						alternative = false;
						download(manifest, builder, client, archive);
						progress = (float) index / task.getFiles().size();
					}

                    progress = 1;
                    startTime = -1;
				}
			}
		} catch (URISyntaxException | IOException ex) {
			Logger.getLogger(ArchiveWorker.class.getName()).log(Level.SEVERE, null, ex);
			if (patcher.getError() == null)
				patcher.error(ex);
		}
	}

}
