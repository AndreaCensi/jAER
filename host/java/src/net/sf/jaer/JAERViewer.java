/*
 * JAERViewer.java
 *
 * Created on January 30, 2006, 10:41 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.sf.jaer;
import java.util.logging.Level;
import net.sf.jaer.JAERViewer.ToggleLoggingAction;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.eventio.*;
import net.sf.jaer.graphics.*;
import net.sf.jaer.util.*;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.*;
import javax.swing.*;
/**
 * Used to show multiple chips simultaneously. A single viewer is launched initially, with a default chip. New ones can be constructed from the File menu.
 * @author tobi
 */
public class JAERViewer{
    static Preferences prefs;
    static Logger log;
    /** Can be used to globallhy display data */
    static public JAERDataViewer globalDataViewer = new JAERDataViewer("Global data viewer");

    /**
     * @return the chipClassNames
     */
    public static List<String> getChipClassNames (){
        return chipClassNames;
    }
    private ArrayList<AEViewer> viewers = new ArrayList<AEViewer>();
    private boolean syncEnabled = prefs.getBoolean("JAERViewer.syncEnabled",true);
    ArrayList<AbstractButton> syncEnableButtons = new ArrayList<AbstractButton>(); // list of all viewer sync enable buttons, used here to change boolean state because this is not property of Action that buttons understand
    private ToggleSyncEnabledAction toggleSyncEnabledAction = new ToggleSyncEnabledAction();

    public ToggleSyncEnabledAction getToggleSyncEnabledAction (){
        return toggleSyncEnabledAction;
    }
    volatile boolean loggingEnabled = false;
    //private boolean electricalTimestampResetEnabled=prefs.getBoolean("JAERViewer.electricalTimestampResetEnabled",false);
//    private String aeChipClassName=prefs.get("JAERViewer.aeChipClassName",Tmpdiff128.class.getName());
    private WindowSaver windowSaver; // TODO: encapsulate
    private boolean playBack = false;
    private static List<String> chipClassNames; // cache expensive search for all AEChip classes
    //some time variables for timing across threads
    static public long globalTime1, globalTime2, globalTime3;
    private SyncPlayer syncPlayer = new SyncPlayer(null,this); // TODO ugly, create here and then recreate later

