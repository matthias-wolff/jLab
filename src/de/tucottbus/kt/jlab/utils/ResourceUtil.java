package de.tucottbus.kt.jlab.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public class ResourceUtil
{

  /**
   * Returns a resource file (a file bundled with Java packages). The method
   * does not check if the file actually exists.
   * 
   * @param file
   *          The file name (the package separator '.' must be replaced by a
   *          slash '/')
   * @return The file.
   * @throws FileNotFoundException
   *           If the resource file was not found.
   */
  public static File getResourceFile(String file) throws FileNotFoundException
  {
    if (file==null) return null;
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    URL resource = classLoader.getResource(file);
    if (resource==null) throw new FileNotFoundException(file);
    try
    {
      File f = new File(resource.getFile());
      if (!f.exists()) throw new FileNotFoundException(file);
      return f;
    }
    catch (Exception e)
    {
      FileNotFoundException e2 = new FileNotFoundException(file+" ("+e.toString()+")");
      e2.initCause(e);
      throw e2;
    }
  }

  /**
   * Returns a resource image descriptor (for an image file bundled with Java 
   * packages).
   * 
   * @param fileName
   *          The file name (the package separator '.' must be replaced by a
   *          slash '/')
   * @return The image descriptor.
   */
  public static ImageDescriptor getImageDescriptor(String fileName)
  {
    if (fileName==null) return null;
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    URL url = cl.getResource(fileName);
    if (url==null) System.err.println("Resource file "+fileName+" not found.");
    return ImageDescriptor.createFromURL(url);
  }

  /**
   * Loads a resource image (an image file bundled with Java packages).
   * 
   * @param fileName
   *          The file name (the package separator '.' must be replaced by a
   *          slash '/')
   * @return The image or <code>null</code> if the resource file was not found.
   */
  public static Image loadImage(String fileName)
  {
    return getImageDescriptor(fileName).createImage();
  }
  
}
