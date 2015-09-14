package de.tucottbus.kt.dlabpro.recognizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tucottbus.kt.dlabpro.Executable;

/**
 * A low-level Java wrapper for the dLabPro recognizer executable.
 * 
 * <p><b>Life Cycle</b></p>
 * <p>
 * Creating an instance of this class starts and initializes the dLabPro recognizer executable. The
 * wrapper maintains a connection and dispatches commands to and messages from the recognizer.
 * Should the executable crash it will be automatically restarted and put into the most recent known
 * state as accurately as possible. If the wrapper is no longer needed, applications <em>must</em>
 * call {@link #dispose()}. This terminates the recognizer executable and releases all system
 * resources. Once disposed, the instance cannot be re-activated. Subsequent method invocations will
 * throw an {@link IllegalStateException}.
 * </p>
 * 
 * <p><b>Receiving Recognizer Messages</b></p>
 * <p>
 * Instances of this class are observable. Applications may register as observers through
 * {@link #addObserver(Observer)}. The <code>arg</code> parameter of subsequent invocations the
 * observer's {@link Observer#update(Observable, Object)} is a string whose first character
 * specifies the message type and whose remaining characters are the actual message. There are four
 * message types:
 * </p>
 * <table>
 *   <tr><th>Type</th><th>Description</th></tr>
 *   <tr><td>{@link #MSGT_OUT}</td><td>One line of the recognizer executable's standard output.</td></tr>
 *   <tr><td>{@link #MSGT_ERR}</td><td>One line of the recognizer executable's error output.</td></tr>
 *   <tr><td>{@link #MSGT_IN} </td><td>Echo of one line of the recognizer executable's standard input.</td></tr>
 *   <tr><td>{@link #MSGT_WRP}</td><td>A message from the wrapper instance.</td></tr>
 * </table>
 * <p>
 * The following snippet shows how to process recognizer messages 
 * </p>
 * <pre>
 * recognizer.addObserver(new Observer()
 * {
 *    public void update(Observable o, Object arg)
 *    {
 *      char   type = ((String)arg).charAt(0);
 *      String msg  = ((String)arg).substring(1);
 *        
 *      switch (type)
 *      {
 *      case Recognizer.MSGT_OUT:
 *        String stdOutMessage = msg;
 *        ...
 *        break;
 *      case Recognizer.MSGT_ERR:
 *        String stdErrMessage = msg;
 *        ...
 *        break;
 *     case Recognizer.MSGT_IN:
 *        String stdInMessage = msg;
 *        ...
 *        break;
 *      default:
 *        String wrapperMessage = msg;
 *        ...
 *        break;
 *      }
 *    }
 * });</pre>
 * <p>
 * See documentation of the dLabPro recognizer for a description of output and error messages.
 * </p>
 * 
 * <p><b>Controlling the Recognizer</b></p>
 * <p>
 * The recognizer can be controlled by inputting commands through the {@link #enterCommand(String)}
 * method. See documentation of the dLabPro recognizer for a list acceptable commands.
 * </p>
 * 
 * @author Matthias Wolff
 */
public class Recognizer extends Executable
{
  // -- Constants --
 
  /**
   * The option setting command of the recognizer.
   */
  protected static final String CMD_SET = "set";
  
  /**
   * The command waking up the recognizer from sleeping mode. 
   */
  protected static final String CMD_LISTEN = "dlgupd __WAKEUP__";
  
  /**
   * The command putting the recognizer to sleep. 
   */
  protected static final String CMD_SLEEP = "dlgupd __SLEEP__";
  
  /**
   * The exit command of the recognizer.
   */
  protected static final String CMD_EXIT = "exit";
  
  /**
   * The key of the VAD offline option.
   */
  protected static final String KEY_VADOFFLINE = "vad.offline";
  
  /**
   * The positive boolean option value.
   */
  protected static final String VAL_TRUE = "yes";
  
  /**
   * The negative boolean option value.
   */
  protected static final String VAL_FALSE = "no";
  
  /**
   * The path to the default recognizer resource files. 
   */
  protected static final String DIR_DEFRESOURCE
    = "de/tucottbus/kt/dlabpro/recognizer/resources/de";

  // -- Fields --

  /**
   * The current configuration.
   */
  protected Properties config;

