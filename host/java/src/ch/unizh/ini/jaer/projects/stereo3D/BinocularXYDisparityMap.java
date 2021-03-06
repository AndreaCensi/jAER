/*
 * BinocularXYDisparityMap.java
 * 
 * display BinocularXYDisparityEvents in a disparity map
 *
 * Paul Rogister, Created on FEbruary, 2009
 *
 */


package ch.unizh.ini.jaer.projects.stereo3D;
import net.sf.jaer.chip.*;
import net.sf.jaer.eventprocessing.EventFilter2D;
import net.sf.jaer.event.*;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.graphics.*;
import com.sun.opengl.util.*;
//import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.InputEvent;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import javax.imageio.*;
import java.awt.image.*;
import java.nio.ByteBuffer;

import java.io.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.*;
import javax.media.opengl.glu.GLU;


import java.text.*;

/**
 * BinocularXYDisparityMap:
 * Open two Display Frame that show the accumulated value of the pixels activation
 * one for the left and one for the right retina when mounted on the stereoboard
 *
 * @author rogister
 */
public class BinocularXYDisparityMap extends EventFilter2D implements FrameAnnotater, Observer /*, PreferenceChangeListener*/ {
    
    
    protected final int RIGHT = 1;
    protected final int LEFT = 0;
   
    int maxGCs = 100;
    
    protected AEChip chip;
    private AEChipRenderer renderer;
    
    private boolean logDataEnabled=false;
    private PrintStream logStream=null;
    
    // Parameters appearing in the GUI
    
 
       
    private int intensityZoom = getPrefs().getInt("BinocularXYDisparityMap.intensityZoom",3);
    {setPropertyTooltip("intensityZoom","zoom for display window");}

     private int disparityScaling = getPrefs().getInt("BinocularXYDisparityMap.disparityScaling",32);
    {setPropertyTooltip("disparityScaling","disparity display scaling factor");}

   private int decayTimeLimit=getPrefs().getInt("BinocularXYDisparityMap.decayTimeLimit",10000);
    {setPropertyTooltip("decayTimeLimit","[microsec (us)] for decaying accumulated events");}


     private int midpoint=getPrefs().getInt("BinocularXYDisparityMap.midpoint",40);
    {setPropertyTooltip("midpoint","color scale midpoint");}

     private int maxD = getPrefs().getInt("BinocularXYDisparityMap.maxD",0);
    {setPropertyTooltip("maxD","color scale max point");}

    private boolean showMatch = getPrefs().getBoolean("BinocularXYDisparityMap.showMatch",false);
    private boolean showColor = getPrefs().getBoolean("BinocularXYDisparityMap.showColor",false);


     private boolean saveAll = getPrefs().getBoolean("BinocularXYDisparityMap.saveAll",false);

   private boolean showAxes = getPrefs().getBoolean("BinocularXYDisparityMap.showAxes",true);


    // do not forget to add a set and a getString/is method for each new parameter, at the end of this .java file
    
    
    // global variables
    
    private int retinaSize=128;//getPrefs().getInt("GravityCentersImageDumper.retinaSize",128);
   // float[][] colorScale = new float[retinaSize][3]; // 128 rgb values
    float[][] colorScale = new float[160][3]; // 160 rgb values to include diagonal disparity vectors
    boolean firstRun = true;
//    boolean windowDeleted = true;

   
 //   boolean firstRun = true;
   
    boolean logAccPNG=false;
    boolean accLogged=false;
    protected int currentTime;
   
    public static DateFormat loggingFilenameDateFormat=new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ssZ");
   
    float[][] disparityPoints = new float[retinaSize][retinaSize];
    int[][] disparityTimes = new int[retinaSize][retinaSize];

    BinocularXYDisparityEvent[][] disparityEvents = new BinocularXYDisparityEvent[retinaSize][retinaSize];


    int highlightX = -1;
    int highlightY = -1;

    int saveTime = 100000;
    int saveCounter = 0;
    int nbMaps = 0;
    /** Creates a new instance of GravityCentersImageDumper */
    public BinocularXYDisparityMap(AEChip chip) {
        super(chip);
        this.chip=chip;
        renderer=(AEChipRenderer)chip.getRenderer();        
       // initFilter();
        chip.addObserver(this);       
    }
    
