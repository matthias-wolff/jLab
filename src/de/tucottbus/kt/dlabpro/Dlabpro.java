package de.tucottbus.kt.dlabpro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import de.tucottbus.kt.dlabpro.recognizer.Recognizer;

public class Dlabpro extends Executable
{
  public Dlabpro(ArrayList<String> arguments)
  throws FileNotFoundException,IllegalArgumentException
  {
    super(findExecutable("dlabpro"),arguments);
  }
  
  public Dlabpro(File exeFile,ArrayList<String> arguments)
  throws FileNotFoundException,IllegalArgumentException
  {
    super(exeFile,arguments);
  }

  @Override
  public ArrayList<String> getFixArguments()
  {
    ArrayList<String> args = new ArrayList<String>();
    return args;
  }
  
  @Override
  public String getExitCommand()
  {
    return "quit";
  }

  @Override
  public boolean isLineMode()
  {
    return false;
  }

  // -- Main method --
  
  /**
   * DEBUGGING: Runs the dLabPro {@linkplain Recognizer recognizer wrapper} with
   * a test recognition network (<code>de.tudresden.ias.dlabpro.recognizer.resources.de.default.dlg.txt</code>).
   * The standard and error outputs of the recognizer will be echoed, as well as
   * the standard input:
   * <table>
   *   <tr><th>Line format</th><th>Description</th></tr>
   *   <tr><td><code>[REC&lt; ....]</code></td><td>Recognizer stdout</td></tr>
   *   <tr><td><code>[REC! ....]</code></td><td>Recognizer stderr</td></tr>
   *   <tr><td><code>[REC&gt; ....]</code></td><td>Recognizer stdin</td></tr>
   *   <tr><td><code>[REC: ....]</code></td><td>Wrapper message</td></tr>
   * </table>
   * Commands may be entered on the console (see documentation
   * if dLabPro recognizer for a list), <code>exit</code> will terminate the
   * program.
   * 
   * @param args
   *          -- not used --
   */
  public static void main(String[] args)
  {
    try
    {
      final Dlabpro dlabpro = new Dlabpro(null);
      dlabpro.addObserver(new Observer()
      {
        
        public void update(Observable o, Object arg)
        {          
          char   type = ((String)arg).charAt(0);
          String msg  = ((String)arg).substring(1);
          while (msg.endsWith("\n"))
            msg = msg.substring(0,msg.length()-1);
          if (msg.length()==0) return;
          String echo = String.format("\n[EXE%c] %s",type,msg);
          
          switch (type)
          {
          case Executable.MSGT_OUT: // Fall through
          case Executable.MSGT_IN:  // Fall through
          case Executable.MSGT_WRP:
            System.out.print(echo);
            break;
          case Executable.MSGT_ERR:
            System.err.print(echo);
            break;
          case Executable.MSGT_EXIT:
            // HACK: Get rid of blocked readLine in main loop
            System.exit(0);
          }
        }
        
        /*
        public void update(Observable o, Object arg)
        {          
          char   type = ((String)arg).charAt(0);
          String msg  = ((String)arg).substring(1);
          
          switch (type)
          {
          case Executable.MSGT_OUT:
            System.out.print(msg);
            break;
          case Executable.MSGT_ERR:
            System.err.print(msg);
            break;
          case Executable.MSGT_EXIT:
            // HACK: Get rid of blocked readLine in main loop
            System.exit(0);
          }
        }
        */
      });
      
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      while (dlabpro.isAlive())
      {
        String input = in.readLine();
        if (dlabpro.isAlive())
          dlabpro.enterCommand(input);
      }
      
      dlabpro.dispose();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

}