    /** Creates a new instance of JAERViewer */
    public JAERViewer (){
        Thread.UncaughtExceptionHandler handler = new LoggingThreadGroup("jAER UncaughtExceptionHandler");
        Thread.setDefaultUncaughtExceptionHandler(handler);
//        Thread test=new Thread("UncaughtExceptionHandler Test"){
//            public void run(){
//                try {
//                    Thread.sleep(2000);
//                    throw new RuntimeException("test exception 1");
//                } catch (InterruptedException ex) {
//                }
//            }
//        };
//        test.start();
//        Thread test2=new Thread("UncaughtExceptionHandler Test2"){
//            public void run(){
//                try {
//                    Thread.sleep(5000);
//                    throw new RuntimeException("test exception 2");
//                } catch (InterruptedException ex) {
//                }
//            }
//        };
//        test2.start();
//        log.addHandler(handler);
        log.info("java.vm.version=" + System.getProperty("java.vm.version"));
        windowSaver = new WindowSaver(this,prefs);
        Toolkit.getDefaultToolkit().addAWTEventListener(windowSaver,AWTEvent.WINDOW_EVENT_MASK); // adds windowSaver as JVM-wide event handler for window events
        SwingUtilities.invokeLater(new Runnable(){
            public void run (){
                AEViewer v = new AEViewer(JAERViewer.this); // this call already adds the viwer to our list of viewers
//                player=new SyncPlayer(v); // associate with the initial viewer
//                v.pack();
                v.setVisible(true);
            }
        });
        try{
            // Create temp file.
            File temp = new File("JAERViewerRunning.txt");

            // Delete temp file when program exits.
            temp.deleteOnExit();

            // Write to temp file
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            out.write("JAERViewer started " + new Date());
            out.close();
        } catch ( IOException e ){
            log.warning(e.getMessage());
        }
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run (){
                log.info("JAERViewer shutdown hook - saving window settings");
                if ( windowSaver != null ){
                    try{
                        windowSaver.saveSettings();
                    } catch ( IOException e ){
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /** The main launcher for AEViewer's. 
    @param args the first argument can be a recorded AE data filename (.dat) with full path; the viewer will play this file
     */
    public static void main (String[] args){
        //redirect output to DataViewer window
        // should be before any logger is initialized 
//        globalDataViewer.redirectStreams(); // tobi removed because AEViewerConsoleOutputFrame replaces this logging output

        //init static fields
        prefs = Preferences.userNodeForPackage(JAERViewer.class);
        log = Logger.getLogger("JAERViewer");

        // cache expensive search for all AEChip classes

        if ( System.getProperty("os.name").startsWith("Windows") ){
            Runnable runnable = new Runnable(){
                public void run (){
                    try{
                        Thread.sleep(5000); // wait while starting up to speed startup
                    } catch ( InterruptedException ex ){
                        Logger.getLogger(JAERViewer.class.getName()).log(Level.SEVERE,null,ex);
                    }
                    chipClassNames = SubclassFinder.findSubclassesOf(AEChip.class.getName());
                }
            };
            Thread t = new Thread(runnable);
            t.setName("subclassFinder");
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();

            String exepath = System.getProperty("exepath");
            if ( exepath != null ){
                System.out.println("exepath (set from JSmooth launcher) = " + exepath);
            }
        }
        if ( args.length > 0 ){
            log.info("starting with args[0]=" + args[0]);
            final File f = new File(args[0]);
            try{
                JAERViewer jv = new JAERViewer();
                while ( jv.getNumViewers() == 0 ){
                    Thread.sleep(300);
                }
                jv.getSyncPlayer().startPlayback(f);
            } catch ( Exception e ){
                JOptionPane.showMessageDialog(null,"<html>Trying to start JAERViewer with <br>file=\"" + f + "\"<br>Caught " + e);
            }
        } else{
            SwingUtilities.invokeLater(new Runnable(){
                public void run (){
                    new JAERViewer();
                }
            });
        }
    }

    public void addViewer (AEViewer aEViewer){
        getViewers().add(aEViewer);
        aEViewer.addWindowListener(new java.awt.event.WindowAdapter(){
            @Override
            public void windowClosing (java.awt.event.WindowEvent evt){
                if ( evt.getSource() instanceof AEViewer ){
                    log.info("removing " + evt.getSource() + " from list of AEViewers");
                    removeViewer((AEViewer)evt.getSource());
                }
            }
        });
        buildMenus(aEViewer);
    }

    public void saveSetup (File f){
        JOptionPane.showMessageDialog(null,"Saving viewer setup not implemented yet - please request this feature.");

//        File setupFile;
//            JFileChooser fileChooser=new JFileChooser();
//            String lastFilePath=prefs.get("JAERViewer.lastFile",""); // get the last folder
//            File cwd=new File(lastFilePath);
//            fileChooser.setCurrentDirectory(cwd); // sets the working directory of the chooser
//            int retValue=fileChooser.showOpenDialog(null);
//            if(retValue==JFileChooser.APPROVE_OPTION){
//                try{
//                    setupFile=fileChooser.getSelectedFile();
////                    if(lastFile!=null) recentFiles.addFile(lastFile);
//
//                    lastFilePath=setupFile.getPath();
//                    prefs.put("JAERViewer.lastFile",lastFilePath);
//                }catch(FileNotFoundException fnf){
//                    fnf.printStackTrace();
//                }
//            }
//            fileChooser=null;
    }

    public void loadSetup (File f){
        JOptionPane.showMessageDialog(null,"Loading viewer setup Not implemented yet - please request this feature");
    }

    void buildMenus (AEViewer v){
//        log.info("building AEViewer sync menus");
        JMenu m = v.getFileMenu();

        ToggleLoggingAction action = new ToggleLoggingAction(v);
        v.getLoggingButton().setAction(action);
        v.getLoggingMenuItem().setAction(action);

        // adds to each AEViewers syncenabled check box menu item the toggleSyncEnabledAction
        AbstractButton b = v.getSyncEnabledCheckBoxMenuItem();
        b.setAction(getToggleSyncEnabledAction());
        syncEnableButtons.add(b);   // we need this stupid list because java 1.5 doesn't have Action property to support togglebuttons selected state (1.6 adds it)
        b.setSelected(isSyncEnabled());

        AbstractButton bbb = v.getPlayerControls().getSyncPlaybackCheckBox(); // TODO dependency, depends on existing player control panel
        syncEnableButtons.add(bbb);
        bbb.setSelected(isSyncEnabled());
        bbb.setAction(getToggleSyncEnabledAction());


        boolean en = true; //viewers.size()>1? true:false;
        for ( AbstractButton bb:syncEnableButtons ){
            bb.setEnabled(en);
        }

        syncPlayer.getSupport().addPropertyChangeListener(v.getPlayerControls()); // TODO not very clean way of adding property change support....

//        if(en==false) syncEnableButtons.get(0).setSelected(false); // disable sync if there is only one viewer
    }

    public void removeViewer (AEViewer v){
        if ( getViewers().remove(v) == false ){
            log.warning("JAERViewer.removeViewer(): " + v + " is not in viewers list");
        } else{
            syncEnableButtons.remove(v.getSyncEnabledCheckBoxMenuItem());
        }
        boolean en = true; //viewers.size()>1? true:false;
        for ( AbstractButton bb:syncEnableButtons ){
            bb.setEnabled(en);
        }
    }

    /** @return collection of viewers we manage */
    public ArrayList<AEViewer> getViewers (){
        return viewers;
    }

    public int getNumViewers (){
        return viewers.size();
    }
    File indexFile = null;
    final String indexFileNameHeader = "JAERViewer-";
    final String indexFileSuffix = AEDataFile.INDEX_FILE_EXTENSION;
    DateFormat loggingFilenameDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ssZ");

    private String getDateString (){
        String dateString = loggingFilenameDateFormat.format(new Date());
        return dateString;
    }


    /** Creates the index file at location path with timestamped name */
    private File createIndexFile (String path){
        String indexFileName = indexFileNameHeader + getDateString() + indexFileSuffix;
        log.info("createIndexFile " + path + File.separator + indexFileName);
        indexFile = new File(path + File.separator + indexFileName);
        if ( indexFile.isFile() ){
            throw new RuntimeException("index file " + indexFile + " already exists");
        }
        return indexFile;
    }

    public void startSynchronizedLogging (){
        log.info("starting synchronized logging");

        if ( viewers.size() > 1 ){// && !isElectricalSyncEnabled()){
//            zeroTimestamps();  // TODO this is commented out because there is still a bug of getting old timestamps at start of recording, causing problems when synchronized playback is enabled.
        } else{
            // log.info("not zeroing all board timestamps because they are specified electrically synchronized");
        }
        for ( AEViewer v:viewers ){
            File f = v.startLogging();

        }

        loggingEnabled = true;
    }

    public void stopSynchronizedLogging (){
        log.info("stopping synchronized logging");
        FileWriter writer = null;
        boolean writingIndex = false;
        // pause all viewers
        viewers.get(0).aePlayer.pause();

        try{
            for ( AEViewer v:viewers ){
                File f = v.stopLogging(getNumViewers() == 1); // only confirm filename if there is only a single viewer

                if ( f.exists() ){ // if not cancelled
                    if ( getNumViewers() > 1 ){

                        if ( writer == null ){
                            writingIndex = true;
                            createIndexFile(f.getParent());
                            writer = new FileWriter(indexFile);
                        }
                        writer.write(f.getName() + "\n");//  .getPath()+"\n");
                    }
                }
            }
            if ( viewers.size() > 1 && writingIndex ){
                writer.close();
            }
            if ( indexFile != null ){
                for ( AEViewer v:viewers ){
                    v.getRecentFiles().addFile(indexFile);
                }
                log.info("Saved index file " + indexFile.getAbsolutePath());
//                JOptionPane.showMessageDialog(null,"Saved index file " + indexFile.getAbsolutePath());
            }
        } catch ( IOException e ){
            log.warning("creating index file " + indexFile);
            e.printStackTrace();
        }
        // resume all viewers
        viewers.get(0).aePlayer.resume();


        loggingEnabled = false;
    }

    public void toggleSynchronizedLogging (){
        //TODO - unchecking synchronized logging in AEViewer still comes here and logs sychrnoized
        loggingEnabled = !loggingEnabled;
        if ( loggingEnabled ){
            startSynchronizedLogging();
        } else{
            stopSynchronizedLogging();
        }
    }


    public void zeroTimestamps (){
//        if(!isElectricalSyncEnabled()){
        log.info("JAERViewer.zeroTimestamps(): zeroing timestamps on all AEViewers");
        for ( AEViewer v:viewers ){
            v.zeroTimestamps();
        }
//        }else{
//            log.warning("JAERViewer.zeroTimestamps(): electricalSyncEnabled, not resetting all viewer device timestamps");
//        }
    }
//    public class ViewerAction extends AbstractAction{
//        AEViewer viewer;
//        public ViewerAction(AEViewer viewer){
//            this.viewer=viewer;
//        }
//        public void actionPerformed(ActionEvent e){
//            throw new UnsupportedOperationException("this Action doesn't do anything, use subclass");
//        }
//    }
    File logIndexFile;
    /** this action toggles logging, possibily for all viewers depending on switch */
    public class ToggleLoggingAction extends AbstractAction{
        AEViewer viewer; // to find source of logging action

        public ToggleLoggingAction (AEViewer viewer){
            this.viewer = viewer;
            putValue(NAME,"Start logging");
            putValue(SHORT_DESCRIPTION,"Controls synchronized logging on all viewers");
            putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_L,0));
            putValue(MNEMONIC_KEY,new Integer(KeyEvent.VK_L));
        }

        public void actionPerformed (ActionEvent e){
//            log.info("JAERViewer.ToggleLoggingAction.actionPerformed");
            if ( isSyncEnabled() ){
                toggleSynchronizedLogging();
                if ( loggingEnabled ){
                    putValue(NAME,"Stop logging");
                } else{
                    if ( viewers.get(0).getPlayMode() == AEViewer.PlayMode.PLAYBACK ){
                        putValue(NAME,"Start Re-logging");
                    } else{
                        putValue(NAME,"Start logging");
                    }
                }
                log.info("loggingEnabled=" + loggingEnabled);
            } else{
                viewer.toggleLogging();
            }
        }
    }

    /** Toggles player synchronization over all viewers.
     * 
     */
    public class ToggleSyncEnabledAction extends AbstractAction{
        public ToggleSyncEnabledAction (){
            String name = "Synchronize AEViewer logging/playback";
            putValue(NAME,name);
            putValue(SHORT_DESCRIPTION,"<html>When enabled, multiple viewer logging and playback are synchronized. <br>Does not affect timestamp synchronization except to send timestamp reset to all viewers." +
                    "<br>Device electrical synchronization is independent of this setting.");
        }

        public void actionPerformed (ActionEvent e){
            log.info("JAERViewer.ToggleSyncEnabledAction.actionPerformed");
            setSyncEnabled(!isSyncEnabled());
            for ( AbstractButton b:syncEnableButtons ){
                b.setSelected(isSyncEnabled());
            }
            for ( AEViewer v:viewers ){
                AbstractAEPlayer p = isSyncEnabled() ? syncPlayer : v.aePlayer;
                v.getPlayerControls().setAePlayer(p);
            }
        }
    }

    /** Controls whether multiple viewers are synchronized for logging and playback.
     *
     * @return true if sychronized.
     */
    public boolean isSyncEnabled (){
        return syncEnabled;
    }

    /** Controls whether multiple viewers are synchronized for logging and playback.
     *
     * @param syncEnabled true to be synchronized.
     */
    public void setSyncEnabled (boolean syncEnabled){
        this.syncEnabled = syncEnabled;
        prefs.putBoolean("JAERViewer.syncEnabled",syncEnabled);
    }

    public void pause (){
        log.info("this pause shouldn't normally be called");
    }

    public SyncPlayer getSyncPlayer (){
        return syncPlayer;
    }

    /** @return true if boards are electrically connected and this connection synchronizes the local timestamp value */
    /*public boolean isElectricalSyncEnabled(){
    return electricalTimestampResetEnabled;
    }*/
    /* public void setElectricalSyncEnabled(boolean b) {
    electricalTimestampResetEnabled=b;
    prefs.putBoolean("JAERViewer.electricalTimestampResetEnabled",electricalTimestampResetEnabled);
    for(AEViewer v:viewers){
    v.getElectricalSyncEnabledCheckBoxMenuItem().setSelected(b);
    }
    }*/
    public boolean isPlayBack (){
        return playBack;
    }

    public void setPlayBack (boolean playBack){
        this.playBack = playBack;
    }
}
