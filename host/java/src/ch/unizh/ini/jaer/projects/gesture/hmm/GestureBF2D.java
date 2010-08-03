/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.unizh.ini.jaer.projects.gesture.hmm;

import ch.unizh.ini.jaer.projects.gesture.virtualdrummer.BlurringFilter2DTracker;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.*;
import javax.swing.Timer;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.eventprocessing.*;
import net.sf.jaer.eventprocessing.tracking.ClusterPathPoint;
import net.sf.jaer.graphics.FrameAnnotater;
import net.sf.jaer.util.filter.LowpassFilter2d;

/**
 * Gesture recognition system using a single DVS sensor.
 * BluringFilter2DTracker is used to obtain the trajectory of moving object (eg, hand)
 * HMM is used for classification. But, HMM is not used for spoting gestures (i.e., finding the start and end timing of gestures)
 * Gesture spotting is done by the tracker by assuming that there is slow movement between gestures.
 *
 * @author Jun Haeng Lee
 */
public class GestureBF2D extends EventFilter2D implements FrameAnnotater,Observer{
    /**
     * a cluster with points more than this amount will be checked for gesture recognition.
     */
    private int numPointsThreshold = getPrefs().getInt("GestureBF2D.numPointsThreshold", 100);

    /**
     * retries HMM after this percents of head points is removed from the trajectory when the first tiral is failed.
     */
    private int headTrimmingPercents = getPrefs().getInt("GestureBF2D.headTrimmingPercents", 30);

    /**
     * retries HMM after this percents of tail points is removed from the trajectory when the first tiral is failed.
     */
    private int tailTrimmingPercents = getPrefs().getInt("GestureBF2D.tailTrimmingPercents", 10);

    /**
     * speed threshold of the cluster to be a gesture candidate (in kPPT).
     */
    private float maxSpeedThreshold_kPPT = getPrefs().getFloat("GestureBF2D.maxSpeedThreshold_kPPT", 0.1f);

    /**
     * enables lowpass filter to smooth the gesture trajectory
     */
    private boolean enableLPF = getPrefs().getBoolean("GestureBF2D.enableLPF", true);

    /**
     * lowpass filter time constant for gesture trajectory in ms
     */
    private float tauPathMs = getPrefs().getFloat("GestureBF2D.tauPathMs",5.0f);

    /**
     * refractory time in ms between gestures.
     */
    private int refractoryTimeMs = getPrefs().getInt("GestureBF2D.refractoryTimeMs", 700);



    
    /**
     * true if the gesture recognition system is activated.
     */
    private boolean login = false;

    /**
     * images for gestures
     */
    private Image imgHi, imgBye, imgLeft, imgRight, imgUp, imgDown, imgCW, imgCCW, imgCheck;

    /**
     * timmings in the current and previous gestures
     */
    private int startTimeGesture, endTimeGesture, endTimePrevGesture = 0;

    /**
     * 'Check' gesture is recognized by a check shape or a sequence of 'SlashDown' and 'SlashUp'
     * checkActivated is true if 'SlashDown' is detected. It's false otherwise.
     */
    private boolean checkActivated = false;

    /**
     * time duration limit between 'SlashDown' and 'SlashUp' to make a valid 'Check' gesture
     */
    private static int checkActivationTimeUs = 500000;



    /**
     * moving object tracker
     */
    protected BlurringFilter2DTracker tracker;

    /**
     * feature extractor
     */
    FeatureExtraction fve = new FeatureExtraction(16, 16);

    /**
     * Hand drawing panel with gesture HMM module
     */
    HmmDrawingPanel hmmDP;

    /**
     * low pass filter to smoothe the trajectory of gestures
     */
    LowpassFilter2d lpf;




    /**
     * constructor
     * 
     * @param chip
     */
    public GestureBF2D(AEChip chip) {
        super(chip);

        String trimming = "Trimming", selection  = "Selection", lpfilter = "Low pass filter", gesture = "Gesture";
        setPropertyTooltip(selection,"numPointsThreshold","a cluster with points more than this amount will be checked for gesture recognition.");
        setPropertyTooltip(selection,"maxSpeedThreshold_kPPT","speed threshold of the cluster to be a gesture candidate (in kPPT).");
        setPropertyTooltip(trimming,"headTrimmingPercents","retries HMM after this percents of head points is removed from the trajectory when the first tiral is failed.");
        setPropertyTooltip(trimming,"tailTrimmingPercents","retries HMM after this percents of tail points is removed from the trajectory when the first tiral is failed.");
        setPropertyTooltip(lpfilter,"enableLPF","enables lowpass filter to smooth the gesture trajectory");
        setPropertyTooltip(lpfilter,"tauPathMs","lowpass filter time constant for gesture trajectory in ms");
        setPropertyTooltip(gesture,"refractoryTimeMs","refractory time in ms between gestures");

        // low pass filter
        this.lpf = new LowpassFilter2d();

        // hand drawing panel with gesture HMM
        String [] bNames = {"Add", "Remove", "Reset", "Show", "Learn", "Guess"};
        hmmDP = new HmmDrawingPanel("HMM based gesture recognition test using hand drawing panel", bNames);
        hmmDP.setVisible(false);

        // load gesture images into the memory
        loadGestureImages();

        // encloses tracker
        filterChainSetting ();
    }

