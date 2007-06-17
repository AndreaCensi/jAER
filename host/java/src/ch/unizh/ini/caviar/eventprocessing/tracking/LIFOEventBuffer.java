package ch.unizh.ini.caviar.eventprocessing.tracking;

import ch.unizh.ini.caviar.event.*;
import ch.unizh.ini.caviar.event.BasicEvent;
import java.util.Iterator;


/** A circular buffer of events with methods for adding new events and getting
 back old ones in the order of addition, last in is first out.
 *@author tobi delbruck
 */
public class LIFOEventBuffer implements Iterable<BasicEvent> {
    private int length = 100;
    private BasicEvent[] array;
    private int nextIn = 0; // points to next location to add event
    private int size = 0;
    private Itr itr = null;
    
    /** Make a new instance of LIFOEventBuffer
     *@param length the number of events to hold
     */
    public LIFOEventBuffer(int length){
        if(length<=0) throw new RuntimeException("must have length >=0 (length="+length+")");
        this.length=length;
        array = new BasicEvent[length];
        for(int i=0;i<array.length;i++){
            array[i]=new BasicEvent();
        }
    }
    
    public String toString(){
        return "LIFOEventBuffer length="+length+" nextIn="+nextIn+" size="+size;
    }
    
    /** Adds an event to the end of the list. The iterator will iterate over the list, starting with this most-recently addeed event.
     *@param e the event
     */
    public void add(BasicEvent e){
        // debug
//        int lastIn=nextIn-1;
//        if(lastIn<0) lastIn=length-1;
//        BasicEvent oldEvent=array[lastIn];
//        if(oldEvent!=null){
//            int told=array[lastIn].timestamp;
//            int dt=e.timestamp-told;
//            if(dt<0){
//                System.err.println("adding older event, dt="+dt);
//            }
//            if(oldEvent.serial>=e.serial){
//                System.err.println("previous serial="+oldEvent.serial+" this serial="+e.serial);
//            }
//        }
        size++;
//        array[nextIn]=e; // can't just copy reference because the event object could be used by someone else in a new packet
        array[nextIn].copyFrom(e); // copy fields to this array event object
        nextIn++;
        if(nextIn>=length) nextIn=0;
        if(size>=length) size=length;
//        System.out.println("added "+e+" to "+this);
    }
    
    /** Resets the pointers and empties the size */
    public void clear(){
        nextIn=0;
        size=0;
    }
    
    /** Returns the number of events presently stored.
     *@return the number of events
     */
    public int size(){
        return size;
    }
    
    /** Returns the capacity
     *@return the capacity
     */
    public int capacity(){
        return length;
    }
    
    /** Returns an event added <code>k</code> ago.
     @param k the event to get back, 0 being the last event added
     */
    private BasicEvent getBackEvent(int k){
        if(k>size) return null;
        int outInd = nextIn-k;
        if(outInd>=0) return array[outInd]; // event is before this location in array
        outInd=length+outInd; // array is after this location in array, wraps around to here
        return array[outInd];
    }
    
    private Iterator it;
    
    private final class Itr implements Iterator {
        int cursor = 0;
        
        public final boolean hasNext() {
            return cursor < size;
        }
        
        public final BasicEvent next() {
            return getBackEvent(++cursor);
        }
        public void reset(){
            cursor=0;
        }
        /** Unsupported operation */
        public void remove(){
            throw new UnsupportedOperationException();
        }
        
        public String toString(){
            return "EventBuffer cursor="+cursor+" for EventBuffer with size="+size;
        }
    }
    
    /** Returns the iterator over events. This iterator starts with the most recently added event and ends with the first event added or with the
     *capacity event if more have been added than the buffer's capacity.
     @return an iterator that can iterate over past events. Starts with most recently added event.
     */
    public final Iterator<BasicEvent> iterator(){
        if (itr==null){
            itr = new Itr();
        }else{
            itr.reset();
        }
        return itr;
    }
    
    public static void main(String[] args){
        LIFOEventBuffer b=new LIFOEventBuffer(3);
        for(int i=0;i<10;i++){
            BasicEvent e=new BasicEvent();
            e.timestamp=i;
            for(BasicEvent old:b){
                System.out.println("had old "+old);
            }
            b.add(e);
            System.out.println("added new "+e+"\n");
        }
        System.out.println("****");
        for(BasicEvent e:b){
            System.out.println("got back "+e);
        }
    }
}