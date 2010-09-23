/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.unizh.ini.jaer.projects.einsteintunnel.sensoryprocessing;

import ch.unizh.ini.jaer.projects.einsteintunnel.sensoryprocessing.BlurringTunnelTracker.Cluster;
import ch.unizh.ini.jaer.projects.einsteintunnel.sensoryprocessing.OSCutils.*;

import java.net.*;
import java.util.*;
import java.util.logging.*;

/**
 *
 * @author braendch
 */
public class ClusterOSCInterface {

    public int DEFAULT_PORT = 9997;

    public OSCutils utils ;

    public ClusterOSCInterface(){
        InetAddress address = null;
        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            Logger.getLogger(ClusterOSCInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
        utils = new OSCutils(address, DEFAULT_PORT);
    }

    public ClusterOSCInterface(int port){
        InetAddress address = null;
        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            Logger.getLogger(ClusterOSCInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
        utils = new OSCutils(address, port);
    }

    public void sendActivity(short[] xHistogram){
        Object args[] = new Object[xHistogram.length];
        for(int i = 0; i<xHistogram.length; i++){
            args[i] = new Float(xHistogram[i]);
        }
        utils.sendMessage("/jAER/histogram", args);
    }

    public void sendCluster(Cluster c){

        Object[] clusterNumber = { new Integer(c.hashCode())};
        OSCMessage msg1 = utils.new OSCMessage("/jAER/nr", clusterNumber);

        Object[] clusterPosX = { new Float(c.getLocation().x)};
        OSCMessage msg2 = utils.new OSCMessage("/jAER/pos/x", clusterPosX);

        Object[] clusterPosY = { new Float(c.getLocation().y)};
        OSCMessage msg3 = utils.new OSCMessage("/jAER/pos/y", clusterPosY);

        Object[] clusterVelX = { new Float(c.getVelocityPPS().x)};
        OSCMessage msg4 = utils.new OSCMessage("/jAER/vel/x", clusterVelX);

        Object[] clusterVelY = { new Float(c.getVelocityPPS().y)};
        OSCMessage msg5 = utils.new OSCMessage("/jAER/vel/y", clusterVelY);

        Object[] clusterMass = { new Float(c.mass)};
        OSCMessage msg6 = utils.new OSCMessage("/jAER/mass", clusterMass);

        // create a timeStamped bundle of the messages
        OSCPacket[] packets = {msg1, msg2, msg3, msg4, msg5, msg6};
        Date newDate = new Date();
        long time = newDate.getTime();
        newDate.setTime(time);

        OSCBundle bundle = utils.new OSCBundle(packets, newDate);

        utils.sendBundle(bundle);

    }

    public void sendXPosition(float position){
        Object[] args = {new Float(position)};
        utils.sendMessage("/jAER/position/x", args);
    }
    public void sendYPosition(float position){
        Object[] args = {new Float(position)};
        utils.sendMessage("/jAER/position/y", args);
    }

    public void sendSpeed(float speed){
        Object[] args = {new Float(speed)};
        utils.sendMessage("/jAER/speed", args);
    }

    public void close(){
        utils.socket.close();
    }

}