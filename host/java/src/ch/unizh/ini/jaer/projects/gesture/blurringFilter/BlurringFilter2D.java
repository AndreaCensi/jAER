/*
 * Last updated on April 23, 2010, 11:40 AM
 *
 *  * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.unizh.ini.jaer.projects.gesture.blurringFilter;

import ch.unizh.ini.jaer.projects.gesture.blurringFilter.LIFNeuronJHLee.ADAPTATION_TYPE;
import com.sun.opengl.util.GLUT;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Point2D.Float;
import javax.media.opengl.GL;
import java.awt.geom.Point2D;
import java.util.*;
import javax.media.opengl.GLAutoDrawable;
import net.sf.jaer.chip.*;
import net.sf.jaer.event.*;
import net.sf.jaer.eventprocessing.EventFilter2D;
import net.sf.jaer.graphics.FrameAnnotater;

/**
 * Finds clusters of events using spatio-temporal correlation between events.
 * Events occured within the specified area (called a LIFNeuron) are considered strongly correalated.
 * How much the events are correlated is evaluated using a parameter called 'membranePotential'.
 * By thresholding the membranePotential of a neuron, firing neurons can be defined.
 * Then the clusters of events can be detected from the neuron groups.
 * The neuron group is a group of firing neurons which are linked each other. (If two adjacent neurons are firing at the same time, they are linked).
 * (Notice) BlurringFilter2D is a cluster finder rather than a filter. It does NOT filters any events.
 *
 * @author Jun Haeng Lee
 */
public class BlurringFilter2D extends EventFilter2D implements FrameAnnotater, Observer {

    /**
     * Time constant of LIF neurons membrane potential. It decays exponetially unless a new event is added.
     */
    protected int MPTimeConstantUs = getPrefs().getInt("BlurringFilter2D.MPTimeConstantUs", 22000);

    /**
     * initial value of the membrane potential in percents of the MPThreshold
     */
    protected float MPInitialPercnetTh = getPrefs().getFloat("BlurringFilter2D.MPInitialPercnetTh", 50.0f);

    /**
     * Life time of LIF neuron.
     * A neuron will be reset if there is no additional event within this value of micro seconds since the last update.
     */
    protected int neuronLifeTimeUs = getPrefs().getInt("BlurringFilter2D.neuronLifeTimeUs", 2000000);

    /**
     * threshold of membrane potetial required for firing.
     */
    private int MPThreshold = getPrefs().getInt("BlurringFilter2D.MPThreshold", 60);

    /**
     * size of the receptive field of an LIF neuron.
     */
    protected int receptiveFieldSizePixels = getPrefs().getInt("BlurringFilter2D.receptiveFieldSizePixels", 8);

    /**
     * half size of the receptive field of an LIF neuron.
     * We are going to use this parameter mostly than receptiveFieldSizePixels.
     */
    protected int halfReceptiveFieldSizePixels;

    /**
     * Membrane potential of a neuron jumps down by this amount after firing.
     */
    protected float MPJumpAfterFiringPercentTh = getPrefs().getFloat("BlurringFilter2D.MPJumpAfterFiringPercentTh", 10.0f);

    /**
     * if true, the receptive field of firing neurons are displayed on the screen.
     */
    private boolean showFiringNeurons = getPrefs().getBoolean("BlurringFilter2D.showFiringNeurons", false);

    /**
     * if true, the receptive field of firing neurons are displayed with filled sqaures.
     * if false, they are shown with hallow squares.
     */
    private boolean filledReceptiveField = getPrefs().getBoolean("BlurringFilter2D.filledReceptiveField", true);

    /**
     * shows neurons with firing type of FIRING_ON_BORDER only.
     */
    private boolean showBorderNeuronsOnly = getPrefs().getBoolean("BlurringFilter2D.showBorderNeuronsOnly", true);

    /**
     * shows neurons with firing type of FIRING_INSIDE only.
     */
    private boolean showInsideNeuronsOnly = getPrefs().getBoolean("BlurringFilter2D.showInsideNeuronsOnly", true);

    /**
     * shows MPThreshold.
     */
    private boolean showMPThreshold = getPrefs().getBoolean("BlurringFilter2D.showMPThreshold", false);

    /**
     * color to draw the receptive field of firing neurons
     */
    private COLOR_CHOICE colorToDrawRF = COLOR_CHOICE.valueOf(getPrefs().get("BlurringFilter2D.colorToDrawRF", COLOR_CHOICE.orange.toString()));

    /**
     * if true, uses Adaptation.
     */
    private boolean enableAdaptation = getPrefs().getBoolean("BlurringFilter2D.enableAdaptation", false);

    /**
     * type of adaptation
     */
    private ADAPTATION_TYPE adaptationType = ADAPTATION_TYPE.valueOf(getPrefs().get("BlurringFilter2D.adaptationType", ADAPTATION_TYPE.REFRACTORY_PERIOD.toString()));

    /**
     * time constant of LIF neuron's adaptation parameter
     */
    private float adaptationParamTauMs = getPrefs().getFloat("BlurringFilter2D.adaptationParamTauMs", 10.0f);

    /**
     * max value of the control parameter for LIF neuron's adaptation parameter
     */
    private float adaptationParamMax = getPrefs().getFloat("BlurringFilter2D.adaptationParamMax", 100.0f);

    /**
     * delta of the control parameter for LIF neuron's adaptation parameter
     */
    private float adaptationParamDelta = getPrefs().getFloat("BlurringFilter2D.adaptationParamDelta", 0.1f);

    /**
     * slope of the control parameter for LIF neuron's adaptation parameter
     */
    private float adaptationParamSlop = getPrefs().getFloat("BlurringFilter2D.adaptationParamSlop", 30.0f);

    /**
     * maximum value of LIF neuron's refractory period
     */
    private float RefractoryPeriodMaxMs = getPrefs().getFloat("BlurringFilter2D.RefractoryPeriodMaxMs", 1.0f);


    /**
     * names of color
     */
    public static enum COLOR_CHOICE {black, blue, cyan, darkgray, gray, green, lightgray, magenta, orange, pink, red, white, yellow}

    /**
     * A map containing a mapping from color names to color values
     */
     private final static HashMap<COLOR_CHOICE, Color> colors = new HashMap<COLOR_CHOICE, Color>();

     /**
      * The base set of colors
      */
     static {
        colors.put(COLOR_CHOICE.black, Color.black);
        colors.put(COLOR_CHOICE.blue, Color.blue);
        colors.put(COLOR_CHOICE.cyan, Color.cyan);
        colors.put(COLOR_CHOICE.darkgray, Color.darkGray);
        colors.put(COLOR_CHOICE.gray, Color.gray);
        colors.put(COLOR_CHOICE.green, Color.green);
        colors.put(COLOR_CHOICE.lightgray, Color.lightGray);
        colors.put(COLOR_CHOICE.magenta, Color.magenta);
        colors.put(COLOR_CHOICE.orange, Color.orange);
        colors.put(COLOR_CHOICE.pink, Color.pink);
        colors.put(COLOR_CHOICE.red, Color.red);
        colors.put(COLOR_CHOICE.white, Color.white);
        colors.put(COLOR_CHOICE.yellow, Color.yellow);
    }

    /**
     * RGB value of color
     */
     protected float[] rgb = new float[4];



    /**
     * Constants to define neighbor neurons.
     * upper neighbor.
     */
    static int UPDATE_UP = 0x01;
    /**
     * Constants to define neighbor neurons.
     * lower neighbor.
     */
    static int UPDATE_DOWN = 0x02;
    /**
     * Constants to define neighbor neurons.
     * right neighbor.
     */
    static int UPDATE_RIGHT = 0x04;
    /**
     * Constants to define neighbor neurons.
     * left neighbor.
     */
    static int UPDATE_LEFT = 0x08;




    /**
     * DVS Chip
     */
    protected AEChip mychip;

    /**
     * number of neurons in x (column) directions.
     */
    protected int numOfNeuronsX = 0;

    /**
     * number of neurons in y (row) directions.
     */
    protected int numOfNeuronsY = 0;

    /**
     * array of neurons (numOfNeuronsX x numOfNeuronsY)
     */
    protected ArrayList<LIFNeuron> lifNeurons = new ArrayList<LIFNeuron>();

    /**
     * index of firing neurons
     */
//    private HashSet<Integer> firingNeurons = new HashSet();

    /**
     * neuron groups found
     */
    private HashMap<Integer, NeuronGroup> neuronGroups = new HashMap<Integer, NeuronGroup>();

    /**
     * number of neuron groups found
     */
    protected int numOfGroup = 0;

    /**
     * last updat time. It is the timestamp of the latest event.
     */
    protected int lastTime;

    /**
     * random
     */
    protected Random random = new Random();




