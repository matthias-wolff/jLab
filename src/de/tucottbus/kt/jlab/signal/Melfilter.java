/* jLab type JlMelfilter
 * - Mel feature extraction 
 *
 * AUTHOR  : Matthias Eichner
 * UPDATE  : $Date: 2008-08-13 13:27:54 +0200 (Mi, 13 Aug 2008) $
 *           $Revision: 136 $
 * PACKAGE : de.tudresden.ias.jlab.speech
 * RCS-ID  : $Id: JlMelfilter.java 136 2008-08-13 11:27:54Z wittenb $
 */

package de.tucottbus.kt.jlab.signal;

import java.text.DecimalFormat;
import java.util.Random;

import de.tucottbus.kt.jlab.kernel.JlFifoQueue;
import de.tucottbus.kt.jlab.kernel.JlObject;

/**
 * Class is equivalent to the dLabPro class "melproc". It provides the same 
 * functionality but the inner structure is more methode oriented. This is 
 * meaningful for derived classes.
 * 
 * @author Matthias Eichner, TU Dresden
 */
public class Melfilter extends JlFifoQueue
{
  private   final static long     serialVersionUID = -2013901112898914451L;

  // constants
  protected final static short    WTYPE_MEL        = 1;                         // Center frequencies according to the mel scale
  protected final static short    WTYPE_BILINEAR   = 2;                         // Center frequencies accroding to Bilinear warping scale
  protected final static short    FTYPE_SINC       = 1;                         // Sinc function as transfer function of the filters (cepstral smoothing)
  protected final static short    FTYPE_TRIANGULAR = 2;                         // Triangular function as transfer function of ther filters
  protected final static short    ERROR_WTYPE      = -1;                        // Generation of convolution core failed because of unknown scale type  
  protected final static short    ERROR_FTYPE      = -2;                        // Generation of convolution core failed because of unknown transfer function type
  protected final static short    O_K              = 1;
  protected final static int      REAL             = 0;
  protected final static int      IMAG             = 1;

  
  // Settings 
  private   final        boolean  bCHECK           = false;                     // for protocol messages
  protected final        int      samplingFrequ    = 16000;                     // sampling frequency
  protected final        short    dimension        = 30;                        // dimension of the feature vector
  protected final        int      fftLen           = 512;                       // fft lenght
  protected final        int      nQuantization    = 16;
  protected final        short    wtype            = WTYPE_MEL;
  protected final        short    ftype            = FTYPE_SINC;
  protected final        float    alpha            = 0.0f;
  protected final        float    fftError         = 16.f;
  
  // dependent constants
  protected final        int      nHalfFftLen      = fftLen >> 1; 
  // Math.log => log to basis e == ln  and lg(x)=ln(x)/ln(10)
  protected final        double   log10            = Math.log(10.0f);
  protected final        int      logFftLenDivLog2 = (int)(Math.log(fftLen) / Math.log(2.0f));
  protected final        float    oneDivByFftLen   = 1.0f / fftLen;  
  protected final        float    nMinLog          = (float)Math.log(Math.pow(2,-nQuantization+1));

  // necessary global objects
  private                float[]  cosa;
  private                float[]  sina;
  protected              convcore c;
  private   final        int      nWhatToDo; 
  protected final        Random   rand             = new Random(0);

  /**
   * Creates a new <code>JlMelfilter</code> object with the given capacity and 
   * output queue
   *  
   * @param nCapacity
   *          queue capacity
   * @param iOutputQueue
   *          output queue
   */
  public Melfilter(int nCapacity, JlFifoQueue iOutputQueue)
  {
    super(nCapacity);
    this.iOutputQueue = iOutputQueue;
    c = new convcore();
    this.init();
    this.nWhatToDo = 0;
  }
  
  /**
   * Creates a new <code>JlMelfilter</code> object with the given capacity, 
   * output queue and the processing to be undertaken. 
   * 
   * @param nCapacity
   *          queue capacity
   * @param iOutputQueue
   *          output queue
   * @param nWhatToDo
   * </br>    0 - default: logarithmic short time power spectrum and convolve (Mel-filtering)
   * </br>    1 - short time spectrum (FFT)
   * </br>    2 - short time power spectrum 
   * </br>    3 - logarithmic short time power spectrum
   * </br>    4 - convolve (Mel-filtering)
   * </br>    5 - inverse FFT
   * </br>    6 - magnitude
   * </br>    7 - logarithmic magnitude and convolve (Mel-filtering)
   * </br>    8 - logarithm and convolve (Mel-filtering)
   * </br>    9 - logarithmic magnitude
   */
  public Melfilter(int nCapacity, JlFifoQueue iOutputQueue, int nWhatToDo)
  {
    super(nCapacity);
    this.iOutputQueue = iOutputQueue;
    c = new convcore();
    this.init();
    this.nWhatToDo = nWhatToDo;

  }
  
