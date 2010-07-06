/*
 * TrackdefineFilter.java
 *
 * Created on July 1, 2010, 01:05 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package ch.unizh.ini.jaer.projects.virtualslotcar;
import net.sf.jaer.eventprocessing.filter.*;
import java.awt.*;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import net.sf.jaer.chip.*;
import net.sf.jaer.event.*;
import net.sf.jaer.eventprocessing.EventFilter2D;
import net.sf.jaer.graphics.ChipCanvas;
import net.sf.jaer.graphics.FrameAnnotater;
import java.awt.Graphics2D;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.*;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.awt.geom.Point2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
/**
 * An AE filter that first creates a histogram of incoming pixels, and then lets the user define
 * the detected track.
 *
 * @author Michael Pfeiffer
 */

/**
 * A utility class for points in a priority queue, ordered by their distance to
 * other points in the queue.
 */
class TrackPoint implements Comparable<TrackPoint> {
    public int x;
    public int y;
    public float minDistance;

    public TrackPoint(int x, int y, float minDistance) {
        this.x = x;
        this.y = y;
        this.minDistance = minDistance;
    }

    public int compareTo(TrackPoint p) {
        if (p.minDistance < minDistance)
            return +1;
        else if (p.minDistance == minDistance)
            return 0;
        else
            return -1;
    }
}



public class TrackdefineFilter extends EventFilter2D implements FrameAnnotater,Observer,MouseListener,MouseMotionListener{
    public static String getDescription (){
        return "Detects a track from incoming pixels and user input";
    }
    // Variables declared in XYTypeFilter
    public short x = 0, y = 0;
    public byte type = 0;
    private GLCanvas glCanvas;
    private ChipCanvas canvas;
    private Point currentMousePoint = null;
    private int currentPointIdx;

    // Start here with new variable declarations

    // Dimensions of the pixel array
    int numX;
    int numY;

    // Histogram data
    private float[][] pixData = null;

    // Total sum of histogram points
    float totalSum;

    // Draw histogram in annotate or not
    private boolean drawHist = prefs().getBoolean("TrackdefineFilter.drawHist", false);

    // Threshold for accepting points as track points
    private float histThresh = prefs().getFloat("TrackdefineFilter.histThresh",0.0001f);

    // Size of erosion mask
    private int erosionSize = prefs().getInt("TrackdefineFilter.erosionSize", 1);

    // Minimum distance between track points
    private float minDistance = prefs().getFloat("TrackdefineFilter.minDistance", 10.0f);

    // Maximum distance between track points
    private float maxDistance = prefs().getFloat("TrackdefineFilter.maxDistance", 50.0f);

    // Interpolation step size for spline curve
    private float stepSize = prefs().getFloat("TrackdefineFilter.stepSize", 0.05f);

    // Whether to draw smooth interpolated track
    private boolean drawSmooth = prefs().getBoolean("TrackdefineFilter.drawSmooth", false);

    // Delete or move track points on mouse click
    private boolean deleteOnClick = prefs().getBoolean("TrackdefineFilter.deleteOnClick", false);

    // Tolerance for mouse clicks
    private float clickTolerance = prefs().getFloat("TrackdefineFilter.clickTolerance", 5.0f);

    private int counter = 0;

    // List of extracted track points
    private LinkedList<Point2D> extractPoints;

    // The extracted slotcar track
    private SlotcarTrack extractedTrack;

    // Smooth display points of the interpolated track
    private LinkedList<Point2D> smoothPoints;

    // Display extracted Points
    private boolean displayTrack = prefs().getBoolean("TrackdefineFilter.displayTrack", true);

