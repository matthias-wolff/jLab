package de.tucottbus.kt.jlab.datadisplays.widgets;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;

import de.tucottbus.kt.jlab.datadisplays.data.DataCompInfo;
import de.tucottbus.kt.jlab.datadisplays.data.DataException;
import de.tucottbus.kt.jlab.datadisplays.events.DisplayEvent;
import de.tucottbus.kt.jlab.datadisplays.events.HdetailEvent;
import de.tucottbus.kt.jlab.datadisplays.events.IDisplayEventListener;
import de.tucottbus.kt.jlab.datadisplays.interfaces.Playable;
import de.tucottbus.kt.jlab.datadisplays.utils.DdUtils;
import de.tucottbus.kt.jlab.datadisplays.utils.DpiConverter;
import de.tucottbus.kt.jlab.datadisplays.widgets.displays.AbstractDataDisplay;
import de.tucottbus.kt.jlab.datadisplays.widgets.displays.LabelDisplay;
import de.tucottbus.kt.jlab.datadisplays.widgets.displays.Oscillogram;
import de.tucottbus.kt.jlab.datadisplays.widgets.displays.ThreeDDisplay;
import de.tucottbus.kt.jlab.datadisplays.widgets.rulers.DummyRuler;
import de.tucottbus.kt.jlab.datadisplays.widgets.rulers.Ruler;
import de.tucottbus.kt.jlab.datadisplays.widgets.rulers.RulerCalculator;
import de.tucottbus.kt.jlab.kernel.JlData;

/**
 * this panel is the basis for all displays and rulers. it manages the zooming
 * and scrolling of the components as well as the layout
 * 
 * @author Stephan Larws
 * 
 */
public class ComponentPanel extends Composite {
	
  private   Vector m_iDisplayEventListeners;
  protected LinkedList<DataDisplayPanel> mDataDisplayPanelList;
  private   LinkedList mVerticalSeparatorList;
  private   HorizontalRulerPanel mHorizontalRulerPanel;
  protected Ruler mHorizontalRuler;
  private   JlData m_data;
  private   Point m_iSizeBuf;
  private   Color backgroundColor;
  private   SelectionListener hsbrSelectionListener;

	// -- Constructors --
	
	/**
	 * Default constructor, initializes all attributes to default values
	 * 
	 * @param parent
	 *            The parent widget
	 * @wbp.parser.constructor
	 */
	public ComponentPanel(Composite parent)
	{
	  this(parent,(JlData)null,"");
	}

	/**
	 * Constructor for a ComponentPanel
	 * 
	 * @param parent
	 *          The parent widget
	 * @param iData
	 *          The data object from which the displays are created
	 * @param sProps
	 *          Property string defining the layout (see <code>DataCompInfo</code>)
	 */
	public ComponentPanel(Composite parent, JlData iData, String sProps)
	{
		super(parent, SWT.H_SCROLL);
		if (iData==null)
		{
		  iData = new JlData(new double[16000],"");
		  iData.rinc  = 1./16.;
		  iData.runit = "ms";
		}
		m_data = iData;
    backgroundColor = parent.getBackground();
		mDataDisplayPanelList = new LinkedList();
		mVerticalSeparatorList = new LinkedList();
    setBackground(backgroundColor);

    try
    {
      DataCompInfo[] aDci = DataCompInfo.createFromData(iData,sProps);
      initialSetup(aDci);
    }
    catch (DataException e)
    {
      e.printStackTrace();
    }

    m_iDisplayEventListeners = new Vector();
		addListeners();
	}

	/**
	 * Constructor for exception states 
	 * 
	 * @param parent
	 * @param e
	 */
	public ComponentPanel(Composite parent, Exception e)
  {
    super(parent, SWT.H_SCROLL);
    m_data = null;
    backgroundColor = parent.getBackground();
    mDataDisplayPanelList = new LinkedList();
    mVerticalSeparatorList = new LinkedList();
    setBackground(backgroundColor);

    initialSetup(e);
  }

	// -- Layout --

