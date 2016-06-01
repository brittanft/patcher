package org.summoners.patcher.worker.impl;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.*;
import java.util.stream.*;

import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.impl.client.*;
import org.summoners.cache.*;
import org.summoners.cache.pkg.*;
import org.summoners.cache.pkg.Package;
import org.summoners.math.*;
import org.summoners.patcher.patch.impl.*;
import org.summoners.patcher.worker.*;
import org.summoners.util.*;

/**
 * The package downloader worker.
 * @author Brittan Thomas
 */
public class PackageWorker extends Worker {
	
	/**
	 * The version being handled.
	 */
	private String version;
	
	/**
	 * The branch being handled.
	 */
	private String branch;
	
	/**
	 * The project being handled.
	 */
	private String project;
	
	/**
	 * The list of culled files needing patches.
	 */
	private LinkedList<RiotFileManifest> culledFiles = new LinkedList<>();
	
	/**
	 * The list of the list of ranges for each bin.
	 */
	private LinkedHashMap<String, LinkedList<Range2l>> ranges = new LinkedHashMap<>();
	
	/**
	 * A map converting bin name to package.
	 */
	private LinkedHashMap<String, Package> packages = new LinkedHashMap<>(); // binname -> filelist
	
	/**
	 * A map converting bin name to package file.
	 */
	private LinkedHashMap<String, PackageFile> fileMap = new LinkedHashMap<>(); // name -> packf
	
	/**
	 * The total amount of bytes read.
	 */
	private long total = 0;
	
	/**
	 * The most recent timestamp of a sync event.
	 */
	private long lastSyncTime;

	/**
	 * Instantiates a new package worker.
	 *
	 * @param branch
	 *            the branch
	 * @param project
	 *            the project
	 * @param version
	 *            the version
	 * @param culledFiles
	 *            the list of culled files needing patches
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 */
	public PackageWorker(String branch, String project, String version, LinkedList<RiotFileManifest> culledFiles) throws IOException {
		this.version = version;
		this.branch = branch;
		this.project = project;
		this.culledFiles = culledFiles;

		URIBuilder builder = new URIBuilder().setScheme("http").setHost("l3cdn.riotgames.com")
								.setPath("/releases/" + branch + "/projects/" + project + "/releases/" + version + "/packages/files/packagemanifest");
		try {
			try (CloseableHttpClient client = HttpUtil.getDefaultClient()) {
				try (CloseableHttpResponse response = client.execute(HttpUtil.getRequest(builder.build(), "l3cdn.riotgames.com"))) {
					Validate.require(response.getStatusLine().getStatusCode() == 200, "Http responded with invalid code." + response.getStatusLine().getStatusCode(), IOException.class);
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
						Validate.check(reader.readLine(), h -> h.equals("PKG1"), h -> "Header does not equal PKG1. Actual header is: " + h, IOException.class);
						String line;
						while ((line = reader.readLine()) != null) 
							fileMap.put(line.split(",")[0], new PackageFile(line.split(",")));
					}
				}
			}
		} catch (URISyntaxException exception) {
			Logger.getLogger(PackageWorker.class.getName()).log(Level.SEVERE, null, exception);
		}
		