    public TrackdefineFilter (AEChip chip){
        super(chip);

        resetFilter();
        if ( chip.getCanvas() != null && chip.getCanvas().getCanvas() != null ){
            canvas = chip.getCanvas();
            glCanvas = (GLCanvas)chip.getCanvas().getCanvas();
        }
        // final String y = "y", x = "x", t = "type";
        setPropertyTooltip("drawHist", "Draw Histogram");
        setPropertyTooltip("histThresh", "Threshold of histogram points to display");
        setPropertyTooltip("drawSmooth", "Draw smooth track or only points");
        setPropertyTooltip("displayTrack", "Display extracted Track Points");
        setPropertyTooltip("minDistance", "Minimum distance between extracted track points");
        setPropertyTooltip("minDistance", "Maximum distance between extracted track points");
        setPropertyTooltip("stepSize", "Interpolation step size for spline curve");
        setPropertyTooltip("deleteOnClick", "Delete track points on mouse click (otherwise move)");
        setPropertyTooltip("clickTolerance", "Tolerance for mouse clicks (deleting, dragging)");

        // New in TrackdefineFilter
        // Initialize histogram
        // numX = chip.getSizeX();
        // numY = chip.getSizeY();
        numX = numY = -1;

        currentPointIdx = -1;

        resetFilter();

        extractPoints = null;
        extractedTrack = null;
        smoothPoints = null;
    }

    /**
     * Constructs the histogram
     *@param in input events can be null or empty.
     *@return the processed events, may be fewer in number. filtering may occur in place in the in packet.
     */
    synchronized public EventPacket filterPacket (EventPacket in){
        if ( enclosedFilter != null ){
            in = enclosedFilter.filterPacket(in);
        }
        int i;

        int n = in.getSize();
        if ( n == 0 ){
            return in;
        }
        checkOutputPacketEventType(in);
        OutputEventIterator outItr = out.outputIterator();
        
        // for each event only write it to the tmp buffers if it matches
        for ( Object obj:in ){
            BasicEvent e = (BasicEvent)obj;
            if ((e.x >= 0) && (e.y >= 0) &&
                    (e.x <numX) && (e.y < numY)) {
                // Increase histogram count
                pixData[e.y][e.x] += 1.0f;
                totalSum += 1.0f;
            }
     //       if ((pixData[e.y][e.x] / totalSum) > histThresh)
            pass(outItr,e);
        }

        return out;
    }

    private void pass (OutputEventIterator outItr,BasicEvent e){
        outItr.nextOutput().copyFrom(e);
    }

    private void pass (OutputEventIterator outItr,TypedEvent te){
        outItr.nextOutput().copyFrom(te);
    }

    synchronized public void resetFilter (){
//        startX=0; endX=chip.getSizeX();
//        startY=0; endY=chip.getSizeY();
//        startType=0; endType=chip.getNumCellTypes();

        int oldNumX = numX;
        int oldNumY = numY;
        numX = chip.getSizeX();
        numY = chip.getSizeY();

        if ((oldNumX != numX) || (oldNumY != numY)) {
            pixData = new float[numY][numX];

            for (int i=0; i<numY; i++)
                for (int j=0; j<numX; j++)
                    pixData[i][j]=0.0f;

            totalSum = 0.0f;
        }
    }

    public void initFilter (){
        resetFilter();
    }

    private int clip (int val,int limit){
        if ( val > limit && limit != 0 ){
            return limit;
        } else if ( val < 0 ){
            return 0;
        }
        return val;
    }


    // Morphological erosion of track histogram
    private boolean[][] erosion() {
        boolean[][] bitmap = new boolean[numY][numX];
        int erSize = erosionSize;
        if (erSize <= 0) {
            // Return original image
            for (int i=0; i<numY; i++) {
                for (int j=0; j<numX; j++) {
                    if ((pixData[i][j] / totalSum) > histThresh) {
                        bitmap[i][j] = true;
                    } else
                        bitmap[i][j] = false;
                }
            }
            return bitmap;
        }


        for (int i=0; i<numY; i++) {
            for (int j=0; j<numX; j++) {
                boolean keep = true;
                for (int k=-erSize; k<=erSize; k++) {
                    for (int l=-erSize; l<=erSize; l++) {
                        int pixY = clip(i+k,numY);
                        int pixX = clip(j+l,numX);
                        if ((pixData[pixY][pixX] / totalSum) < histThresh) {
                            keep = false;
                            break;
                        }
                    }
                    if (keep == false)
                        break;
                }
                bitmap[i][j] = keep;
            }
        }

        return bitmap;
    }


