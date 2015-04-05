package de.tucottbus.kt.jlab.datadisplays.utils;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;

import de.tucottbus.kt.jlab.utils.ResourceUtil;

/**
 * Utility class for data display play-back actions.
 * 
 * @author Matthias Wolff
 */
public class PlayActionUtil
{
  /**
   * The icon resource path.
   */
  static String ICONPATH = "de/tucottbus/kt/jlab/datadisplays/resources/icons";
  
  /**
   * Play icon image descriptor.
   */
  static ImageDescriptor imgdPlay
    = ResourceUtil.getImageDescriptor(ICONPATH+"/etool16/play.gif");
  
  /**
   * Play-with-warning image descriptor.
   */
  static ImageDescriptor imgdPlayW
    = ResourceUtil.getImageDescriptor(ICONPATH+"/etool16/playw.gif");
  
  /**
   * Stop icon image descriptor.
   */
  static ImageDescriptor imgdStop
    = ResourceUtil.getImageDescriptor(ICONPATH+"/etool16/stop.gif");

  /**
   * Sets the play-back action UI to "play" state.
   * 
   * @param action
   *          The action.
   */
  public static void setPlay(IAction action)
  {
    action.setImageDescriptor(imgdPlay);
    action.setEnabled(true);
    action.setToolTipText("Play");
  }

  /**
   * Sets the play-back action UI to "play-with-warning" state.
   * 
   * @param action
   *          The action.
   * @param message
   *          A brief message describing the reason for the warning (will be 
   *          displayed in the tool tip), may be <code>null</code>.
   */
  public static void setPlayWarning(IAction action, String message)
  {
    action.setImageDescriptor(imgdPlayW);
    action.setEnabled(true);
    if (message!=null && message.length()>0) 
      action.setToolTipText("Play (Warning: "+message+")");
    else
      action.setToolTipText("Play (Warning)");
  }
  
  /**
   * Sets the play-back action UI to "stop" state.
   * 
   * @param action
   *          The action.
   */
  public static void setStop(IAction action)
  {
    action.setImageDescriptor(imgdStop);
    action.setEnabled(true);
    action.setToolTipText("Stop");
  }
  
  /**
   * Sets the play-back action UI to the disabled state.
   * 
   * @param action
   *          The action.
   * @param message
   *          A brief message describing why play-back is disabled (will be 
   *          displayed in the tool tip), may be <code>null</code>.
   */
  public static void setDisabled(IAction action, String message)
  {
    action.setImageDescriptor(imgdPlay);
    action.setEnabled(false);
    if (message!=null && message.length()>0) 
      action.setToolTipText("Play (Disabled because "+message+")");
    else
      action.setToolTipText("Play");
  }
}

// EOF