		setName("Summoners-Package-Worker");
	}
	
	/**
	 * Gets a packaged file from the specified file manifest.
	 *
	 * @param manifest
	 *            the file manifest describing the packaged file
	 * @return the retrieved packaged file
	 */
	private PackageFile getPackageFile(RiotFileManifest manifest) {
		String url = "/projects/" + project + "/releases/" + manifest.getRelease() + "/files/" + manifest.getPath()
						+ manifest.getName() + (manifest.getFileType().ordinal() > 0 ? ".compressed" : "");
		PackageFile pkgFile = fileMap.get(url);
		pkgFile.setManifest(manifest);
		return pkgFile;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		updateRanges(culledFiles);
	}
	
	/**
	 * Updates the list of ranges from the list of manifests.
	 *
	 * @param files
	 *            the list of file manifests to be parsed
	 */
	public void updateRanges(LinkedList<RiotFileManifest> files) {
		files = new LinkedList<>(files);
		Iterator<RiotFileManifest> it = files.iterator();
		while (it.hasNext()) {
			if (getPackageFile(it.next()) == null)
				it.remove();
		}
		
		for (RiotFileManifest manifest : files) {
			PackageFile pkgFile = getPackageFile(manifest);
			Package pkg = packages.computeIfAbsent(pkgFile.getBin(), bin -> new Package(bin));
			pkg.getFiles().add(pkgFile);
		}
		
		packages.values().stream().forEach(pkg -> pkg.getFiles().sort((p1, p2) -> Long.compare(p1.getOffset(), p2.getOffset())));
		for (Entry<String, Package> entry : packages.entrySet()) {
			LinkedList<Range2l> rangeList = ranges.computeIfAbsent(entry.getKey(), s -> new LinkedList<>());
			for (PackageFile pkgFile : entry.getValue().getFiles()) {
				Range2l last = rangeList.isEmpty() ? null : rangeList.getLast();
				if (last == null || pkgFile.getOffset() > last.getMaximum()) {
					last = new Range2l(pkgFile.getOffset(), pkgFile.getOffset() + pkgFile.getSize());
					rangeList.add(last);
				} else
					last.setMaximum(Math.max(last.getMaximum(), pkgFile.getOffset() + pkgFile.getSize()));
			}
		}
		
		total = 0;
		for (LinkedList<Range2l> list : ranges.values())
			for (Range2l range : list)
				total += range.getSpan();
	}
	
	/**
	 * Download the ranges using an {@link CloseableHttpClient}.
	 *
	 * @param patcher
	 *            the active patcher instance
	 * @return the linked list of downloaded ranges
	 * @throws FileNotFoundException
	 *             the file not found exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 */
	public LinkedList<RiotFileManifest> downloadRanges(ArchivePatcher patcher) throws FileNotFoundException, IOException, URISyntaxException {
		lastSyncTime = System.currentTimeMillis();
		long total = 0;

		URIBuilder builder = new URIBuilder().setScheme("http").setHost("l3cdn.riotgames.com");
		try (CloseableHttpClient client = HttpUtil.getDefaultClient()) {
			for (Entry<String, LinkedList<Range2l>> entry : ranges.entrySet()) {
				LinkedList<Range2l> rangeList = entry.getValue();
				for (Range2l range : rangeList) {
					URI uri = builder.setPath("/releases/" + branch + "/projects/" + project + "/releases/" + version
														+ "/packages/files/" + entry.getKey()).build();
					try (CloseableHttpResponse response = client.execute(HttpUtil.getRequest(uri, "l3cdn.riotgames.com", false, range))) {
						try (InputStream inputStream = response.getEntity().getContent()) {
							Validate.require(response.getStatusLine().getStatusCode() == 200, "Http responded with invalid code." + response.getStatusLine().getStatusCode(), IOException.class);
							int read; long offset = range.getMinimum(); byte[] buffer = new byte[1024];
							while ((read = inputStream.read(buffer)) != -1) {
								pushBytes(read, offset, buffer, entry.getKey(), patcher);
								record(read);
								total += read; offset += read;
								Validate.requireFalse(offset > range.getMaximum(), "More bytes received than expected.", IOException.class);
								patcher.setDownloadProgress(100F * total / this.total);
								if (patcher.isFinished())
									return new LinkedList<>();
							}

							Package pkg = packages.get(entry.getKey());
							for (PackagedData file : pkg.getPackagedData())
								file.getOutputStream().close();
							
							pkg.getPackagedData().clear();
						}
					}
				}
			}
		}
		
		patcher.syncAllArchives();
		return packages.values().stream().flatMap(pkg -> pkg.getFiles().stream()).map(pkgFile -> pkgFile.getManifest())
				.collect(Collectors.toCollection(() -> new LinkedList<>()));
	}
	
    /** 
     * Push the given bytes to all consumers. Such as archives or normal files. (Sends
     * a slice of the given bytes to the appropriate outputstream(s))
     * 
     * This function assumes that the package file never contains two (or more) 
     * overlapping files that belong to the same archive. If overlap does happen
     * within the same archive, patching will silently create invalid data.
     * Overlapping files that belong to another package, or files that are outside 
     * of any package, are supported however.
     * 
     * I believe that packages never contain two overlapping files that belong to the
     * same package. However, this may change in the future, or I may be wrong right now.
	 *
	 * @param read
	 *            the amount of bytes read
	 * @param offset
	 *            the offset in the bin file
	 * @param buffer
	 *            the byte buffer
	 * @param bin
	 *            the name of the bin file
	 * @param p
	 *            a reference to the patcher object
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void pushBytes(int read, long offset, byte[] buffer, String bin, ArchivePatcher patcher) throws IOException {
		Package pkg = packages.get(bin);
		long os;
		while (pkg.getIndex() < pkg.getFiles().size() && (os = pkg.next().getOffset()) < offset + read && os >= offset) {
			if (pkg.next().getOffset() > offset + read)
				System.err.println("PackageFile Offset exceeds limit.");
			PackageFile pkgFile = pkg.next();
			pkg.open(pkgFile, patcher.getArchive(pkgFile.getManifest().getRelease()), patcher.getFileDirectory(pkgFile.getManifest()));
			pkg.setIndex(pkg.getIndex() + 1);
		}
		
		for (int index = 0; index < pkg.getPackagedData().size(); index++) {
			PackagedData pkgData = pkg.getPackagedData().get(index);
			if (index == 0)
				patcher.setStatus(pkgData.getPackageFile().getManifest().getName());
			
			int off = Math.max(0, (int) (pkgData.getPackageFile().getOffset() - offset));
			int remaining = Math.max(0, (int) ((pkgData.getPackageFile().getOffset() + pkgData.getPackageFile().getSize()) - offset));
			int length = Math.min(read, remaining) - off;
			try {
				pkgData.getOutputStream().write(buffer, off, length);
			} catch (IOException e) {
				System.err.println("o=" + off + " l=" + length + " bufl=" + buffer.length + " offset=" + offset + " foffset=" + pkgData.getPackageFile().getOffset()
							+ " flen=" + pkgData.getPackageFile().getSize() + " ofl=" + pkg.getPackagedData().size() + " read=" + read);
				throw e;
			}
			if (remaining + off <= read) {
				pkgData.getOutputStream().close();
				pkg.getPackagedData().remove(index--);
			}
		}
		
		if (System.currentTimeMillis() - lastSyncTime > 5000L) {
			patcher.syncAllArchives();
			lastSyncTime = System.currentTimeMillis();
		}
	}
}