    /**
     * sets the BlurringFilter2DTracker as a enclosed filter to find cluster
     */
    protected void filterChainSetting (){
        tracker = new BlurringFilter2DTracker(chip);
        ( (EventFilter2D)tracker ).addObserver(this);
        setEnclosedFilterChain(new FilterChain(chip));
        getEnclosedFilterChain().add((EventFilter2D)tracker);
        ( (EventFilter2D)tracker ).setEnclosed(true,this);
        ( (EventFilter2D)tracker ).setFilterEnabled(isFilterEnabled());
    }


    @Override
    public EventPacket<?> filterPacket(EventPacket<?> in) {
        out = tracker.filterPacket(in);
        return out;
    }

    @Override
    public void initFilter() {
        tracker.initFilter();
        endTimePrevGesture = 0;
        lpf.setTauMs(tauPathMs);
    }

    @Override
    public void resetFilter() {
        tracker.resetFilter();
        endTimePrevGesture = 0;
        lpf.setTauMs(tauPathMs);
    }

    @Override
    public synchronized void setFilterEnabled (boolean filterEventsEnabled){
        super.setFilterEnabled(filterEventsEnabled);
        
        if ( hmmDP != null ){
            if ( filterEventsEnabled ){
                hmmDP.setVisible(true);
            } else{
                hmmDP.setVisible(false);
            }
        }
    }

    public void annotate(GLAutoDrawable drawable) {
        // do nothing
    }

    /**
     * load gesture Images
     */
    private void loadGestureImages(){
        Toolkit myToolkit = Toolkit.getDefaultToolkit();
        
        imgHi = myToolkit.getImage("D:/user/gesture pictures/hi.jpg");
        hmmDP.putImage(imgHi);
        imgBye = myToolkit.getImage("D:/user/gesture pictures/bye.jpg");
        hmmDP.putImage(imgBye);
        imgLeft = myToolkit.getImage("D:/user/gesture pictures/left.jpg");
        hmmDP.putImage(imgLeft);
        imgRight = myToolkit.getImage("D:/user/gesture pictures/right.jpg");
        hmmDP.putImage(imgRight);
        imgUp = myToolkit.getImage("D:/user/gesture pictures/up.jpg");
        hmmDP.putImage(imgUp);
        imgDown = myToolkit.getImage("D:/user/gesture pictures/Down.jpg");
        hmmDP.putImage(imgDown);
        imgCW = myToolkit.getImage("D:/user/gesture pictures/clockwise.jpg");
        hmmDP.putImage(imgCW);
        imgCCW = myToolkit.getImage("D:/user/gesture pictures/counterclockwise.jpg");
        hmmDP.putImage(imgCCW);
        imgCheck = myToolkit.getImage("D:/user/gesture pictures/check.jpg");
        hmmDP.putImage(imgCheck);
    }

    public void update(Observable o, Object arg) {
        List<BlurringFilter2DTracker.Cluster> cl = tracker.getClusters();
        ArrayList<ClusterPathPoint> path = selectClusterTrajectory(cl);

        if(path != null){
            if(login){
                // estimates the best matching gesture
                String bmg = estimateGesture(path);
                System.out.println("Best matching gesture is " + bmg);

                if(afterRecognitionProcess(bmg))
                    endTimePrevGesture = endTimeGesture;

            } else {
                if(detectStartingGesture(path)){
                    System.out.println("Gesture recognition system is enabled.");
                    afterRecognitionProcess("Infinite");
                }
            }
        }
    }

    /**
     * detects the startinf gesture (ie. 'Infinite' shape)
     * It tries several times by trimming the input trajectory.
     * 
     * @param path
     * @return
     */
    private boolean detectStartingGesture(ArrayList<ClusterPathPoint> path){
        boolean ret = tryStartingGesture(path);

        if(!ret){
            for(int i = 1; i <= 2 ; i++){
                for(int j = 0; j<=1; j++){
                    // retries with the head trimming if failed
                    ArrayList<ClusterPathPoint> trimmedPath = trajectoryTrimming(path, j*headTrimmingPercents/2, j*tailTrimmingPercents);
                    if(trimmedPath.size() >= numPointsThreshold && checkSpeedCriterion(trimmedPath)){
                        ret = tryStartingGesture(trimmedPath);
                    } else
                        break;

                    if(ret)
                        break;
                }
            }
        }

        return ret;
    }

    /**
     * returns true if the given trajectory matches to the starting gesture (ie. 'Infinite' shape)
     *
     * @param path
     * @return
     */
    private Boolean tryStartingGesture(ArrayList<ClusterPathPoint> path){
        String[] codewards = fve.convTrajectoryToCodewords(path);
        Boolean ret = false;

        for(String gName:hmmDP.gestureItems){
            if(gName.startsWith("Infinite")){
                ret = hmmDP.ghmm.tryGesture(gName, codewards, fve.vectorAngleSeq);
                if(ret)
                    break;
            }
        }

        return ret;
    }

