package net.sf.jaer.util.filter;

/**
 A first-order lowpass IIR filter. The first value filtered initializes the filter to the value and stores the first time as reference.
 */
public  class LowpassFilter extends Filter{

    /** Constructs a new LowpassFilter with default time constant.
     * 
     */
    public LowpassFilter() {
    }


    /** Constructs a new LowpassFilter filter with a specified RC time constant.
     *
     * @param tauMs the time constant in ms.
     */
    public LowpassFilter(float tauMs) {
        this();
        setTauMs(tauMs);
    }

    
    /** The current state of the filter */
    protected float lpVal;
    
    /** The last value */
    protected float lastVal=0;
    
    /** @param val the new input value
     @param time the time in us - note units here, microseconds!
     */
    @Override
    public float filter(float val, int time){
        if(!initialized){
            lpVal=val;
            lastVal=val;
            lastTime=time;
            initialized=true;
            return val;
        }
        int dt=time-lastTime;
        if(dt<0) dt=0;
        lastTime=time;
        float fac=(float)dt/tauMs/TICK_PER_MS;
        if(fac>1) fac=1;
        lpVal=lpVal+(val-lpVal)*fac;
        lastVal=val;
        return lpVal;
    }
    public String toString(){ return "LP: "+lastVal+"->"+lpVal; }
    
    /** Sets the internral value; used to initialize filter
     @param value the value 
     */
    public void setInternalValue(float value) {
        lpVal=value;
    }

    /** @return output of filter */
    public float getValue() {
        return lpVal;
    }
}

