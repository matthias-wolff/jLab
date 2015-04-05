package de.tucottbus.kt.jlab.datadisplays.interfaces;

import de.tucottbus.kt.jlab.datadisplays.events.DisplayInfoMouseMoveEvent;
import de.tucottbus.kt.jlab.datadisplays.events.DisplayInfoScrollEvent;

public interface IDisplayInfoListener {

	/**
	 * Informs the listener that new information is available after
	 * the mouse was moved
	 * 
	 * @param e
	 * 			The event containing the information
	 */
	public void informationChangedMouseMove(DisplayInfoMouseMoveEvent e);
	
	/**
	 * Resets the information about the mouse position
	 */
	public void clearMouseInformation();
	
	/**
	 * Informs the listener that new information is available after
	 * the display was scrolled
	 * 
	 * @param e
	 * 			The event containing the information
	 */
	public void informationChangedScroll(DisplayInfoScrollEvent e);	
}
