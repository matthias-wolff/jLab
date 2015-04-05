package de.tucottbus.kt.jlab.datadisplays.interfaces;


public interface IVerticalZoomSubject {
	
	/**
	 * Adds a new listener to the list of listeners
	 * 
	 * @param l		the new listener for zoom events
	 */
	public void addZoomListener(VerticalActionListener l);
	
	/**
	 * tells the listening objects that a vertical zoom-in occurred.
	 */
	public void fireVerticalZoomInEvent();
	
	/**
	 * tells the listening objects that a vertical zoom-out occurred.
	 */
	public void fireVerticalZoomOutEvent();
	
	/**
	 * tells the listening objects that a vertical scroll occurred.
	 */
	public void fireVerticalScrollEvent(int direction);
}