  private final void init()
  {
    // Init sine and cosine tables
    double da = 2 * Math.PI / fftLen;
    double a;
    int i;
    cosa = new float[fftLen];
    sina = new float[fftLen];
    for (a = 0, i = 0; i < fftLen; i++, a += da)
    {
      cosa[i] = (float)Math.cos(a);
      sina[i] = (float)Math.sin(a);
    }
  }

  /**
   * Fast Fourier Transformation
   * 
   * @param in
   *         short values of a real signal as an 1-dim. array 
   * 
   * @return
   *         short time spectrum as an 2-dim. array with:
   * </br>    one array for the real values (in[REAL][])
   * </br>    one array for the imaginary values (in[IMAG][])
   * @deprecated Use class {@link FFT}!
   */
  private final float[][] fft(short[] in)
  {
    float[][] out = new float[2][fftLen];
    int i=0;
    try 
    { 
      for (; ; i++)
      {
        out[REAL][i] = (float)in[i];
        out[IMAG][i] = 0.0f;
      }
    }
    catch (ArrayIndexOutOfBoundsException e){}
    if(in.length < fftLen)                                                      /* maybe zero padding */
    {
      for(;i<fftLen; i++)                                                       /* add values less then quant. noise instead of zeros */
      {
        out[REAL][i] = (float)((rand.nextDouble() - 0.5) * Math.exp(nMinLog));
        out[IMAG][i] = 0.0f;
      }
    }
    return fft(out);
  }
  
 /**
  * Fast Fourier Transformation
  * 
  * @param in
  *         float values of a real signal as an 1-dim. array 
  * 
  * @return
  *         short time spectrum as an 2-dim. array with:
  * </br>    one array for the real values (in[REAL][])
  * </br>    one array for the imaginary values (in[IMAG][])
  * @deprecated Use class {@link FFT}!
  */
  protected final float[][] fft(float[] in)
  {
    float[][] out = new float[2][in.length];
    int i=0;
    try
    { 
      for (; ; i++)
      {
        out[REAL][i] = in[i];
        out[IMAG][i] = 0.0f;
      }
    }
    catch (ArrayIndexOutOfBoundsException e){}
    if(in.length < fftLen)                                                      /* maybe zero padding */
    {
      for(;i<fftLen; i++)                                                       /* add values less then quant. noise instead of zeros */
      {
        out[REAL][i] = (float)((rand.nextDouble() - 0.5) * Math.exp(nMinLog));
        out[IMAG][i] = 0.0f;
      }
    }
    return fft(out);
  }
  
  /**
   * Fast Fourier Transformation
   * 
   * @param in
             values of an complex signal as an 2-dim. array with:
   * </br>    one array for the real values (in[REAL][])
   * </br>    one array for the imaginary values (in[IMAG][])
   * 
   * @return
   *         short time spectrum as an 2-dim. array with:
   * </br>    one array for the real values (in[REAL][])
   * </br>    one array for the imaginary values (in[IMAG][])
  * @deprecated Use class {@link FFT}!
   */
  protected final float[][] fft(float[][] in)
  {
    int li, l, nv2, nm1, lix, lmx, lm, step;
    int csIdx = 0, snIdx = 0, ra1Idx = 0, ra2Idx = 0;
    int xj1Idx = 0, xj2Idx = 0;
    float[][] out = new float[2][fftLen];
    float hr, hi, sine, cosine;
    
    // use local variable
    float real[] = new float[fftLen];
    float imag[] = new float[fftLen];

    // type cast form short to float
    try
    {
      for (int i = 0;; i++)
      {
        real[i] = (float)in[REAL][i];
        imag[i] = (float)in[IMAG][i];
      }
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
    }
    int order = logFftLenDivLog2;
    lmx = fftLen;
    step = 1;

    for (l = 1; l <= order; l++)
    {
      lix = lmx;
      lmx >>= 1;
      snIdx = 0;
      csIdx = 0;
      ra1Idx = 0;
      ra2Idx = lmx;
      for (lm = 0; lm < lmx; lm++)
      {
        sine = sina[snIdx];
        cosine = cosa[csIdx];
        csIdx += step;
        snIdx += step;
        xj1Idx = ra1Idx++;
        xj2Idx = ra2Idx++;
        for (li = lix; li <= fftLen; li += lix)
        {
          hr = real[xj1Idx] - real[xj2Idx];
          hi = imag[xj1Idx] - imag[xj2Idx];
          real[xj1Idx] += real[xj2Idx];
          imag[xj1Idx] += imag[xj2Idx];
          real[xj2Idx] = cosine * hr + sine * hi;
          imag[xj2Idx] = cosine * hi - sine * hr;
          xj1Idx += lix;
          xj2Idx += lix;
        }
      }
      step *= 2;
    }
    /* Bitreversal */
    int j, k;
    nm1 = fftLen - 1;
    nv2 = fftLen / 2;
    j = 1;
    ra1Idx = 0;
    for (l = 0; l < nm1; l++, ra1Idx++)
    {
      ra2Idx = j - 1;
      if (l + 1 < j)
      {
        hr = real[ra2Idx];
        hi = imag[ra2Idx];
        real[ra2Idx] = real[ra1Idx];
        imag[ra2Idx] = imag[ra1Idx];
        real[ra1Idx] = hr;
        imag[ra1Idx] = hi;
      }
      k = nv2;
      while (k < j)
      {
        j -= k;
        k /= 2;
      }
      j += k;
    }
    out[REAL] = real;
    out[IMAG] = imag;

    return out;
  }