    public void initFilter() {
        
    }
            
    private void initDefault(String key, String value){
        if(getPrefs().get(key,null)==null) getPrefs().put(key,value);
    }
     
     // the method that actually does the tracking
    synchronized private void track(EventPacket<BinocularDisparityEvent> ae){
                      
        int n=ae.getSize();
        if(n==0) return;
       
        currentTime = ae.getLastTimestamp();

      //  if(firstRun){
      ///      firstRun = false;
      //      createColorScale();
      //  }
        BinocularXYDisparityEvent eb = null;

        for(BinocularDisparityEvent e:ae){
          int eye = e.eye == BinocularEvent.Eye.LEFT ? 0 : 1;
            if (eye == LEFT) {
                if (e.disparity != 0) {
                    if (e instanceof BinocularXYDisparityEvent) {
                        eb = (BinocularXYDisparityEvent)e;
                        disparityPoints[e.x][e.y] = eb.getType();
                        disparityEvents[e.x][e.y] = eb;
                    } else {
                        disparityPoints[e.x][e.y] = e.disparity;
                    }
                    //   disparityPoints[e.x][e.y] = Math.round((float)disparityPoints[e.x][e.y]*0.9 + (float)e.d*0.1);
                    disparityTimes[e.x][e.y] = e.timestamp;
                }
            }
        }
              
    }

    void createColorScale(){
       // int midpoint = 40;
        for (int i=0;i<retinaSize;i++){
            if(i<=midpoint){
                colorScale[i][0] = 0.0f;
                colorScale[i][1] = (float)(i)/(float)midpoint;
                colorScale[i][2] = (float)(midpoint-i)/(float)midpoint;
            } else {
                colorScale[i][0] = (float)(i-midpoint)/(float)(maxD-midpoint);
                colorScale[i][1] = (float)(maxD-(i-midpoint))/(float)(maxD-midpoint);
                colorScale[i][2] = 0.0f;

            }

        }
    }
    
    public String toString(){
        String s="BinocularXYDisparityMap";
        return s;
    }
    
    
    public Object getFilterState() {
        return null;
    }
    
    private boolean isGeneratingFilter() {
        return false;
    }

    synchronized public void resetFilter() {
        
      //  System.out.println ("BinocularXYDisparityMap resetFilter ");
        logAccPNG = false;
        accLogged = false;
        disparityPoints = new float[retinaSize][retinaSize];
        disparityEvents = new BinocularXYDisparityEvent[retinaSize][retinaSize];

        disparityTimes = new int[retinaSize][retinaSize];
        saveCounter = 0;
        nbMaps=0;

        createColorScale();
    }
    
    

    public EventPacket filterPacket(EventPacket in) {
        if(in==null) return null;
        if(!filterEnabled) return in;
        if(enclosedFilter!=null) in=enclosedFilter.filterPacket(in);
        if(!(in.getEventPrototype() instanceof BinocularDisparityEvent)) {
             System.out.println("not a disparity event!");
            return in;
        }

        int tempcurrentTime = in.getLastTimestamp();
        if(tempcurrentTime!=0){
            currentTime = tempcurrentTime; // to avoid wrong timing to corrupt data
            if(saveCounter==0){
                saveCounter = currentTime;
            }
        }

        
        if(accLogged&&logAccPNG){
            logAccPNG=false;
            accLogged=false;
           
            writePNG(image3DOpenGL,"DisparityMap");
            
        }
                        
        checkDisplayFrame();
                
        track(in);
                        
        displayCanvas.repaint();

        if(saveAll){
            if(currentTime-saveCounter>saveTime){
                saveCounter = currentTime;
                nbMaps++;
                // save disparity map
                String filename = new String("dmap"+nbMaps+".txt");
                saveData(filename);
            }
        }



        return in;
    }
    
   
    
