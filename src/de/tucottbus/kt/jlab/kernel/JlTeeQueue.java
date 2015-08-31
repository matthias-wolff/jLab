package de.tucottbus.kt.jlab.kernel;

/**
 * This queue writes all data immediately into the two output queues.
 * 
 * @author Matthias Eichner, TU Dresden
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class JlTeeQueue extends JlFifoQueue
{
  protected JlFifoQueue iOutputQueue2 = null;
  protected boolean     bOutput1      = true;
  protected boolean     bOutput2      = true;

  /**
   * Creates a new JlTeeQueue. The queue has no limit in size.
   * 
   * @param iOutputQueue1
   *          Output queue 1
   * @param iOutputQueue2
   *          Output queue 2
   */
  public JlTeeQueue(JlFifoQueue iOutputQueue1, JlFifoQueue iOutputQueue2)
  {
    super(0);
    if (bOutput1) this.iOutputQueue = iOutputQueue1;
    if (bOutput2) this.iOutputQueue2 = iOutputQueue2;
  }

  // -- Queue implementation --
  
  /**
   * Implementation of <code>JlFifoQueue.process</code>.
   */
  protected void process(boolean bFlush)
  {
    while (length() > 0)
    {
      if (bOutput1 && iOutputQueue != null) iOutputQueue.put(get(HEAD));
      if (bOutput2 && iOutputQueue2 != null) iOutputQueue2.put(get(HEAD));
      remove(HEAD);
      if (!bFlush) break;
    }
  }

  // -- Getters and setters --

  /**
   * Sets a new output queue 1.
   * 
   * @param outputQueue1
   *          The new output queue.
   */
  public void setOutputQueue1(JlFifoQueue outputQueue1)
  {
    setOutputQueue(outputQueue1);
  }
  
  /**
   * Sets a new output queue 2.
   * 
   * @param output1
   *          The new output queue.
   */
  public void setOutputQueue2(JlFifoQueue outputQueue2)
  {
    this.iOutputQueue2 = outputQueue2;
  }
  
  /**
   * Toggles state of output queues. If queue 1 is enabled and queue 2 is
   * disabled using <code>setOutput2(false)</code> a call of
   * {@code toggleOutput()} will result in a disables queue 1 and an enabled
   * queue 2. This method is good for alternating between to processing paths.
   */
  public void toggleOutput()
  {
    bOutput1 = !bOutput1;
    bOutput2 = !bOutput2;
  }
  
  /**
   * Test state of output queue 1.
   * 
   * @return true if output 1 is enabled, false otherwise.
   */
  public boolean isOutput1Enabled()
  {
    return bOutput1;
  }

  /**
   * Enable/disable output queue 1.
   * 
   * @param output1
   *          true to enable, false to diable queue.
   */
  public void enableOutput1(boolean output1)
  {
    bOutput1 = output1;
  }

  /**
   * Test state of output queue 2.
   * 
   * @return true if output 2 is enabled, false otherwise.
   */
  public boolean isOutput2Enabled()
  {
    return bOutput2;
  }

  /**
   * Enable/disable output queue 1.
   * 
   * @param output1
   *          true to enable, false to diable queue.
   */
  public void enableOutput2(boolean output2)
  {
    bOutput2 = output2;
  }
}