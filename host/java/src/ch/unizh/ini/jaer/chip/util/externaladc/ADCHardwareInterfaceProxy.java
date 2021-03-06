package ch.unizh.ini.jaer.chip.util.externaladc;

import ch.unizh.ini.jaer.chip.dvs320.cDVSTest20;
import ch.unizh.ini.jaer.chip.dvs320.cDVSTestHardwareInterface;
import java.util.logging.Logger;
import net.sf.jaer.chip.Chip;
import net.sf.jaer.hardwareinterface.HardwareInterfaceException;
import net.sf.jaer.hardwareinterface.HardwareInterfaceProxy;

/**
 * A proxy to wrap around the actual hardware interface to expose the ADC controls
 * for purposes of GUI building using ParameterControlPanel.
 */
public class ADCHardwareInterfaceProxy extends HardwareInterfaceProxy {

    static final Logger log = Logger.getLogger("HardwareInterfaceProxy");
    private boolean adcEnabled;
    private short trackTime, refOnTime, refOffTime, idleTime;
    private boolean UseCalibration;
    // following define limits for slider controls that are automagically constucted by ParameterControlPanel
    private final int minRefOffTime = 0;
    private final int maxRefOffTime = 100;
    private final int minRefOnTime = 1;
    private final int maxRefOnTime = 100;
    private final int minTrackTime = 0;
    private final int maxTrackTime = 100;
    private final int minIdleTime = 0;
    private final int maxIdleTime = 100;
    private final int minADCchannel = 0;
    private final int maxADCchannel = 3;
    private boolean printedWarning = false;
    private ADCHardwareInterface hw;
    private static final int adcChannelshift = 5;
    private static final short adcConfig = (short) 0x100;   //normal power mode, single ended, sequencer unused : (short) 0x908;
    private final static short adcConfigLength = (short) 12;
    private final byte adcChannel;

    public ADCHardwareInterfaceProxy(Chip chip) {
        super(chip);
        adcChannel = (byte) getPrefs().getInt("CochleaAMS1cHardwareInterface.ADCchannel", 3);
        adcEnabled = getPrefs().getBoolean("CochleaAMS1cHardwareInterface.adcEnabled", true);

        UseCalibration = getPrefs().getBoolean("CochleaAMS1cHardwareInterface.UseCalibration", false);
        adcEnabled = getPrefs().getBoolean("CochleaAMS1cHardwareInterface.adcEnabled", true);
        trackTime = (short) getPrefs().getInt("CochleaAMS1cHardwareInterface.TrackTime", 50);
        refOnTime = (short) getPrefs().getInt("CochleaAMS1cHardwareInterface.RefOnTime", 20);
        refOffTime = (short) getPrefs().getInt("CochleaAMS1cHardwareInterface.RefOffTime", 20);
        idleTime = (short) getPrefs().getInt("CochleaAMS1cHardwareInterface.IdleTime", 10);
        UseCalibration = getPrefs().getBoolean("CochleaAMS1cHardwareInterface.UseCalibration", false);
    }

    private boolean checkHw() {
        if (hw == null) {
            if (!printedWarning) {
                printedWarning = true;
                log.warning("null hardware, not doing anything with ADC hardware");
            }
            return false;
        }
        return true;
    }

    public ADCHardwareInterface getHw() {
        return hw;
    }

    /**
     * @param hw the hardware interface to set
     */
    public void setHw(ADCHardwareInterface hw) {
        this.hw = hw;
    }

    public void doSendConfiguration() throws HardwareInterfaceException {
        if (!checkHw()) {
            return;
        }
        hw.sendADCConfiguration();
    }

    public void setADCEnabled(boolean yes) throws HardwareInterfaceException {
        if (!checkHw()) {
            return;
        }
        hw.setADCEnabled(yes);
    }

    public boolean isADCEnabled() {
        if (!checkHw()) {
            return false;
        }
        return hw.isADCEnabled();
    }

//    public void setUseCalibration(boolean se) {
//        if (!checkHw()) {
//            return;
//        }
//        hw.setUseCalibration(se);
//    }
    public void setTrackTime(int trackTimeUs) {
        if (!checkHw()) {
            return;
        }
        hw.setTrackTime((short) trackTimeUs);
    }

//    public void setSelect5Tbuffer(boolean se) {
//        if (!checkHw()) {
//            return;
//        }
//        hw.setSelect5Tbuffer(se);
//    }
    public void setRefOnTime(int trackTimeUs) {
        if (!checkHw()) {
            return;
        }
        hw.setRefOnTime((short) trackTimeUs);
    }

    public void setRefOffTime(int trackTimeUs) {
        if (!checkHw()) {
            return;
        }
        hw.setRefOffTime((short) trackTimeUs);
    }

    public void setIdleTime(int trackTimeUs) {
        if (!checkHw()) {
            return;
        }
        hw.setIdleTime((short) trackTimeUs);
    }

    public void setADCchannel(int chan) {
        if (!checkHw()) {
            return;
        }
        hw.setADCchannel((byte) chan);
    }

//    public synchronized void resetTimestamps() {
//        if (!checkHw()) {
//            return;
//        }
//        hw.resetTimestamps();
//    }

//    public boolean isUseCalibration() {
//        if (!checkHw()) {
//            return false;
//        }
//        return hw.isUseCalibration();
//    }
//
//    public boolean isSelect5Tbuffer() {
//        if (!checkHw()) {
//            return false;
//        }
//        return hw.isSelect5Tbuffer();
//    }
//    public boolean isChipReset() {
//        if (!checkHw()) {
//            return false;
//        }
//        return hw.isChipReset();
//    }
    public int getTrackTime() {
        if (!checkHw()) {
            return -1;
        }
        return hw.getTrackTime();
    }

    public int getRefOnTime() {
        if (!checkHw()) {
            return -1;
        }
        return hw.getRefOnTime();
    }

    public int getRefOffTime() {
        if (!checkHw()) {
            return -1;
        }
        return hw.getRefOffTime();
    }

    public int getIdleTime() {
        if (!checkHw()) {
            return -1;
        }
        return hw.getIdleTime();
    }

    public int getADCchannel() {
        if (!checkHw()) {
            return -1;
        }
        return hw.getADCchannel();
    }

    public int getMinRefOnTime() {
        return minRefOnTime;
    }

    public int getMaxRefOnTime() {
        return maxRefOnTime;
    }

    public int getMinTrackTime() {
        return minTrackTime;
    }

    public int getMaxTrackTime() {
        return maxTrackTime;
    }

    public int getMinIdleTime() {
        return minIdleTime;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    public int getMinADCchannel() {
        return minADCchannel;
    }

    public int getMaxADCchannel() {
        return maxADCchannel;
    }

    public int getMinRefOffTime() {
        return minRefOffTime;
    }

    public int getMaxRefOffTime() {
        return maxRefOffTime;
    }
}