  // -- Constructors --

  /**
   * Creates a new speech recognizer instance. Applications <em>must</em> call {@link #dispose()}
   * when the instance is not longer needed.
   * 
   * @param exeFile
   *          The recognizer executable file, can be <code>null</code>. In the latter case the
   *          executable <code>recognizer[.exe]</code> is expected by be accessible through the path
   *          environment variable.
   * @param config
   *          The configuration, can be <code>null</code>. In the latter case a default
   *          configuration is used.
   * @throws FileNotFoundException
   *           If the executable was not found.
   * @throws IOException
   *           If the configuration file could not be read or no functional default configuration
   *           could be created.
   */
  public Recognizer(File exeFile, Properties config)
  throws FileNotFoundException, IllegalArgumentException
  {
    super(exeFile,null);
    this.config  = config!=null ? new Properties(config) : new Properties();
  }
  
  protected Recognizer(File exeFile, ArrayList<String> args)
  throws FileNotFoundException
  {
    super(exeFile,args);
  }

  // -- Input and output --
  
  /**
   * Sets an option.
   * 
   * @param key
   *          The key.
   * @param value
   *          The new value.
   */

  public void setOption(String key, String value)
  {
    try
    {
      if (key==null || key.length()==0) return;
      if (value==null) value = "";
      enterCommand(CMD_SET+" "+key+" "+value);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      dispatchMessage(MSGT_ERR,e.toString());
    }
  }

