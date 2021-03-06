package net.sf.jaer.event;
/*
 * TypedEvent.java
 *
 * Created on May 28, 2006, 9:20 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 *
 *Copyright May 28, 2006 Tobi Delbruck, Inst. of Neuroinformatics, UNI-ETH Zurich
 */

/**
 * Represents an event with a byte type. This is a legacy class to support previous implementations.
 * @author tobi
 */
public class TypedEvent extends BasicEvent {
    
    public byte type=0;
    
    /** Creates a new instance of TypedEvent */
    public TypedEvent() {
    }
    
    @Override public int getType(){
        return type;
    }
    
    @Override public String toString(){
        return super.toString()+" type="+type;
    }
    
    /** copies fields from source event src to this event
     @param src the event to copy from
     */
    @Override public void copyFrom(BasicEvent src){
        TypedEvent e=(TypedEvent)src;
        super.copyFrom(e);
        this.type=e.type;
    }
    
    
}