	/**
	 * Initial setup of data displays. This method is called by the constructor
   * and must not be called otherwise.
   * @param aDci
   *          Array of DataCompInfo objects defining the display data
	 */
	private final void initialSetup(DataCompInfo[] aDci)
	{
    GridLayout gl = new GridLayout(1, false);
    gl.marginHeight = 0;
    gl.marginWidth = 0;
    gl.horizontalSpacing = 0;
    gl.verticalSpacing = 0;
    setLayout(gl);
    hsbrSelectionListener = new SelectionListener()
    {
      public void widgetSelected(SelectionEvent e)
      {
        scrollComponentsHorizontal();
      }
      
      public void widgetDefaultSelected(SelectionEvent e)
      {
      }
    };
    if (aDci!=null)
    {
      createDisplays(aDci);
      createHorizontalRuler();      
      initScrollbar();
    }
	}

	/**
	 * Initial setup of the component panel to display an exception state.
	 * 
	 * @param e
	 *          the exception
	 */
	private final void initialSetup(Exception e)
  {
    setLayout(new GridLayout(1, false));
    Composite iHead = new Composite(this,SWT.NONE);
    iHead.setLayout(new RowLayout());
    Label iLabel;
    iLabel = new Label(iHead,SWT.NONE);
    try
    {
      iLabel.setImage(getDisplay().getSystemImage(SWT.ICON_WARNING));
    }
    catch (Throwable e2)
    {
    }
    iLabel = new Label(iHead,SWT.NONE);
    iLabel.setText("Problems opening or refreshing the editor.");
    iLabel = new Label(this,SWT.NONE);
    iLabel.setText(e.getMessage());
    getHorizontalBar().setVisible(false);
  }
	
	/**
	 * Performs changes of the data displays as defined by <code>aDci</code>
   * @param aDci
   *          Array of DataCompInfo objects defining the display data
	 */
  public final void setup(DataCompInfo[] aDci)
  {
    if (isDisposed()) return;
    if (aDci!=null) m_data = aDci[0].iData;
    Point rd = getRecDetail();
    Rectangle r = getBounds();
    clearPanel();
    createDisplays(aDci);
    createHorizontalRuler();
    initScrollbar();
    setBounds(r);
    if (rd!=null) setRecDetail(rd.x,rd.y,false);
    layout();
  }	

  /**
   * Clears the entire component panel
   */
  private final void clearPanel() {
    Control c;
    for (int i = 0; i < mDataDisplayPanelList.size(); i++) {
      c = (Control) mDataDisplayPanelList.get(i);
      if (c != null) {
        c.dispose();
      }
      if (i < mVerticalSeparatorList.size()) {
        c = (Control) mVerticalSeparatorList.get(i);
        if (c != null) {
          c.dispose();
        }
      }
    }
    if (mHorizontalRulerPanel!=null)
      mHorizontalRulerPanel.dispose();
    mVerticalSeparatorList = new LinkedList();
    mDataDisplayPanelList = new LinkedList();
  }
	
  /**
   * Creates all data displays.
   * 
   * @param aDci
   *          Array of DataCompInfo objects defining the display data.
   */
  private final void createDisplays(DataCompInfo[] aDci)
  {
    if (aDci==null || aDci.length==0) return;                                   // Must have data comp. info array!
    Vector  iDciGrp  = new Vector();                                            // Components of current group
    int     nXC      = aDci.length;                                             // Number of components
    int     nVisible = 0;                                                       // No. visible comps. in current group
    boolean bFirst   = true;                                                    // First display flag

    for (int nC=0; nC<nXC; nC++)                                                // Loop over components
    {                                                                           // >>
      iDciGrp.add(aDci[nC]);                                                    //   Add to component vector
      if (aDci[nC].bVisible) nVisible++;                                        //   Count visible components
      if                                                                        //   If current component is
      (                                                                         //   |
        nC==nXC-1                          ||                                   //   | the last one           OR
        aDci[nC].nGroup<0                  ||                                   //   | standing alone         OR
        aDci[nC+1].nGroup!=aDci[nC].nGroup                                      //   | the last of its group
      )                                                                         //   |
      {                                                                         //   >>
        if (iDciGrp.size()>0 && nVisible>0)                                     //     Visible comps. in current group
        {                                                                       //     >>
          if (!bFirst) createAVerticalSeparator();                              //       Separate display panels
          DataCompInfo[] aDciGrp = {};                                          //       Comp. array of current group
          aDciGrp = (DataCompInfo[])iDciGrp.toArray(aDciGrp);                   //       Get component info array
          createDisplay(aDciGrp);                                               //       Create display
          bFirst = false;                                                       //       The next one is not the first
        }                                                                       //     <<
        iDciGrp.clear();                                                        //     Start new component vector
        nVisible = 0;                                                           //     Clear no. of visible components
      }                                                                         //   <<
    }                                                                           // <<
  }
	