    // Draws the histogram (only points above threshold)
    private void drawHistogram(GL gl) {
        // System.out.println("Drawing histogram..." + gl);
        boolean[][] bitmap = erosion();
        gl.glColor3f(1.0f,1.0f,0);
        gl.glBegin(gl.GL_POINTS);
        for (int i=0; i<numY; i++) {
            for (int j=0; j<numX; j++) {
                // if ((pixData[i][j] / totalSum) > histThresh) {
                if (bitmap[i][j]) {
                    gl.glVertex2i(j, i);
                    // gl.glRecti(i, j, i+1, j+1);
                }
            }
        }
        gl.glEnd();

    }

    private void histStatistics() {
        float maxH = 0;
        int count = 0;
        for (int i=0; i<numY; i++) {
            for (int j=0; j<numX; j++) {
                float cur = pixData[i][j] / totalSum;
                if (cur > maxH)
                    maxH = cur;
                if (cur > histThresh)
                    count++;
            }
        }

        System.out.println("Max: " + maxH + " / Count: " + count);
    }


    /** Displays the extracted track points */
    private void drawExtractedTrack(GL gl) {
        if (extractedTrack != null) {

            // Draw extracted points
            gl.glColor3d(1.0f, 0.0f, 1.0f);
            gl.glPointSize(5.0f);
            gl.glBegin(gl.GL_POINTS);
            for (Point2D p:extractPoints) {
                gl.glVertex2d(p.getX(), p.getY());
            }
            gl.glEnd();

            gl.glPointSize(1.0f);
            gl.glBegin(gl.GL_LINE_STRIP);
            for (Point2D p:extractPoints) {
                gl.glVertex2d(p.getX(), p.getY());
            }
            gl.glEnd();

            if (drawSmooth) {
                // Draw smooth interpolated track

                gl.glColor3f(0.0f,1.0f,1.0f);
                gl.glBegin(gl.GL_LINE_LOOP);
                for (Point2D p:smoothPoints) {
                    gl.glVertex2d(p.getX(), p.getY());
                }
                gl.glEnd();
            }
        } else
            System.out.println("No Track!!");

    }


    public void annotate (GLAutoDrawable drawable){
        if (counter < 1)
            counter++;
        else {
            if ( drawable == null || chip.getCanvas() == null ){
                System.out.println("Null, why?");
                return;
            }
            canvas = chip.getCanvas();
            glCanvas = (GLCanvas)canvas.getCanvas();
            GL gl = drawable.getGL();

            // histStatistics();

            gl.glPushMatrix();
            if (drawHist) {
                drawHistogram(gl);
            }
            gl.glPopMatrix();

            if (displayTrack) {
                drawExtractedTrack(gl);
            }

            counter = 0;
        }

    }


    public void update (Observable o,Object arg){
    }

    public void mousePressed (MouseEvent e){
        Point p = canvas.getPixelFromMouseEvent(e);
        System.out.println("Pressing " + currentPointIdx);
    }

    public void mouseReleased (MouseEvent e){
        if (( currentPointIdx < 0 ) || (extractedTrack == null)) {
            return;
        }

        // Move point if selected
        System.out.println("Releasing mouse for point " + currentPointIdx);

        currentMousePoint = canvas.getPixelFromMouseEvent(e);

        if ((currentMousePoint.getX() >= 0) && (currentMousePoint.getY() >= 0) &&
                (currentMousePoint.getX() < numX) && (currentMousePoint.getY() < numY)) {

            // Move point 
            extractedTrack.setPoint(currentPointIdx, currentMousePoint);
            extractedTrack.updateSpline();
            extractPoints = extractedTrack.getPointList();
            smoothPoints = extractedTrack.getSmoothPoints(stepSize);
        }
        
        currentPointIdx = -1;
    }

