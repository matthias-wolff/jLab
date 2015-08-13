package de.tucottbus.kt.jlab.kernel;

import java.util.Observable;


public class JlDataStreamer extends Observable implements Runnable
{
  private Thread      runner;
  private boolean     bActive;
  private long        nInterval;   // Interval between data packages in nanoseconds
  private boolean     bPause;
  private JlFifoQueue target;
  private int         nBlockLength;
  private JlData      idSrc;
  private int         nComp;
  private int         nRec;
  
  /**
   * Initializes this data streamer. 
   * 
   * @param target
   *          The queue to stream the audio to
   * @param nBlockLength
   *          Number of records to stream at a time
   */
  public void init(JlFifoQueue target, int nBlockLength)
  {
    this.target       = target;
    this.nBlockLength = Math.max(nBlockLength,1);
  }

  // -- Getters and setters --
  
  /**
   * Returns <code>true</code> if this streamer is playing or paused.
   * 
   * @see #isPaused()
   */
  public boolean isActive()
  {
    if (runner==null) return false;
    return runner.isAlive();
  }
  
  /**
   * Returns <code>true</code> is this streamer is paused.
   * 
   * @see #isActive()
   */
  public boolean isPaused()
  {
    if (runner==null) return false;
    return bPause;
  }
  
  // -- Operations --

  /**
   * Streams a {@link JlData} instance.
   * 
   * @param idSrc
   *          The data to be streamed
   * @param nComp
   *          The zero-based index in <code>idData</code> if the component to
   *          be streamed 
   * @throws IllegalThreadStateException
   *          if streaming is already in progress
   * @throws IllegalArgumentException
   *          if <code>nComp</code> is not a valid component index 
   */
  public synchronized void stream(JlData idSrc, int nComp)
  throws IllegalThreadStateException, IllegalArgumentException
  {
    JlObject.log("\n   JlDataStreamer.stream(idSrc,"+nComp+");");
    JlObject.log("\n   - Block length      : "+this.nBlockLength+" records");
    
    if (idSrc==null) return;
    if (nComp<0 || nComp>=idSrc.getDimension())
      throw new IllegalArgumentException("Invalid component index "+nComp);
    if (runner!=null)
      throw new IllegalThreadStateException("Already streaming");
    
    this.bPause = false;
    this.idSrc  = idSrc;
    this.nComp  = nComp;
    this.nRec   = 0;
    
    // Compute interval
    if ("s".equals(new String(idSrc.runit)))
      nInterval = (long)(this.nBlockLength*idSrc.rinc*1000000000);
    else if ("ms".equals(new String(idSrc.runit)))
      nInterval = (long)(this.nBlockLength*idSrc.rinc*1000000);
    else
    {
      JlObject.WARNING("Cannot stream in real time (unknown runit \"" + 
        (new String(idSrc.runit)) + "\")");
      nInterval = 1;
    }
    if (nInterval<=0)
    {
      JlObject.WARNING("Cannot stream in real time. Increase buffer length!");
      nInterval = 1;
    }
    JlObject.log("\n   - Timer interval    : "+nInterval+" ns");
    
    // Start streamer thread
    runner = new Thread(this);
    runner.setDaemon(true);
    runner.setPriority(Thread.MAX_PRIORITY);
    runner.start();
    setChanged();
    notifyObservers();
  }

  /**
   * Stops streaming.
   */
  public synchronized void stop()
  {
    stop(true);
  }
  
  /**
   * Stops streaming.
   */
  public synchronized void stop(boolean joinRunner)
  {
    JlObject.log("\n\n   JlDataStreamer.stop("+joinRunner+")");
    if (runner==null) return;
    bActive = false;
    if (joinRunner)
    {
      try { runner.join(100); } catch (InterruptedException e) {}
      if (runner.isAlive())
        JlObject.WARNING("Data streamer did not stop.");
    }
    if (target!=null) target.put(null);
    runner = null;
    idSrc = null;
    nComp = -1;
    nRec = -1;
    bPause = false;
    setChanged();
    notifyObservers();
  }

  /**
   * Pauses this streamer. The method does nothing if the streamer
   * <ul>
   *   <li>is not active (i.e. if {@link #isActive()} returns <code>false</code>) or</li>
   *   <li>is already paused (i.e. if {@link #isPaused()} returns <code>true</code>).</li>
   * </ul>
   */
  public void pause()
  {
    if (!isActive()) return;
    if ( isPaused()) return;
    this.bPause = true;
    setChanged();
    notifyObservers();
  }

  /**
   * Resumes this streamer. The method does nothing if the streamer
   * <ul>
   *   <li>is not active (i.e. if {@link #isActive()} returns <code>false</code>) or</li>
   *   <li>is not paused (i.e. if {@link #isPaused()} returns <code>false</code>).</li>
   * </ul>
   */
  public void resume()
  {
    if (!isActive()) return;
    if (!isPaused()) return;
    this.bPause = false;
    setChanged();
    notifyObservers();
  }
  
  // -- Implementation of the Runnable interface --
  
  @Override
  public void run()
  {
    bActive         = true;
    long nStartTime = System.nanoTime();
    long nIte       = 0;
    
    while (bActive && idSrc!=null)
    {
      if (!bPause)
        synchronized (this)
        {
          // Stream data
          int nCount = Math.min(nBlockLength,idSrc.getLength()-nRec);
          if (target!=null)
            target.put(idSrc.selectRecs(nRec,nCount).getComp(nComp));
          nRec += nBlockLength;

          // At the end of data
          if (nRec>=idSrc.getLength()) { stop(false); break; }
        }
      
      // Compute precise time-out until next data package
      nIte++;
      long nActualInterval = nStartTime+nIte*nInterval-System.nanoTime();
      if (nActualInterval<0)
        JlObject.WARNING("Data buffer dispatched too late");
      nActualInterval =  Math.max(nActualInterval-500000,0); // Wake up 0.5 ms before time-out
      int nIntervalMs = (int)(nActualInterval/1000000);
      int nIntervalNs = (int)(nActualInterval%1000000);
      try { Thread.sleep(nIntervalMs,nIntervalNs); }
      catch (InterruptedException e) {}
    }
  }

}