  /**
   * Returns the magnitudes of an array of complex values
   * 
   * @param  in
   *          2-dim. array with:
   * </br>           one array for the real values (in[REAL][])
   * </br>           one array for the imaginary values (in[IMAG][])
   * @return out
   *          an array of magnitudes 
   * @deprecated Use class {@link FFT}!
   */
  protected float[] magnitude(float[][] in)
  {
    float[] out = new float[fftLen];
    for (int i = 0; i < nHalfFftLen ; i++) 
    {
      out[i] = (float)Math.sqrt((in[REAL][i] * in[REAL][i]) + (in[IMAG][i] * in[IMAG][i]));
      // mirror the spectrum
      out[fftLen - 1 - i] = out[i];
    }
    return out;
  }

  /**
   * This methode calculates the level (10*lg(in[i])) after margins the indiviual 
   * bins of a short time power spectrum:
   * </br> fftError is the lower boundary 
   * </br> Float.MAX_Value is the upper boundary 
   * 
   * @param   in
   *           short time power spectrum 
   * @return  out
   *           logarithmic short time power spectrum in dB
   */
  protected float[] logarithmise(float[] in)
  {
    float[] out = new float[fftLen];   
    for (int i = 0; i < nHalfFftLen ; i++) 
    {
      if (in[i] == Float.POSITIVE_INFINITY)in[i] = Float.MAX_VALUE;
      //out[i] = (float)(0.5 * (Math.log(in[i]) / log10));
      out[i] = (float)Math.max(nMinLog,Math.log(in[i])); // 0.5 kommt magnitude wo die Wurzelbildung gespart werden kann
      out[fftLen - 1 - i] = out[i];
    }     
    return out;

  }
  
  /**
   * Inverse Fast Fourier Transformation
   * 
   * @param in
   *         
   * @return 
   * @deprecated Use class {@link FFT}!
   */
  protected  float[][] inverseFFT(float[][] in)
  {
    float[][] out;
    for(int i=0; i<fftLen; i++) in[IMAG][i] = -in[IMAG][i];

    out = fft(in);
  
    for(int i=0; i<fftLen; i++) out[IMAG][i] = -out[IMAG][i];
    for( int i = 0; i<fftLen; i++ )
    {
      out[REAL][i] = out[REAL][i] * oneDivByFftLen; // >> logFftLenDivLog2;
      out[IMAG][i] = out[IMAG][i] * oneDivByFftLen; // >> logFftLenDivLog2;
    }
    return out;
  }

