/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.unizh.ini.jaer.projects.gesture.hmm;

import ch.unizh.ini.jaer.projects.gesture.stereo.BlurringFilterStereoTracker;
import ch.unizh.ini.jaer.projects.gesture.blurringFilter.BlurringFilter2DTracker;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.eventprocessing.EventFilter2D;
import net.sf.jaer.eventprocessing.FilterChain;
import net.sf.jaer.eventprocessing.tracking.ClusterPathPoint;

/**
 * Gesture recognition system using two DVS sensors in stereo vision.
 * BluringFilterStereoTracker is used to obtain the trajectory of moving object (eg, hand)
 * HMM is used for classification. But, HMM is not used for spoting gestures (i.e., finding the start and end timing of gestures)
 * Gesture spotting is done by the tracker by assuming that there is slow movement or abrupt change of moving direction between gestures.
 * @author Jun Haeng Lee
 */
public class GestureBFStereo extends GestureBF2D{
    /**
     * queue for disparity
     * this is to detect Push-gesture. but Push gesture is currently not supported.
     */
    protected LinkedList<Integer> disparityQueue = new LinkedList<Integer>();

    /**
     * max size of the disparity queue
     * this is to detect Push-gesture. but Push gesture is currently not supported.
     */
    protected int disparityQueueMaxSize = 10;

    /**
     * initial disparity
     */
    public final static int IDLE_STATE_DISPARITY = -1000;

    /**
     * mean disparity of the starting gesture (i.e Infinite-shaped gesture)
     */
    protected int meanDisparityOfStartGesture = IDLE_STATE_DISPARITY;

    /**
     * disparity threshold for Push-gesture
     * Push gesture is currently not supported.
     */
    protected int disparityThresholdPushGesture = 10;

    /**
     * true if Push-gesture is detected
     */
    protected boolean pushDetected = false;

    /**
     * Constructor
     * @param chip
     */
    public GestureBFStereo(AEChip chip) {
        super(chip);

        this.chip = chip;
        chip.addObserver(this);
        
        // deactivates disparity limit
        ((BlurringFilterStereoTracker) super.tracker).setEnableDisparityLimit(false);
    }

    

    @Override
    protected void filterChainSetting() {
        super.tracker = new BlurringFilterStereoTracker(chip);
        ( (EventFilter2D) super.tracker ).addObserver(this);
        setEnclosedFilterChain(new FilterChain(chip));
        getEnclosedFilterChain().add((EventFilter2D)super.tracker);
        ( (EventFilter2D) super.tracker ).setEnclosed(true,this);
        ( (EventFilter2D) super.tracker ).setFilterEnabled(isFilterEnabled());
    }