    public void mouseMoved (MouseEvent e){
        currentMousePoint = canvas.getPixelFromMouseEvent(e);
    }

    public void mouseExited (MouseEvent e){
    }

    public void mouseEntered (MouseEvent e){
    }

    public void mouseDragged (MouseEvent e){
        // System.out.println("Dragging " + currentPointIdx);
        currentMousePoint = canvas.getPixelFromMouseEvent(e);
        if (!deleteOnClick) {
            // Select point for dragging
            if ((extractedTrack != null) && (currentPointIdx < 0)) {
                int idx = extractedTrack.findClosest(currentMousePoint, clickTolerance);
                // System.out.println("New drag " + idx + " / " + clickTolerance);
                if (idx >= 0) {
                    currentPointIdx = idx;
                }
            }
        }
    }

    private int min (int a,int b){
        return a < b ? a : b;
    }

    private int max (int a,int b){
        return a > b ? a : b;
    }

    public void mouseClicked (MouseEvent e){
        // System.out.println("Click " + currentPointIdx);
        Point p = canvas.getPixelFromMouseEvent(e);
        if (deleteOnClick) {
            // Delete point
            if (extractedTrack != null) {
                int idx = extractedTrack.findClosest(p, clickTolerance);
                if (idx >= 0) {
                    extractedTrack.deletePoint(idx);
                    extractedTrack.updateSpline();
                    extractPoints = extractedTrack.getPointList();
                    smoothPoints = extractedTrack.getSmoothPoints(stepSize);
                }
            }
        }
    }

    @Override
    public synchronized void setFilterEnabled (boolean yes){
        super.setFilterEnabled(yes);
        if ( glCanvas == null ){
            return;
        }
        if ( yes ){
            glCanvas.addMouseListener(this);
            glCanvas.addMouseMotionListener(this);

        } else{
            glCanvas.removeMouseListener(this);
            glCanvas.removeMouseMotionListener(this);
        }
    }

    public boolean isDrawHist() {
        return drawHist;
    }

    public void setDrawHist(boolean drawHist) {
        this.drawHist = drawHist;
        getPrefs().putBoolean("TrackdefineFilter.drawHist",drawHist);
    }

    public float getHistThresh() {
        return histThresh;
    }

    public void setHistThresh(float histThresh) {
        this.histThresh = histThresh;
        getPrefs().putFloat("TrackdefineFilter.histThresh",histThresh);
    }

    public int getErosionSize() {
        return erosionSize;
    }

    public void setErosionSize(int erosionSize) {
        this.erosionSize = erosionSize;
        getPrefs().putInt("TrackdefineFilter.erosionSize",erosionSize);
    }

    public float getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(float minDistance) {
        this.minDistance = minDistance;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
    }

    public boolean isDisplayTrack() {
        return displayTrack;
    }

    public void setDisplayTrack(boolean displayTrack) {
        this.displayTrack = displayTrack;
    }

    public float getStepSize() {
        return stepSize;
    }

    public void setStepSize(float stepSize) {
        this.stepSize = stepSize;
        if (extractedTrack != null) {
            smoothPoints = extractedTrack.getSmoothPoints(stepSize);
        }
    }

    public boolean isDrawSmooth() {
        return drawSmooth;
    }

    public void setDrawSmooth(boolean drawSmooth) {
        this.drawSmooth = drawSmooth;
    }

    public boolean isDeleteOnClick() {
        return deleteOnClick;
    }

    public void setDeleteOnClick(boolean deleteOnClick) {
        this.deleteOnClick = deleteOnClick;
    }

    public float getClickTolerance() {
        return clickTolerance;
    }