  protected void process(boolean bFlush)
  { 
    short[]   aInput      = new short[fftLen];
    float[]   aOutputConv = new float[dimension];
    float[]   aOutputMag  = new float[fftLen];
    float[]   aOutputLog  = new float[fftLen];
    float[][] aOutputIFFT = new float[2][fftLen];
    float[][] aOutputFFT  = new float[2][fftLen];
   
    while (length() > 0)
    {
      if (iOutputQueue == null)
      {
        if(bCHECK) JlObject.log("JLMelfilter: no  output queue available");
        remove(HEAD);
        return;
      }
      if (get(HEAD) != null)
      {
        switch (nWhatToDo)
        {
          case 9:
              aOutputLog = logarithmise(magnitude((float[][])get(HEAD)));
              iOutputQueue.put(aOutputLog);
            break;
        
          case 8:
              aOutputLog = logarithmise((float[])get(HEAD));
              aOutputConv = c.convolve(aOutputLog);
              iOutputQueue.put(aOutputConv);
             break;
   
           case 7:
               aOutputLog = logarithmise(magnitude((float[][])get(HEAD)));
               aOutputConv = c.convolve(aOutputLog);
               iOutputQueue.put(aOutputConv);
             break;
        
           case 6:
               aOutputMag = magnitude((float[][])get(HEAD));
               iOutputQueue.put(aOutputMag);
             break;
        
           case 5:
               aOutputIFFT = inverseFFT((float[][])get(HEAD));
               iOutputQueue.put(aOutputIFFT);
             break;
             
           case 4:
               aOutputConv = c.convolve((float[])get(HEAD));
               iOutputQueue.put(aOutputConv);
             break;
             
           case 3:
               System.arraycopy((short[])get(HEAD), 0, aInput, 0, ((short[])get(HEAD)).length);
               aOutputFFT = fft(aInput);
               aOutputMag = magnitude(aOutputFFT);
               aOutputLog = logarithmise(aOutputMag);
               iOutputQueue.put(aOutputLog);
             break;

           case 2:
               aOutputMag = magnitude(fft((short[])get(HEAD)));
               iOutputQueue.put(aOutputMag);
               break;
             
           case 1:
               aOutputFFT = fft((short[])get(HEAD));
               iOutputQueue.put(aOutputFFT);
             break;
             
           case 0: // data in queue are time signals  
               aOutputMag = magnitude(fft((short[])get(HEAD)));
               if(ftype == Melfilter.FTYPE_TRIANGULAR)
               {
                 aOutputConv = c.convolve(aOutputMag);
                 for(int i = 0; i < dimension; i++)
                 {
                   aOutputLog[i] = (float)Math.log(aOutputConv[i]) - nMinLog;
                 }
               }
               else
               {
                 aOutputConv = c.convolve(logarithmise(aOutputMag));
                 for(int i = 0; i < dimension; i++)
                 {
                   aOutputLog[i] = aOutputConv[i] - nMinLog;
                 }
               }
               iOutputQueue.put(aOutputLog);
             break;

           default:
             break;
        }
      }
      else
      {
        iOutputQueue.put(null);
      }
      remove(HEAD);
      if (!bFlush) break;
    }
    return;
  }

  /**
   * @return Returns the dimension.
   */
  public short getDimension()
  {
    return dimension;
  }

  protected class convcore
  {
    short     n_in         = nHalfFftLen; /* input channels */
    short     n_out        = dimension;  /* output channels */
    float[]   mid;
    float[][] width;
    float[]   norm;       /* array of normal. coeff. */
    float[]   z;
    float[][] a;          /* matrix of core coeff. */

    public convcore() 
    {
      init();
    }
    
    /**
     * Initialize convolution core data struct.
     * 
     * @param nIn
     *          Number of input channels                        
     * @param nOut
     *          Number of output channels 
     * @return <code>O_K</code> if successfull, a (negative) error code otherwise
     */
    public convcore(short nIn, short nOut) 
    {
      /* Basic initialization */
      if(nIn>0)
        this.n_in      = nIn;
      if(nOut>0)
        this.n_out     = nOut;
      init(); 
    }
   

