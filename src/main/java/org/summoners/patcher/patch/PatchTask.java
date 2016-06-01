package org.summoners.patcher.patch;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.logging.*;

import org.summoners.patcher.patch.impl.*;

/**
 * A task to patch stored data.
 * @author Xupwup
 * @author Brittan Thomas
 */
public abstract class PatchTask extends Thread {
	
	/**
	 * Patches its assigned portion of the client.
	 *
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 */
	public abstract void patch() throws MalformedURLException, IOException, NoSuchAlgorithmException;
	
	/**
	 * The status of the patcher to be conveyed to the user.
	 */
	protected String status;
	
	/**
	 * Gets the status of the patcher to be conveyed to the user.
	 *
	 * @return the status of the patcher to be conveyed to the user
	 */
	public String getStatus() {
		return status;
	}
	
	/**
	 * Sets the status of the patcher to be conveyed to the user.
	 *
	 * @param status
	 *            the new status of the patcher to be conveyed to the user
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	
	/**
	 * The percent completion of this patch task.
	 */
	protected float percentage = 0F;
    
	/**
	 * Gets the percentage completion of this patch task.
	 *
	 * @return the percentage completion of this task
	 */
	public float getPercentage() {
		return percentage;
	}
	
	/**
	 * Sets the percent completion of this patch task.
	 *
	 * @param percentage
	 *            the new percent completion of this patch task
	 */
	public void setPercentage(float percentage) {
		this.percentage = percentage;
	}
	
	/**
	 * If the patcher has finished.
	 */
	protected boolean finished;

	/**
	 * Checks if the patcher has finished.
	 *
	 * @return if the patcher has finished
	 */
	public boolean isFinished() {
		return false;
	}
	
	/**
	 * Sets if the patcher has finished.
	 *
	 * @param finished
	 *            if the patcher has finished
	 */
	public void setFinished(boolean finished) {
		this.finished = finished;
	}
    
	/**
	 * The most relevant error message to be displayed.
	 */
	protected String error;
	
	/**
	 * Gets the most relevant error message to be displayed.
	 *
	 * @return the most relevant error message to be displayed
	 */
	public String getError() {
		return error;
	}
	
	/**
	 * Sets the most relevant error message to be displayed.
	 *
	 * @param error
	 *            the new most relevant error message to be displayed
	 */
	public void setError(String error) {
		this.error = error;
	}
	
	/**
	 * Notifies the patcher of an error event.
	 * 
	 * @param message
	 * 			  the message describing the error
	 */
	public void error(String message) {
		setError(message);
		Logger.getLogger(ArchivePatcher.class.getName()).log(Level.SEVERE, message);
	}

	/**
	 * Notifies the patcher of an exception thrown event.
	 *
	 * @param ex
	 *            the exception that was thrown
	 */
	public void error(Exception ex) {
		setError(ex.getMessage());
		ex.printStackTrace();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try {
			patch();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