    public void setClickTolerance(float clickTolerance) {
        this.clickTolerance = clickTolerance;
    }




    /**
     * Re-initializes the histogram of events.
     */
    public void doInitHistogram() {
            pixData = new float[numY][numX];
            for (int i=0; i<numY; i++)
                for (int j=0; j<numX; j++)
                    pixData[i][j]=0.0f;

            totalSum = 0.0f;
    }

    /** Invalidates all points within minDistance of (x,y) in the queue */
    private void invalidateQueue(PriorityQueue<TrackPoint> pq, int x, int y) {
        Point2D.Float pos = new Point2D.Float(x,y);

        Iterator<TrackPoint> it = pq.iterator();
        LinkedList<TrackPoint> toAdd = new LinkedList<TrackPoint>();

        while(it.hasNext()) {
            TrackPoint p = it.next();
            float distance = (float) pos.distance(p.x, p.y);
            it.remove();
            if (distance > minDistance) {
                p.minDistance = distance;
                toAdd.add(p);
            }
        }

        // Add all updated points
        pq.addAll(toAdd);
    }

    /**
     * Extracts the track from the histogram of events.
     */
    public void doExtractTrack() {
        boolean[][] trackPoints = erosion();

        // Find starting point
        int maxX = -1, maxY = -1;
        float maxVal = Float.NEGATIVE_INFINITY;
        for (int i=0; i<numY; i++) {
            for (int j=0; j<numX; j++) {
                if (pixData[i][j] > maxVal) {
                    maxVal = pixData[i][j];
                    maxX = j;
                    maxY = i;
                }
            }
        }

        // Insert starting point
        extractPoints = new LinkedList<Point2D>();
        extractPoints.add(new Point2D.Float((float)maxX, (float)maxY));
        boolean trackFinished = false;


        // Prepare queue of potential track points
        PriorityQueue<TrackPoint> pq = new PriorityQueue<TrackPoint>();

        for (int i=0; i<numY; i++) {
            for (int j=0; j<numX; j++) {
                if (trackPoints[i][j]) {
                    // Insert into queue
                    float dist = (float) Point.distance(maxX, maxY, j, i);
                    if (dist > minDistance) {
                        TrackPoint p = new TrackPoint(j,i,dist);
                        pq.add(p);
                    }
                }
            }
        }

        int curX = maxX;
        int curY = maxY;
        while (!trackFinished) {
            // Delete track point which are too close to current points
            invalidateQueue(pq, curX, curY);

            if (pq.isEmpty()) {
                trackFinished = true;
            } else {
                // Add new track point
                TrackPoint nextPoint = pq.remove();
                if (Point2D.distance(nextPoint.x, nextPoint.y, curX, curY) < maxDistance) {
                    extractPoints.add(new Point2D.Float(nextPoint.x, nextPoint.y));
                    curX = nextPoint.x;
                    curY = nextPoint.y;
                } else {
                    System.out.println("No more points in distance!");
                    trackFinished = true;
                }
            }
        }

        System.out.println("Extracted " + extractPoints.size() + " track points!");

        // Create track object and spline
        extractedTrack = new SlotcarTrack();
        extractedTrack.create(extractPoints);

        smoothPoints = extractedTrack.getSmoothPoints(stepSize);
    }

    /**
     * Saves the extracted track to an external file.
     */
    public void doSaveTrack() {
        if (extractedTrack == null) {
            // No track defined
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter() {
                @Override public boolean accept (File f) {
                    return f.isDirectory() ||
                            f.getName().toLowerCase().endsWith(".track");
                }

                @Override public String getDescription() {
                    return "Track files";
                }
            }
        );

        // TODO: Why is manual entry of file-name not working?
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setSelectedFile(new File("test.track"));
        int state = fc.showSaveDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            System.out.println("Selected " + file.getName());
        } else {
            System.out.println("Cancelled saving!");
        }
    }
}