    @Override
    public void update(Observable o, Object arg) {
        if ( !(o instanceof BlurringFilterStereoTracker) )
            return;

        UpdateMessage msg = (UpdateMessage)arg;
        if( enableGestureRecognition ){
            List<BlurringFilter2DTracker.Cluster> cl = tracker.getClusters();
            ArrayList<ClusterPathPoint> path = selectClusterTrajectory(cl);

            if(path == null)
                return;

            // default trimming
            ArrayList<ClusterPathPoint> trimmedPath = trajectoryTrimmingPointBase(path, 1, 1);

            // doesn't have to classify short trajectroies
            if(trimmedPath.size() < getNumPointsThreshold())
            {
                storePath(trimmedPath, true);
                return;
            }

            // checks 2D gestures
            String bmg = null;
            if(isLogin()){
                // estimates the best matching gesture
                bmg = estimateGesture(trimmedPath);
                 // tries again with prevPath if bmg is null
                if(isUsePrevPath() && bmg == null)
                    bmg = estimateGestureWithPrevPath(trimmedPath, getMaxTimeDiffCorrSegmentsUs(), true, true);

                System.out.println("Best matching gesture is " + bmg);

                if(bmg != null){
                    if(afterRecognitionProcess(bmg, trimmedPath)){
                        endTimePrevGesture = endTimeGesture;

                        // starts or restarts the auto logout timer
                        if(isLogin()){ // has to check if the gesture recognition system is still active (to consider logout)
                            if(autoLogoutTimer.isRunning())
                                autoLogoutTimer.restart();
                            else
                                autoLogoutTimer.start();
                        }

                        pushDetected = false;
                        }
                } else
                    savePath = true;

            } else { // if the gesture recognition system is inactive, checks the start gesture only
                if(detectStartingGesture(trimmedPath)){
                    System.out.println("Gesture recognition system is enabled.");
                    afterRecognitionProcess("Infinite", trimmedPath);
                    endTimePrevGesture = endTimeGesture;

                    // set maximum disparity of the vergence filter based on mean disparity of the start gesture
                    meanDisparityOfStartGesture = findMeanDisparity(trimmedPath);
                    BlurringFilterStereoTracker tmpTracker = (BlurringFilterStereoTracker) super.tracker;
                    if(tmpTracker.getAutoThresholdSlope() >= 0){
                        tmpTracker.setDisparityLimit(meanDisparityOfStartGesture-2, true);
                    }else{
                        tmpTracker.setDisparityLimit(meanDisparityOfStartGesture+2, false);
                    }
                    tmpTracker.setEnableDisparityLimit(true);
                    pushDetected = false;
                } else
                    savePath = true;
            }
            if(savePath)
                storePath(trimmedPath, false);
            else
                resetPrevPath();
        }

        // callback to update() of any listeners on us, e.g. VirtualDrummer
        callUpdateObservers(msg.packet, msg.timestamp);
    }

    /**
     * calculates mean disparity of a trajectory
     *
     * @param path
     * @return
     */
    private int findMeanDisparity(ArrayList<ClusterPathPoint> path){
        int meanDisparity = 0;

        // calculates mean disparity using medial points only
        ArrayList<ClusterPathPoint> trimmedPath = trajectoryTrimming(path, 25, 25, FeatureExtraction.calTrajectoryLength(path));
        for(int i=0; i<trimmedPath.size(); i++){
            meanDisparity += trimmedPath.get(i).stereoDisparity;
        }

        return meanDisparity/trimmedPath.size();
    }

    @Override
    public BlurringFilterStereoTracker getTracker() {
        return (BlurringFilterStereoTracker)tracker;
    }
    

    @Override
    protected void doLogin() {
        super.doLogin();
    }

    /**
     * defines jobs to do during logout
     */
    @Override
    protected void doLogout() {
        super.doLogout();

        meanDisparityOfStartGesture = IDLE_STATE_DISPARITY;

        // deactivates lower disparity limit
        ((BlurringFilterStereoTracker) super.tracker).setEnableDisparityLimit(false);

        // releases maximum disparity change limit
//        ((BlurringFilterStereoTracker) super.tracker).setMaxDisparityChangePixels(100);
    }

    @Override
    protected void doCCW() {
        super.doCCW();
    }

    @Override
    protected void doCW() {
        super.doCW();
    }

    @Override
    protected void doCheck() {
        super.doCheck();
    }

    @Override
    protected void doUp() {
        super.doUp();
    }

    @Override
    protected void doDown() {
        super.doDown();
    }

    @Override
    protected void doLeft() {
        super.doLeft();
    }

    @Override
    protected void doRight() {
        super.doRight();
    }

    @Override
    protected void doSlashDown() {
        super.doSlashDown();
    }

    @Override
    protected void doSlashUp() {
        super.doSlashUp();
    }

    @Override
    protected void doDCCW() {
        super.doDCCW();
    }

    @Override
    protected void doDCW() {
        super.doDCW();
    }


    /**
     * defines jobs to do when Push is detected
     */
    @Override
    protected void doPush() {
        super.doPush(); 
    }
}