     protected void writePNG( BufferedImage Image3D, String label){
        try {
            String dateString = loggingFilenameDateFormat.format(new Date());
            String filename = "disparitymap-" + label + "-" + dateString + ".png";

            String homeDir = System.getProperty("user.dir");
            if (Image3D != null) {
                ImageIO.write(Image3D, "png", new File(homeDir, filename));
                System.out.println("logged: " + homeDir + " " + filename);
            } else {
                System.out.println("null: not logged: " + homeDir + " " + filename);
            }



        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
     
   
     
     
     
     
     

    public void update(Observable o, Object arg) {
        initFilter();
    }
    

       protected void saveData( ) {
      JFileChooser fileChooser=new JFileChooser();

        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setDialogTitle("Save results to:");
        fileChooser.setMultiSelectionEnabled(false);

        int retValue=fileChooser.showSaveDialog(displayFrame);
        if(retValue==JFileChooser.APPROVE_OPTION){
            try{
                File file=fileChooser.getSelectedFile();
                String suffix="";
                if(!file.getName().endsWith(".txt")) suffix=".txt";
                file=new File(file.getPath()+suffix);
                saveToFile(file);

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

         protected void saveData( String filename ) {

            try{

                File file=new File(filename);
                saveToFile(file);

            }catch(Exception e){
                e.printStackTrace();
            }

    }

     protected void saveToFile(File file) {

        PrintStream logStream = null;
        try {
            logStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));

               for (int i = 0; i<disparityPoints.length; i++){
                    for (int j = 0; j<disparityPoints[i].length; j++){
                        float f = disparityPoints[j][i];
                        logStream.print(f+" ");
                    }
                    logStream.println("");
               }


            logStream.flush();
            logStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   
    // show 2D view
    
    void checkDisplayFrame(){
        if(displayFrame==null) createDisplayFrame();
    }
    
    JFrame displayFrame=null;
    GLCanvas displayCanvas=null;
    BufferedImage image3DOpenGL=null;
    
    private static final GLU glu = new GLU();
    
  
//    GLUT glut=null;
    void createDisplayFrame(  ){
        displayFrame=new JFrame("Display Disparity Map");
        displayFrame.setPreferredSize(new Dimension(retinaSize*intensityZoom,retinaSize*intensityZoom));
        displayFrame.setSize(new Dimension(retinaSize*intensityZoom,retinaSize*intensityZoom));
        displayFrame.setMaximumSize(new Dimension(retinaSize*intensityZoom,retinaSize*intensityZoom));
       
        displayCanvas=new GLCanvas();
        
        displayCanvas.addKeyListener( new KeyListener(){
            /** Handle the key typed event from the text field. */
            public void keyTyped(KeyEvent e) {
                
            }
            
            public void keyPressed(KeyEvent e) {
                
            }
            
            public void keyReleased(KeyEvent e) {
               
                
        
                
                if(e.getKeyCode()==KeyEvent.VK_L){
                    // log all grativity points to a png image
                    logAccPNG = true;
                     System.out.println("DisplayFrame:  logAccPNG: "+logAccPNG);
                    //leftdisplayCanvas.display();
                    
                }

                if(e.getKeyCode()==KeyEvent.VK_M){
                   showMatch = !showMatch;

                }
                if (e.getKeyCode() == KeyEvent.VK_P) {
                    // mustSave = true;
                    saveData();
                }
               
            
            }
        });
        
        displayCanvas.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                
                int dx=displayCanvas.getWidth()-1;
                int dy=displayCanvas.getHeight()-1;
                
                // 3 is window's border width
               
                //int x = (int)((evt.getX()-3)  / intensityZoom)+1;
             //   int x = (int)((evt.getX()-2)  / intensityZoom);
                int x = (int)((evt.getX())  / intensityZoom);
                int y = (int)((dy-evt.getY())  / intensityZoom);
                
               
             //   System.out.println("got x:"+x+" y:"+y+" from ["+evt.getX()+","+evt.getY()+"]");


                if (evt.getButton()==1){
                    // if distinguishing button is important ...
                    highlightX = x;
                    highlightY = y;
                    float d = -1;
//                      if(currentTime-disparityTimes[x][y]<=decayTimeLimit){
                    if(x>0&&x<128&&y>0&&y<128){
                        d = disparityPoints[x][y];
//
                        System.out.println("disparity["+x+"]["+y+"] ="+d);

                        if(disparityEvents[x][y]!=null){
                        System.out.println("disparity["+x+"]["+y+"] dx="+disparityEvents[x][y].xdisparity
                                +" dy="+disparityEvents[x][y].ydisparity);
                        System.out.println("disparity["+x+"]["+y+"] time="+disparityEvents[x][y].timestamp
                                +" mtime="+disparityEvents[x][y].matchtime);
                        }
                    }
//                    }

                } else {
                      highlightX = -1;
                      highlightY = -1;
                }
                       
                displayCanvas.display();
                
            }
            public void mouseReleased(MouseEvent e){
                
                
            }
        });
        
        
        displayCanvas.addGLEventListener(new GLEventListener(){
            public void init(GLAutoDrawable drawable) {
            }
     
            private void drawAllPoints(float[][] points, GL gl) {
                
                    //    System.out.println("1. display drawEventPoints  time: "+currentTime+" length: "+eventPoints.length);

                    gl.glClearColor(0, 0, 0, 0);
                  //  System.out.println("drawAllPoints: maxDepth: "+maxDepth+" ,minDepth : "+minDepth);
                    
                  for (int i = 0; i < retinaSize; i++) {
                    for (int j = 0; j < retinaSize; j++) {
                            float d = points[i][j];
                            
                            // d ranges from min -127 to max +127 but unlikely
                            //
                            float f = 0.5f + d/disparityScaling;
                            // ^ to putString zero at 0.5 so both negative and positive disparity appears
                            // divide by a factor to scale to [0,1]

                            if(currentTime-disparityTimes[i][j]>decayTimeLimit){
                                f = 0.5f;
                            }
                            if(i==highlightX&&j==highlightY){
                                gl.glColor3f(1, 1, 0.7f);
                            } else {

                                gl.glColor3f(f,f,f);

                            }
                            gl.glRectf(i * intensityZoom, j * intensityZoom, (i + 1) * intensityZoom, (j + 1) * intensityZoom);
                            

                   if (showAxes) {
                        gl.glColor3f(1, 0, 0);
                        gl.glRectf(0 * intensityZoom, (0) * intensityZoom, (10) * intensityZoom, (1) * intensityZoom);
                        gl.glRectf(0 * intensityZoom, (0) * intensityZoom, (1) * intensityZoom, (10) * intensityZoom);
                    }
                    }
                  }


                
            }


            private void drawAllPointsColor(float[][] points, GL gl) {

                    //    System.out.println("1. display drawEventPoints  time: "+currentTime+" length: "+eventPoints.length);

                    gl.glClearColor(0, 0, 0, 0);
                  //  System.out.println("drawAllPoints: maxDepth: "+maxDepth+" ,minDepth : "+minDepth);

                  for (int i = 0; i < retinaSize; i++) {
                    for (int j = 0; j < retinaSize; j++) {
                            int d = (int)points[i][j];

                            // d ranges from min -127 to max +127 but unlikely
                            //

                            if(currentTime-disparityTimes[i][j]>decayTimeLimit){
                                d = 0;
                            }
                            if(i==highlightX&&j==highlightY){
                                gl.glColor3f(1, 1, 0.7f);
                            } else {
                                if(d<0)d =-d;
                                gl.glColor3f(colorScale[d][0], colorScale[d][1], colorScale[d][2]);
                                // gl.glColor3f(f,f,f);

                            }
                            gl.glRectf(i * intensityZoom, j * intensityZoom, (i + 1) * intensityZoom, (j + 1) * intensityZoom);


                   if (showAxes) {
                        gl.glColor3f(1, 0, 0);
                        gl.glRectf(0 * intensityZoom, (0) * intensityZoom, (10) * intensityZoom, (1) * intensityZoom);
                        gl.glRectf(0 * intensityZoom, (0) * intensityZoom, (1) * intensityZoom, (10) * intensityZoom);
                    }
                    }
                  }
            }


            private void drawAllPointsColorMatch(float[][] points, GL gl) {

                    //    System.out.println("1. display drawEventPoints  time: "+currentTime+" length: "+eventPoints.length);

                    gl.glClearColor(0, 0, 0, 0);
                  //  System.out.println("drawAllPoints: maxDepth: "+maxDepth+" ,minDepth : "+minDepth);

                  for (int i = 0; i < retinaSize; i++) {
                    for (int j = 0; j < retinaSize; j++) {
                            int d = (int)points[i][j];

                            // d ranges from min -127 to max +127 but unlikely
                            //

                            if(currentTime-disparityTimes[i][j]>decayTimeLimit){
                                d = 0;
                            }
                            if(i==highlightX&&j==highlightY){
                                gl.glColor3f(1, 1, 0.7f);
                            } else {
                                if(d<0)d =-d;
                                gl.glColor3f(colorScale[d][0], colorScale[d][1], colorScale[d][2]);
                                // gl.glColor3f(f,f,f);

                            }
                            gl.glRectf(i * intensityZoom, j * intensityZoom, (i + 1) * intensityZoom, (j + 1) * intensityZoom);

                        if (d != 0) {
                            if (i == highlightX && j == highlightY) {
                                gl.glColor3f(1, 1, 0.7f);
                            } else {
                                if (d < 0) {
                                    d = -d;
                                }
                                gl.glColor3f(0,0,0);


                            }
                            int xm = i+disparityEvents[i][j].xdisparity;
                            int ym = j+disparityEvents[i][j].ydisparity;
                            gl.glRectf(xm * intensityZoom, ym * intensityZoom, (xm + 1) * intensityZoom, (ym + 1) * intensityZoom);

                        }

                   if (showAxes) {
                        gl.glColor3f(1, 0, 0);
                        gl.glRectf(0 * intensityZoom, (0) * intensityZoom, (10) * intensityZoom, (1) * intensityZoom);
                        gl.glRectf(0 * intensityZoom, (0) * intensityZoom, (1) * intensityZoom, (10) * intensityZoom);
                    }
                    }
                  }



            }





            private void drawColorScale(GL gl) {

                    //    System.out.println("1. display drawEventPoints  time: "+currentTime+" length: "+eventPoints.length);

                    gl.glClearColor(0, 0, 0, 0);
                  //  System.out.println("drawAllPoints: maxDepth: "+maxDepth+" ,minDepth : "+minDepth);

                  for (int i = 0; i < retinaSize; i++) {


                            gl.glColor3f(colorScale[i][0], colorScale[i][1], colorScale[i][2]);

                            gl.glRectf(129 * intensityZoom, i * intensityZoom, (139) * intensityZoom, (i+1) * intensityZoom);

                  }
            }


            private void drawMatchedPoints(float[][] points, GL gl) {

                //    System.out.println("1. display drawEventPoints  time: "+currentTime+" length: "+eventPoints.length);

                gl.glClearColor(0, 0, 0, 0);
                //  System.out.println("drawAllPoints: maxDepth: "+maxDepth+" ,minDepth : "+minDepth);

                for (int i = 0; i < retinaSize; i++) {
                    for (int j = 0; j < retinaSize; j++) {
                        float d = points[i][j];
                        // d ranges from min -128 to max +128 but unlikey

                        if ((currentTime - disparityTimes[i][j] < decayTimeLimit)&&disparityTimes[i][j]!=0) {
                            gl.glColor3f(0, 0, 1);

                            gl.glRectf(i * intensityZoom, j * intensityZoom, (i + 1) * intensityZoom, (j + 1) * intensityZoom);

                            gl.glColor3f(1, 0, 0);

                            gl.glRectf((i + d) * intensityZoom, j * intensityZoom, (i + d + 1) * intensityZoom, (j + 1) * intensityZoom);

                        } else {
                            gl.glColor3f(0.5f, 0.5f, 0.5f);
                            gl.glRectf(i * intensityZoom, j * intensityZoom, (i + 1) * intensityZoom, (j + 1) * intensityZoom);
                            gl.glRectf((i + d) * intensityZoom, j * intensityZoom, (i + d + 1) * intensityZoom, (j + 1) * intensityZoom);
                        }
                    }
                }
            }

            void grabImage(GLAutoDrawable d) {

                System.out.println("grab image :  logAccPNG: "+logAccPNG);
          
        GL gl = d.getGL();
        int width = 128; //d.getWidth();
        int height = 128; //d.getHeight();
        
        // Allocate a buffer for the pixels
        ByteBuffer rgbData = BufferUtil.newByteBuffer(width * height * 3);
        
        // Set up the OpenGL state.
        gl.glReadBuffer(GL.GL_FRONT);
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
        
        // Read the pixels into the ByteBuffer
        gl.glReadPixels(0,
                0,
                width,
                height,
                GL.GL_RGB,
                GL.GL_UNSIGNED_BYTE,
                rgbData);
        
        // Allocate space for the converted pixels
        int[] pixelInts = new int[width * height];
        
        // Convert RGB bytes to ARGB ints with no transparency. Flip
        // imageOpenGL vertically by reading the rows of pixels in the byte
        // buffer in reverse - (0,0) is at bottom left in OpenGL.
        
        int p = width * height * 3; // Points to first byte (red) in each row.
        int q;                  	// Index into ByteBuffer
        int i = 0;                 // Index into target int[]
        int bytesPerRow = width*3; // Number of bytes in each row
        
        for (int row = height - 1; row >= 0; row--) {
            p = row * bytesPerRow;
            q = p;
            for (int col = 0; col < width; col++) {
                int iR = rgbData.get(q++);
                int iG = rgbData.get(q++);
                int iB = rgbData.get(q++);
                
                pixelInts[i++] = ( (0xFF000000)
                | ((iR & 0xFF) << 16)
                | ((iG & 0xFF) << 8)
                | (iB & 0xFF) );
            }
        }
        
        // Set the data for the BufferedImage
        if(image3DOpenGL==null || image3DOpenGL.getWidth()!=width || image3DOpenGL.getHeight()!=height) {
            image3DOpenGL = new BufferedImage(width,height, BufferedImage.TYPE_INT_ARGB);
        }
        image3DOpenGL.setRGB(0, 0, width, height, pixelInts, 0, width);
    }
  
          
            
            synchronized public void display(GLAutoDrawable drawable) {
                
                GL gl=drawable.getGL();
                gl.glLoadIdentity();
                //gl.glScalef(drawable.getWidth()/2000,drawable.getHeight()/180,1);//dist to gc, orientation?
                gl.glClearColor(0,0,0,0);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
                int font = GLUT.BITMAP_HELVETICA_12;
 
                
                
                synchronized (disparityPoints) {
                    if(showMatch){
                         //drawMatchedPoints(disparityPoints,gl);
                        drawAllPointsColorMatch(disparityPoints,gl);
                    } else if(showColor){
                        drawAllPointsColor(disparityPoints,gl);
                    } else {
                          drawAllPoints(disparityPoints,gl);

                    }
                    drawColorScale(gl);
                }
                

                
                if(logAccPNG){
                    grabImage(drawable);
                    accLogged=true;
                }
                
                int error=gl.glGetError();
                if(error!=GL.GL_NO_ERROR){
                    // if(glu==null) glu=new GLU();
                    //log.warning("GL error number "+error+" "+glu.gluErrorString(error));
                }
            }
            
            synchronized public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
                GL gl=drawable.getGL();
                final int B=10;
                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glLoadIdentity(); // very important to load identity matrix here so this works after first resize!!!
                gl.glOrtho(0,drawable.getWidth(),0,drawable.getHeight(),0,1);
               // gl.glOrtho(-B,drawable.getWidth()+B,-B,drawable.getHeight()+B,10000,-10000);
                gl.glMatrixMode(GL.GL_MODELVIEW);
                gl.glViewport(0,0,width,height);
            }
            
            public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
            }
        });
        displayFrame.getContentPane().add(displayCanvas);
        displayFrame.pack();
        displayFrame.setVisible(true);
    }
    

  
  
 
   
    
    /***********************************************************************************
     * // drawing on player window
     ********************************************************************************/
    