    /**
     * Constructor of BlurringFilter2D
     * @param chip
     */
    public BlurringFilter2D(AEChip chip) {
        super(chip);
        this.mychip = chip;

        // initializes filter
        initFilter();
        colors.get(colorToDrawRF).getRGBComponents(rgb);

        // adds this class as an observer
        chip.addObserver(this);
        addObserver(this);

        // adds tooltips
        final String lif_neuron = "LIF Neuron", adapt = "Adaptation", disp = "Display";

        setPropertyTooltip(lif_neuron, "MPTimeConstantUs", "Time constant of LIF neurons membrane potential. It decays exponetially unless a new event is added.");
        setPropertyTooltip(lif_neuron, "neuronLifeTimeUs", "A neuron will be reset if there is no additional event within this value of micro seconds since the last update.");
        setPropertyTooltip(lif_neuron, "MPThreshold", "threshold of membrane potetial required for firing.");
        setPropertyTooltip(lif_neuron, "MPInitialPercnetTh", "initial value of the membrane potential in percents of the MPThreshold.");
        setPropertyTooltip(lif_neuron, "MPJumpAfterFiringPercentTh", "Membrane potential decrease of a neuron in percents of its threshold after firing.");
        setPropertyTooltip(lif_neuron, "receptiveFieldSizePixels", "size of the receptive field of an LIF neuron.");

        setPropertyTooltip(disp, "showFiringNeurons", "if true, the receptive field of firing neurons are displayed on the screen.");
        setPropertyTooltip(disp, "filledReceptiveField", "if true, the receptive field of firing neurons are displayed with filled sqaures. Otherwise, they are shown with hallow squares.");
        setPropertyTooltip(disp, "showBorderNeuronsOnly", "shows neurons with firing type of FIRING_ON_BORDER only.");
        setPropertyTooltip(disp, "showInsideNeuronsOnly", "shows neurons with firing type of FIRING_INSIDE only.");
        setPropertyTooltip(disp, "colorToDrawRF", "color to draw the receptive field of firing neurons");
        setPropertyTooltip(disp, "showMPThreshold", "shows membrane potential threshold to fire a spike");

        setPropertyTooltip(adapt, "enableAdaptation", "enables LIF neuron's refractory period");
        setPropertyTooltip(adapt, "adaptationType", "selects adaptation type");
        setPropertyTooltip(adapt, "adaptationParamTauMs", "time constant of LIF neuron's adaptation parameter");
        setPropertyTooltip(adapt, "adaptationParamMax", "max value of the control parameter for LIF neuron's adaptation parameter");
        setPropertyTooltip(adapt, "adaptationParamDelta", "delta of the control parameter for LIF neuron's adaptation parameter");
        setPropertyTooltip(adapt, "adaptationParamSlop", "slop of the control parameter for LIF neuron's adaptation parameter");
        setPropertyTooltip(adapt, "RefractoryPeriodMaxMs", "maximum value of LIF neuron's refractory period");
    }