  /**
   * Puts the recognizer in the off-line, listening or sleeping mode. There is
   * only a difference between the listening and the sleeping mode if the
   * recognizer was configured to use a finite state dialog model (configuration
   * key <code>data.dialog</code>). Additionally, actually putting the
   * recognizer into listening or sleeping mode requires an appropriate
   * transition originating from current state in the dialog FST. The recognizer
   * can, however, always be toggled between the off-line and online ("online"
   * = listening <em>or</em> sleeping mode).
   * 
   * @param mode
   *          The new listening mode:
   *          <ul>
   *            <li>-1: enter off-line mode (ignore all speech input),</li>
   *            <li>0: enter sleeping mode (wait for voice activation)<sup>1</sup>,</li>
   *            <li>1: enter listening mode (wait for speech input)<sup>2</sup>.</li>
   *          </ul>
   */
  public void setListenMode(int mode)
  {
    try
    {
      if (mode<0)
      {
        setOption(KEY_VADOFFLINE,VAL_TRUE);
      }
      else if (mode==0)
      {
        setOption(KEY_VADOFFLINE,VAL_FALSE);
        enterCommand(CMD_SLEEP);
      }
      else
      {
        setOption(KEY_VADOFFLINE,VAL_FALSE);
        enterCommand(CMD_LISTEN);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      dispatchMessage(MSGT_ERR,e.toString());
    }
  }

  @Override
  protected synchronized void dispatchMessage(char type, String message)
  {
    // Message filters
    if (type==MSGT_OUT)
    {
      // - Successful start
      if ("sta: online recognizer initialized".equals(message))
        setAutoRestart(true);
      
      // - Option dumps
      Pattern pattern = Pattern.compile("opt: (.*?) (.*?)");
      Matcher matcher = pattern.matcher(message);
      if (matcher.matches())
      {
        String key   = matcher.group(1);
        String value = matcher.group(2);
        config.setProperty(key,value);
        setChanged();
        notifyObservers(MSGT_WRP+"Option updated "+key+"="+value);
        return;
      }
    }

    super.dispatchMessage(type, message);
  }

  // -- Life cycle --
  
  @Override
  protected void launch() throws FileNotFoundException
  {
    try
    {
      // Write temporary configuration file 
      supplyResource(config,"data.feainfo","feainfo.object");
      supplyResource(config,"data.dialog" ,"dialog.fst"    );
      supplyResource(config,"data.gmm"    ,"3_15.gmm"      );
      supplyResource(config,"data.vadinfo","3_10_mod.vad"  );
      supplyResource(config,"data.sesinfo","sesinfo.object");

      File cfgFile = File.createTempFile("cfg",null);
      cfgFile.deleteOnExit();
      FileWriter fw = new FileWriter(cfgFile);
      for (String key : config.stringPropertyNames())
        fw.write(key+" = "+config.getProperty(key)+System.getProperty("line.separator"));
      fw.close();

      // Tweak program arguments
      arguments = new ArrayList<String>();
      arguments.add("-cfg");
      arguments.add(cfgFile.getAbsolutePath());
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    
    // Launch executable
    super.launch();
  }  
  
  // -- Implementation of abstract methods --
  
  @Override
  public boolean isLineMode()
  {
    return true;
  }

  @Override
  public ArrayList<String> getFixArguments()
  {
    ArrayList<String> args = new ArrayList<String>();
    args.add("-output");
    args.add("gui");
    return args;
  }

  @Override
  public String getExitCommand()
  {
    return CMD_EXIT;
  }

  // -- Auxiliary methods -- 
  
  /**
   * Supplies a recognizer resource file. The method checks the value of <code>key</code> in
   * <code>config</code>. If the key does not exist, the method creates it and sets the value to a
   * default resource file.
   * 
   * @param config
   *          The recognizer configuration.
   * @param key
   *          The key of the resource.
   * @param def
   *          The unqualified name of the default resource file (looked up in package
   *          <code>resouce.de</code>).
   */
  private void supplyResource(Properties config, String key, String def)
  {
    String msg = "Resource: "+key+"=";
    String val = config.getProperty(key);
    if (val==null)
    {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      URL resource = classLoader.getResource(DIR_DEFRESOURCE);
      File file = new File(resource.getFile());
      val = file.getAbsolutePath().replace('\\','/')+"/"+def;
      config.setProperty(key,val);
      msg+=val+" (default)";
    }
    else
      msg+=val+" (configured)";
    dispatchMessage(MSGT_WRP,msg);
  }

  // -- Static API --
  
  public static AbstractList<String> listAudioDevices(File exeFile)
  {
    ArrayList<String> audioDevices = new ArrayList<String>();
    Pattern pattern = Pattern.compile("<Device (\\d+)\\:.+? Name: (.+)");
    
    try
    {
      ArrayList<String> args = new ArrayList<String>(); args.add("-l");
      Executable recognizer = new Executable(exeFile,args)
      {
        
        @Override
        public boolean isLineMode()
        {
          return false;
        }
        
        @Override
        public ArrayList<String> getFixArguments()
        {
          return null;
        }
        
        @Override
        public String getExitCommand()
        {
          return null;
        }
      };
      recognizer.addObserver(new Observer()
      {
        @Override
        public void update(Observable o, Object arg)
        {
          if (arg!=null && arg instanceof String)
          {
            String s = (String)arg;
            Matcher m = pattern.matcher(s.trim());
            if (m.matches())
              audioDevices.add(m.group(1)+":"+m.group(2));
          }
        }
      });
      while (recognizer.isAlive())
        try { Thread.sleep(100); } catch (InterruptedException e) {}
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return audioDevices;
  }
  
  public static int getAudioDeviceIdForName(File exeFile, String name)
  {
    AbstractList<String> audioDevices = listAudioDevices(exeFile);
    for (String s:audioDevices)
    {
      String[] fields = s.split(":");
      if (fields[1].contains(name))
        return Integer.valueOf(fields[0]);
    }
    return -1;
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
      final Recognizer rec = new Recognizer(findExecutable("recognizer"),(Properties)null);
      rec.addObserver(new Observer()
      {
        @Override
        public void update(Observable o, Object arg)
        {
          char   type = ((String)arg).charAt(0);
          String msg  = ((String)arg).substring(1);
          String echo = String.format("\n[REC%c %s]",type,msg);
          
          switch (type)
          {
          case Recognizer.MSGT_OUT: // Fall through
          case Recognizer.MSGT_IN:  // Fall through
          case Recognizer.MSGT_WRP:
            if (!msg.startsWith("gui:"))
              System.out.print(echo);
            break;
          case Recognizer.MSGT_ERR:
            System.err.print(echo);
            break;
          }
        }
      });
      
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      while (true)
      {
        String input = in.readLine();
        if (input==null || Recognizer.CMD_EXIT.equals(input)) break;
        rec.enterCommand(input);
      }
      
      System.out.print("\nShutting down...");
      rec.dispose();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    System.out.print("\nEnd of main method\n");  
  }
}