    /**
     * estimates best matching gesture
     * It tries several times by trimming the input trajectory.
     *
     * @param path
     * @return
     */
    private String estimateGesture(ArrayList<ClusterPathPoint> path){
        String bmg = getBestmatchingGesture(path, -200);

        if(bmg == null){
            for(int i = 1; i <= 2 ; i++){
                for(int j = 0; j<=1; j++){
                    // retries with the head trimming if failed
                    ArrayList<ClusterPathPoint> trimmedPath = trajectoryTrimming(path, j*headTrimmingPercents/2, j*tailTrimmingPercents);
                    if(trimmedPath.size() >= numPointsThreshold && checkSpeedCriterion(trimmedPath)){
                        bmg = getBestmatchingGesture(trimmedPath, -200 + ((i-1)*2+j+1)*100);
                    } else
                        break;

                    if(bmg != null)
                        break;
                }
            }
        }

        return bmg;
    }

    /**
     * returns the best matching gesture
     *
     * @param path
     * @return
     */
    private String getBestmatchingGesture(ArrayList<ClusterPathPoint> path, int offset){
        String[] codewards = fve.convTrajectoryToCodewords(path);
        String bmg = hmmDP.ghmm.getBestMatchingGesture(codewards, fve.vectorAngleSeq);
        
/*      
        // draws the quantized vectors
        if(offset == -200)
            hmmDP.clearImage();
        hmmDP.drawTrajectory(FeatureExtraction.convAnglesToTrajectoryInScaledArea(new Point2D.Float(hmmDP.centerX+offset, hmmDP.centerY+offset), hmmDP.centerY/2, fve.vectorAngleSeq));

        // draws the trajectory
        ArrayList<Point2D.Float> tmpPath = new ArrayList<Point2D.Float>();
        for(ClusterPathPoint pt:path)
            tmpPath.add(new Point2D.Float(pt.x*2 + 200 + offset, pt.y*2));
        hmmDP.drawTrajectoryDot(tmpPath);

        hmmDP.repaint();
        System.out.println(offset + ": " + bmg);
*/
        return bmg;
    }

    /**
     * puts an image on the screen based on the result of gesture recognition
     *
     * @param bmg
     * @return
     */
    private boolean afterRecognitionProcess(String bmg){
        if(bmg == null)
            return false;

        boolean ret = true;

        if(login){
            if(bmg.startsWith("Infinite")){
                doLogout();
            } else if(bmg.startsWith("Push")){
                doPush();
            } else if(bmg.startsWith("SlashUp")){
                ret = doSlashUp();
            } else {
                if(bmg.startsWith("SlashDown")){
                    doSlashDown();
                } else {
                    // doesn't have consider refractory time for CW and CCW
                    if(bmg.startsWith("CW")){
                        doCW();
                    }else if(bmg.startsWith("CCW")){
                        doCCW();
                    }

                    // has to consider refractory time for Left, Right, Up, Down, and Check
                    // doesn't have to consider refractory time if checkActivated is true (i.e. SlashDown is detected) becase SlashDown is a partial gesture
                    if(checkActivated || startTimeGesture >= endTimePrevGesture + refractoryTimeMs*1000){
                        if(bmg.startsWith("Left")){
                            hmmDP.putImage(imgLeft);
                        }else if(bmg.startsWith("Right")){
                            hmmDP.putImage(imgRight);
                        }else if(bmg.startsWith("Up")){
                            hmmDP.putImage(imgUp);
                        }else if(bmg.startsWith("Down")){
                            hmmDP.putImage(imgDown);
                        }else if(bmg.startsWith("Check")){
                            hmmDP.putImage(imgCheck);
                        }
                    } else {
                        ret = false;
                    }

                    checkActivated = false;
                }
            }
        } else {
            if(bmg.startsWith("Infinite")){
                doLogin();
            }
        }

        return ret;
    }


    /**
     * selects the best trajectory from clusters
     *
     * @param cl
     * @return
     */
    private ArrayList<ClusterPathPoint> selectClusterTrajectory(List<BlurringFilter2DTracker.Cluster> cl){
        ArrayList<ClusterPathPoint> selectedTrj = null;
        BlurringFilter2DTracker.Cluster selectedCluster = null;

        int maxNumPoint = 0;

        // select a candidate trajectory
        for (BlurringFilter2DTracker.Cluster c: cl){
            // doesn't have to check alive cluster
            if (!c.isDead()){
                continue;
            }

            // checks number of points
            if(c.getPath().size() < numPointsThreshold){
                continue;
            } else {
                // search the largest cluster
                ArrayList<ClusterPathPoint> path = c.getPath();
                if(path.size() > maxNumPoint){
                    selectedTrj = path;
                    maxNumPoint = path.size();
                    selectedCluster = c;
                }
            }
        }
        if(selectedTrj == null)
            return null;
        
        // gesture speed check
        if(!checkSpeedCriterion(selectedTrj)){
            return null;
        }

        // low-pass filtering
        ArrayList<ClusterPathPoint> retTrj = null;
        if(enableLPF)
            retTrj = lowPassFiltering(selectedTrj);
        else
            retTrj = selectedTrj;

        // records start and end time of the selected trajectory
        if(retTrj != null){
            startTimeGesture = selectedCluster.getBirthTime();
            endTimeGesture = selectedCluster.getLastEventTimestamp();
        }

        return retTrj;
    }

