// jLab

package de.tucottbus.kt.jlab.signal;

import de.tucottbus.kt.jlab.kernel.JlData;
import de.tucottbus.kt.jlab.kernel.JlMath;

/**
 * This class provides psychoacoustic functions.
 * 
 * @author Matthias Wolff
 */
public class PsychoAcoustics
{
  // -- Per-frequency methods --

  /**
   * Returns the absolute threshold of hearing (ATH) at a given frequency.
   * 
   * @param f
   *          The frequency in Hz to return the ATH for.
   * @return The absolute threshold of hearing measured in dB<sub>SPL</sub>.
   */
  public static double getATH(double f)
  {
    f /= 1000.; // Convert f to kHz
    if (f<=0) return 0.;
    return 3.64*Math.pow(f,-0.8)
           - 6.5*Math.exp(-0.6*(f-3.3)*(f-3.3))
           + 0.001*f*f*f*f;
  }

  /**
   * Returns the masked threshold at a given frequency for a given sinusoidal masker.
   * 
   * @param f
   *          The frequency in Hz to return the ATH for.
   * @param fm
   *          The frequency of the masking tone in Hz.
   * @param Lm
   *          The level of the masking tone in dB.
   * @return The masked threshold measured in dB.
   */
  public static double getMT(double f, double fm, double Lm)
  {
    double nS  = f<fm ? 27. : (24. + 230./f - 0.2*Lm);
    double nMt = Lm - Math.abs(freqToTonheit(f)-freqToTonheit(fm))*nS;
    return Math.max(nMt,getATH(f));
  }

  /**
   * Converts from frequency (in Hz) to Tonheit (in Bark).
   * 
   * @param f
   *          The frequency in Hz.
   * @return The Tonheit in Bark.
   */
  public static double freqToTonheit(double f)
  {
    return 13.*Math.atan(0.00076*f)+3.5*Math.atan((f/7500.)*(f/7500.));
  }
  
  // -- Per spectrum methods --
  
  /**
   * Computes the absolute threshold of hearing (ATH) measured in
   * dB<sub>SPL</sub>. The frequency range of output is 
   * [0...{@code finc}&middot;({@code lines}-1)] Hz.
   * 
   * @param lines
   *          The number of spectral lines to compute.
   * @param finc
   *          The frequency increment between spectral lines in Hz.
   * @return The global absolute threshold of hearing in dB<sub>SPL</sub>.
   * @throws IllegalArgumentException
   *           if {@code finc} is not positive.
   */
  public static float[] getATH(int lines, float finc)
  throws IllegalArgumentException
  {
    if (finc<=0)
      throw new IllegalArgumentException("Frequency increment not positive");

    float[] ath = new float[lines];
    for (int l=0; l<lines; l++)
      ath[l] = (float)getATH(l*finc);
    return ath;
  }