    @Override
    public String toString() {
        String s = lifNeurons != null ? Integer.toString(numOfNeuronsX).concat(" by ").concat(Integer.toString(numOfNeuronsY)) : null;
        String s2 = "BlurringFilter2D with " + s + " neurons ";
        return s2;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o == this) {
            UpdateMessage msg = (UpdateMessage) arg;
            updateNeurons(msg.timestamp); // at least once per packet update list
        } else if (o instanceof AEChip) {
            initFilter();
        }
    }

    /**
     * Definition of location types of LIF neurons
     * CORNER_* : neurons that are located in corners
     * EDGE_* : neurons that are located in edges
     * INSIDE: all neurons except corner and egde neurons
     */
    public static enum LocationType {
        CORNER_00, CORNER_01, CORNER_10, CORNER_11, EDGE_0Y, EDGE_1Y, EDGE_X0, EDGE_X1, INSIDE
    }

    /**
     * Definition of firing types of neurons
     */
    public static enum FiringType {

        /**
         * does not fire due to the low membrane potential
         */
        SILENT,
        /**
         * fires alone. Its neighbor neurons don't fire.
         */
        FIRING_ISOLATED,
        /**
         * fires together with at least one of its neighbors
         */
        FIRING_WITH_NEIGHBOR,
        /**
         * firing neuron which makes the boundary of a group of simultaneously firing neurons
         */
        FIRING_ON_BORDER,
        /**
         * non-border firing neuron which belongs a group of simultaneously firing neurons
         */
        FIRING_INSIDE
    }

    /**
     * Firing type update type
     */
    static enum FiringTypeUpdate {
        /**
         * updates forcibly
         */
        FORCED,
        /**
         * updates if necessary based on the current condition
         */
        CHECK
    }
    
    /**
     * Neuron group addion mode
     */
    public static enum Add_Mode {
        /**
         * group location is decided by averging membrane potential of neurons.
         * This is used for uder threshold tracking.
         */
        MEMBRANE_POTENTAIL_AVERAGE,
        /**
         * group location is decided by averging firing rate of neurons.
         */
        FIRING_RATE_AVERAGE;
    }

    /**
     * Definition of leaky integrate and fire (LIF) neuron.
     * The receptive field is a partial area of events-occuring space.
     * Events within the receptive field of a neuron are considered strongly correalated.
     * Spacing of the receptive field of two adjacent LIF neurons is decided to the half of side length of the receptive field to increase the spatial resolution.
     * Thus, each neuron shares half area of the receptive field with its neighbor.
     */
    public class LIFNeuron extends LIFNeuronJHLee{

        /**
         * Neuron index in (x_index, y_index)
         */
        public Point2D.Float index = new Point2D.Float();

        /**
         *  spatial location of a neuron in chip pixels
         */
        public Point2D.Float location = new Point2D.Float();

        /**
         * location type of a neuron. One of {CORNER_00, CORNER_01, CORNER_10, CORNER_11, EDGE_0Y, EDGE_1Y, EDGE_X0, EDGE_X1, INSIDE}
         */
        LocationType locationType;

        /**
         * firing type of a neuron. One of {SILENT, FIRING_ISOLATED, FIRING_WITH_NEIGHBOR, FIRING_ON_BORDER, FIRING_INSIDE}
         */
        FiringType firingType;

        /**
         * Tag to identify the group which the neuron belongs to.
         */
        protected int groupTag = -1;

        /**
         * true if the neuron fired a spike.
         */
        protected boolean fired = false;

        /**
         *  number of firing neighbors
         */
        protected int numFiringNeighbors = 0;


        /**
         * Construct an LIF neuron with index.
         *
         * @param cellNumber : cell number
         * @param index : cell index
         * @param location : location on DVS pixels (x,y)
         * @param numSynapses : number of synapses
         * @param tauMP : RC time constant of the membrane potential
         * @param thresholdMP : threshold of the membrane potential to fire a spike
         * @param MPDecreaseArterFiringPercentTh : membrane potential jump after the spike in the percents of thresholdMP
         */
        public LIFNeuron(int cellNumber, Point2D.Float index, Point2D.Float location, float tauMP, float thresholdMP, float MPDecreaseArterFiringPercentTh) {
            super(cellNumber, tauMP, thresholdMP, MPDecreaseArterFiringPercentTh);

            // sets invariable parameters
            this.index.setLocation(index);
            this.location.setLocation(location);

            setFiringType(FiringType.SILENT);
            groupTag = -1;
            fired = false;
            membranePotential = MPInitialPercnetTh*thresholdMP;
            numFiringNeighbors = 0;
        }

        /**
        * Resets a neuron with initial values
        */
        @Override
        public void reset() {
            setFiringType(FiringType.SILENT);
            resetGroupTag();
            fired = false;
            membranePotential = MPInitialPercnetTh*thresholdMP;
            numFiringNeighbors = 0;
            lastEventTimestamp = 0;
            numSpikes = 0;
            RefractoryPeriod = 0;
            adaptationParam = 0;
            lastAboveThresholdTimestamp = -1;
            lastSpikeTimestamp = 0;
        }


        /** Draws the neuron using OpenGL.
         *
         * @param drawable area to drawReceptiveField this.
         */
        public void drawReceptiveField(GLAutoDrawable drawable) {
            final float BOX_LINE_WIDTH = 2f; // in chip
            GL gl = drawable.getGL();

            // set color and line width
            gl.glColor3fv(rgb, 0);
            gl.glLineWidth(BOX_LINE_WIDTH);

            // draws the receptive field of a neuron
            gl.glPushMatrix();
            gl.glTranslatef((int) getLocation().x, (int) getLocation().y, 0);

            if (filledReceptiveField) {
                gl.glBegin(GL.GL_QUADS);
            } else {
                gl.glBegin(GL.GL_LINE_LOOP);
            }

            int halfSize = (int) receptiveFieldSizePixels/2;
            gl.glVertex2i(-halfSize, -halfSize);
            gl.glVertex2i(+halfSize, -halfSize);
            gl.glVertex2i(+halfSize, +halfSize);
            gl.glVertex2i(-halfSize, +halfSize);

            gl.glEnd();
            gl.glPopMatrix();
        }

        /**
         * returns firing type
         *
         * @return
         */
        private FiringType getFiringType() {
            return firingType;
        }

        /**
         * sets firing type
         *
         * @param firingType
         */
        private void setFiringType(FiringType firingType) {
            this.firingType = firingType;
        }

        /**
         * sets the firing type of the neuron to FIRING_ON_BORDER.
         * If neuronFiringTypeUpdateType is FiringTypeUpdate.CHECK, an inside neuron cannot be a border neuron.
         *
         * @param groupTag
         * @param neuronFiringTypeUpdateType
         */
        private void setFiringTypeToBorder(int groupTag, FiringTypeUpdate neuronFiringTypeUpdateType) {
            if (neuronFiringTypeUpdateType == FiringTypeUpdate.CHECK) {
                if (this.firingType != FiringType.FIRING_INSIDE) {
                    setFiringType(FiringType.FIRING_ON_BORDER);
                }
            } else {
                setFiringType(FiringType.FIRING_ON_BORDER);
            }
            setGroupTag(groupTag);
        }

        /**
         * returns true if the neuron fired a spike.
         * Otherwise, returns false.
         *
         * @return
         */
        final public boolean isFired() {
            return fired;
        }

        /**
         * checks if the neuron's membrane potential is above the threshold
         *
         * @return
         */
        public boolean isAboveThreshold() {
            if(numSpikes == 0){
                fired = false;
                firingType = FiringType.SILENT;
            }else{
                // fires a spike
                fired = true;
                firingType = FiringType.FIRING_ISOLATED;
            }

            // reset groupTag
            groupTag = -1;

            return fired;
        }

        @Override
        public String toString() {
            return String.format("LIF Neuron number=%d index=(%d, %d), location = (%d, %d), membrane potential = %.2f",
                    index,
                    (int) index.x, (int) index.y,
                    (int) location.x, (int) location.y,
                    getMPNow(lastTime));
        }


        /**
         * returns index
         *
         * @return
         */
        public Float getIndex() {
            return index;
        }

        /**
         * returns the neuron's location in pixels.
         *
         * @return
         */
        final public Point2D.Float getLocation() {
            return location;
        }

        /**
         * returns location type
         *
         * @return
         */
        private LocationType getLocationType() {
            return locationType;
        }

        /**
         * sets location type
         *
         * @param locationType
         */
        private void setLocationType(LocationType locationType) {
            this.locationType = locationType;
        }

        /**
         * returns the number of simutaneously firing neighbors
         *
         * @return
         */
        public int getNumFiringNeighbors() {
            return numFiringNeighbors;
        }

        /**
         * sets the number of simutaneously firing neighbors
         *
         * @param numFiringNeighbors
         */
        public void setNumFiringNeighbors(int numFiringNeighbors) {
            this.numFiringNeighbors = numFiringNeighbors;
        }

        /**
         * increases the number of firing neighbors
         *
         */
        public void increaseNumFiringNeighbors() {
            if(!fired)
                return;

            numFiringNeighbors++;
            if (firingType != FiringType.FIRING_ON_BORDER) {
                firingType = FiringType.FIRING_WITH_NEIGHBOR;
            }
        }

        /**
         * returns the group tag
         *
         * @return group tag
         */
        public int getGroupTag() {
            return groupTag;
        }

        /**
         * sets the group tag
         *
         * @param groupTag
         */
        public void setGroupTag(int groupTag) {
            // If groupTag is a negative value, give a new group tag
            if (groupTag < 0) {
                if (this.groupTag < 0) {
                    this.groupTag = numOfGroup;
                    numOfGroup++;
                }
            } else {
                this.groupTag = groupTag;
            }
        }

        /**
         * resets group tag
         *
         */
        public void resetGroupTag() {
            this.groupTag = -1;
        }
    } // End of class LIFNeuron



    /** Definition of NeuronGroup
     * NeuronGroup is a group of simultaneously firing neurons which are linked each other.
     * Any two neighboring neurons are called linked if they are firing simultaneously.
     * Each member neuron within the NeuronGroup has its FiringType which is one of {FIRING_ON_BORDER, FIRING_INSIDE}.
     * Member neurons with FIRING_ON_BORDER are the border neurons making the boundary of the group.
     * All member neurons except the border neurons should have FIRING_INSIDE type.
     * NeuronGroups are utilized as a basis for finding clusters.
     */
    public class NeuronGroup {
        /**
         * location of the group in chip pixels.
         * Center of member neurons location weighted by their membranePotential.
         */
        public Point2D.Float location = new Point2D.Float();

        /**
         * Sum of the membranePotential of all member neurons.
         */
        protected float totalMP;

        /**
         * This is the last time in timestamp ticks that the group was updated by an event.
         * The largest one among the lastUpdateTime of all member neurons becomes groups's lastEventTimestamp.
         */
        protected int lastEventTimestamp;

        /** Parameters to represent the area of the group.
         * minX(Y) : minimum X(Y) among the locations of member neurons
         * maxX(Y) : maximum X(Y) among the locations of member neurons
         */
        protected float minX, maxX, minY, maxY;

        /**
         *Group number (index)
         */
        protected int tag;

        /**
         *Indicates if this group is hitting edge
         */
        protected boolean hitEdge = false;

        /**
         * used in tracker
         * When a tracked cluster registered this group as its next cluster, it sets this value true.
         * Then, other clusters cannot consider this group as its next one.
         */
        protected boolean matched = false;

        /**
         * Member neurons consisting of this group
         */
        HashSet<LIFNeuron> memberNeurons = null;


        @Override
        public String toString() {
            return String.format("Neuron Group tag=%d, location = (%d, %d), totalMP = %.2f",
                    tag,
                    (int) location.x, (int) location.y,
                    totalMP);
        }


        /**
         * Constructor of Neurongroup
         */
        public NeuronGroup() {
            memberNeurons = new HashSet();
            reset();
        }

        /**
         * constructor with the first member
         *
         * @param firstNeuron
         * @param mode
         */
        public NeuronGroup(LIFNeuron firstNeuron, Add_Mode mode) {
            this();
            add(firstNeuron, mode);
        }

        /**
         * resets the neuron group
         *
         */
        public final void reset() {
            location.setLocation(-1f, -1f);
            totalMP = 0;
            tag = -1;
            memberNeurons.clear();
            maxX = maxY = 0;
            minX = chip.getSizeX();
            minY = chip.getSizeX();
            hitEdge = false;
            matched = false;
        }

        /**
         * adds a neuron into the group
         *
         * @param newNeuron
         * @param mode
         */
        public final void add(LIFNeuron newNeuron, Add_Mode mode) {
            // if this is the first one
            float effectiveMP;
            if(newNeuron.MPDecreaseArterFiringPercentTh == 0){
                effectiveMP = newNeuron.getMP();
            } else {
                if(mode == Add_Mode.MEMBRANE_POTENTAIL_AVERAGE)
                    effectiveMP = newNeuron.getMP() + newNeuron.numSpikes;
                else
                    effectiveMP = newNeuron.numSpikes;
            }
            
            if (tag < 0) {
                if(mode == Add_Mode.MEMBRANE_POTENTAIL_AVERAGE)
                    tag = numOfNeuronsX*numOfNeuronsY;
                else
                    tag = newNeuron.getGroupTag();

                lastEventTimestamp = newNeuron.getLastEventTimestamp();
                location.setLocation(newNeuron.getLocation());
                totalMP = effectiveMP;
            } else { // if this is not the first one
                // if this neuron has been added before, it doesn't have to be added again.
                if(memberNeurons.contains(newNeuron))
                    return;

                float prevMP = totalMP;
                float leakyFactor;

                if (lastEventTimestamp < newNeuron.getLastEventTimestamp()) {
                    leakyFactor = (float) Math.exp(((float) lastEventTimestamp - newNeuron.getLastEventTimestamp()) / MPTimeConstantUs);
                    totalMP = effectiveMP + prevMP * leakyFactor;
                    location.x = (newNeuron.location.x * effectiveMP + location.x * prevMP * leakyFactor) / (totalMP);
                    location.y = (newNeuron.location.y * effectiveMP + location.y * prevMP * leakyFactor) / (totalMP);

                    lastEventTimestamp = newNeuron.getLastEventTimestamp();
                } else {
                    leakyFactor = (float) Math.exp(((float) newNeuron.getLastEventTimestamp() - lastEventTimestamp) / MPTimeConstantUs);
                    totalMP = prevMP + effectiveMP * leakyFactor;
                    location.x = (newNeuron.location.x * effectiveMP * leakyFactor + location.x * prevMP) / (totalMP);
                    location.y = (newNeuron.location.y * effectiveMP * leakyFactor + location.y * prevMP) / (totalMP);
                }
            }

            // updates boundary of the group
            if (newNeuron.getLocation().x < minX) {
                minX = newNeuron.getLocation().x;
            } else if(newNeuron.getLocation().x > maxX) {
                maxX = newNeuron.getLocation().x;
            } else {}

            if (newNeuron.getLocation().y < minY) {
                minY = newNeuron.getLocation().y;
            } else if (newNeuron.getLocation().y > maxY) {
                maxY = newNeuron.getLocation().y;
            } else {}

            // check if this group is hitting edges
            if (!hitEdge && ((int) newNeuron.getIndex().x == 0 || (int) newNeuron.getIndex().y == 0 || (int) newNeuron.getIndex().x == numOfNeuronsX - 1 || (int) newNeuron.getIndex().y == numOfNeuronsY - 1)) {
                hitEdge = true;
            }

            memberNeurons.add(newNeuron);

        }

        /**
         * merges two groups
         *
         * @param targetGroup
         */
        public void merge(NeuronGroup targetGroup) {
            if (targetGroup == null) {
                return;
            }

            float prevMP = totalMP;
            float leakyFactor;

            if (lastEventTimestamp < targetGroup.lastEventTimestamp) {
                leakyFactor = (float) Math.exp(((float) lastEventTimestamp - targetGroup.lastEventTimestamp) / MPTimeConstantUs);
                totalMP = targetGroup.totalMP + totalMP * leakyFactor;
                location.x = (targetGroup.location.x * targetGroup.totalMP + location.x * prevMP * leakyFactor) / (totalMP);
                location.y = (targetGroup.location.y * targetGroup.totalMP + location.y * prevMP * leakyFactor) / (totalMP);

                lastEventTimestamp = targetGroup.lastEventTimestamp;
            } else {
                leakyFactor = (float) Math.exp(((float) targetGroup.lastEventTimestamp - lastEventTimestamp) / MPTimeConstantUs);
                totalMP += (targetGroup.totalMP * leakyFactor);
                location.x = (targetGroup.location.x * targetGroup.totalMP * leakyFactor + location.x * prevMP) / (totalMP);
                location.y = (targetGroup.location.y * targetGroup.totalMP * leakyFactor + location.y * prevMP) / (totalMP);
            }

            if (targetGroup.minX < minX) {
                minX = targetGroup.minX;
            }
            if (targetGroup.maxX > maxX) {
                maxX = targetGroup.maxX;
            }
            if (targetGroup.minY < minY) {
                minY = targetGroup.minY;
            }
            if (targetGroup.maxY > maxY) {
                maxY = targetGroup.maxY;
            }

            for(LIFNeuron tmpNeuron : targetGroup.getMemberNeurons()) {
                tmpNeuron.setGroupTag(tag);
                memberNeurons.add(tmpNeuron);
            }

            targetGroup.reset();
        }

        /**
         * calculates the distance between two groups in pixels
         *
         * @param targetGroup
         * @return
         */
        public float locationDistancePixels(NeuronGroup targetGroup) {
            return (float) Math.sqrt(Math.pow(location.x - targetGroup.location.x, 2.0) + Math.pow(location.y - targetGroup.location.y, 2.0));
        }

        /**
         * returns memberNeurons
         *
         * @return
         */
        public HashSet<LIFNeuron> getMemberNeurons() {
            return memberNeurons;
        }

        /**
         * returns the number of member neurons
         *
         * @return
         */
        public int getNumMemberNeurons() {
            return memberNeurons.size();
        }

        /**
         * returns the group membranePotential.
         * Time constant is not necessary.
         *
         * @return
         */
        public float getTotalMP() {
            return totalMP;
        }

        /**
         * returns the last event timestamp of the group.
         *
         * @return
         */
        public int getLastEventTimestamp() {
            return lastEventTimestamp;
        }

        /**
         * returns the location of the group.
         *
         * @return
         */
        public Float getLocation() {
            return location;
        }

        /**
         * returns the inner radius of the group.
         *
         * @return
         */
        public float getInnerRadiusPixels() {
            return Math.min(Math.min(Math.abs(location.x - minX), Math.abs(location.x - maxX)), Math.min(Math.abs(location.y - minY), Math.abs(location.y - maxY)));
        }

        /**
         * returns the outter radius of the group.
         *
         * @return
         */
        public float getOutterRadiusPixels() {
            return Math.max(Math.max(Math.abs(location.x - minX), Math.abs(location.x - maxX)), Math.max(Math.abs(location.y - minY), Math.abs(location.y - maxY)));
        }

        /**
         * returns dimension the group
         *
         * @return
         */
        public Dimension getDimension(){
            Dimension ret = new Dimension();

            ret.width = (int) (maxX - minX);
            ret.height = (int) (maxY - minY);

            return ret;
        }

        /**
         * returns the raidus of the group area by assuming that the shape of the group is a square.
         *
         * @return
         */
        public float getAreaRadiusPixels() {
            return (float) Math.sqrt((float) getNumMemberNeurons()) * halfReceptiveFieldSizePixels / 2;
        }

        /**
         * checks if the targer location is within the inner radius of the group.
         *
         * @param targetLoc
         * @return
         */
        public boolean isWithinInnerRadius(Float targetLoc) {
            boolean ret = false;
            float innerRaidus = getInnerRadiusPixels();

            if (Math.abs(location.x - targetLoc.x) <= innerRaidus && Math.abs(location.y - targetLoc.y) <= innerRaidus) {
                ret = true;
            }

            return ret;
        }

        /**
         * checks if the targer location is within the outter radius of the group.
         *
         * @param targetLoc
         * @return
         */
        public boolean isWithinOuterRadius(Float targetLoc) {
            boolean ret = false;
            float outterRaidus = getOutterRadiusPixels();

            if (Math.abs(location.x - targetLoc.x) <= outterRaidus && Math.abs(location.y - targetLoc.y) <= outterRaidus) {
                ret = true;
            }

            return ret;
        }

        /**
         * checks if the targer location is within the area radius of the group.
         *
         * @param targetLoc
         * @return
         */
        public boolean isWithinAreaRadius(Float targetLoc) {
            boolean ret = false;
            float areaRaidus = getAreaRadiusPixels();

            if (Math.abs(location.x - targetLoc.x) <= areaRaidus && Math.abs(location.y - targetLoc.y) <= areaRaidus) {
                ret = true;
            }

            return ret;
        }

        /**
         * checks if the group contains the given event.
         * It checks the location of the events
         * @param ev
         * @return
         */
/*        public boolean contains(BasicEvent ev) {
            boolean ret = false;

            int subIndexX = (int) ev.getX() / halfReceptiveFieldSizePixels;
            int subIndexY = (int) ev.getY() / halfReceptiveFieldSizePixels;

            if (subIndexX >= numOfNeuronsX && subIndexY >= numOfNeuronsY) {
                ret = false;
            }

            if (!ret && subIndexX != numOfNeuronsX && subIndexY != numOfNeuronsY) {
                ret = firingNeurons.contains(subIndexX + subIndexY * numOfNeuronsX);
            }
            if (!ret && subIndexX != numOfNeuronsX && subIndexY != 0) {
                ret = firingNeurons.contains(subIndexX + (subIndexY - 1) * numOfNeuronsX);
            }
            if (!ret && subIndexX != 0 && subIndexY != numOfNeuronsY) {
                ret = firingNeurons.contains(subIndexX - 1 + subIndexY * numOfNeuronsX);
            }
            if (!ret && subIndexY != 0 && subIndexX != 0) {
                ret = firingNeurons.contains(subIndexX - 1 + (subIndexY - 1) * numOfNeuronsX);
            }

            return ret;
        }
*/
        /**
         * returns true if the group contains neurons which locate on edges or corners.
         *
         * @return
         */
        public boolean isHitEdge() {
            return hitEdge;
        }

        /**
         * Returns true if the group is matched to a cluster.
         * This is used in a tracker module.
         *
         * @return
         */
        public boolean isMatched() {
            return matched;
        }

        /**
         * Sets true if the group is matched to a cluster
         * So, other cluster cannot take this group as a cluster
         * @param matched
         */
        public void setMatched(boolean matched) {
            this.matched = matched;
        }
    } // End of class NeuronGroup



    /**
     * Processes the incoming events to have blurring filter output after first running the blurring to update the neurons.
     *
     * @param in the input packet.
     * @return the packet after filtering by the enclosed FilterChain.
     */
    @Override
    synchronized public EventPacket<?> filterPacket (EventPacket<?> in){
        if ( in == null ){
            return in;
        }

        if ( enclosedFilter != null )
            in = getEnclosedFilterChain().filterPacket(in);

        return blurring(in);
    }

    /** Allocate the incoming events into the neurons
     *
     * @param in the input packet of BasicEvent
     * @return the original input packet
     */
    synchronized protected EventPacket<?> blurring(EventPacket<?> in) {
        if(in == null)
            return in;

        if (in.getSize() == 0) {
            return in;
        }

        try {
            // add events to the corresponding neuron
            for(int i=0; i<in.getSize(); i++){
                BasicEvent ev = in.getEvent(i);
                lastTime = ev.getTimestamp();

                int subIndexX = (int) (ev.getX() / halfReceptiveFieldSizePixels);
                if (subIndexX == numOfNeuronsX)
                    subIndexX--;
                int subIndexY = (int) (ev.getY() / halfReceptiveFieldSizePixels);
                if (subIndexY == numOfNeuronsY)
                    subIndexY--;

                if (subIndexX >= numOfNeuronsX && subIndexY >= numOfNeuronsY) {
                    initFilter();
                    return in;
                }

                if (subIndexX != numOfNeuronsX && subIndexY != numOfNeuronsY) {
                    lifNeurons.get(subIndexX + subIndexY * numOfNeuronsX).addEvent(ev, 1.0f);
                }
                if (subIndexX != numOfNeuronsX && subIndexY != 0) {
                    lifNeurons.get(subIndexX + (subIndexY - 1) * numOfNeuronsX).addEvent(ev, 1.0f);
                }
                if (subIndexX != 0 && subIndexY != numOfNeuronsY) {
                    lifNeurons.get(subIndexX - 1 + subIndexY * numOfNeuronsX).addEvent(ev, 1.0f);
                }
                if (subIndexY != 0 && subIndexX != 0) {
                    lifNeurons.get(subIndexX - 1 + (subIndexY - 1) * numOfNeuronsX).addEvent(ev, 1.0f);
                }
                
                maybeCallUpdateObservers(in, lastTime);

            }
        } catch (IndexOutOfBoundsException e) {
            initFilter();
            // this is in case neuron list is modified by real time filter during updating neurons
            log.warning(e.getMessage());
        }

        return in;
    }


    /**
     * Updates all neurons at time t.
     *
     * Checks if the neuron is firing.
     * Checks if the neuron has (a) simultaneously firing neighbor(s).
     * Checks if the neuron belongs to a group.
     * Set the neuron's firing type based on the test results.
     *
     * @param t
     */
    synchronized protected void updateNeurons(int t) {
        // makes the list of firing neurons and neuron groups empty before update
        neuronGroups.clear();
        // resets number of group before starting update
        numOfGroup = 0;

       if (!lifNeurons.isEmpty()) {
//            int timeSinceSupport;

            LIFNeuron upNeuron, downNeuron, leftNeuron, rightNeuron;
            for(int i=0; i<lifNeurons.size(); i++){
                LIFNeuron tmpNeuron = lifNeurons.get(i);
//                try {
                    // reset stale neurons
//                    timeSinceSupport = t - tmpNeuron.lastEventTimestamp;
//                    if (timeSinceSupport > neuronLifeTimeUs) {
//                        tmpNeuron.reset();
//                    }

                    int id = tmpNeuron.id;

                    tmpNeuron.numFiringNeighbors = 0;

                    switch (tmpNeuron.locationType) {
                        case CORNER_00:
                            // gets neighbor neurons
                            upNeuron = lifNeurons.get(id + numOfNeuronsX);
                            rightNeuron = lifNeurons.get(id + 1);

                            // checks the threshold of the first neuron
                            tmpNeuron.isAboveThreshold();

                            // checks upNeuron
                            if (upNeuron.isAboveThreshold()) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }
                            // checks rightNeuron
                            if (rightNeuron.isAboveThreshold()) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            // Updates neuron groups
                            if (tmpNeuron.numFiringNeighbors == 2) {
                                tmpNeuron.setGroupTag(-1);
                                tmpNeuron.firingType = FiringType.FIRING_INSIDE;

                                upNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.FORCED);
                                rightNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.FORCED);
                                updateGroup(tmpNeuron, UPDATE_UP | UPDATE_RIGHT, Add_Mode.FIRING_RATE_AVERAGE);
                            }
                            break;
                        case CORNER_01:
                            if(!tmpNeuron.fired)
                                break;

                            downNeuron = lifNeurons.get(id - numOfNeuronsX);
                            rightNeuron = lifNeurons.get(id + 1);

                            if (downNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }
                            if (rightNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if (tmpNeuron.numFiringNeighbors == 2) {
                                if (rightNeuron.groupTag == downNeuron.groupTag) {
                                    tmpNeuron.setGroupTag(downNeuron.groupTag);
                                } else {
                                    tmpNeuron.setGroupTag(-1);
                                }

                                tmpNeuron.firingType = FiringType.FIRING_INSIDE;

                                rightNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                downNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                updateGroup(tmpNeuron, UPDATE_DOWN | UPDATE_RIGHT, Add_Mode.FIRING_RATE_AVERAGE);
                            }
                            break;
                        case CORNER_10:
                            upNeuron = lifNeurons.get(id + numOfNeuronsX);
                            leftNeuron = lifNeurons.get(id - 1);

                            if (upNeuron.isAboveThreshold()) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if(!tmpNeuron.fired)
                                break;


                            if (leftNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if (tmpNeuron.numFiringNeighbors == 2) {
                                tmpNeuron.setGroupTag(-1);
                                tmpNeuron.firingType = FiringType.FIRING_INSIDE;

                                upNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.FORCED);
                                leftNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                updateGroup(tmpNeuron, UPDATE_UP | UPDATE_LEFT, Add_Mode.FIRING_RATE_AVERAGE);
                            }
                            break;
                        case CORNER_11:
                            if(!tmpNeuron.fired)
                                break;

                            downNeuron = lifNeurons.get(id - numOfNeuronsX);
                            leftNeuron = lifNeurons.get(id - 1);

                            if (downNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }
                            if (leftNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if (tmpNeuron.numFiringNeighbors == 2) {
                                if (leftNeuron.groupTag == downNeuron.groupTag) {
                                    tmpNeuron.setGroupTag(downNeuron.groupTag);
                                } else {
                                    if (leftNeuron.groupTag > 0 && downNeuron.groupTag > 0) {
                                        tmpNeuron.setGroupTag(Math.min(downNeuron.groupTag, leftNeuron.groupTag));

                                        // do merge here
                                        int targetGroupTag = Math.max(downNeuron.groupTag, leftNeuron.groupTag);
                                        neuronGroups.get(tmpNeuron.groupTag).merge(neuronGroups.get(targetGroupTag));
                                        neuronGroups.remove(targetGroupTag);
                                    } else if (leftNeuron.groupTag < 0 && downNeuron.groupTag < 0) {
                                        tmpNeuron.setGroupTag(-1);
                                    } else {
                                        tmpNeuron.setGroupTag(Math.max(downNeuron.groupTag, leftNeuron.groupTag));
                                    }
                                }

                                tmpNeuron.firingType = FiringType.FIRING_INSIDE;

                                downNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                leftNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                updateGroup(tmpNeuron, UPDATE_DOWN | UPDATE_LEFT, Add_Mode.FIRING_RATE_AVERAGE);
                            }
                            break;
                        case EDGE_0Y:
                            upNeuron = lifNeurons.get(id + numOfNeuronsX);
                            downNeuron = lifNeurons.get(id - numOfNeuronsX);
                            rightNeuron = lifNeurons.get(id + 1);

                            if (upNeuron.isAboveThreshold()) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if(!tmpNeuron.fired)
                                break;

                            if (downNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }
                            if (rightNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if (tmpNeuron.numFiringNeighbors == 3) {
                                if (rightNeuron.groupTag == downNeuron.groupTag) {
                                    tmpNeuron.setGroupTag(downNeuron.groupTag);
                                } else {
                                    tmpNeuron.setGroupTag(-1);
                                }

                                tmpNeuron.firingType = FiringType.FIRING_INSIDE;

                                upNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.FORCED);
                                downNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                rightNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                updateGroup(tmpNeuron, UPDATE_UP | UPDATE_DOWN | UPDATE_RIGHT, Add_Mode.FIRING_RATE_AVERAGE);
                            }
                            break;
                        case EDGE_1Y:
                            upNeuron = lifNeurons.get(id + numOfNeuronsX);
                            downNeuron = lifNeurons.get(id - numOfNeuronsX);
                            leftNeuron = lifNeurons.get(id - 1);

                            if (upNeuron.isAboveThreshold()) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if(!tmpNeuron.fired)
                                break;

                            if (downNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }
                            if (leftNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if (tmpNeuron.numFiringNeighbors == 3) {
                                if (leftNeuron.groupTag == downNeuron.groupTag) {
                                    tmpNeuron.setGroupTag(downNeuron.groupTag);
                                } else {
                                    if (leftNeuron.groupTag > 0 && downNeuron.groupTag > 0) {
                                        tmpNeuron.setGroupTag(Math.min(downNeuron.groupTag, leftNeuron.groupTag));

                                        // do merge here
                                        int targetGroupTag = Math.max(downNeuron.groupTag, leftNeuron.groupTag);
                                        neuronGroups.get(tmpNeuron.groupTag).merge(neuronGroups.get(targetGroupTag));
                                        neuronGroups.remove(targetGroupTag);
                                    } else if (leftNeuron.groupTag < 0 && downNeuron.groupTag < 0) {
                                        tmpNeuron.setGroupTag(-1);
                                    } else {
                                        tmpNeuron.setGroupTag(Math.max(downNeuron.groupTag, leftNeuron.groupTag));
                                    }
                                }

                                tmpNeuron.firingType = FiringType.FIRING_INSIDE;

                                upNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.FORCED);
                                downNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                leftNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                updateGroup(tmpNeuron, UPDATE_UP | UPDATE_DOWN | UPDATE_LEFT, Add_Mode.FIRING_RATE_AVERAGE);
                            }
                            break;
                        case EDGE_X0:
                            upNeuron = lifNeurons.get(id + numOfNeuronsX);
                            rightNeuron = lifNeurons.get(id + 1);
                            leftNeuron = lifNeurons.get(id - 1);

                            if (upNeuron.isAboveThreshold()) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }
                            if (rightNeuron.isAboveThreshold()) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if(!tmpNeuron.fired)
                                break;

                            if (leftNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if (tmpNeuron.numFiringNeighbors == 3) {
                                tmpNeuron.setGroupTag(-1);
                                tmpNeuron.firingType = FiringType.FIRING_INSIDE;

                                upNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.FORCED);
                                rightNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.FORCED);
                                leftNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                updateGroup(tmpNeuron, UPDATE_UP | UPDATE_RIGHT | UPDATE_LEFT, Add_Mode.FIRING_RATE_AVERAGE);
                            }
                            break;
                        case EDGE_X1:
                            if(!tmpNeuron.fired)
                                break;

                            downNeuron = lifNeurons.get(id - numOfNeuronsX);
                            rightNeuron = lifNeurons.get(id + 1);
                            leftNeuron = lifNeurons.get(id - 1);

                            if (downNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }
                            if (rightNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }
                            if (leftNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if (tmpNeuron.numFiringNeighbors == 3) {
                                if (rightNeuron.groupTag == downNeuron.groupTag) {
                                    tmpNeuron.setGroupTag(downNeuron.groupTag);
                                }

                                if (leftNeuron.groupTag == downNeuron.groupTag) {
                                    tmpNeuron.setGroupTag(downNeuron.groupTag);
                                } else {
                                    if (leftNeuron.groupTag > 0 && downNeuron.groupTag > 0) {
                                        tmpNeuron.setGroupTag(Math.min(downNeuron.groupTag, leftNeuron.groupTag));

                                        // do merge here
                                        int targetGroupTag = Math.max(downNeuron.groupTag, leftNeuron.groupTag);
                                        neuronGroups.get(tmpNeuron.groupTag).merge(neuronGroups.get(targetGroupTag));
                                        neuronGroups.remove(targetGroupTag);
                                    } else if (leftNeuron.groupTag < 0 && downNeuron.groupTag < 0) {
                                        tmpNeuron.setGroupTag(-1);
                                    } else {
                                        tmpNeuron.setGroupTag(Math.max(downNeuron.groupTag, leftNeuron.groupTag));
                                    }
                                }

                                tmpNeuron.firingType = FiringType.FIRING_INSIDE;

                                rightNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.FORCED);
                                downNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                leftNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                updateGroup(tmpNeuron, UPDATE_DOWN | UPDATE_RIGHT | UPDATE_LEFT, Add_Mode.FIRING_RATE_AVERAGE);
                            }
                            break;
                        case INSIDE:
                            upNeuron = lifNeurons.get(id + numOfNeuronsX);
                            downNeuron = lifNeurons.get(id - numOfNeuronsX);
                            rightNeuron = lifNeurons.get(id + 1);
                            leftNeuron = lifNeurons.get(id - 1);

                            if (upNeuron.isAboveThreshold()) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if(!tmpNeuron.fired)
                                break;

                            if (downNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }
                            if (rightNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }
                            if (leftNeuron.fired) {
                                tmpNeuron.increaseNumFiringNeighbors();
                            }

                            if (tmpNeuron.numFiringNeighbors == 4) {
                                if (rightNeuron.groupTag == downNeuron.groupTag) {
                                    tmpNeuron.setGroupTag(downNeuron.groupTag);
                                }

                                if (leftNeuron.groupTag == downNeuron.groupTag) {
                                    tmpNeuron.setGroupTag(downNeuron.groupTag);
                                } else {
                                    if (leftNeuron.groupTag > 0 && downNeuron.groupTag > 0) {
                                        tmpNeuron.setGroupTag(Math.min(downNeuron.groupTag, leftNeuron.groupTag));

                                        // do merge here
                                        int targetGroupTag = Math.max(downNeuron.groupTag, leftNeuron.groupTag);
                                        neuronGroups.get(tmpNeuron.groupTag).merge(neuronGroups.get(targetGroupTag));
                                        neuronGroups.remove(targetGroupTag);
                                    } else if (leftNeuron.groupTag < 0 && downNeuron.groupTag < 0) {
                                        tmpNeuron.setGroupTag(-1);
                                    } else {
                                        tmpNeuron.setGroupTag(Math.max(downNeuron.groupTag, leftNeuron.groupTag));
                                    }
                                }

                                tmpNeuron.firingType = FiringType.FIRING_INSIDE;

                                upNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.FORCED);
                                downNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                rightNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);
                                leftNeuron.setFiringTypeToBorder(tmpNeuron.groupTag, FiringTypeUpdate.CHECK);

                                updateGroup(tmpNeuron, UPDATE_UP | UPDATE_DOWN | UPDATE_RIGHT | UPDATE_LEFT, Add_Mode.FIRING_RATE_AVERAGE);
                            }
                            break;
                        default:
                            break;
                    } // End of switch
                    
                    // reset numSpikes
                    tmpNeuron.numSpikes = 0;