    public void annotate(Graphics2D g) {
    }
    
    protected void drawBoxCentered(GL gl, int x, int y, int sx, int sy){
        gl.glBegin(GL.GL_LINE_LOOP);
        {
            gl.glVertex2i(x-sx,y-sy);
            gl.glVertex2i(x+sx,y-sy);
            gl.glVertex2i(x+sx,y+sy);
            gl.glVertex2i(x-sx,y+sy);
        }
        gl.glEnd();
    }
    
    protected void drawBox(GL gl, int x, int x2, int y, int y2){
        gl.glBegin(GL.GL_LINE_LOOP);
        {
            gl.glVertex2i(x,y);
            gl.glVertex2i(x2,y);
            gl.glVertex2i(x2,y2);
            gl.glVertex2i(x,y2);
        }
        gl.glEnd();
    }
    
    synchronized public void annotate(GLAutoDrawable drawable) {
        final float LINE_WIDTH=5f; // in pixels
        if(!isFilterEnabled()) return;
        
        
        GL gl=drawable.getGL(); // when we getString this we are already set up with scale 1=1 pixel, at LL corner
        if(gl==null){
            log.warning("null GL in GravityCentersImageDumper.annotate");
            return;
        }
        float[] rgb=new float[4];
        gl.glPushMatrix();
        try{
      
            // like draw door
            //    gl.glColor3f(0,1,0);
            //    drawBox(gl,door_xa,door_xb,door_ya,door_yb);

      
        }catch(java.util.ConcurrentModificationException e){
            // this is in case cluster list is modified by real time filter during rendering of clusters
            log.warning(e.getMessage());
        }
        gl.glPopMatrix();
    }
    
//    void drawGLCluster(int x1, int y1, int x2, int y2)
    
