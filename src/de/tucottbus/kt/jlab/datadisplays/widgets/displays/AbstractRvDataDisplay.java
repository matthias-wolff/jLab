package de.tucottbus.kt.jlab.datadisplays.widgets.displays;

import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.widgets.Composite;

import de.tucottbus.kt.jlab.datadisplays.data.DataCompInfo;
import de.tucottbus.kt.jlab.datadisplays.data.DataException;
import de.tucottbus.kt.jlab.datadisplays.events.DisplayInfoMouseMoveEvent;
import de.tucottbus.kt.jlab.datadisplays.events.DisplayInfoScrollEvent;
import de.tucottbus.kt.jlab.datadisplays.utils.NumberFormatter;
import de.tucottbus.kt.jlab.datadisplays.widgets.rulers.Ruler;
import de.tucottbus.kt.jlab.datadisplays.widgets.rulers.RulerCalculator;
import de.tucottbus.kt.jlab.datadisplays.widgets.rulers.RulerScaleLine;
import de.tucottbus.kt.jlab.kernel.JlData;

/**
 * Abstract data display showing data values at the ordinate and records at the
 * abscissa.
 */
public abstract class AbstractRvDataDisplay extends AbstractDataDisplay {

	/**
	 * The vertical ruler of this display, may be <code>null</code>
	 */
	protected Ruler m_iVruler;

  /**
   * Default RV data display constructor.
   * 
   * @param iParent
   *          A composite control which will be the parent of the new instance,
   *          cannot be <code>null</code>.
   * @param nStyle
   *          The style of control to construct.
   * @param aDci
   *          An array of data component information objects defining the data
   *          to be displayed, cannot be <code>null</code>. 
   * @param iHruler
   *          The horizontal ruler of this display, cannot be <code>null</code>.
   * @throws DataException 
   *          if there is a problem with <code>aDci</code>
   * @throws NullPointerException
   *          if <code>iHruler</code> is <code>null</code>
   */
	public AbstractRvDataDisplay
	(
	  Composite      iParent,
    DataCompInfo[] aDci,
	  Ruler          iHruler
	)
  throws DataException 
	{
		super(iParent, aDci, iHruler);
	}

	/*
	 * (non-Javadoc)
	 */
	protected double getCompValueAt(int nX, int nY) {
		if (m_aDci.length != 1)
			return Double.NaN;
		JlData iData = getJlData();
		return iData.cofs + m_aDci[0].nComp * iData.cinc;
	}

	/*
	 * (non-Javadoc)
	 */
	protected double getDataValueAt(int nX, int nY) {
		return m_iVruler.getValOfPos(nY);
	}

	/*
	 * (non-Javadoc)
	 */
	protected Ruler int_createVerticalRuler(Composite iParent)
  {
	  String sUnit = "";
	  double vinc  = 0.;
	  try
	  {
      vinc  = this.m_aDci[0].iData.vinc;
	    sUnit = new String(this.m_aDci[0].iData.vunit);
	  }
	  catch (Exception e) {}
    m_iVruler = new Ruler(iParent,true);
    m_iVruler.setRange(getMinValue(),getMaxValue(),vinc,sUnit); 
    return m_iVruler;
  }

	/*
	 * (non-Javadoc)
	 */
	protected void paintCanvas(GC iGc, Rectangle iDamage, RulerCalculator iHrc, RulerCalculator iVrc)
  {
    super.paintCanvas(iGc,iDamage,iHrc,iVrc);

    // Initialize
    iGc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
    iGc.setLineStyle(SWT.LINE_DOT);
    //iGc.setLineWidth(1);

    // Paint horizontal guidelines
    LinkedList<RulerScaleLine> iScale = iVrc.getScale();
    int hSize = iHrc.getLength();
    int vSize = iVrc.getLength();
    for (int i = 0; i < iScale.size(); i++)
    {
      if (!iScale.get(i).isMainLine()) continue;
      int n = iScale.get(i).getPos();
      iGc.drawLine(0,n,hSize,n);
    }

    // Paint vertical guidelines
    iScale = iHrc.getScale();
    for (int i = 0; i < iScale.size(); i++)
    {
      if (!iScale.get(i).isMainLine()) continue;
      int n = iScale.get(i).getPos();
      iGc.drawLine(n,0,n,vSize);
    }

    // Clean up
    iGc.setLineStyle(SWT.LINE_SOLID);
  }
	