    /**
     * checks speed criterion.
     * returns true if a certain number of points have velocity higher than maxSpeedThreshold_kPPT
     *
     * @param path
     * @return
     */
    private boolean checkSpeedCriterion(ArrayList<ClusterPathPoint> path){
        boolean ret = true;

        // gesture speed check, At least 5% of the points velocity have to exceed speed threshold.
        int numValidPoints = Math.max(1, (int) (path.size()*0.05));
        for(int i=0; i<path.size(); i++){
            ClusterPathPoint point = path.get(i);
            if(point.velocityPPT != null){
                double speed = 1000*Math.sqrt(Math.pow(point.velocityPPT.x, 2.0)+Math.pow(point.velocityPPT.y, 2.0));
                if(speed >= maxSpeedThreshold_kPPT)
                    numValidPoints--;
            }
        }
        if(numValidPoints > 0)
            ret = false;

        return ret;
    }

    /**
     * trims a trajectory
     *
     * @param trajectory
     * @param headTrimmingPercets
     * @param tailTrimmingPercets
     * @return
     */
    private ArrayList<ClusterPathPoint> trajectoryTrimming(ArrayList<ClusterPathPoint> trajectory, int headTrimmingPercets, int tailTrimmingPercets){
        ArrayList<ClusterPathPoint> trimmedTrj;
        int numPointsHeadTrimming = (int) (trajectory.size()*0.01*headTrimmingPercets);
        int numPointsTailTrimming = (int) (trajectory.size()*0.01*tailTrimmingPercets);
        if(numPointsHeadTrimming + numPointsTailTrimming > 0){
            trimmedTrj = new ArrayList<ClusterPathPoint>(trajectory.size() - numPointsHeadTrimming - numPointsTailTrimming);
            for(int j=numPointsHeadTrimming; j<trajectory.size()-numPointsTailTrimming; j++)
                trimmedTrj.add(trajectory.get(j));
        } else
            trimmedTrj = trajectory;

        return trimmedTrj;
    }

    /**
     * does low-pass filtering to smoothe the trajectory
     *
     * @param path
     * @return
     */
    private ArrayList<ClusterPathPoint> lowPassFiltering(ArrayList<ClusterPathPoint> path){
        ArrayList<ClusterPathPoint> lpfPath = new ArrayList<ClusterPathPoint>(path.size());
        ClusterPathPoint p = (ClusterPathPoint) path.get(0).clone();

        lpfPath.add(p);
        lpf.setInternalValue2d(path.get(0).x, path.get(0).y);
        for(int i=1; i<path.size(); i++){
            p = (ClusterPathPoint) path.get(i).clone();
            Point2D.Float pt = lpf.filter2d(p.x, p.y, p.t);
            p.x = pt.x;
            p.y = pt.y;
            lpfPath.add(p);
        }

        return lpfPath;
    }

    /**
     * returns maxSpeedThreshold_kPPT
     *
     * @return
     */
    public float getMaxSpeedThreshold_kPPT() {
        return maxSpeedThreshold_kPPT;
    }

    /** sets maxSpeedThreshold_kPPT
     *
     * @param maxSpeedThreshold_kPPT
     */
    public void setMaxSpeedThreshold_kPPT(float maxSpeedThreshold_kPPT) {
        float old = this.maxSpeedThreshold_kPPT;
        this.maxSpeedThreshold_kPPT = maxSpeedThreshold_kPPT;
        getPrefs().putFloat("GestureBF2D.maxSpeedThreshold_kPPT",maxSpeedThreshold_kPPT);
        support.firePropertyChange("maxSpeedThreshold_kPPT",old,this.maxSpeedThreshold_kPPT);
    }

    /** returns numPointsThreshold
     *
     * @return
     */
    public int getNumPointsThreshold() {
        return numPointsThreshold;
    }

    /** sets numPointsThreshold
     *
     * @param numPointsThreshold
     */
    public void setNumPointsThreshold(int numPointsThreshold) {
        int old = this.numPointsThreshold;
        this.numPointsThreshold = numPointsThreshold;
        getPrefs().putInt("GestureBF2D.numPointsThreshold",numPointsThreshold);
        support.firePropertyChange("numPointsThreshold",old,this.numPointsThreshold);
    }

    /** returns headTrimmingPercents
     *
     * @return
     */
    public int getHeadTrimmingPercents() {
        return headTrimmingPercents;
    }

    /** sets headTrimmingPercents
     *
     * @param headTrimmingPercents
     */
    public void setHeadTrimmingPercents(int headTrimmingPercents) {
        int old = this.headTrimmingPercents;
        this.headTrimmingPercents = headTrimmingPercents;
        getPrefs().putInt("GestureBF2D.headTrimmingPercents",headTrimmingPercents);
        support.firePropertyChange("headTrimmingPercents",old,this.headTrimmingPercents);
    }

    /** returns tailTrimmingPercents
     *
     * @return
     */
    public int getTailTrimmingPercents() {
        return tailTrimmingPercents;
    }

    /** sets tailTrimmingPercents
     *
     * @param tailTrimmingPercents
     */
    public void setTailTrimmingPercents(int tailTrimmingPercents) {
        int old = this.tailTrimmingPercents;
        this.tailTrimmingPercents = tailTrimmingPercents;
        getPrefs().putInt("GestureBF2D.tailTrimmingPercents",tailTrimmingPercents);
        support.firePropertyChange("tailTrimmingPercents",old,this.tailTrimmingPercents);
    }