 /**
   * Computes the masked threshold from a logarithmic auto-power spectrum. 
   * Spectral lines must be measured in dB<sub>SPL</sub>. The frequency range of
   * input and output is [0...{@code finc}&middot;({@code laps.length}-1)] Hz.
   * 
   * @param laps
   *          The logarithmic auto-power spectrum in dB<sub>SPL</sub>.
   *          <i style="color:red">Values will be overwritten!</i>
   * @param finc
   *          The frequency increment between spectral lines in Hz.
   * @param bw
   *          The bandwidth of spectral peaks in Hz, must be positive. The 
   *          value is used to compute the level of masking tones. 16&nbsp;Hz 
   *          is a reasonable choice.
   * @param fcutoff
   *          The frequency cutoff in Hz. No masking tones will be detected
   *          below this frequency. Non-positive for no cutoff.
   * @param lcutoff
   *          The level cutoff relative to the spectral peak in dB<sub>SPL</sub>,
   *          must be negative. Spectral lines lower than {@code peak} + 
   *          {@code lcutoff} will not be regarded as masking tones. Negative
   *          infinity for no cutoff.
   * @param maxMaskers
   *           The maximal number of detected masking tones, no limit if 
   *           negative.
   * @param idMaskers
   *          Filled with a list of detected masking tones, can be {@code null}.
   * @return The global masked threshold in dB<sub>SPL</sub>.
   * @throws IllegalArgumentException
   *           if  {@code laps} is {@code null} or empty, if {@code finc} is not
   *           positive, or if {@code bw} is not positive.
   */
  public static float[] getMT
  (
    float[] laps,
    float   finc, 
    float   bw,
    float   fcutoff,
    float   lcutoff,
    int     maxMaskers,
    JlData  idMaskers
  )
  throws IllegalArgumentException
  {
    if (laps==null || laps.length==0)
      throw new IllegalArgumentException("Input spectrum null or emtpy");
    if (finc<=0)
      throw new IllegalArgumentException("Frequency increment not positive");
    if (bw<=0)
      throw new IllegalArgumentException("Bandwidth not positive");
    
    // 1. Initialize
    JlData idMaskersInt = new JlData();
    int nCompFe = idMaskersInt.addComp(float.class,"~f");
    int nCompB = idMaskersInt.addComp(float.class,"B");
    float nLmaxx = JlMath.max(laps);
    int nFmin = frequencyToLine(fcutoff,finc);
    int nB = Math.max(frequencyToLine(bw,finc),2);
    nB += nB%2;
    float[] aAth = new float[laps.length];
    float[] aMt  = new float[laps.length];
    for (int n=0; n<laps.length; n++)
    {
      aAth[n] = (float)PsychoAcoustics.getATH(lineToFrequency(n,finc));
      aMt[n] = aAth[n];
      if (n<nFmin) // Below frequency cutoff
        laps[n]=Float.NEGATIVE_INFINITY;
    }

    while (idMaskersInt.getLength()<maxMaskers)
    {
      // 2. Set levels below current masked threshold to -infinity
      for (int n=0; n<laps.length; n++)
        if (laps[n]<aMt[n]) laps[n] = Float.NEGATIVE_INFINITY;

      // 3. Find spectral maximum
      float nLpeak = JlMath.max(laps);
      int   nIpeak = JlMath.imax(laps);

      if (nIpeak>=0 && nIpeak<laps.length && nLpeak>=Math.max(nLmaxx+lcutoff,0))
      {
        // 4. Partial found
        float nFpeak = lineToFrequency(nIpeak,finc);
        if (nFpeak>=fcutoff)
        {
          int nRec = idMaskersInt.addRecs(1,0);
          idMaskersInt.dStore(nFpeak,nRec,nCompFe);
          idMaskersInt.dStore(bw,nRec,nCompB);
        }
        
        // 5. Update masked threshold
        int nFirst = Math.max(nIpeak-nB/2,0);
        int nLast = Math.min(nIpeak+nB/2,laps.length-1);
        for (int n=nFirst; n<=nLast; n++)
        {
          aMt[n]  = Math.max(aMt[n],laps[n]);
          laps[n] = Float.NEGATIVE_INFINITY;
        }
        for (int n=0; n<laps.length; n++)
        {
          float nMt 
            = (float)PsychoAcoustics.getMT(lineToFrequency(n,finc),nFpeak,nLpeak);
          aMt[n] = Math.max(aMt[n],nMt);
          if (laps[n]<nMt) laps[n] = Float.NEGATIVE_INFINITY;
        }
      }
      else
        break;

      // Proceed with 2.
    }
    
    if (idMaskers!=null)
      idMaskers.copy(idMaskersInt);
    return aMt;
  }

  // -- Auxiliary methods --
  
  /**
   * Converts spectral line index to frequency.
   * 
   * @param n
   *          The spectral line index.
   * @param finc
   *          The frequency increment between spectral lines in Hz.
   * @return The frequency in Hz.
   */
  public static float lineToFrequency(int n, float finc)
  {
    return n*finc;
  }

  /**
   * Converts frequency to spectral line index.
   * 
   * @param f
   *          The frequency in Hz.
   * @param finc
   *          The frequency increment between spectral lines in Hz.
   * @return The spectral line index.
   */
  public static int frequencyToLine(float f, float finc)
  {
    return (int)Math.round(f/finc);
  }

}

// EOF

