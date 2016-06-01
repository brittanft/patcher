package org.summoners.patcher.patch.impl;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.stream.*;

import org.summoners.cache.*;

/**
 * The SLN patching task.
 * @author Xupwup
 * @author Brittan Thomas
 */
public class SLNPatchTask extends CopyTask {

	/**
	 * Instantiates a new SLN patch task.
	 *
	 * @param gameVersion
	 *            the game version being patched
	 * @param slnVersion
	 *            the sln version being patched
	 * @param force
	 *            whether the patch should be forced or not
	 */
	public SLNPatchTask(String gameVersion, String slnVersion, boolean force) {
		super(RiotFileUtil.getRADSFile("projects/lol_game_client/releases/" + gameVersion + "/deploy/"),
				RiotFileUtil.getRADSFile("solutions/lol_game_client_sln/releases/" + slnVersion + "/deploy/"), true);
		this.slnVersion = slnVersion;
		this.force = force;
	}
	
	/**
	 * The sln version being patched.
	 */
	private final String slnVersion;
	
	/**
	 * Whether the patch should be forced or not.
	 */
	private final boolean force;
	
	/* (non-Javadoc)
	 * @see org.summoners.patcher.impl.CopyTask#patch()
	 */
	@Override
	public void patch() throws MalformedURLException, IOException, NoSuchAlgorithmException {
		File solutions = RiotFileUtil.getRADSFile("solutions/lol_game_client_sln/releases/");
		String[] directories = solutions.list((dir, name) -> name.matches("([0-9]+\\.){3}[0-9]+"));
		if (directories.length > 1)
			Stream.of(directories).filter(dir -> !dir.equals(slnVersion)).forEach(dir -> RiotFileUtil.deleteDir(new File(solutions, dir)));
		
		if (destination.exists() && new File(destination.getParent(), "S_OK").exists() && !force) {
			percentage = 100F;
			finished = true;
		} else {
			super.patch();
			new File(destination.getParent(), "S_OK").createNewFile();
		}
	}
}