    /** returns enableLPF
     *
     * @return
     */
    public boolean isEnableLPF() {
        return enableLPF;
    }

    /** sets enableLPF
     * 
     * @param enableLPF
     */
    public void setEnableLPF(boolean enableLPF) {
        boolean old = this.enableLPF;
        this.enableLPF = enableLPF;
        getPrefs().putBoolean("GestureBF2D.enableLPF", enableLPF);
        support.firePropertyChange("enableLPF",old,this.enableLPF);
    }


    /**
     * @return the tauMs
     */
    public float getTauPathMs (){
        return tauPathMs;
    }

    /**
     * The lowpass time constant of the trajectory.
     *
     * @param tauPathMs the tauMs to set
     */
    synchronized public void setTauPathMs (float tauPathMs){
        float old = this.tauPathMs;
        this.tauPathMs = tauPathMs;
        getPrefs().putFloat("GestureBF2D.tauPathMs",tauPathMs);
        support.firePropertyChange("tauPathMs",old,this.tauPathMs);
        lpf.setTauMs(tauPathMs);
    }

    /**
     * returns refractoryTimeMs
     *
     * @return
     */
    public int getRefractoryTimeMs() {
        return refractoryTimeMs;
    }

    /**
     * sets refractoryTimeMs
     * 
     * @param refractoryTimeMs
     */
    public void setRefractoryTimeMs(int refractoryTimeMs) {
        int old = this.refractoryTimeMs;
        this.refractoryTimeMs = refractoryTimeMs;
        getPrefs().putInt("GestureBF2D.refractoryTimeMs", refractoryTimeMs);
        support.firePropertyChange("refractoryTimeMs",old,this.refractoryTimeMs);
    }

    /**
     * Class for HMM and GUI
     */
    class HmmDrawingPanel extends TrajectoryDrawingPanel implements ItemListener{
        /**
         * Button names
         */
        public final String REMOVE = "Remove";
        public final String ADD = "Add";
        public final String SHOW = "Show";
        public final String RESET = "Reset";
        public final String LEARN = "Learn";
        public final String GUESS = "Guess";

        /**
         * Optional HMM models
         */
        public final String ERGODIC = "ERGODIC";
        public final String LR = "LR";
        public final String LRB = "LRB";
        public final String LRC = "LRC";
        public final String LRBC = "LRBC";

        /**
         * Stores gesture names in a set to guarantee the uniqueness of names
         */
        public HashSet<String> gestureItems = new HashSet<String>();

        /**
         * combo box for choosing a gesture from the registered gesture set
         */
        JComboBox gestureChoice;
        /**
         * combo box for choosing a HMM model
         */
        JComboBox hmmModelChoice;
        /**
         * text field for entering the name of a new gesture to register
         */
        JTextField newGesture;
        /**
         * for saving and loading of gesture HMM
         */
        JFileChooser fileChooser;
        /**
         * make it true to manually activate gesture recognition system
         */
        JCheckBoxMenuItem checkGestureAction;

        /**
         * All gestures have the same number of states.
         */
        int numState = 5;

        /**
         *  Feature vector space consists of 16 quantized vectors.
         */
        String[] featureVectorSpace = new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};

        /**
         * use dynamic threshold model. If you set 'false' instead of 'true', you can use a static threshold model.
         */
        GestureHmm ghmm = new GestureHmm(featureVectorSpace, GestureHmm.GAUSSIAN_THRESHOLD);

        /**
         * Output statement buffer
         */
        String msg = "";

        /**
         * x of the center x of image panel
         */
        float centerX = imgPanelWidth/2;
        /**
         * y of the center of image panel
         */
        float centerY = imgPanelHeight/2;
        /**
         * size of show panel
         */
        float showPanelSize = Math.min(centerX, centerY);

        /**
         * timer for image load
         */
        Timer timer;

        /**
         * constructor
         * @param title
         * @param buttonNames
         */
        public HmmDrawingPanel(String title, String[] buttonNames) {
            super(title, 700, 700, buttonNames);

            //creates a file chooser
            fileChooser = new JFileChooser();

            // creates a timer
            timer = new Timer(700, clearImageAction);
        }

        @Override
        public void buttonLayout(String[] componentNames) {
            gestureChoice = new JComboBox();
            gestureChoice.setName("gestureChoice");
            gestureChoice.addItem("Select a gesture");
            hmmModelChoice = new JComboBox();
            hmmModelChoice.setName("hmmModelChoice");
            hmmModelChoice.addItem("Select HMM model");
            hmmModelChoice.addItem(ERGODIC);
            hmmModelChoice.addItem(LR);
            hmmModelChoice.addItem(LRB);
            hmmModelChoice.addItem(LRC);
            hmmModelChoice.addItem(LRBC);
            newGesture = new JTextField();
            newGesture.setText("New gesture name");

            // configuration of button panel
            buttonPanel.setLayout(new GridLayout(2, (componentNames.length+3)/2));

            // adds gesture choice
            buttonPanel.add(gestureChoice, "1");
            gestureChoice.addItemListener(this);

            // adds new gesture name
            buttonPanel.add(newGesture, "2");

            // adds HMM model choice
            buttonPanel.add(hmmModelChoice, "3");
            hmmModelChoice.addItemListener(this);

            // adds buttons
            JButton newButton;
            for(int i = 0; i< componentNames.length; i++){
                newButton = new JButton(componentNames[i]);
                buttonPanel.add(newButton, ""+(i+4));
                newButton.addActionListener(buttonActionListener);
            }
            JButton clearButton = new JButton(clearButtonName);
            buttonPanel.add(clearButton, ""+ (componentNames.length + 4));
            clearButton.addActionListener(buttonActionListener);
        }

