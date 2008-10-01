/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.unizh.ini.caviar.eventio;

import ch.unizh.ini.caviar.aemonitor.AEPacketRaw;
import ch.unizh.ini.caviar.aemonitor.EventRaw;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * An AE input stream that uses a generic InputStream such as a URL connection to an http server.
 * 
 * @author tobi
 */
public class AEInputStream implements Closeable{

    private InputStream is;
    AEPacketRaw packet = new AEPacketRaw();
    int lastPacketEndTimestamp = 0;
    static Logger log = Logger.getLogger("AEInputStream");
    protected ArrayList<String> header = new ArrayList<String>();
    private Class addressType = Short.TYPE; // default address type, unless file header specifies otherwise
    DataInputStream dis;
    boolean readFirstEventFlag = false;
        PushbackInputStream pbis;

    public AEInputStream(InputStream is) throws IOException {
        this.is = is;
        dis = new DataInputStream(pbis=new PushbackInputStream(is)); // we need to be able to push back bytes after scanning header
        skipHeader();
    }
    
    EventRaw tmpEvent = new EventRaw();
    

    private EventRaw readEvent() throws IOException {
        int addr;
        int ts;
        if (addressType == Integer.TYPE) {
            addr = dis.readInt();
        } else {
            addr = dis.readShort();
        }
        ts = dis.readInt();
        tmpEvent.address = addr;
        tmpEvent.timestamp = ts;
        return tmpEvent;
    }

   /** Reads all available events from the stream
    * @return available event packet
    */
    public AEPacketRaw readAvailablePacket() throws IOException{
        packet.setNumEvents(0);
        int n=dis.available();
        int es=addressType==Integer.TYPE?8:4;
        int ne=n/es;
          for (int i = 0; i < n; i++) {
            packet.addEvent(readEvent());
        }
        return packet;
    }
    
    /** Reads a raw event packet of n events
    @param n the number of events to read
    @throws IOException if there is a problem, e.g. end of file
     */
    public AEPacketRaw readPacketByNumber(int n) throws IOException {
        packet.setNumEvents(0);
        for (int i = 0; i < n; i++) {
            packet.addEvent(readEvent());
        }
        return packet;
    }

    /**
     * returns an AEPacketRaw at least dt long up to the max size of the buffer or until end-of-file.
     * Events are read as long as the timestamp until (and including) the event whose timestamp is greater (for dt>0) than
     * startTimestamp+dt, where startTimestamp is the last timestamp from the previous call.
     *
     * @param dt the timestamp different in units of the timestamp (usually us)
    @return the packet, always at least one event even if there is no event in the interval dt.
    @throws IOException if there is any problem
     */
    public AEPacketRaw readPacketByTime(int dt) throws IOException {
        packet.setNumEvents(0);
        int ts;
        if(!readFirstEventFlag) lastPacketEndTimestamp=readEvent().timestamp;
        do{
            EventRaw e=readEvent();
            packet.addEvent(e);
            ts=e.timestamp;
        }while(ts-lastPacketEndTimestamp<dt);
        return packet;
    }

    /** skips the header lines (if any) */
    protected void skipHeader() throws IOException {
        readHeader();
    }

    /** reads the header comment lines. Assumes we are rewound to position(0).
     */
    protected void readHeader() throws IOException {
        String s;
        while ((s = readHeaderLine()) != null) {
            header.add(s);
            parseFileFormatVersion(s);
        }
        StringBuffer sb = new StringBuffer();
        sb.append("File header:");
        for (String str : getHeader()) {
            sb.append(str);
            sb.append("\n");
        }
        log.info(sb.toString());
    }

    /** parses the file format version 
    @see ch.unizh.ini.caviar.eventio.AEDataFile
     */
    protected void parseFileFormatVersion(String s) {
        float version = 1f;
        if (s.startsWith(AEDataFile.DATA_FILE_FORMAT_HEADER)) { // # stripped off by readHeaderLine
            try {
                version = Float.parseFloat(s.substring(AEDataFile.DATA_FILE_FORMAT_HEADER.length()));
            } catch (NumberFormatException numberFormatException) {
                log.warning("While parsing header line " + s + " got " + numberFormatException.toString());
            }
            if (version < 2) {
                addressType = Short.TYPE;
            } else if (version >= 2) { // this is hack to avoid parsing the AEDataFile. format number string...
                addressType = Integer.TYPE;
            }
            log.info("Data file version=" + version + " and has addressType=" + addressType);
        }

    }

    /** assumes we are positioned at start of line and that we may either read a comment char '#' or something else
    leaves us after the line at start of next line or of raw data. Assumes header lines are written using the AEOutputStream.writeHeaderLine().
    @return header line
     */
    String readHeaderLine() throws IOException {
        StringBuffer s = new StringBuffer();
        int c = pbis.read();
        if (c != AEDataFile.COMMENT_CHAR) {
            pbis.unread(c);
            return null;
        }
        while (((char) (c = pbis.read())) != '\r') {
            if (c < 32 || c > 126) {
                throw new IOException("Non printable character (<32 || >126) detected in header line, aborting header read and resetting to start of file because this file may not have a real header");
            }
            s.append((char) c);
        }
        if ((c = pbis.read()) != '\n') {
            log.warning("header line \"" + s.toString() + "\" doesn't end with LF"); // get LF of CRLF
        }
        return s.toString();
    }

    /** Gets the header strings from the file
    @return list of strings, one per line
     */
    public ArrayList<String> getHeader() {
        return header;
    }

    public void close() throws IOException {
        if(is!=null) is.close();
    }
}