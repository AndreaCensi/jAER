/*
 * BasicEvent.java
 *
 * Created on November 6, 2005, 10:31 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package net.sf.jaer.event;

/**
 * Base class for events. This class is extended by producers offering extended event type information.
 Instances are used in EventPacket. This class is the new-style event that replaces the original Event2D.
 *
 * @author tobi
 */
public class BasicEvent implements EventInterface<BasicEvent> {
    
//    public int serial=-1;
    
    /** timestamp of event, by convention in us */
    public int timestamp;
    
    /** x address of event (horizontal coordinate, by convention starts at left of image) */
    public short x;
    
    /** y address of event (vertical coordinate, by convention starts at bottom of image) */
    public short y;
    
//    /** Marks whether event is filtered away; false is default value and filters can set true to mark
//     the event as unused for further processing.
//     */
//    public boolean filteredAway=false;
    
    /**
     * Creates a new instance of BasicEvent
     */
    public BasicEvent() {
    }
    
    /**
     * create an BasicEvent with a timestamp, x, y, and a variable length number of bytes types
     */
    public BasicEvent(int timestamp, short x, short y, byte type, byte... types){
        this.timestamp=timestamp;
        this.x=x;
        this.y=y;
    }
    
    /** copies fields from source event src to this event
     @param e the event to copy from
     */
    public void copyFrom(BasicEvent e){
        this.timestamp=e.timestamp;
        this.x=e.x;
        this.y=e.y;
//        this.filteredAway=e.filteredAway;
    }
    
    
    /**
     * Creates a new instance of BasicEvent
     */
    public BasicEvent(int t) {
        timestamp=t;
    }
    
    public String toString(){
//        return getClass().getSimpleName()+" serial="+serial+" timestamp="+timestamp+" x="+x+" y="+y;
        return getClass().getSimpleName()+" timestamp="+timestamp+" x="+x+" y="+y;
    }
    
    public int getNumCellTypes() {
        return 1;
    }
    
    public int getType() {
        return 1;
    }
    
    final public int getTimestamp() {
        return timestamp;
    }
    
    final public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
    
    final public short getX() {
        return x;
    }
    
    final public void setX(short x) {
        this.x = x;
    }
    
    final public short getY() {
        return y;
    }
    
    final public void setY(short y) {
        this.y = y;
    }

//    /** True if an EventFilter has marked this event to be ignored */
//    public boolean isFilteredAway() {
//        return filteredAway;
//    }
//
//    public void setFilteredAway(boolean filteredAway) {
//        this.filteredAway=filteredAway;
//    }
    
    
}