  /**
   * Creates a new data display panel for one or several data components.
   * 
   * @param aDci
   *          Array of DataCompInfo objects defining the data of a single
   *          display.
   */
  private void createDisplay(DataCompInfo[] aDci)
  {
    int nRightSpacer = 10;
    int nGridStyle   = GridData.FILL_BOTH;
    
    if (aDci==null || aDci.length==0) return;

    try
    {
      // What to create?
      Class iDspCls = Class.forName(aDci[0].sDisplayType);
      DdUtils.MSG("Creating a "+aDci[0].sDisplayType+
        " for comps "+aDci[0].nComp+" through "+(aDci[0].nComp+aDci.length-1)+
        " ("+aDci[0].iData.getLength()+" records)");
      if (iDspCls.equals(Oscillogram.class)) nRightSpacer = 0;
      
      // Create a new data display panel and data display
      DataDisplayPanel iPanel = new DataDisplayPanel(this);
      Class[] aCnsSgn =
      { Composite.class, int.class, DataCompInfo[].class, Ruler.class };
      Constructor iDspCns = iDspCls.getConstructor(aCnsSgn);
      Object[] aCnsArgs = { iPanel, new Integer(0), aDci, mHorizontalRuler };
      AbstractDataDisplay iDisplay = (AbstractDataDisplay)iDspCns
          .newInstance(aCnsArgs);
      
      iDisplay.addMouseListener(new MouseListener() 
      {
		
    		public void mouseUp(MouseEvent e) 
    		{
    		  // TODO Auto-generated method stub
    		  //Event ev;
    		  Event ev=new Event ();
    		  ev.x=e.x;
    		  ev.y=e.y;
    		  notifyListeners(SWT.MouseUp, ev);
    		  //notifyListeners(SWT.MouseUp,new Event);
    		}
    		
    		public void mouseDown(MouseEvent e) 
    		{
    		  // TODO Auto-generated method stub
    		  //JlObject.log("\n\n Mausklick \n\n");
    		  Event ev=new Event ();
    		  ev.x=e.x;
    	      ev.y=e.y;
    		  notifyListeners(SWT.MouseDown, ev);
    		}
    		
    		public void mouseDoubleClick(MouseEvent e) 
    		{
    		  // TODO Auto-generated method stub
    		  //JlObject.log("\n\n Doppelklick \n\n");
    		  Event ev=new Event ();
    	      ev.x=e.x;
    		  ev.y=e.y;	
    		  notifyListeners(SWT.MouseDoubleClick, new Event());
    		}
    	});

      // Create vertical ruler, spacer and layout
      Ruler iVRuler = iDisplay.createVerticalRuler(iPanel);
      iPanel.addVerticalRuler(iVRuler);
      iPanel.addDataDisplay(iDisplay);
      iPanel.addRightSpacer(nRightSpacer);
      mDataDisplayPanelList.add(iPanel);
      if (iVRuler instanceof DummyRuler) nGridStyle = GridData.FILL_HORIZONTAL;
      else                               nGridStyle = GridData.FILL_BOTH; 
      iPanel.setLayoutData(new GridData(nGridStyle));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Returns a point describing the receiver's size. The method is implemented
   * asynchronously and may be called form outside the UI process.
   * 
   * @return the receiver's size
   */
  public Point getSizeSync()
  {
    try
    {
      getDisplay().syncExec(new Runnable()
      {
        public void run()
        {
          m_iSizeBuf = getSize();
        }
      });
    }
    catch (IllegalStateException e)
    {
      Display.getDefault().syncExec(new Runnable()
      {
        public void run()
        {
          m_iSizeBuf = getSize();
        }
      });
    }
    return m_iSizeBuf;
  }

  // -- Sound Play Back --
  
  /**
   * Determines if the set of display panels maintained by this component
   * panel is suitable for playback and returns the (first)
   * <code>Playable</code> which can be played back at the current time.
   * 
   * @return The first playable data display <code>null</code> if no such
   *         display exists
   */
  public final Playable canPlay()
  {
    DataDisplayPanel iPanel = null;                                             // The current display panel
    Playable iPdd = null;                                                       // The current playable display

    for (int i = 0; i < mDataDisplayPanelList.size(); i++)                      // Loop over display panels
    {                                                                           // >>
      iPanel = (DataDisplayPanel)mDataDisplayPanelList.get(i);                  //   Get 'em panel
      if (iPanel.isDisposed()) continue;                                        //   Disposed -> forget it!
      if (!iPanel.isEnabled()) continue;                                        //   Is disabled -> forget it!
      try                                                                       //   Try
      {                                                                         //   >>
        iPdd = (Playable)iPanel.getDataDisplay();                               //     Display is playable
        if (iPdd != null && iPdd.canPlay()) return iPdd;                        //     ... and not indisposed :(
      }                                                                         //   <<
      catch (ClassCastException e)                                              //   Not playable
      {                                                                         //   >>
      }                                                                         //   <<
    }                                                                           // <<
    return null;                                                                // Return "none playable"
  }
  
// -- Printing --
  /*
   * Gives the height of rotated letter "X" (widths of vertical rulers).
   * 
   */
  public int getRhX(GC iGc)
  {
    Point           R    = iGc.getDevice().getDPI();       // Output device resolution
    DpiConverter    iXc  = new DpiConverter(R.x);          // X-lengths converter
    FontData        iFd  = iGc.getFont().getFontData()[0]; // Get data of font selected in iGc
    int             nRhX = iXc.pt2px(iFd.height);          // Height of rotated letter "X"
    
    return nRhX;
  }

  /**
   * Draws this component panel on a device context.
   * 
   * @param iGc
   *          The device context (must be initialized, and a suitable font must be selected).
   * @param bounds
   *          The bounds, in device pixels, of the drawing area.
   */
  public void drawOn(GC iGc, Rectangle bounds)
  {
    Point           R    = iGc.getDevice().getDPI();       // Output device resolution
    int             nL   = bounds.x;                       // Left base line (in pixels)
    int             nT   = bounds.y;                       // Top base line (in pixels)
    int             nW   = bounds.width;                   // Diagram width (in pixels)
    int             nH   = bounds.height;                  // Diagram height (in pixels)
    int             nHd  = 0;                              // Height of currently drawn display (in pixels)
    int             nRf  = 0;                              // Ruler flags
    int             nPf  = AbstractDataDisplay.DRAW_DATA;  // Paint flags
    Transform       iTf  = new Transform(iGc.getDevice()); // Drawing transform
    DpiConverter    iXc  = new DpiConverter(R.x);          // X-lengths converter
    DpiConverter    iYc  = new DpiConverter(R.y);          // Y-lengths converter
    FontData        iFd  = iGc.getFont().getFontData()[0]; // Get data of font selected in iGc
    int             nUhX = iYc.pt2px(iFd.height);          // Height of upright letter "X"
    int             nRhX = iXc.pt2px(iFd.height);          // Height of rotated letter "X"
    RulerCalculator iHrc = null;                           // X ruler calculator
    RulerCalculator iVrc = null;                           // Y ruler calculator 
    
    // Make ruler drawing flags
    nRf |= Ruler.DRAW_SCALE;
    nRf |= Ruler.DRAW_TICKS;
    
    // Make display paint flags
    nPf |= AbstractDataDisplay.DRAW_CANVAS;

    // Transform
    iTf.translate(nL,nT); iGc.setTransform(iTf);

    // - Print title
    iGc.drawLine(0,0,0,-nUhX);
    iGc.drawLine(-nUhX/4,-nUhX/2,0,-nUhX);
    iGc.drawLine( nUhX/4,-nUhX/2,0,-nUhX);

    // - Print displays and vertical rulers
    iHrc = new RulerCalculator(getHorizontalRuler().getCalculator());
    iHrc.setPosInfo(nW); iHrc.notifyOfGC(iGc);
    int nDataDisplays = 0;
    int nVspaceAvail = nH;
    for (int i=0; i<mDataDisplayPanelList.size(); i++)
    {
      DataDisplayPanel iDdp = (DataDisplayPanel)mDataDisplayPanelList.get(i);
      AbstractDataDisplay iDd = iDdp.getDataDisplay();
      if (iDd instanceof LabelDisplay)
        nVspaceAvail -= 3*nRhX/2;
      else
        nDataDisplays++;
    }

    for (int i=0; i<mDataDisplayPanelList.size(); i++)
    {
      DataDisplayPanel iDdp = (DataDisplayPanel)mDataDisplayPanelList.get(i);
      AbstractDataDisplay iDd = iDdp.getDataDisplay();

      iVrc = new RulerCalculator(iDdp.getVerticalRuler().getCalculator());
      nHd  = (int)Math.round((double)nVspaceAvail/(double)nDataDisplays);
      if (iDd instanceof LabelDisplay)
        nHd = 3*nRhX/2; // Label displays special!
      else
      {
        nVspaceAvail -= nHd;
        nDataDisplays--;
      }
      iVrc.setPosInfo(nHd); iVrc.notifyOfGC(iGc);
      
      iGc.setForeground(iGc.getDevice().getSystemColor(SWT.COLOR_BLACK));
      iGc.setBackground(iGc.getDevice().getSystemColor(SWT.COLOR_WHITE));
      iDd.drawOn(iGc,iHrc,iVrc,nPf);
      iGc.setForeground(iGc.getDevice().getSystemColor(SWT.COLOR_BLACK));
      iGc.drawLine(-3*nRhX/2,nHd,nW+nRhX,nHd);
      iGc.drawLine(nW+nRhX/2,nHd-nRhX/4,nW+nRhX,nHd);
      iGc.drawLine(nW+nRhX/2,nHd+nRhX/4,nW+nRhX,nHd);
      iGc.drawLine(0,0,0,nHd);
      
      if (!(iDdp.getVerticalRuler() instanceof DummyRuler))
      {
        iTf.translate(-3*nRhX/2,0); iGc.setTransform(iTf);
        //iGc.setClipping(0,0,3*nRhX/2,iVrc.getLength());
        iGc.setForeground(iGc.getDevice().getSystemColor(SWT.COLOR_BLACK));
        iGc.setBackground(iGc.getDevice().getSystemColor(SWT.COLOR_WHITE));
        iDdp.getVerticalRuler().drawOn(iGc,null,iVrc,3*nRhX/2,nRf);
        //iGc.setClipping((Rectangle)null);
        iTf.translate(3*nRhX/2,0); iGc.setTransform(iTf);
      }
      
      iTf.translate(0,nHd); iGc.setTransform(iTf);
    }

    // - Print horizontal ruler
    iGc.setForeground(iGc.getDevice().getSystemColor(SWT.COLOR_BLACK));
    iGc.setBackground(iGc.getDevice().getSystemColor(SWT.COLOR_WHITE));
    mHorizontalRuler.drawOn(iGc,null,iHrc,3*nUhX/2,nRf);
    iGc.drawLine(0,0,0,3*nUhX/2);

    // Clean-up
    iGc.setTransform(null);
    iTf.dispose();
  }
  
	// -- Other Stuff --
	
  public void addDisplayEventListener(IDisplayEventListener iListener)
  {
    m_iDisplayEventListeners.add(iListener);
  }
  
  private void fireDisplayEvent(DisplayEvent iEvent)
  {
    if (m_iDisplayEventListeners==null) return;
    for (int i=0; i<m_iDisplayEventListeners.size(); i++)
      try
      {
        IDisplayEventListener iDel =
          (IDisplayEventListener)m_iDisplayEventListeners.get(i);
        iDel.onDisplayEvent(iEvent);
      }
      catch (Exception e)
      {
        // Silently ignore all exceptions
      }
  }
  
  public Ruler getHorizontalRuler()
  {
    return mHorizontalRuler;
  }
  
  public Point getRecDetail()
  {
    Point rd = new Point(0,m_data.getLength()-1);
   
    try
    {
      // If there is at least one data display, return its record detail
      DataDisplayPanel iPanel = (DataDisplayPanel)mDataDisplayPanelList.get(0);
      return iPanel.getDataDisplay().getRecDetail();
    }
    catch (Exception e)
    {
      
    }

    return rd;
  }
  
  public void setRecDetail(int nFirst, int nLast, boolean bUpdate)
  {
    if (nFirst < 0                   ) nFirst = 0;
    if (nLast  > m_data.getLength()-1) nLast  = m_data.getLength()-1;
    mHorizontalRuler.setDataRange(nFirst,nLast-nFirst+1,m_data.rofs,m_data.rinc,m_data.runit);

    DataDisplayPanel panel = null;
    AbstractDataDisplay display = null;

    for (int i = 0; i < mDataDisplayPanelList.size(); i++)
    {
      panel = (DataDisplayPanel)mDataDisplayPanelList.get(i);
      if (panel != null)
      {
        display = panel.getDataDisplay();
        display.setRecDetail(nFirst,nLast);
      }
    }
    if (nFirst == 0 && nLast == m_data.getLength() - 1) deactivateScrollbar();
    else activateScrollbar(nFirst,nLast);
    if (bUpdate)
    {
      update();
      mHorizontalRuler.update();
    }
  }
  
	/**
   * all AbstractDataDisplays shown on this panel will be zoomed in horizontally.
   */
	public void zoomComponentsInHorizontal()
	{
		int start_rec = mHorizontalRuler.getDataPointOfPos(0);
		int end_rec = mHorizontalRuler.getDataPointOfPos(getSize().x);
		int diff = end_rec - start_rec;
		start_rec += (int)(diff * 0.25);
		end_rec -= (int)(diff * 0.25);
		setRecDetail(start_rec,end_rec,true);
	}

	/**
	 * all AbstractDataDisplays on this panel will be zoomed out in vertical
	 * direction
	 */
	public void zoomComponentsOutHorizontal()
  {
    int start_rec = mHorizontalRuler.getDataPointOfPos(0);
    int end_rec = mHorizontalRuler.getDataPointOfPos(getSize().x);
    int diff = end_rec - start_rec;
    start_rec -= (int)(diff * 0.5);
    end_rec += (int)(diff * 0.5);
    setRecDetail(start_rec, end_rec, true);
  }

	public void diagnoseVerticalRulers()
  {
    Ruler v = null;
    DataDisplayPanel d = null;
    for (int i = 0; i < mDataDisplayPanelList.size(); i++)
    {
      d = (DataDisplayPanel)mDataDisplayPanelList.get(i);
      if (d != null)
      {
        v = d.getVerticalRuler();
        DdUtils.MSG(v.toString());
      }
    }
  }
  
	public final void switchToCenteredRulers() {
		// DataDisplayPanel p;
		// AbstractDataDisplay d;
		//
		// for (int i = 0; i < mDataDisplayPanelList.size(); i++) {
		// p = (DataDisplayPanel) mDataDisplayPanelList.get(i);
		// d = p.getDataDisplay();
		//
		// if (d instanceof Oscillogram) {
		// p.getVerticalRuler().setStrategy(
		// new VerticalRulerCenteredStrategy());
		// p.getDataDisplay().redraw();
		// }
		// }
		//
		// layout();
	}

	public final void switchToUncenteredRulers() {
		// DataDisplayPanel p;
		// AbstractDataDisplay d;
		//
		// for (int i = 0; i < mDataDisplayPanelList.size(); i++) {
		// p = (DataDisplayPanel) mDataDisplayPanelList.get(i);
		// d = p.getDataDisplay();
		//
		// if (d instanceof Oscillogram) {
		// p.getVerticalRuler().setStrategy(new VerticalRulerStrategy());
		// p.getDataDisplay().redraw();
		// }
		// }
		//
		// layout();
	}

	private final void createAVerticalSeparator() {
		VerticalSeparator vs = new VerticalSeparator(this);
		vs.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mVerticalSeparatorList.add(vs);
	}
	
	private final void createHorizontalRuler()
  {
    mHorizontalRulerPanel = new HorizontalRulerPanel(this);
    mHorizontalRuler = new Ruler(mHorizontalRulerPanel,false);
    mHorizontalRuler.setDataRange(0,m_data.getLength(),m_data.rofs,m_data.rinc,m_data.runit);
    mHorizontalRulerPanel.addHorizontalRuler(mHorizontalRuler);
    mHorizontalRulerPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    setHorizontalRuler();
  }

	private final void initScrollbar() {
		ScrollBar hBar = getHorizontalBar();
		hBar.setMaximum(m_data.getLength()- 1);
		hBar.setMinimum(0);
		hBar.setVisible(false);	
		hBar.setPageIncrement(hBar.getThumb()/2);
		hBar.removeSelectionListener(hsbrSelectionListener);
		hBar.addSelectionListener(hsbrSelectionListener);
	}
	
	private final void activateScrollbar(int start, int end) {
		ScrollBar hBar = getHorizontalBar();
		if(!hBar.isVisible())
			hBar.setVisible(true);
		
		hBar.setThumb(end - start);
		hBar.setSelection(start);
    getParent().layout(true,true);
	}
	
	private final void deactivateScrollbar() {
		getHorizontalBar().setVisible(false);
		getParent().layout(true,true);
	}

	private final void addListeners() {
		addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				ComponentPanel.this.widgetDisposed(e);
			}

		});
	}

	private void widgetDisposed(DisposeEvent e) {

	}

	public final void scrollComponentsHorizontal()
  {
    ScrollBar hBar = getHorizontalBar();
    DataDisplayPanel p = null;
    AbstractDataDisplay a = null;
    int val = hBar.getSelection();
    int thumb = hBar.getThumb();
    int r_st = val;
    int r_en = val + thumb;
    if (r_en == hBar.getMaximum()) r_en++;
    hBar.setPageIncrement(hBar.getThumb()/2);

    mHorizontalRuler.setDataRange(r_st,r_en-r_st,m_data.rofs,m_data.rinc,m_data.runit);

    for (int i = 0; i < mDataDisplayPanelList.size(); i++)
    {
      p = (DataDisplayPanel)mDataDisplayPanelList.get(i);
      if (p != null)
      {
        a = p.getDataDisplay();
        a.setRecDetail(val,val + thumb);
      }
    }
    fireDisplayEvent(new HdetailEvent(null,val,val+thumb));
    update();
  }

	private final void setHorizontalRuler()
  {
	  boolean bAll3D = true;
    DataDisplayPanel p = null;
    de.tucottbus.kt.jlab.datadisplays.widgets.displays.AbstractDataDisplay d = null;
    for (int i = 0; i < mDataDisplayPanelList.size(); i++)
    {
      p = (DataDisplayPanel)mDataDisplayPanelList.get(i);
      if (p != null)
      {
        try
        {
          d = p.getDataDisplay();
          d.setHorizontalRuler(mHorizontalRuler);
          if (!(d instanceof ThreeDDisplay)) bAll3D = false; 
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }
    mHorizontalRuler.setEnabled(!bAll3D);
  }

	public LinkedList<DataDisplayPanel> getDataDisplayPanelList()
	{
	  return mDataDisplayPanelList; 
	}

	public JlData getJlData()
	{
	  DataDisplayPanel iPnl = (DataDisplayPanel)mDataDisplayPanelList.getFirst();
	  if (iPnl==null) return null;
	  return iPnl.getDataDisplay().getJlData();
	}
}
