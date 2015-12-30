package de.tucottbus.kt.jlab.kernel;

public class JlStatistics
{

  /**
   * Computes the Kullback-Leibler-Divergence between two probability 
   * distributions {@code P} and {@code Q}.
   *    
   * @param P
   *          First operand, must not be {@code null}
   * @param Q
   *          Second operand, must not be {@code null}
   * @return The Kullback-Leibler-Divergence.
   * @throws IllegalArgumentException
   *           if {@code P} and {@code Q} have different array sizes, if 
   *           {@code P} or {@code Q} or both contain negative values or do not 
   *           sum-up to unity.
   */
  public static float dkl(float[] P, float[] Q)
  throws IllegalArgumentException
  {
    if (P.length!=Q.length)
      throw new IllegalArgumentException("Operands have different sizes ("
      + P.length+"!="+Q.length+")");

    double sumP = 0;
    double sumQ = 0;
    double dkl  = 0;
    for (int i=0; i<P.length; i++)
    {
      if (P[i]<0) throw new IllegalArgumentException("P["+i+"] < 0");
      if (Q[i]<0) throw new IllegalArgumentException("Q["+i+"] < 0");
      sumP += P[i];
      sumQ += Q[i];
      double p = Math.max(1E-100,P[i]);
      double q = Math.max(1E-100,Q[i]);
      dkl += P[i]*(Math.log(p)-Math.log(q));
    }
    if (Math.abs(1-sumP)>1E-5)
      throw new IllegalArgumentException("P does not sum up to 1 (but to "+sumP+")");
    if (Math.abs(1-sumQ)>1E-5)
      throw new IllegalArgumentException("Q does not sum up to 1 (but to "+sumQ+")");

    return (float)dkl;
  }
  
}
