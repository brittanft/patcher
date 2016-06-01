package org.summoners.patcher.patch.impl;

import java.io.*;
import java.net.*;
import java.security.*;

import org.summoners.patcher.patch.*;

/**
 * A task that runs a variable runnable instance.
 * @author Brittan Thomas
 */
public class RunTask extends PatchTask {
	
	/**
	 * Instantiates a new run task.
	 *
	 * @param runnable
	 *            the runnable instance to be ran
	 * @param description
	 *            the description of this task
	 */
	public RunTask(Runnable runnable, String description) {
		this(runnable, description, false);
	}
	
	/**
	 * Instantiates a new run task.
	 *
	 * @param runnable
	 *            the runnable instance to be ran
	 * @param description
	 *            the description of this task
	 * @param stop
	 *            whether this run task should stop next cycle
	 */
	public RunTask(Runnable runnable, String description, boolean stop) {
		this.runnable = runnable;
		this.description = description;
		this.stop = stop;
	}
	
	/**
	 * A runnable instance to be ran by this task.
	 */
	private final Runnable runnable;
	
	/**
	 * The description of this run task.
	 */
	private final String description;
	
	/**
	 * Whether this run task should stop next cycle.
	 */
	private boolean stop;
	
	/**
	 * Checks whether this run task should stop next cycle.
	 *
	 * @return whether this run task should stop next cycle
	 */
	public boolean shouldStop() {
		return stop;
	}
	
	/**
	 * Sets whether this run task should stop next cycle.
	 *
	 * @param stop
	 *            whether this run task should stop next cycle
	 */
	public void setStop(boolean stop) {
		this.stop = stop;
	}

	/* (non-Javadoc)
	 * @see org.summoners.patcher.PatchTask#patch()
	 */
	@Override
	public void patch() throws MalformedURLException, IOException, NoSuchAlgorithmException {
		percentage = 0;
		status = description;
		runnable.run();
		percentage = 100;
		finished = !stop;
	}

}
