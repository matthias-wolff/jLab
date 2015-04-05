// dLabPro Plugin for Eclipse
// - VisEditor problem-with-display-data exception
// 

package de.tucottbus.kt.jlab.datadisplays.data;

/**
 * Exceptions of this class are thrown when there are any problems with the
 * display data.
 */
public class DataException extends Exception
{
  private String sMsg; 
  
  public DataException(String sMsg)
  {
    this.sMsg = sMsg;
  }
  
  public String getMessage()
  {
    return sMsg;
  }

  private static final long serialVersionUID = -4340348536924446080L;
}

// EOF
