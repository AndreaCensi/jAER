/*
 * BiasgenPanel.java
 *
 * Created on September 24, 2005, 10:05 PM
 */

package net.sf.jaer.biasgen;

/**
 * A panel for controlling a bias generator, 
 * with a Masterbias and an IPotArray. 
 * This is added to the content panel of BiasgenFrame. 
 * It builds the PotPanel, the MasterbiasPanel, and the 
 * extra control panel if there is one.
 *
 * @author  tobi
 */
public class BiasgenPanel extends javax.swing.JPanel {
    public Biasgen biasgen;
    MasterbiasPanel masterbiasPanel;
    PotPanel iPotPanel;
    BiasgenFrame frame;
    
    /** Creates new form BiasgenPanel
     * @param biasgen the source of the parameters
     * @param frame the parent enclosing frame
     */
    public BiasgenPanel(Biasgen biasgen, BiasgenFrame frame) {
        this.biasgen=biasgen;
        this.frame=frame;
        if(biasgen==null) throw new RuntimeException("null biasgen while trying to construct BiasgenPanel");
        masterbiasPanel=new MasterbiasPanel(biasgen.getMasterbias());
        iPotPanel=new PotPanel(biasgen.getPotArray());
        
        initComponents();
        add(jTabbedPane1);
        jTabbedPane1.addTab("Pots",iPotPanel);
//        if(biasgen instanceof ChipControlPanel){
//            jTabbedPane1.addTab("Controls",((ChipControlPanel)biasgen).getControlPanel());
//        }
        jTabbedPane1.addTab("Master Bias",masterbiasPanel);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jTabbedPane1 = new javax.swing.JTabbedPane();

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration//GEN-END:variables
    
    

}
