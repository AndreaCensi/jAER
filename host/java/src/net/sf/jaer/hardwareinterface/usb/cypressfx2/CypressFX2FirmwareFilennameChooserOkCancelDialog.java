/*
 * CypressFX2FirmwareFilennameChooserOkCancelDialog.java
 *
 * Created on October 20, 2008, 4:03 PM
 */
package net.sf.jaer.hardwareinterface.usb.cypressfx2;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;

/**
 * A dialog for choosing a firmware file.
 * @author  tobi
 */
public class CypressFX2FirmwareFilennameChooserOkCancelDialog extends javax.swing.JDialog {
    public static final String defaultRelativeFirmwarePath="/net/sf/jaer/hardwareinterface/usb/cypressfx2";
    
    static Logger log = Logger.getLogger("CypressFX2FirmwareFilennameChooserOkCancelDialog");
    static Preferences prefs = Preferences.userNodeForPackage(CypressFX2FirmwareFilennameChooserOkCancelDialog.class);
    /** A return status code - returned if Cancel button has been pressed */
    public static final int RET_CANCEL = 0;
    /** A return status code - returned if OK button has been pressed */
    public static final int RET_OK = 1;

    /** Creates new form CypressFX2FirmwareFilennameChooserOkCancelDialog */
    public CypressFX2FirmwareFilennameChooserOkCancelDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        filenameTextField.setText(getLastFile());
        filenameTextField.setToolTipText(filenameTextField.getText());
    }

    /** The chosen file selected by the user.
     * Defaults to the last one selected or null if none has ever been selected.
     * @return the full path to the file.
     */
    public String getLastFile(){
        return prefs.get("CypressFX2FirmwareFilennameChooserOkCancelDialog.lastFile", null);
    }
    
    public void setLastFile(String f){
        prefs.put("CypressFX2FirmwareFilennameChooserOkCancelDialog.lastFile", f);
    }
    
    /** @return the return status of this dialog - one of RET_OK or RET_CANCEL */
    public int getReturnStatus() {
        return returnStatus;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        infoLabel = new javax.swing.JLabel();
        filenameTextField = new javax.swing.JTextField();
        chooseButton = new javax.swing.JButton();

        setTitle("CypressFX2 firmware file chooser");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        infoLabel.setText("<html>Choose the .bix firmware file you wish to download to this device.<p>These files are usually located in either the folder <em>deviceFirmwarePCBLayout</em> or the package <em>ch.unizh.ini.caviar.hardwareinterface.usb</em><html>");

        filenameTextField.setText("The chosen file");
        filenameTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filenameTextFieldActionPerformed(evt);
            }
        });

        chooseButton.setText("Choose...");
        chooseButton.setToolTipText("Browses for a firmware file");
        chooseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(infoLabel)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(filenameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 572, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(chooseButton)
                            .addComponent(cancelButton))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(infoLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filenameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chooseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        setLastFile(filenameTextField.getText());
        doClose(RET_OK);
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        doClose(RET_CANCEL);
    }//GEN-LAST:event_cancelButtonActionPerformed

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        doClose(RET_CANCEL);
    }//GEN-LAST:event_closeDialog

private void filenameTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filenameTextFieldActionPerformed
    File f = new File(filenameTextField.getText());
    if (!f.exists()) {
        filenameTextField.selectAll();
        filenameTextField.setForeground(Color.RED);
    }else{
        filenameTextField.setForeground(Color.BLACK);
    }
}//GEN-LAST:event_filenameTextFieldActionPerformed

private void chooseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooseButtonActionPerformed
    
    String startFolder=filenameTextField.getText();
    if(startFolder.equals("")){
        startFolder=System.getProperty("user.dir")+defaultRelativeFirmwarePath;
    }
    JFileChooser chooser=new JFileChooser(startFolder);
    chooser.setApproveButtonText("Choose");
    chooser.setFileFilter(new FirmwareFileFilter());
    chooser.setMultiSelectionEnabled(false);
    int returnVal = chooser.showOpenDialog(this);
    if(returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                filenameTextField.setText(chooser.getSelectedFile().getCanonicalPath());
                filenameTextField.setToolTipText(filenameTextField.getText());
            } catch (IOException ex) {
                log.warning(ex.toString());
            }
    }
}//GEN-LAST:event_chooseButtonActionPerformed

    private void doClose(int retStatus) {
        returnStatus = retStatus;
        setVisible(false);
        dispose();
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                CypressFX2FirmwareFilennameChooserOkCancelDialog dialog = new CypressFX2FirmwareFilennameChooserOkCancelDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
                dialog.getReturnStatus();
                System.exit(0);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton chooseButton;
    private javax.swing.JTextField filenameTextField;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JButton okButton;
    // End of variables declaration//GEN-END:variables

    private int returnStatus = RET_CANCEL;
    
       private class FirmwareFileFilter extends javax.swing.filechooser.FileFilter{

        @Override
        public boolean accept(File f) {
            if(f.isDirectory()) return true;
            String s=f.getName().toLowerCase();
//            if(s.endsWith(".hex")) return true;  // we only download binaries uniformly in CypressFX2, not hex files which are handled separately. TODO fix this handling
//            if(s.endsWith(".iic")) return true;
            if(s.endsWith(".bix")) return true;
            return false;
        }

        @Override
        public String getDescription() {
            return "Cypress firmware file (hex, iic or bix)";
        }
        
    }
}