//                } catch (java.util.ConcurrentModificationException e) {
//                    // this is in case neuron list is modified by real time filter during updating neurons
//                    initFilter();
//                    log.warning(e.getMessage());
//                }
                  
            } // End of for
        } // End of if
    }

    /**
     * updates a neuron group with a new member
     *
     * @param newMemberNeuron : new member neuron
     * @param updateOption : option for updating neighbor neurons. Selected neighbors are updated together.
     * All neighbor neurons are updated together with option 'UPDATE_UP | UPDATE_DOWN | UPDATE_RIGHT | UPDATE_LEFT'.
     */
    private void updateGroup(LIFNeuron newMemberNeuron, int updateOption, Add_Mode mode) {
        NeuronGroup tmpGroup = null;
        if (neuronGroups.containsKey(newMemberNeuron.getGroupTag())) {
            tmpGroup = neuronGroups.get(newMemberNeuron.getGroupTag());
            tmpGroup.add(newMemberNeuron, mode);
        } else {
            tmpGroup = new NeuronGroup(newMemberNeuron, mode);
            neuronGroups.put(tmpGroup.tag, tmpGroup);
        }

        int indexX = (int) newMemberNeuron.getIndex().x;
        int indexY = (int) newMemberNeuron.getIndex().y;
        int up = indexX + (indexY + 1) * numOfNeuronsX;
        int down = indexX + (indexY - 1) * numOfNeuronsX;
        int right = indexX + 1 + indexY * numOfNeuronsX;
        int left = indexX - 1 + indexY * numOfNeuronsX;

        if ((updateOption & UPDATE_UP) > 0) {
            tmpGroup.add(lifNeurons.get(up), mode);
        }
        if ((updateOption & UPDATE_DOWN) > 0) {
            tmpGroup.add(lifNeurons.get(down), mode);
        }
        if ((updateOption & UPDATE_RIGHT) > 0) {
            tmpGroup.add(lifNeurons.get(right), mode);
        }
        if ((updateOption & UPDATE_LEFT) > 0) {
            tmpGroup.add(lifNeurons.get(left), mode);
        }
    }

    @Override
    public void annotate(GLAutoDrawable drawable) {
        if (!isFilterEnabled()) {
            return;
        }
        GL gl = drawable.getGL(); // when we getString this we are already set up with scale 1=1 pixel, at LL corner
        if (gl == null) {
            log.warning("null GL in BlurringFilter2D.annotate");
            return;
        }
        gl.glPushMatrix();

        if(showMPThreshold){
            int font = GLUT.BITMAP_HELVETICA_18;
            GLUT glut = chip.getCanvas ().getGlut ();
            gl.glColor3f (1,1,1);

            gl.glRasterPos3f (10,5,0);
            glut.glutBitmapString (font,String.format ("MPThreshold = %d", MPThreshold));
        }

        try {
            if (showFiringNeurons) {
                LIFNeuron tmpNeuron;
                for (int i = 0; i < lifNeurons.size(); i++) {
                    tmpNeuron = lifNeurons.get(i);

                    if (showBorderNeuronsOnly && tmpNeuron.getFiringType() == FiringType.FIRING_ON_BORDER)
                        tmpNeuron.drawReceptiveField(drawable);

                    if (showInsideNeuronsOnly && tmpNeuron.getFiringType() == FiringType.FIRING_INSIDE)
                        tmpNeuron.drawReceptiveField(drawable);

                    if (!showBorderNeuronsOnly && !showInsideNeuronsOnly)
                        if(tmpNeuron.getFiringType() != FiringType.SILENT)
                            tmpNeuron.drawReceptiveField(drawable);
                }
            }
        } catch (java.util.ConcurrentModificationException e) {
            // this is in case neuron list is modified by real time filter during rendering of neurons
            log.warning(e.getMessage());
        }
        gl.glPopMatrix();
    }


    @Override
    synchronized public void initFilter() {
        int prev_numOfNeuronsX = numOfNeuronsX;
        int prev_numOfNeuronsY = numOfNeuronsY;

        if(receptiveFieldSizePixels < 2)
            receptiveFieldSizePixels = 2;
        halfReceptiveFieldSizePixels = receptiveFieldSizePixels/2;

        // calculate the required number of neurons
        if (mychip.getSizeX() % halfReceptiveFieldSizePixels == 0) {
            numOfNeuronsX = (int) (mychip.getSizeX() / halfReceptiveFieldSizePixels) - 1;
        } else {
            numOfNeuronsX = (int) (mychip.getSizeX() / halfReceptiveFieldSizePixels);
        }

        if (mychip.getSizeY() % halfReceptiveFieldSizePixels == 0) {
            numOfNeuronsY = (int) (mychip.getSizeY() / halfReceptiveFieldSizePixels) - 1;
        } else {
            numOfNeuronsY = (int) (mychip.getSizeY() / halfReceptiveFieldSizePixels);
        }

        lastTime = 0;
//        firingNeurons.clear();
        neuronGroups.clear();
        numOfGroup = 0;


        // initialize all neurons
        if ((numOfNeuronsX > 0 && numOfNeuronsY > 0) &&
                (prev_numOfNeuronsX != numOfNeuronsX || prev_numOfNeuronsY != numOfNeuronsY)) {
            if (!lifNeurons.isEmpty()) {
                lifNeurons.clear();
            }

            for (int j = 0; j < numOfNeuronsY; j++) {
                for (int i = 0; i < numOfNeuronsX; i++) {

                    // creates a new neuron
                    int neuronNumber = i+j*numOfNeuronsX;
                    Point2D.Float neuronIndex = new Point2D.Float(i, j);
                    Point2D.Float neuronLocationPixels = new Point2D.Float((i+1)*halfReceptiveFieldSizePixels, (j+1)*halfReceptiveFieldSizePixels);
                    LIFNeuron newNeuron = new LIFNeuron(neuronNumber, 
                                                        neuronIndex,
                                                        neuronLocationPixels,
                                                        MPTimeConstantUs,
                                                        MPThreshold,
                                                        MPJumpAfterFiringPercentTh);

                    newNeuron.setMP(MPInitialPercnetTh*MPThreshold);
                    if (i == 0) {
                        if (j == 0) {
                            newNeuron.setLocationType(LocationType.CORNER_00);
                        } else if (j == numOfNeuronsY - 1) {
                            newNeuron.setLocationType(LocationType.CORNER_01);
                        } else {
                            newNeuron.setLocationType(LocationType.EDGE_0Y);
                        }
                    } else if (i == numOfNeuronsX - 1) {
                        if (j == 0) {
                            newNeuron.setLocationType(LocationType.CORNER_10);
                        } else if (j == numOfNeuronsY - 1) {
                            newNeuron.setLocationType(LocationType.CORNER_11);
                        } else {
                            newNeuron.setLocationType(LocationType.EDGE_1Y);
                        }
                    } else {
                        if (j == 0) {
                            newNeuron.setLocationType(LocationType.EDGE_X0);
                        } else if (j == numOfNeuronsY - 1) {
                            newNeuron.setLocationType(LocationType.EDGE_X1);
                        } else {
                            newNeuron.setLocationType(LocationType.INSIDE);
                        }
                    }

                    lifNeurons.add(newNeuron.getID(), newNeuron);
                }
            }
        }
    }

    @Override
    public void resetFilter() {
        for (LIFNeuron n : lifNeurons) {
            n.reset();
        }

        lastTime = 0;
//        firingNeurons.clear();
        neuronGroups.clear();
        numOfGroup = 0;
    }

    /**
     * returns the time constant of the neuron's membranePotential
     *
     * @return
     */
    public int getMPTimeConstantUs() {
        return MPTimeConstantUs;
    }

    /**
     * sets MPTimeConstantUs
     *
     * @param MPTimeConstantUs
     */
    public void setMPTimeConstantUs(int MPTimeConstantUs) {
        this.MPTimeConstantUs = MPTimeConstantUs;
        getPrefs().putInt("BlurringFilter2D.MPTimeConstantUs", MPTimeConstantUs);

//        for(LIFNeuron neuron:lifNeurons)
//            neuron.tauMP = MPTimeConstantUs;
    }

    /**
     * returns neuronLifeTimeUs
     *
     * @return
     */
    public int getNeuronLifeTimeUs() {
        return neuronLifeTimeUs;
    }

    /**
     * sets neuronLifeTimeUs
     *
     * @param neuronLifeTimeUs
     */
    public void setNeuronLifeTimeUs(int neuronLifeTimeUs) {
        this.neuronLifeTimeUs = neuronLifeTimeUs;
        getPrefs().putInt("BlurringFilter2D.neuronLifeTimeUs", neuronLifeTimeUs);
    }

    /**
     * returns receptiveFieldSizePixels
     *
     * @return
     */
    public int getReceptiveFieldSizePixels() {
        return receptiveFieldSizePixels;
    }

    /**
     * set receptiveFieldSizePixels
     *
     * @param receptiveFieldSizePixels
     */
    synchronized public void setReceptiveFieldSizePixels(int receptiveFieldSizePixels) {
        this.receptiveFieldSizePixels = receptiveFieldSizePixels;
        getPrefs().putInt("BlurringFilter2D.receptiveFieldSizePixels", receptiveFieldSizePixels);
        initFilter();
    }

    /**
     * returns MPThreshold
     *
     * @return
     */
    public int getMPThreshold() {
        return MPThreshold;
    }

    /**
     * sets MPThreshold
     *
     * @param MPThreshold
     */
    public void setMPThreshold(int MPThreshold) {
        this.MPThreshold = MPThreshold;
        getPrefs().putInt("BlurringFilter2D.MPThreshold", MPThreshold);

        for(LIFNeuron neuron:lifNeurons)
            neuron.thresholdMP = MPThreshold;
    }

    /**
     * returns showFiringNeurons
     *
     * @return
     */
    public boolean isShowFiringNeurons() {
        return showFiringNeurons;
    }

    /**
     * sets showFiringNeurons
     *
     * @param showFiringNeurons
     */
    public void setShowFiringNeurons(boolean showFiringNeurons) {
        this.showFiringNeurons = showFiringNeurons;
        getPrefs().putBoolean("BlurringFilter2D.showFiringNeurons", showFiringNeurons);
    }

    /**
     * returns showBorderNeuronsOnly
     *
     * @return
     */
    public boolean isShowBorderNeuronsOnly() {
        return showBorderNeuronsOnly;
    }

    /**
     * sets showBorderNeuronsOnly
     *
     * @param showBorderNeuronsOnly
     */
    public void setShowBorderNeuronsOnly(boolean showBorderNeuronsOnly) {
        this.showBorderNeuronsOnly = showBorderNeuronsOnly;
        getPrefs().putBoolean("BlurringFilter2D.showBorderNeuronsOnly", showBorderNeuronsOnly);
    }

    /**
     * returns showInsideNeuronsOnly
     *
     * @return
     */
    public boolean isShowInsideNeuronsOnly() {
        return showInsideNeuronsOnly;
    }

    /**
     * sets showInsideNeuronsOnly
     *
     * @param showInsideNeuronsOnly
     */
    public void setShowInsideNeuronsOnly(boolean showInsideNeuronsOnly) {
        this.showInsideNeuronsOnly = showInsideNeuronsOnly;
        getPrefs().putBoolean("BlurringFilter2D.showInsideNeuronsOnly", showInsideNeuronsOnly);
    }

    /**
     * returns filledReceptiveField
     *
     * @return
     */
    public boolean isFilledReceptiveField() {
        return filledReceptiveField;
    }

    /**
     * sets filledReceptiveField
     *
     * @param filledReceptiveField
     */
    public void setFilledReceptiveField(boolean filledReceptiveField) {
        this.filledReceptiveField = filledReceptiveField;
        getPrefs().putBoolean("BlurringFilter2D.filledReceptiveField", filledReceptiveField);
    }

    /**
     * returns numOfGroup
     *
     * @return
     */
    public int getNumOfGroup() {
        return numOfGroup;
    }

    /**
     * returns collection of neuronGroups
     *
     * @return
     */
    public Collection getNeuronGroups() {
        return neuronGroups.values();
    }

    /**
     * makes a neuron group with location and radius given
     *
     * @param location
     * @param radius
     * @param timestamp
     * @return
     */
    public NeuronGroup getVirtualNeuronGroup(Point2D.Float location, float radius, int timestamp){
            NeuronGroup ng = new NeuronGroup();

            int subIndexX = (int) (location.x / halfReceptiveFieldSizePixels);
            if (subIndexX == numOfNeuronsX)
                subIndexX--;
            int subIndexY = (int) (location.y / halfReceptiveFieldSizePixels);
            if (subIndexY == numOfNeuronsY)
                subIndexY--;
            
            int radiusNeurons = (int) (radius/halfReceptiveFieldSizePixels);
            
            int xIndexStart = subIndexX - radiusNeurons;
            if(xIndexStart < 0)
                xIndexStart = 0;
            
            int xIndexEnd = subIndexX + radiusNeurons;
            if(xIndexEnd > numOfNeuronsX - 1)
                xIndexEnd = numOfNeuronsX - 1;
            
            int yIndexStart = subIndexY - radiusNeurons;
            if(yIndexStart < 0)
                yIndexStart = 0;
            
            int yIndexEnd = subIndexY + radiusNeurons;
            if(yIndexEnd > numOfNeuronsY - 1)
                yIndexEnd = numOfNeuronsY - 1;

            for(int x = xIndexStart; x <= xIndexEnd; x++){
                for(int y = yIndexStart; y <= yIndexEnd; y++){
                    LIFNeuron n = lifNeurons.get(x+y*numOfNeuronsX);
                    if(n.getMP() < 0.001f)
                        n.membranePotential = 0.001f;
                    ng.add(n, Add_Mode.MEMBRANE_POTENTAIL_AVERAGE);
                }
            }

            return ng;
        }

    /**
     * returns the last timestamp ever recorded at this filter
     *
     * @return the last timestamp ever recorded at this filter
     */
    public int getLastTime() {
        return lastTime;
    }

    /**
     * returns MPJumpAfterFiring
     *
     * @return
     */
    public float getMPJumpAfterFiringPercentTh() {
        return MPJumpAfterFiringPercentTh;
    }

    /**
     * sets MPJumpAfterFiringPercentTh
     *
     * @param MPJumpAfterFiringPercentTh
     */
    public void setMPJumpAfterFiringPercentTh(float MPJumpAfterFiringPercentTh) {
        this.MPJumpAfterFiringPercentTh = MPJumpAfterFiringPercentTh;
        getPrefs().putFloat("BlurringFilter2D.MPJumpAfterFiringPercentTh", MPJumpAfterFiringPercentTh);

        for(LIFNeuron neuron:lifNeurons)
            neuron.MPDecreaseArterFiringPercentTh = MPJumpAfterFiringPercentTh;
    }

    /**
     * returns colorToDrawRF
     *
     * @return
     */
    public COLOR_CHOICE getColorToDrawRF() {
        return colorToDrawRF;
    }

    /**
     * sets colorToDrawRF
     *
     * @param colorToDrawRF
     */
    public void setColorToDrawRF(COLOR_CHOICE colorToDrawRF) {
        this.colorToDrawRF = colorToDrawRF;

        getPrefs().put("BlurringFilter2D.colorToDrawRF",colorToDrawRF.toString());
        colors.get(colorToDrawRF).getRGBComponents(rgb);
    }

    /**
     * returns showMPThreshold
     *
     * @return
     */
    public boolean isShowMPThreshold() {
        return showMPThreshold;
    }

    /**
     * sets showMPThreshold
     *
     * @param showMPThreshold
     */
    public void setShowMPThreshold(boolean showMPThreshold) {
        this.showMPThreshold = showMPThreshold;
        getPrefs().putBoolean("BlurringFilter2D.showMPThreshold", showMPThreshold);
    }

    /**
     *
     * @return MPInitialPercnetTh
     */
    public float getMPInitialPercnetTh() {
        return MPInitialPercnetTh;
    }

    /**
     * sets MPInitialPercnetTh
     * 
     * @param MPInitialPercnetTh
     */
    public void setMPInitialPercnetTh(float MPInitialPercnetTh) {
        this.MPInitialPercnetTh = MPInitialPercnetTh;
        getPrefs().putFloat("BlurringFilter2D.MPInitialPercnetTh", MPInitialPercnetTh);
    }

    /**
     * returns enableAdaptation
     * @return
     */
    public boolean isEnableAdaptation() {
        return enableAdaptation;
    }

    /**
     * sets enableAdaptation
     * @param enableAdaptation
     */
    public void setEnableAdaptation(boolean enableAdaptation) {
        this.enableAdaptation = enableAdaptation;
        getPrefs().putBoolean("BlurringFilter2D.enableAdaptation", enableAdaptation);

        for(LIFNeuron neuron:lifNeurons)
            neuron.enableAdaptation = enableAdaptation;
    }

    /**
     * returns adaptationType
     * @return
     */
    public ADAPTATION_TYPE getAdaptationType() {
        return adaptationType;
    }

    /**
     * sets adaptationType
     *
     * @param adaptationType
     */
    public void setAdaptationType(ADAPTATION_TYPE adaptationType) {
        this.adaptationType = adaptationType;
        getPrefs().put("BlurringFilter2D.adaptationType",adaptationType.toString());

        for(LIFNeuron neuron:lifNeurons)
            neuron.adaptationType = adaptationType;
    }


    /**
     * @return adaptationParamDelta
     */
    public float getAdaptationParamDelta() {
        return adaptationParamDelta;
    }

    /**
     * sets adaptationParamDelta
     *
     * @param adaptationParamDelta
     */
    public void setAdaptationParamDelta(float adaptationParamDelta) {
        this.adaptationParamDelta = adaptationParamDelta;
        getPrefs().putFloat("BlurringFilter2D.adaptationParamDelta", adaptationParamDelta);

        for(LIFNeuron neuron:lifNeurons)
            neuron.adaptationParamDelta = adaptationParamDelta;
    }

    /**
     * returns adaptationParamMax
     * @return
     */
    public float getAdaptationParamMax() {
        return adaptationParamMax;
    }

    /**
     * sets adaptationParamMax
     * @param adaptationParamMax
     */
    public void setAdaptationParamMax(float adaptationParamMax) {
        this.adaptationParamMax = adaptationParamMax;
        getPrefs().putFloat("BlurringFilter2D.adaptationParamMax", adaptationParamMax);

        for(LIFNeuron neuron:lifNeurons)
            neuron.adaptationParamMax = adaptationParamMax;
    }

    /**
     * returns adaptationParamTauMs
     *
     * @return
     */
    public float getAdaptationParamTauMs() {
        return adaptationParamTauMs;
    }

    /**
     * sets adaptationParamTauMs
     *
     * @param adaptationParamTauMs
     */
    public void setAdaptationParamTauMs(float adaptationParamTauMs) {
        this.adaptationParamTauMs = adaptationParamTauMs;
        getPrefs().putFloat("BlurringFilter2D.adaptationParamTauMs", adaptationParamTauMs);

        for(LIFNeuron neuron:lifNeurons)
            neuron.adaptationParamTauMs = adaptationParamTauMs;
    }

    /**
     * returns adaptationParamSlop
     *
     * @return
     */
    public float getAdaptationParamSlop() {
        return adaptationParamSlop;
    }

    /**
     * sets adaptationParamSlop
     *
     * @param adaptationParamSlop
     */
    public void setAdaptationParamSlop(float adaptationParamSlop) {
        this.adaptationParamSlop = adaptationParamSlop;
        getPrefs().putFloat("BlurringFilter2D.adaptationParamSlop", adaptationParamSlop);

        for(LIFNeuron neuron:lifNeurons)
            neuron.adaptationParamSlop = adaptationParamSlop;
    }

    /**
     * returns RefractoryPeriodMaxMs
     *
     * @return
     */
    public float getRefractoryPeriodMaxMs() {
        return RefractoryPeriodMaxMs;
    }

    /**
     * sets RefractoryPeriodMaxMs
     * 
     * @param RefractoryPeriodMaxMs
     */
    public void setRefractoryPeriodMaxMs(float RefractoryPeriodMaxMs) {
        this.RefractoryPeriodMaxMs = RefractoryPeriodMaxMs;
        getPrefs().putFloat("BlurringFilter2D.RefractoryPeriodMaxMs", RefractoryPeriodMaxMs);

        for(LIFNeuron neuron:lifNeurons)
            neuron.RefractoryPeriodMaxMs = RefractoryPeriodMaxMs;
    }

}