    /** annotate the rendered retina frame to show locations of clusters */
    synchronized public void annotate(float[][][] frame) {
        if(!isFilterEnabled()) return;
        // disable for now TODO
        if(chip.getCanvas().isOpenGLEnabled()) return; // done by open gl annotator
        
    }


    public synchronized boolean isLogDataEnabled() {
        return logDataEnabled;
    }
    
    public synchronized void setLogDataEnabled(boolean logDataEnabled) {
        this.logDataEnabled = logDataEnabled;
        if(!logDataEnabled) {
            logStream.flush();
            logStream.close();
            logStream=null;
        }else{
            try{
                logStream=new PrintStream(new BufferedOutputStream(new FileOutputStream(new File("PawTrackerData.txt"))));
                logStream.println("# clusterNumber lasttimestamp x y averageEventDistance");
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    
    
   
   
            
    public void setIntensityZoom(int intensityZoom) {
        this.intensityZoom = intensityZoom;
        
        getPrefs().putInt("BinocularXYDisparityMap.intensityZoom",intensityZoom);
    }
    
    public int getIntensityZoom() {
        return intensityZoom;
    }
    

    public void setSaveAll(boolean saveAll){
        this.saveAll = saveAll;

        getPrefs().putBoolean("BinocularXYDisparityMap.saveAll",saveAll);
    }
    public boolean isSaveAll(){
        return saveAll;
    }
    
    public void setShowAxes(boolean showAxes){
        this.showAxes = showAxes;
        
        getPrefs().putBoolean("BinocularXYDisparityMap.showAxes",showAxes);
    }
    public boolean isShowAxes(){
        return showAxes;
    }

     public void setShowColor(boolean showColor){
        this.showColor = showColor;

        getPrefs().putBoolean("BinocularXYDisparityMap.showColor",showColor);
    }
    public boolean isShowColor(){
        return showColor;
    }

   
    public void setDisparityScaling(int disparityScaling) {
        this.disparityScaling = disparityScaling;

        getPrefs().putInt("BinocularXYDisparityMap.disparityScaling",disparityScaling);
    }
    public int getDisparityScaling() {
        return disparityScaling;
    }

    public void setDecayTimeLimit(int decayTimeLimit) {
        this.decayTimeLimit = decayTimeLimit;

        getPrefs().putInt("BinocularXYDisparityMap.decayTimeLimit",decayTimeLimit);
    }
    public int getDecayTimeLimit() {
        return decayTimeLimit;
    }

      

    public void setMidpoint(int midpoint) {
        this.midpoint = midpoint;

        getPrefs().putInt("BinocularXYDisparityMap.midpoint",midpoint);
    }
    public int getMidpoint() {
        return midpoint;
    }

   public void setMaxD(int maxD) {
        this.maxD = maxD;

        getPrefs().putInt("BinocularXYDisparityMap.maxD",maxD);
    }
    public int getMaxD() {
        return maxD;
    }

}