	/*
	 * (non-Javadoc)
	 * @see de.tudresden.ias.eclipse.dlabpro.editors.vis.components.displays.AbstractDataDisplay#paintMarkers(org.eclipse.swt.graphics.GC, org.eclipse.swt.graphics.Rectangle)
	 */
	protected void paintMarkers(GC iGc, Rectangle iDamage, RulerCalculator iHrc, RulerCalculator iVrc)
	{
	  if (iGc.getDevice() instanceof Printer)
	    if (m_aDci.length==1)
  	  {
	      iGc.setForeground(iGc.getDevice().getSystemColor(SWT.COLOR_BLACK));
        iGc.setBackground(m_iVcm.getBgColor(getDisplay()));
	      String sName = m_aDci[0].iData.getCompName(m_aDci[0].nComp);
	      if (sName!=null && sName.length()>0)
	      {
          Point iDs = new Point(iHrc.getLength(),iVrc.getLength());
  	      Point iTs = iGc.textExtent(sName);
          iGc.fillRectangle(iDs.x-iTs.x-iTs.y*3/4,iTs.y/3,iTs.x+iTs.y/2,3*iTs.y/2);
  	      iGc.drawRectangle(iDs.x-iTs.x-iTs.y*3/4,iTs.y/3,iTs.x+iTs.y/2,3*iTs.y/2);
    	    iGc.drawText(sName,iDs.x-iTs.x-iTs.y/2,iTs.y*2/3,true);
	      }
  	  }
	}
	
	protected DisplayInfoMouseMoveEvent createInfoEventOnMouse(int x, int y)
  {
    String sValA; // Actual (ruler) value
    String sValD; // Data value (at nearest data point)
    int    nValL; // Logical value (data element index)
    String sData; // Nearest data point description
    JlData iData = getJlData();
    DisplayInfoMouseMoveEvent e = new DisplayInfoMouseMoveEvent(this);
    
    // Store X (=record) axis information
    sValA = NumberFormatter.formatAndAdjust(m_iHruler.getValOfPos(x));
    nValL = m_iHruler.getDataPointOfPos(x);
    sValD = NumberFormatter.formatAndAdjust(iData.rofs+nValL*iData.rinc);
    sData = "rec. "+nValL;
    e.StoreX(sValA,sValD,iData.runit!=null?new String(iData.runit):"",sData);
	  
    // Store Y (=value) axis information
    sValA = NumberFormatter.formatAndAdjust(m_iVruler.getValOfPos(y));
    if (m_aDci.length==1)
    {
      nValL = m_aDci[0].nComp;
      sData = new String(iData.getCompName(nValL));
      if (sData.length()>0) sData = " \""+sData+"\"";
      sData = "comp. "+nValL+sData; 
    }
    else
    {
      int nCnt = 0;
      for (int nC=0; nC<m_aDci.length; nC++)
        if (m_aDci[nC].bVisible)
          nCnt++;
      sData = nCnt + " comps.";
    }
    e.StoreY(sValA,"",""/*new String(iData.zunit)*/,sData);
    
    // Store Z axis information
    e.StoreZ("","");

    return e;
  }

	protected DisplayInfoScrollEvent createInfoEventOnScroll()
  {
    double nMinValP;
    double nMaxValP;
    String sRngL;
    String sRngP;
    JlData iData = getJlData();
    DisplayInfoScrollEvent e = new DisplayInfoScrollEvent(this);
    
    // Store X (=record) display range information
    nMinValP = iData.rofs+m_nFirstRec*iData.rinc;
    nMaxValP = iData.rofs+m_nLastRec*iData.rinc;
    sRngP = NumberFormatter.formatAndAdjust(nMinValP)+" ... "+NumberFormatter.formatAndAdjust(nMaxValP);
    sRngL = m_nFirstRec+" ... "+m_nLastRec+" ("+(m_nLastRec-m_nFirstRec+1)+")";
    e.StoreX("records",sRngP,iData.runit!=null?new String(iData.runit):"",sRngL);
    
    // Store Y (=value) display range information
    nMinValP = getMinValue();
    nMaxValP = getMaxValue();
    sRngP = NumberFormatter.formatAndAdjust(nMinValP)+" ... "+NumberFormatter.formatAndAdjust(nMaxValP);
    e.StoreY("values",sRngP,""/*new String(iData.zunit)*/,"");
    
    // Store Z display range information
    e.StoreZ("","","");

    return e;
  }
}