        @Override
        public void menuLayout() {
            // creates and adds drop down menus to the menu bar
            JMenu fileMenu = new JMenu("File");
            menuBar.add(fileMenu);
            JMenu gestureMenu = new JMenu("Gesture");
            menuBar.add(gestureMenu);

            // creates and adds menu items to menus
            JMenuItem newAction = new JMenuItem("New");
            JMenuItem loadAction = new JMenuItem("Load");
            JMenuItem saveAction = new JMenuItem("Save");
            fileMenu.add(newAction);
            fileMenu.add(loadAction);
            fileMenu.add(saveAction);

            // Create and add CheckButton for enabling gesture recognition
            checkGestureAction = new JCheckBoxMenuItem("Activates Gesture Recognition");
            checkGestureAction.setState(login);
            gestureMenu.add(checkGestureAction);

            // add action listeners
            newAction.addActionListener(menuActionListener);
            loadAction.addActionListener(menuActionListener);
            saveAction.addActionListener(menuActionListener);
            checkGestureAction.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    AbstractButton aButton = (AbstractButton) e.getSource();
                    if(aButton.getModel().isSelected()){
                        login = true;
                        System.out.println("Gesture recognition is mannually activated.");
                    }else{
                        login = false;
                        System.out.println("Gesture recognition is mannually Inactivated.");
                    }

                    clearImage();
                }
            });
        }

        @Override
        public void buttonAction(String buttonName) {
            if(buttonName.equals(LEARN)){
                doLearn();
                clearImage();
            } else if(buttonName.equals(ADD)){
                doAddGesture();
            } else if(buttonName.equals(REMOVE)){
                doRemoveGesture();
            } else if(buttonName.equals(GUESS)){
                doGuess();
            } else if(buttonName.equals(RESET)){
                doReset();
                clearImage();
            } else if(buttonName.equals(SHOW)){
                doShow();
            }
        }

        @Override
        public void menuAction(String menuName) {
            if(menuName.equals("New")){
                doNew();
            } else if(menuName.equals("Load")){
                try{
                    doLoad();
                } catch(ClassNotFoundException e){
                    e.printStackTrace();
                }
            } else if(menuName.equals("Save")){
                doSave();
            }

            repaint();
        }


        /**
         * excutes Remove button
         */
        public void doRemoveGesture(){
            String gesName = (String) gestureChoice.getSelectedItem();
            if(gesName == null || gesName.equals("") || gesName.equals("Select a gesture")){
                System.out.println("Warning: Gesture is not selected.");
                return;
            }

            ghmm.removeGesture(gesName);
            gestureChoice.removeItem(gesName);
            gestureItems.remove(gesName);
            System.out.println(gesName + " was removed.");
        }

        /**
         * excutes Add button
         */
        public void doAddGesture(){
            String newGestName = newGesture.getText();
            if(newGestName.equals("")){
                System.out.println("Warning: Gesture name is not specified.");
                return;
            }

            if(((String) hmmModelChoice.getSelectedItem()).startsWith("Select HMM model")) {
                System.out.println("Warning: HMM model is not specified.");
                return;
            }

            String gestName = newGestName+"_"+hmmModelChoice.getSelectedItem();

            if(!gestureItems.contains(gestName)){
                gestureItems.add(gestName);
                gestureChoice.addItem(gestName);
                HiddenMarkovModel.ModelType selectedModel;
                if(hmmModelChoice.getSelectedItem().equals("ERGODIC"))
                    selectedModel = HiddenMarkovModel.ModelType.ERGODIC_RANDOM;
                else if(hmmModelChoice.getSelectedItem().equals("LR"))
                    selectedModel = HiddenMarkovModel.ModelType.LR_RANDOM;
                else if(hmmModelChoice.getSelectedItem().equals("LRB"))
                    selectedModel = HiddenMarkovModel.ModelType.LRB_RANDOM;
                else if(hmmModelChoice.getSelectedItem().equals("LRC"))
                    selectedModel = HiddenMarkovModel.ModelType.LRC_RANDOM;
                else if(hmmModelChoice.getSelectedItem().equals("LRBC"))
                    selectedModel = HiddenMarkovModel.ModelType.LRBC_RANDOM;
                else{
                    System.out.println("Warning: Failed to add a new gesture.");
                    return;
                }

                ghmm.addGesture(gestName, numState,  selectedModel);
                ghmm.initializeGestureRandom(gestName);

                System.out.println("A new gesture ("+ gestName + ") is added.");
            }
            gestureChoice.setSelectedItem(gestName);
            newGesture.setText("");
        }

        /**
         * excutes Learn button
         */
        public void doLearn(){
            String gesName = (String) gestureChoice.getSelectedItem();
            if(gesName == null || gesName.equals("") || gesName.equals("Select a gesture")){
                System.out.println("Warning: Gesture is not selected.");
                return;
            }

            String[] fv = fve.convTrajectoryToCodewords(trajectory);
            if(fv[0] == null){
                System.out.println("Warning: No trajectory is dected.");
                return;
            }
            System.out.println("Learning " + gesName);

            boolean learningSuccess;
            HiddenMarkovModel.ModelType modelType = ghmm.getGestureHmm(gesName).getModelType();

            // for LRC & LRBC, we don't have to update start probability
            if(modelType == HiddenMarkovModel.ModelType.LRC_RANDOM ||  modelType == HiddenMarkovModel.ModelType.LRBC_RANDOM)
                learningSuccess = ghmm.learnGesture(gesName, fv, fve.vectorAngleSeq, false, true, true);
            else
                learningSuccess = ghmm.learnGesture(gesName, fv, fve.vectorAngleSeq, true, true, true);

            if(learningSuccess){
                if(ghmm.getGestureHmm(gesName).getNumTraining() == 1)
                    System.out.println(gesName+" is properly registered. Log{P(O|model)} = " + Math.log10(ghmm.getGestureLikelyhood(gesName, fv)));
                else if(ghmm.getGestureHmm(gesName).getNumTraining() == 2)
                    System.out.println(gesName+" has been trained twice. Log{P(O|model)} = " + Math.log10(ghmm.getGestureLikelyhood(gesName, fv)));
                else
                    System.out.println(gesName+" has been trained " + ghmm.getGestureHmm(gesName).getNumTraining() + " times. Log{P(O|model)} = " + Math.log10(ghmm.getGestureLikelyhood(gesName, fv)));

//                ghmm.printGesture(gesName);
//                ghmm.printThresholdModel();
//                ghmm.getGestureHmm(gesName).viterbi(fv);
//                System.out.println("Viterbi path : " + ghmm.getGestureHmm(gesName).getViterbiPathString(fv.length));
            }
        }

        /**
         * excutes Guess button
         */
        public void doGuess(){
            String[] fv = fve.convTrajectoryToCodewords(trajectory);

            if(fv[0] == null){
                System.out.println("Warning: No trajectory is dected.");
                return;
            }

            String bmg = ghmm.getBestMatchingGesture(fv, fve.vectorAngleSeq);
            gImg.setFont(new Font("Arial", Font.PLAIN, 24));

            // erase previous message
            Color tmpColor = getColor();
            gImg.setColor(this.getBackground());
            gImg.drawString(msg, 40 + imgPanelWidth/2 - msg.length()*12/2, imgPanelHeight - 20);
            gImg.setColor(tmpColor);

            if(bmg == null){
                msg = "No gesture is found.";
                System.out.println(msg);
            }else{
                msg = String.format("Best matching gesture is %s", bmg);
                System.out.println(msg +" with probability "+Math.log10(ghmm.getGestureLikelyhood(bmg, fv)));
//                ghmm.getGestureHmm(bmg).viterbi(fv);
//                System.out.println("Viterbi path : " + ghmm.getGestureHmm(bmg).getViterbiPathString(fv.length));
            }
            gImg.drawString(msg, 40 + imgPanelWidth/2 - msg.length()*12/2, imgPanelHeight - 20);
            repaint();

            resetTrajectory();
        }


        /**
         * excutes Show button
         */
        public void doShow(){
            String gesName = (String) gestureChoice.getSelectedItem();
            if(gesName == null || gesName.equals("") || gesName.equals("Select a gesture")){
                System.out.println("Warning: Gesture is not selected.");
                return;
            }

            double[] meanFVarray = ghmm.getAverageFeaturesToArray(gesName);

            clearImage();

            // draws frame
            int margin = 30;
            int shadow = 10;
            Color tmp = getColor();
            gImg.setColor(Color.DARK_GRAY);
            gImg.fillRect((int) (centerX - showPanelSize/2) - margin + shadow, (int) (centerY - showPanelSize/2) - margin + shadow, (int) showPanelSize + 2*margin, (int) showPanelSize + 2*margin);
            gImg.setColor(Color.WHITE);
            gImg.fillRect((int) (centerX - showPanelSize/2) - margin, (int) (centerY - showPanelSize/2) - margin, (int) showPanelSize + 2*margin, (int) showPanelSize + 2*margin);
            gImg.setColor(Color.BLACK);
            gImg.drawRect((int) (centerX - showPanelSize/2) - margin, (int) (centerY - showPanelSize/2) - margin, (int) showPanelSize + 2*margin, (int) showPanelSize + 2*margin);
            gImg.setFont(new Font("Arial", Font.PLAIN, 24));
            gImg.drawString(gesName+" (# of training: "+ghmm.getGestureHmm(gesName).getNumTraining()+")", (int) centerX - (int) showPanelSize/2 - margin, (int) centerY - (int) showPanelSize/2 - margin - 10);
            gImg.setColor(tmp);

            // draws trajectory
            if(ghmm.getGestureHmm(gesName).getNumTraining() > 0)
                drawTrajectory(FeatureExtraction.convAnglesToTrajectoryInScaledArea(new Point2D.Float(centerX, centerY), showPanelSize, meanFVarray));
            else
                gImg.drawString("Hey, man.", (int) centerX - 50, (int) centerY);

            repaint();
        }

        /**
         * excutes Reset button
         */
        public void doReset(){
            String gesName = (String) gestureChoice.getSelectedItem();
            if(gesName == null || gesName.equals("") || gesName.equals("Select a gesture")){
                System.out.println("Warning: Gesture is not selected.");
                return;
            }

            ghmm.resetGesture(gesName);
            System.out.println(gesName + " is reset now.");
        }

        /**
         * excutes New menu
         */
        public void doNew(){
            ghmm = new GestureHmm(featureVectorSpace, GestureHmm.GAUSSIAN_THRESHOLD);
            gestureItems.clear();
            gestureChoice.removeAllItems();
            gestureChoice.addItem("Select a gesture");

            System.out.println("Created a new gesture set.");
      }

        /**
         * excutes Save menu
         */
        public void doSave(){
            int returnVal = fileChooser.showSaveDialog(HmmDrawingPanel.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                // do saving things here
                try{
                    FileOutputStream fos = new FileOutputStream(file.getAbsoluteFile());
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    ObjectOutputStream oos = new ObjectOutputStream(bos);

                    oos.writeObject(ghmm);
                    oos.close();
                    log.warning("Gesture HMM has been saved in " + file.getAbsoluteFile());
                } catch (IOException e){
                    e.printStackTrace();
                }
            } else {
                // canceled
            }
        }

        /**
         * excutes Save menu
         *
         * @throws ClassNotFoundException
         */
        public void doLoad() throws ClassNotFoundException{
            int returnVal = fileChooser.showOpenDialog(HmmDrawingPanel.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                // do loading things here
                try{
                    FileInputStream fis = new FileInputStream(file.getAbsoluteFile());
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    ObjectInputStream ois = new ObjectInputStream(bis);

                    ghmm = (GestureHmm) ois.readObject();
                    gestureItems.clear();
                    gestureItems.addAll(ghmm.getGestureNames());
                    gestureChoice.removeAllItems();
                    gestureChoice.addItem("Select a gesture");
                    for(String gname:gestureItems)
                        gestureChoice.addItem(gname);
                    
                    ois.close();
                    log.warning("Gesture HMM has been loaded in " + file.getAbsoluteFile());
                } catch (IOException e){
                    e.printStackTrace();
                }
            } else {
                // canceled
            }
        }

        /**
         * puts an image on the drawing panel
         *
         * @param img
         */
        public void putImage(Image img){
            clearImage();
            gImg.drawImage(img, (int) centerX - img.getWidth(this)/2, (int) centerY - img.getHeight(this)/2, this);
            repaint();

            if(timer.isRunning())
                timer.restart();
            else
                timer.start();
        }

        /**
         * action listener for timer events
         */
        ActionListener clearImageAction = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
               clearImage();
               timer.stop();
            }
        };

        @Override
        protected void initialDeco() {
            super.initialDeco();
            Color tmpColor = getColor();
            Font tmpFont = getFont();
            gImg.setFont(new Font("Arial", Font.BOLD|Font.ITALIC, 20));
            if(login){
                gImg.setColor(Color.RED);
                gImg.drawString("Active", imgPanelWidth - 100, 20);
            }else{
                gImg.setColor(Color.GRAY);
                gImg.drawString("Inactive", imgPanelWidth - 100, 20);
            }
            gImg.setColor(tmpColor);
            gImg.setFont(tmpFont);
        }


        /**
         * processes Choice events
         * @param e
         */
        public void itemStateChanged(ItemEvent e) {
            if(String.valueOf(e.getSource()).contains("gestureChoice")){
                if(e.getStateChange() == ItemEvent.SELECTED && !String.valueOf(e.getItem()).equals("Select a gesture")){
                    System.out.println("Gesture selection : " + e.getItem() + " is selected.");
                }
            } else {
                if(e.getStateChange() == ItemEvent.SELECTED && !String.valueOf(e.getItem()).equals("Select HMM model")){
                    System.out.println("HMM model selection: " + e.getItem() + " is selected.");
                }
            }
        }

        @Override
        public void windowClosing(WindowEvent we) {
            // set the window just invisible
            hmmDP.setVisible(false);
        }
    }



    /**
     * Definition of after-gesture processes
     */

    protected void doLogin(){
        hmmDP.putImage(imgHi);
        login = true;
        hmmDP.checkGestureAction.setState(login);
    }

    protected void doLogout(){
        hmmDP.putImage(imgBye);
        login = false;
        hmmDP.checkGestureAction.setState(login);
    }

    protected void doPush(){
        // for stereo vision
    }

    protected boolean doSlashUp(){
        boolean ret = true;

        if(checkActivated && startTimeGesture <= endTimePrevGesture + checkActivationTimeUs)
            hmmDP.putImage(imgCheck);
        else
            ret = false;
        checkActivated = false;

        return ret;
    }

    protected void doSlashDown(){
        checkActivated = true;
    }

    protected void doCW(){
        hmmDP.putImage(imgCW);
    }

    protected void doCCW(){
        hmmDP.putImage(imgCCW);
    }

    protected void doLeft(){
        hmmDP.putImage(imgLeft);
    }

    protected void doRight(){
        hmmDP.putImage(imgRight);
    }

    protected void doUp(){
        hmmDP.putImage(imgUp);
    }

    protected void doDown(){
        hmmDP.putImage(imgDown);
    }

    protected void doCheck(){
        hmmDP.putImage(imgCheck);
    }


}