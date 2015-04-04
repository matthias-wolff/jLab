package de.tucottbus.kt.dlabpro.executables;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 * A low-level wrapper for the dLabPro executables and executable UASR scripts.
 * 
 * <p><b>Life Cycle</b></p>
 * <p>Creating an instance of a derived class starts and initializes a dLabPro
 * executable. The wrapper maintains a connection and dispatches commands to and
 * messages from the executable. Should the executable crash it will be
 * automatically restarted. If the wrapper is no longer needed, applications
 * <em>must</em> call {@link #dispose()}. This terminates the executable and 
 * releases all system resources. Once disposed, the instance cannot be 
 * re-activated. Subsequent method invocations will throw an {@link 
 * IllegalStateException}.</p>
 * 
 * <p><b>Receiving Recognizer Messages</b></p>
 * <p>Instances of this class are observable. Applications may register as
 * observers through {@link #addObserver(Observer)}. The <code>arg</code>
 * parameter of subsequent invocations the observer's {@link 
 * Observer#update(Observable, Object)} is a string whose first character
 * specifies the message type and whose remaining characters are the actual
 * message. There are four message types:</p>
 * <table>
 *   <tr><th>Type</th><th>Description</th></tr>
 *   <tr><td>{@link #MSGT_OUT}</td><td>One line of the recognizer executable's standard output.</td></tr>
 *   <tr><td>{@link #MSGT_ERR}</td><td>One line of the recognizer executable's error output.</td></tr>
 *   <tr><td>{@link #MSGT_IN}</td><td>Echo of one line of the recognizer executable's standard input.</td></tr>
 *   <tr><td>{@link #MSGT_WRP}</td><td>A message from the wrapper instance.</td></tr>
 * </table>
 * <p>The following snippet shows how to process messages:</p>
 * 
 * <pre>
 * executable.addObserver(new Observer()
 * {
 *    public void update(Observable o, Object arg)
 *    {
 *      char   type = ((String)arg).charAt(0);
 *      String msg  = ((String)arg).substring(1);
 *        
 *      switch (type)
 *      {
 *      case Executable.MSGT_OUT:
 *        String stdOutMessage = msg;
 *        ...
 *        break;
 *      case Executable.MSGT_ERR:
 *        String stdErrMessage = msg;
 *        ...
 *        break;
 *     case Executable.MSGT_IN:
 *        String stdInMessage = msg;
 *        ...
 *        break;
 *      default:
 *        String wrapperMessage = msg;
 *        ...
 *        break;
 *      }
 *    }
 * });
 * </pre>
 * 
 * <p><b>Controlling the Executable</b></p>
 * <p>Most dLabPro executables and some UASR scripts can be controlled by 
 * inputting commands through the {@link #enterCommand(String)} method. See 
 * documentation of dLabPro or UASR for a list acceptable commands.</p>
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public abstract class Executable extends Observable implements Runnable
{
  // -- Constants --
  
  /**
   * Standard output message.
   */
  public static final char MSGT_OUT = '<'; 
  
  /**
   * Error output message.
   */
  public static final char MSGT_ERR = '!'; 
  
  /**
   * Standard input message (echo).
   */
  public static final char MSGT_IN = '>'; 
  
  /**
   * Wrapper message.
   */
  public static final char MSGT_WRP = ':'; 
  
  // -- Fields --
  
  /**
   * The executable process monitor thread.
   */
  protected Thread monitor;
  
  /**
   * The executable file.
   */
  protected File exeFile;
  
  /**
   * The executable process.
   */
  protected Process process;

  /**
   * Flag indicating that the executable process should be restarted when it
   * terminates unexpectedly.
   */
  protected boolean autoRestart;
  
  // -- Constructors --

  /**
   * Creates a new executable instance. Applications <em>must</em> call {@link #dispose()}
   * when the instance is not longer needed.
   * 
   * @param exeFile
   *          The executable file.
   * @throws FileNotFoundException
   *           If the executable was not found.
   * @throws IllegalArgumentException
   *           If the <code>exeFile</code> is <code>null</code>.
   */
  public Executable(File exeFile)
  throws FileNotFoundException, IllegalArgumentException
  {
    if (exeFile==null) throw new IllegalArgumentException();
    
    // Initialize
    this.exeFile = exeFile;
    
    // Start
    this.monitor = new Thread(this);
    this.monitor.setName(exeFile.getName()+".MonitorThread");
    this.monitor.setDaemon(true);
    this.monitor.start();
  }

  // -- Input and output --
  
  /**
   * Inputs a command into the executable.
   * 
   * @param command
   *          The command.
   */
  protected void enterCommand(String command)
  {
    if (process==null)
      throw new IllegalThreadStateException("Executable process does not exist.");
    
    // Input command
    dispatchMessage(MSGT_IN,command);
    command = command+"\n";
    try
    {
      process.getOutputStream().write(command.getBytes());
      process.getOutputStream().flush();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Notifies all observers by updating them with a message string.
   * 
   * @param type
   *          One of the <code>MSGT_XXX</code> constants.
   * @param msg
   *          The message.
   */
  protected void dispatchMessage(char type, String msg)
  {
    // Dispatch message to observers
    setChanged();
    notifyObservers(type+msg);
  }

  // -- Life cycle --
  
  /**
   * (Re-)launches the executable process.
   * 
   * @throws IllegalStateException
   *           If the executable is disposed.
   * @throws FileNotFoundException
   *           If the executable file could not be found.
   */
  protected void launch() throws FileNotFoundException
  {
    if (monitor==null) throw new IllegalStateException();
    
    ArrayList<String> cmdline = new ArrayList<String>();    
    cmdline.add(exeFile.getAbsolutePath());
    if (getArguments()!=null)
      cmdline.addAll(getArguments());
    dispatchMessage(MSGT_WRP,"Launching: "+cmdline.toString());
    try
    {
      ProcessBuilder builder = new ProcessBuilder(cmdline);
      process = builder.start();
      (new OutputPipeHandler(process,MSGT_OUT)).start();
      (new OutputPipeHandler(process,MSGT_ERR)).start();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Terminates the executable process. The method first tries to shutdown the executable gracefully
   * by inputting the exit command. If this fails, the process is forcibly terminated. The method
   * blocks until the executable has actually terminated either way. If the executable has already
   * terminated the method does nothing and returns 0. 
   * 
   * @return The processes exit value or {@link Integer#MIN_VALUE} if the process has been
   *         forcefully terminated.
   */
  protected int terminate()
  {
    if (process==null) return 0;
    
    int exitValue = Integer.MIN_VALUE; 

    synchronized (this)
    {
      if (getExitCommand()!=null)
      {
        // Try to terminate gracefully
        dispatchMessage(MSGT_WRP,"Terminating");
        enterCommand(getExitCommand());
        for (int i=0; i<1000; i+=100)
        {
          try { Thread.sleep(100); } catch (InterruptedException e) {}
          try
          {
            exitValue = process.exitValue();
            break;
          }
          catch (IllegalThreadStateException e)
          {
            // Process is still running...
          }
        }
      }
      
      // Use force
      if (exitValue==Integer.MIN_VALUE)
      {
        dispatchMessage(MSGT_WRP,"Forcibly terminating");    
        process.destroy();
      }
    }
    
    return exitValue;
  }
  
  public void run()
  {
    dispatchMessage(MSGT_WRP,"Monitor thread started");
    while (monitor!=null)
    {
      try { Thread.sleep(100); } catch (InterruptedException e) {};
      if (process==null)
      {
        autoRestart = false;
        try
        {
          launch();
        }
        catch (FileNotFoundException e)
        {
          // Fatal...
          return;
        }
      }
      synchronized (this)
      {
        try
        {
          int exitCode = process.exitValue();
          if (autoRestart)
          {
            dispatchMessage(MSGT_WRP,"Terminated unexpectedly");
            try
            {
              launch();
            }
            catch (FileNotFoundException e)
            {
              // Fatal...
              return;
            }
          }
          else
          {
            dispatchMessage(MSGT_WRP,"Executable failed to start (exit code: "
              + exitCode + ")");
            return;
          }
        }
        catch (IllegalThreadStateException e)
        {
          // Process is running -> nothing to be done.
        }
      }
    }
    terminate();
    dispatchMessage(MSGT_WRP,"Monitor thread ended");
  }

  /**
   * Disposes the executable and frees all system resources. The method blocks until disposing is
   * complete. If the executable is already disposed the method does nothing and returns
   * immediately.
   */
  public void dispose()
  {
    if (monitor==null) return;
    Thread ghost = monitor;
    monitor = null;
    try
    {
      ghost.join();
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }

  @Override
  protected void finalize() throws Throwable
  {
    dispose();
    super.finalize();
  }
  
  // -- Abstract API --
  
  public abstract ArrayList<String> getArguments();
  
  /**
   * Returns the exit command. If there is no exit command, the method should
   * return <code>null</code>.
   */
  public abstract String getExitCommand();
  
  // -- Auxiliary methods --

  /**
   * Finds an executable file.
   * 
   * @param name
   *          The file name of the executable without a path, e. g. "dlabpro".
   *          The Windows executable file suffix ".exe" is appended
   *          automatically in Windows and ignored automatically on other
   *          platforms.
   * @return The executable file.
   * @throws FileNotFoundException
   *           If the executable has not been found.
   */
  public static File findExecutable(String name) throws FileNotFoundException
  {
    String path = System.getenv("PATH");
    if (name.endsWith(".exe")) name=name.substring(0,name.length()-4);
    
    final String exeName = name;
    for (String dir : path.split(";"))
    {
      File f = new File(dir);
      String exes[] = f.list(new FilenameFilter()
      {
        public boolean accept(File dir, String name)
        {
          File f = new File(dir,name);
          if (!f.canExecute()) return false;
          if (exeName.equals(name)) return true;
          if ((exeName+".exe").equals(name)) return true;
          return false;
        }
      });
      if (exes!=null && exes.length>0)
        return (new File(f,exes[0]));
    }
    
    throw new FileNotFoundException(exeName+" not found in "+path);
  }
  
  // -- Nested classes --
  
  /**
   * Instances of this class read and dispatch a processes standard or error output.
   */
  protected class OutputPipeHandler extends Thread
  {
    protected Process     process;
    protected InputStream is;
    protected char        type;

    /**
     * Creates a new output pipe handler.
     * 
     * @param process
     *          The process whose output is to be handled.
     * @param type
     *          The pipe type, {@link Recognizer#MSGT_OUT MSGT_OUT} for the standard output,
     *          {@link Recognizer#MSGT_ERR MSGT_ERR} for the error output.
     */
    protected OutputPipeHandler(Process process, char type)
    {
      is = process.getInputStream();
      if (type==Executable.MSGT_ERR)
        is = process.getErrorStream();
      this.type = type;
      //setDaemon(true);
    }

    @Override
    public void run()
    {
      String prefix = type==MSGT_ERR?"Error":"Standard"; 
      dispatchMessage(MSGT_WRP,prefix+" output handler started");
      String line = "";
      while (true)
      {
        try
        {
          process.exitValue();
          break;
        }
        catch (Exception e)
        {
        }
        
        try
        {
          int in = is.read();
          if (in<0) break;
          char c = (char)in;
          if (c=='\n' || c=='\r')
          {
            if (line.length()>0)
              dispatchMessage(type,line);
            line = "";
          }
          else
            line+=c;
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
      }
      try { is.close(); } catch (IOException e) { e.printStackTrace(); }
      dispatchMessage(MSGT_WRP,prefix+" output handler ended");
    }
  }

}