    private short init() 
    {
      short j;
      short k;
      short l;

      this.mid         = new float[this.n_out];
      this.width       = new float[2][this.n_out];
      this.norm        = new float[this.n_out];
      this.z           = new float[this.n_out];
      this.a           = new float[this.n_out][2*n_in];
      
      /* Generate convolution core */
      switch(wtype)
      {
        case WTYPE_MEL:
          mid[0] = 6;
          for (k=1; k<n_out; k++)
          {
            if (k<=9)
              mid[k] = mid[k-1] + 3;
            if ((k>9) && (k<=20))
              mid[k] = mid[k-1] + 4;
            if (k>20)
              mid[k] = mid[k-1] + (mid[k-1] - mid[k-2] + 2);
          }
          break;
      
        case WTYPE_BILINEAR:
          for (k = 0; k < n_out; k++)
          {
            float tmp = (float)((k+1)*n_in)/(float)(n_out+1);
            mid[k] = tmp + (float)((2*n_in)/Math.PI*Math.atan2(-alpha*Math.sin(Math.PI*tmp/(float)n_in), 1+alpha*Math.cos(Math.PI*tmp/(float)n_in)));
          }
          break;

        default:
          return ERROR_WTYPE;
      }

      width[0][0] = mid[1] - mid[0];
      width[1][0] = mid[1] - mid[0];
      for (k=1; k<n_out-1; k++)
      {
        width[0][k] = mid[k]-mid[k-1];
        width[1][k] = mid[k+1]-mid[k];
      }
      width[0][k] = mid[k]-mid[k-1];
      width[1][k] = mid[k]-mid[k-1];
      
      switch(ftype)
      {
        case FTYPE_SINC:
          for (k=0; k<n_out; k++)
          {
            float widthTmp = width[0][k] + width[1][k];
            norm[k] = 0.f;
            for (j=(short)(mid[k]-n_in); j < (short)(mid[k]+n_in); j++)
            {
              l = (short)((j+2*n_in) % (2*n_in));
              if((float)Math.abs(j-mid[k]) < 1.0e-10)
                a[k][l] = 1.f;
              else if(j < mid[k])
                a[k][l] = widthTmp * (float)(Math.sin((float)(((double)j-mid[k])*Math.PI)/widthTmp) / (((double)j-mid[k])*Math.PI));
              else
                a[k][l] = widthTmp * (float)(Math.sin((float)(((double)j-mid[k])*Math.PI)/widthTmp) / (((double)j-mid[k])*Math.PI));
              norm[k] += a[k][l];
            }
          }
          break;
        
        case FTYPE_TRIANGULAR:
          for (k = 0; k < n_out; k++)
          {
            float width_l = 2*width[0][k];
            float width_r = 2*width[1][k];
            norm[k] = 0.f;
            for (j = (short)(mid[k]-width_l+1); j <= (short)(mid[k]+width_r); j++) 
            {
              l = (short)((j+2*n_in) % (2*n_in));
              if(j < mid[k])
                a[k][l] = ((float)j - (mid[k]-width_l)) / width_l;
              else               
                a[k][l] = ((mid[k]+width_r) - (float)j) / width_r;
              norm[k] += a[k][l];
            }
          }
          break;
      
        default:
          return ERROR_FTYPE;

      }
      return O_K;
    }    


   
    /**
     * Convolution
     * 
     * @param in
     *          input data buffer
     * @return 
     *          output data buffer
     */
    public float[] convolve(float[] in)
    {
      float z;
      short j;
      short k;

      float[] out = new float[n_out];
      
      for (k=0; k < n_out; k++)
      {
        z = 0.f;
        for (j=0; j < 2*n_in; j++)
          z += a[k][j] * in[j];
        out[k] = z / norm[k];
      }
      
      return out;
    }

    /**
     * print convolution core parameter
     */
    public void printConvcore()
    {
      short k;
      float f, mel, b, hl, hr;
      DecimalFormat format1 = new DecimalFormat(" 00 ");
      DecimalFormat format2 = new DecimalFormat("000 ");
      DecimalFormat format3 = new DecimalFormat("0000 ");
      DecimalFormat format4 = new DecimalFormat("00.00 ");
      DecimalFormat format5 = new DecimalFormat("0000 ");
      DecimalFormat format6 = new DecimalFormat("000 ");
      DecimalFormat format7 = new DecimalFormat("0000 ");
      DecimalFormat format8 = new DecimalFormat("00.00");

      JlObject.log(" --- convcore: convolution parameter display --------------------------- ");
      JlObject.log("                                                     f_width(Hz)         ");
      JlObject.log("    k      mid f_mid(Hz)    Bark   b[Hz]     width   left  right  norm   ");
      JlObject.log("");
      for (k = 0; k < n_out; k++)
      {
        f = (float)(mid[k] * samplingFrequ) / 512.f;
        hl = (float)(width[0][k] * samplingFrequ) / 512.f;
        hr = (float)(width[1][k] * samplingFrequ) / 512.f;
        mel = 13.0f * (float)Math.atan(0.76 * f / 1000.0) + 3.5f
            * (float)Math.atan(Math.pow((double)(f / 7500.0), 2.0));
        b = 25.0f + 75.0f * (float)Math.pow((double)(1.0 + 1.4 * (float)Math
            .pow((double)f / 1000.0, 2.0)), 0.69);
        JlObject.log(format1.format(k) + format2.format(mid[k])
            + format3.format(f) + format4.format(mel) + format5.format(b)
            + format6.format(width[0][k]+width[1][k]) + format7.format(hl) 
            + format7.format(hr) + format8.format(norm[k]));
      }
      JlObject.log("");
      JlObject
          .log("------------------------------------------------------------------------");
      JlObject.log("");
    }
  }
}

/* EOF */