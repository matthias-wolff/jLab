package de.tucottbus.kt.jlab.executables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class Dlabpro extends Executable
{
  public Dlabpro()
  throws FileNotFoundException,IllegalArgumentException
  {
    super(findExecutable("dlabpro"));
  }
  
  public Dlabpro(File exeFile)
  throws FileNotFoundException,IllegalArgumentException
  {
    super(exeFile);
  }

  @Override
  public ArrayList<String> getArguments()
  {
    ArrayList<String> args = new ArrayList<String>();
    args.add("--pipemode");
    args.add("--logo");
    return args;
  }
  
  @Override
  public String getExitCommand()
  {
    return "quit";
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
      final Dlabpro rec = new Dlabpro();
      rec.addObserver(new Observer()
      {
        public void update(Observable o, Object arg)
        {
          char   type = ((String)arg).charAt(0);
          String msg  = ((String)arg).substring(1);
          String echo = String.format("\n[EXE%c %s]",type,msg);
          
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
          }
        }
      });
      
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      while (true)
      {
        String input = in.readLine();
        if (input==null || rec.getExitCommand().equals(input)) break;
        rec.enterCommand(input);
      }
      
      rec.dispose();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    System.out.print("\n[EXE: End of Dlabpro.main method]\n");  
  }


}
