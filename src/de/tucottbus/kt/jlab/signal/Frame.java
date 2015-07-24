// jLab

package de.tucottbus.kt.jlab.signal;

import de.tucottbus.kt.jlab.kernel.JlFifoQueue;

/**
 * Signal frame grabber queue.
 * 
 * @author Matthias Eichner, TU Dresden
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class Frame extends JlFifoQueue
{
  private Windowing         window;
  private int               crate            = 160;
  private short[]           aLeftover        = null;
  private float             preemCoeff       = -1.0f;                // Preemphasis coefficient
  private boolean           bShortOutput     = false;

  public boolean isNormalize()
  {
    return window.bNormalize;
  }

  public void setNormalize(boolean normalize)
  {
    window.bNormalize = normalize;
    window.genWindow();
  }
  
  /**
   * 
   */
  public Frame(int nCapacity, JlFifoQueue iOutputQueue)
  {
    super(nCapacity, iOutputQueue);
    window = new Windowing();
  }

  /**
   * @deprecated Use class {@link Window} instead!
   */
  class Windowing
  {
    private int     windowLen  = 400;
    private String  windowType = "Hamming";
    private boolean bRect      = false;
    private boolean bNormalize = false;    // Normaize energy of window
    private float[] window;

    public Windowing()
    {
      genWindow();
    }

    protected void apply(short[] values)
    {
      // if (values == null) return;
      if (bRect) return;
      if (values.length != windowLen) throw new IllegalArgumentException(
          "Frame length doesn't match window length!");

      for (int i = 0; i < values.length; i++)
      {
        values[i] = (short)((float)values[i] * window[i]);
      }

      return;
    }

    protected void genWindow()
    {
      window = new float[windowLen];
      bRect = false;

      if (windowType.equals("Rectangular"))
      {
        // Nothing to be done for rectangular window
        bRect = true;
      }
      else if (windowType.equals("Welch"))
      {
        for (int i = 0; i < windowLen; i++)
        {
          float f;
          f = (i - windowLen / 2.0f) / (windowLen / 2.0f);
          window[i] = (1.0f - f * f);
        }
      }
      else if (windowType.equals("Bartlett"))
      {
        int i, j;
        for (j = 0 - windowLen / 2; j < windowLen / 2; j++)
        {
          i = j + windowLen / 2;
          if (j >= 0 && j <= windowLen / 2)
          {
            window[i] = (1.0f - 2.0f * j / windowLen);
          }
          else if (j >= 0 - windowLen / 2 && j < 0)
          {
            window[i] = (1.0f + 2.0f * j / windowLen);
          }
          else window[i] = 0f;
        }
      }
      else if (windowType.equals("Hanning"))
      {
        int i, j;
        for (j = 0 - windowLen / 2; j < windowLen / 2; j++)
        {
          i = j + windowLen / 2;
          window[i] = (0.5f + 0.5f * (float)Math.cos(2.0f * (float)Math.PI * j / windowLen));
        }
      }
      else if (windowType.equals("Hamming"))
      {
        float c1 = 0.54f;
        float c2 = 0.46f;
        double c = (2 * Math.PI) / (double)(windowLen - 1);

        for (int i = 0; i < (windowLen + 1) / 2; i++)
        {
          window[i] = c1 - c2 * (float)Math.cos((double)i * c);
          window[windowLen - 1 - i] = window[i];
        }
      }
      else if (windowType.equals("Blackman"))
      {
        float c1 = 0.42f;
        float c2 = 0.5f;
        float c3 = 0.08f;
        double c = (2 * Math.PI / (double)(windowLen - 1));

        for (int i = 0; i < windowLen; i++)
        {
          window[i] = c1 - c2 * (float)Math.cos((float)i * c) + c3
              * (float)Math.cos(2 * (float)i * c);
        }
      }

      // Normalize to energy
      if (bNormalize)
      {
        float norm = 0.0f;

        for (int i = 0; i < windowLen; i++)
          norm = norm + window[i] * window[i];

        for (int i = 0; i < windowLen; i++)
          window[i] = window[i] / (float)Math.sqrt(norm / windowLen);
      }
    }
  }

  /**
   * @return Returns the window type.
   * @see setWindowType
   */
  public String getWindowType()
  {
    return window.windowType;
  }

  /**
   * @param s
   *          Window type. Must be one of the following strings: Rectangular, Welch, Bartlett,
   *          Hanning or Hamming. No exception is thrown, if s is'nt a valid window type and no
   *          action is performed. Default window type is Hanning.
   */
  public void setWindowType(String s)
  {
    window.windowType = s;
    window.genWindow();
  }

  /**
   * @return Returns the crate.
   */
  public int getCrate()
  {
    return crate;
  }

  /**
   * @param crate
   *          The crate to set.
   */
  public void setCrate(int crate)
  {
    this.crate = crate;
  }

  /**
   * @return Returns the windowLen.
   */
  public int getWindowLen()
  {
    return window.windowLen;
  }

  /**
   * @param windowLen
   *          The windowLen to set.
   */
  public void setWindowLen(int wlen)
  {
    window.windowLen = wlen;
    window.genWindow();
  }

  /**
   * @return Returns the preemphasis coefficient.
   */
  public float getPreemCoeff()
  {
    return preemCoeff;
  }

  /**
   * @param preemCoeff
   *          The preemphasis coefficient to set.
   */
  public void setPreemCoeff(float preemCoeff)
  {
    this.preemCoeff = preemCoeff;
  }

  protected void flushLeftover(short[] aCurrentBuffer, int len)
  {
    short[] aCurrentFrame = new short[window.windowLen]; // new frame

    // if exist copy leftover from current buffer
    if (aCurrentBuffer != null)
    {
      for (int i = 0, j = aCurrentBuffer.length - len; i < len; i++, j++)
        aCurrentFrame[i] = aCurrentBuffer[j];
    }
    // else get leftover from previous buffer
    else if (aLeftover != null && aLeftover.length > 0)
    {
      System.arraycopy(aLeftover, 0, aCurrentFrame, 0, aLeftover.length);
      aLeftover = null;
    }
    // else there is no leftover
    else aCurrentFrame = null;

    if (aCurrentFrame != null)
    {
      if (preemCoeff > 0.0f) doPreemphasis(aCurrentFrame);
      window.apply(aCurrentFrame);
      if (iOutputQueue!=null) iOutputQueue.put(aCurrentFrame);
    }
    if (iOutputQueue!=null) iOutputQueue.put(null);
    // JlInstance.log("*** JlFrame: Send end of utterance ***");
  }

  /**
   * Implementation of <code>JlFifoQueue.process</code>.
   */
  protected void process(boolean bFlush)
  {
    /* use local variable for window length */
    int     windowLen      = window.windowLen;
    short[] aCurrentFrame  = new short[windowLen];
    short[] aCurrentBuffer;
    int     len             = 0;
    int     nTempLength     = 0;                                               /* temporary value to flush the left over       */
    int     nPrepended      = 0;

    // Process all buffers
    while (length() > 0)
    {
      // get current buffer
      aCurrentBuffer = (short[])get(HEAD);

      if (aCurrentBuffer != null)
      {
        // Something left over from previous buffer?
        if (aLeftover != null && aLeftover.length > 0)
        {
          nTempLength = aLeftover.length;
          while(nTempLength>=0)
          {
            // Prepend leftover to first frame of current buffer
            System.arraycopy(aLeftover, aLeftover.length-nTempLength, aCurrentFrame, 0, nTempLength);
            try
            {
              for (int i = nTempLength, j = 0;; i++, j++)
                aCurrentFrame[i] = aCurrentBuffer[j];
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
            }
            if (preemCoeff > 0.0f) doPreemphasis(aCurrentFrame);
            window.apply(aCurrentFrame);
            if (iOutputQueue != null) iOutputQueue.put(aCurrentFrame);
            nTempLength -= crate;
            aCurrentFrame = new short[windowLen];
          }
          nPrepended = -nTempLength;
          aLeftover = null;
        }

        for (len = aCurrentBuffer.length - nPrepended; len > windowLen; len -= crate)
        {
          if (nPrepended > 0) nPrepended = 0;
         // else
         // {
            // Copy frame from buffer and convert from short to float
            try
            {
              for (int i = 0, j = aCurrentBuffer.length - len;; i++, j++)
                aCurrentFrame[i] = aCurrentBuffer[j];
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
            }
          //}

          if (preemCoeff > 0.0f) doPreemphasis(aCurrentFrame);
          window.apply(aCurrentFrame);
          if (iOutputQueue != null) iOutputQueue.put(aCurrentFrame);

          // reset aCurrentFrame
          aCurrentFrame = new short[windowLen];

          // Calculate queue latency (in frames)
          // nLatency = (float)len / (float)aCurrentBuffer.length + length();
        }

        // Save leftover of current buffer
        // aLeftover = new float[len];
        aLeftover = new short[len];
        try
        {
          for (int i = 0, j = aCurrentBuffer.length - len;; i++, j++)
            aLeftover[i] = aCurrentBuffer[j];
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
        }
      }
      else flushLeftover(aCurrentBuffer, len);

      // Remove current buffer from queue
      remove(HEAD);
    }

    // Flush
    if (bFlush) flushLeftover(null, 0);
    return;
  }

  /**
   * @param currentFrame
   *          The frame to process
   * 
   */
  protected void doPreemphasis(short[] currentFrame)
  {
    float p = 0.0f;

    // if (preemCoeff <= 0.0f) return;
    if (currentFrame == null) return;

    for (int i = 0; i < currentFrame.length; i++)
    {
      p = currentFrame[i] = (short)((float)currentFrame[i] - p * preemCoeff);
    }
  }

  /**
   * @return Returns the bShortOutput.
   */
  public boolean isBShortOutput()
  {
    return bShortOutput;
  }

  /**
   * @param shortOutput
   *          The bShortOutput to set.
   */
  public void setShortOutput(boolean shortOutput)
  {
    bShortOutput = shortOutput;
  }
}