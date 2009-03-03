package ch.unizh.ini.jaer.chip.dvs320;

/*
 * IPotSliderTextControl.java
 *
 * Created on September 21, 2005, 12:23 PM
 */



import ch.unizh.ini.jaer.chip.dvs320.ConfigurableIPot.CurrentLevel;
import net.sf.jaer.biasgen.*;
import net.sf.jaer.biasgen.Pot.Sex;
import net.sf.jaer.biasgen.Pot.Type;
import net.sf.jaer.util.*;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.JSlider;
import javax.swing.border.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.*;

/**
 * A GUI control component for controlling a Pot.
 * It shows the name of the Pot, its attributes and 
 provides fields for direct bit editing of the Pot value. 
 Subclasses provide customized control
 of voltage or current biases via the sliderAndValuePanel contents.
 * @author  tobi
 */
public class ConfigurableIPotGUIControl extends javax.swing.JPanel implements  Observer, StateEditable {
    // the IPot is the master; it is an Observable that notifies Observers when its value changes.
    // thus if the slider changes the pot value, the pot calls us back here to update the appearance of the slider and of the
    // text field. likewise, if code changes the pot, the appearance here will automagically be updated.
    
    static Preferences prefs=Preferences.userNodeForPackage(IPotSliderTextControl.class);
    static Logger log=Logger.getLogger("ConfigurableIPotGUIControl");
    static double log2=Math.log(2.);
    
    ConfigurableIPot pot;
    StateEdit edit=null;
    UndoableEditSupport editSupport=new UndoableEditSupport();
    BiasgenFrame frame;

    public static boolean sliderEnabled=prefs.getBoolean("PotGUIControl.sliderEnabled",true);
    public static boolean valueEnabled=prefs.getBoolean("PotGUIControl.valueEnabled",true);
    public static boolean bitValueEnabled=prefs.getBoolean("PotGUIControl.bitValueEnabled",false);
    public static boolean bitViewEnabled=prefs.getBoolean("PotGUIControl.bitViewEnabled",false);
    public static boolean sexEnabled=prefs.getBoolean("PotGUIControl.sexEnabled",true);
    public static boolean typeEnabled=prefs.getBoolean("PotGUIControl.typeEnabled",true);
    
    private boolean addedUndoListener=false;
    
    // see java tuturial http://java.sun.com/docs/books/tutorial/uiswing/components/slider.html
    // and http://java.sun.com/docs/books/tutorial/uiswing/components/formattedtextfield.html
    
    /**
     * Creates new form IPotSliderTextControl
     */
    public ConfigurableIPotGUIControl(ConfigurableIPot pot) {
        this.frame=frame;
        this.pot=pot;
        initComponents(); // this has unfortunate byproduect of resetting pot value to 0... don't know how to prevent stateChanged event
        sexComboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.FALSE);
        typeComboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.FALSE);
        currentLevelComboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.FALSE);
        biasEnabledComboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.FALSE);
        sexComboBox.removeAllItems();
        sexComboBox.setMaximumSize(new Dimension(30,40));
        for(Pot.Sex i:Pot.Sex.values()){
            sexComboBox.addItem(i);
        }
        typeComboBox.removeAllItems();
        for(Pot.Type i:Pot.Type.values()){
            typeComboBox.addItem(i);
        }
        currentLevelComboBox.removeAllItems();
        for(ConfigurableIPot.CurrentLevel i:ConfigurableIPot.CurrentLevel.values()){
            currentLevelComboBox.addItem(i);
        }
        biasEnabledComboBox.removeAllItems();
        for(ConfigurableIPot.BiasEnabled i:ConfigurableIPot.BiasEnabled.values()){
            biasEnabledComboBox.addItem(i);
        }
        if(pot!=null){
            nameLabel.setText(pot.getName()); // the name of the bias
            nameLabel.setHorizontalAlignment(SwingConstants.TRAILING);
            nameLabel.setBorder(null);
            if(pot.getTooltipString()!=null) nameLabel.setToolTipText(pot.getTooltipString());
            
            typeComboBox.setSelectedItem(pot.getType().toString());
//            typeLabel.setText(pot.getType().toString());
//            sexLabel.setText(pot.getSex().toString());
            sexComboBox.setSelectedItem(pot.getSex().toString());
            bitPatternTextField.setColumns(pot.getNumBits()+1);
            
            sliderAndValuePanel.setVisible(true);
            pot.loadPreferences(); // to get around slider value change
            pot.addObserver(this); // when pot changes, so does this gui control view
        }
        updateAppearance();  // set controls up with values from ipot
        allInstances.add(this);
    }
    
    public String toString(){
        return "PotGUIControl for pot "+pot.getName();
    }
    
    void rr(){
        revalidate();
        repaint();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        nameLabel = new javax.swing.JLabel();
        flagsPanel = new javax.swing.JPanel();
        biasEnabledComboBox = new javax.swing.JComboBox();
        sexComboBox = new javax.swing.JComboBox();
        typeComboBox = new javax.swing.JComboBox();
        currentLevelComboBox = new javax.swing.JComboBox();
        sliderAndValuePanel = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        biasSlider = new javax.swing.JSlider();
        biasTextField = new javax.swing.JTextField();
        bufferBiasPanel = new javax.swing.JPanel();
        bufferBiasSlider = new javax.swing.JSlider();
        bufferBiasTextField = new javax.swing.JTextField();
        bitPatternTextField = new javax.swing.JTextField();

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                formMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                formMouseExited(evt);
            }
        });
        addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                formAncestorAdded(evt);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));

        nameLabel.setFont(new java.awt.Font("Microsoft Sans Serif", 1, 12));
        nameLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        nameLabel.setText("name");
        nameLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        nameLabel.setMaximumSize(new java.awt.Dimension(75, 15));
        nameLabel.setMinimumSize(new java.awt.Dimension(50, 15));
        nameLabel.setPreferredSize(new java.awt.Dimension(70, 15));
        add(nameLabel);

        flagsPanel.setMaximumSize(new java.awt.Dimension(32767, 16));
        flagsPanel.setLayout(new javax.swing.BoxLayout(flagsPanel, javax.swing.BoxLayout.X_AXIS));

        biasEnabledComboBox.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        biasEnabledComboBox.setMaximumRowCount(3);
        biasEnabledComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Enabled", "Weakly disabled" }));
        biasEnabledComboBox.setToolTipText("Disable to turn off bias");
        biasEnabledComboBox.setMaximumSize(new java.awt.Dimension(32767, 20));
        biasEnabledComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                biasEnabledComboBoxActionPerformed(evt);
            }
        });
        flagsPanel.add(biasEnabledComboBox);

        sexComboBox.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        sexComboBox.setMaximumRowCount(3);
        sexComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "N", "P" }));
        sexComboBox.setToolTipText("N or P type current");
        sexComboBox.setMaximumSize(new java.awt.Dimension(32767, 20));
        sexComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sexComboBoxActionPerformed(evt);
            }
        });
        flagsPanel.add(sexComboBox);

        typeComboBox.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        typeComboBox.setMaximumRowCount(3);
        typeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Normal", "Cascode" }));
        typeComboBox.setToolTipText("Normal or Cascode (extra diode-connected fet)");
        typeComboBox.setMaximumSize(new java.awt.Dimension(32767, 20));
        typeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeComboBoxActionPerformed(evt);
            }
        });
        flagsPanel.add(typeComboBox);

        currentLevelComboBox.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        currentLevelComboBox.setMaximumRowCount(3);
        currentLevelComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Normal current", "Low current" }));
        currentLevelComboBox.setToolTipText("Normal or low current (shifted source)");
        currentLevelComboBox.setMaximumSize(new java.awt.Dimension(32767, 20));
        currentLevelComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                currentLevelComboBoxActionPerformed(evt);
            }
        });
        flagsPanel.add(currentLevelComboBox);

        add(flagsPanel);

        sliderAndValuePanel.setMaximumSize(new java.awt.Dimension(32767, 16));
        sliderAndValuePanel.setPreferredSize(new java.awt.Dimension(208, 16));
        sliderAndValuePanel.setLayout(new javax.swing.BoxLayout(sliderAndValuePanel, javax.swing.BoxLayout.X_AXIS));

        jPanel5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jPanel5formMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jPanel5formMouseExited(evt);
            }
        });
        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.LINE_AXIS));

        biasSlider.setMajorTickSpacing(100);
        biasSlider.setMaximum(1000);
        biasSlider.setMinorTickSpacing(10);
        biasSlider.setToolTipText("Slide to adjust bias");
        biasSlider.setValue(0);
        biasSlider.setAlignmentX(0.0F);
        biasSlider.setFocusable(false);
        biasSlider.setMaximumSize(new java.awt.Dimension(32767, 16));
        biasSlider.setMinimumSize(new java.awt.Dimension(36, 10));
        biasSlider.setPreferredSize(new java.awt.Dimension(150, 10));
        biasSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                biasSliderMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                biasSliderMouseReleased(evt);
            }
        });
        biasSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                biasSliderStateChanged(evt);
            }
        });
        jPanel5.add(biasSlider);

        biasTextField.setColumns(6);
        biasTextField.setFont(new java.awt.Font("Courier New", 0, 11));
        biasTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        biasTextField.setText("value");
        biasTextField.setToolTipText("Enter bias current here. Up and Down arrows change values.");
        biasTextField.setMaximumSize(new java.awt.Dimension(32767, 16));
        biasTextField.setMinimumSize(new java.awt.Dimension(11, 15));
        biasTextField.setPreferredSize(new java.awt.Dimension(53, 15));
        biasTextField.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                biasTextFieldMouseWheelMoved(evt);
            }
        });
        biasTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                biasTextFieldActionPerformed(evt);
            }
        });
        biasTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                biasTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                biasTextFieldFocusLost(evt);
            }
        });
        biasTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                biasTextFieldKeyPressed(evt);
                valueTextFieldKeyPressed(evt);
            }
        });
        jPanel5.add(biasTextField);

        sliderAndValuePanel.add(jPanel5);

        add(sliderAndValuePanel);

        bufferBiasPanel.setMaximumSize(new java.awt.Dimension(32767, 16));
        bufferBiasPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                bufferBiasPanelformMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                bufferBiasPanelformMouseExited(evt);
            }
        });
        bufferBiasPanel.setLayout(new javax.swing.BoxLayout(bufferBiasPanel, javax.swing.BoxLayout.X_AXIS));

        bufferBiasSlider.setMajorTickSpacing(100);
        bufferBiasSlider.setMaximum(1000);
        bufferBiasSlider.setMinorTickSpacing(10);
        bufferBiasSlider.setToolTipText("Slide to adjust buffer bias");
        bufferBiasSlider.setValue(0);
        bufferBiasSlider.setAlignmentX(0.0F);
        bufferBiasSlider.setFocusable(false);
        bufferBiasSlider.setMaximumSize(new java.awt.Dimension(32767, 50));
        bufferBiasSlider.setMinimumSize(new java.awt.Dimension(36, 10));
        bufferBiasSlider.setPreferredSize(new java.awt.Dimension(50, 10));
        bufferBiasSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                bufferBiasSliderMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                bufferBiasSliderMouseReleased(evt);
            }
        });
        bufferBiasSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                bufferBiasSliderStateChanged(evt);
            }
        });
        bufferBiasPanel.add(bufferBiasSlider);

        bufferBiasTextField.setColumns(6);
        bufferBiasTextField.setFont(new java.awt.Font("Courier New", 0, 11));
        bufferBiasTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        bufferBiasTextField.setText("value");
        bufferBiasTextField.setToolTipText("Enter buffer bias current here. Up and Down arrows change values.");
        bufferBiasTextField.setMaximumSize(new java.awt.Dimension(100, 2147483647));
        bufferBiasTextField.setMinimumSize(new java.awt.Dimension(11, 15));
        bufferBiasTextField.setPreferredSize(new java.awt.Dimension(53, 15));
        bufferBiasTextField.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                bufferBiasTextFieldMouseWheelMoved(evt);
            }
        });
        bufferBiasTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bufferBiasTextFieldActionPerformed(evt);
            }
        });
        bufferBiasTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                bufferBiasTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                bufferBiasTextFieldFocusLost(evt);
            }
        });
        bufferBiasTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                bufferBiasTextFieldKeyPressed(evt);
            }
        });
        bufferBiasPanel.add(bufferBiasTextField);

        add(bufferBiasPanel);

        bitPatternTextField.setColumns(10);
        bitPatternTextField.setEditable(false);
        bitPatternTextField.setFont(new java.awt.Font("Monospaced", 0, 10));
        bitPatternTextField.setText("bitPattern");
        bitPatternTextField.setToolTipText("bit value as bits");
        bitPatternTextField.setMaximumSize(new java.awt.Dimension(32767, 16));
        bitPatternTextField.setMinimumSize(new java.awt.Dimension(11, 15));
        bitPatternTextField.setPreferredSize(new java.awt.Dimension(71, 15));
        add(bitPatternTextField);
    }// </editor-fold>//GEN-END:initComponents

    private void currentLevelComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_currentLevelComboBoxActionPerformed
       // must check if action really changes pot state because combobox throws so many events
        if(currentLevelComboBox.getSelectedItem()==null) return;
        if(currentLevelComboBox.getSelectedItem()==pot.getCurrentLevel()) return;
        startEdit();
       pot.setLowCurrentModeEnabled(currentLevelComboBox.getSelectedItem()==ConfigurableIPot.CurrentLevel.Low?true:false);
       endEdit();
    }//GEN-LAST:event_currentLevelComboBoxActionPerformed

    private void typeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeComboBoxActionPerformed
        if(typeComboBox.getSelectedItem()==null) return;
        if(!evt.getActionCommand().equals("comboBoxChanged") || typeComboBox.getSelectedItem()==pot.getType()) return;
        startEdit();
        pot.setType((Pot.Type)typeComboBox.getSelectedItem());
        endEdit();
    }//GEN-LAST:event_typeComboBoxActionPerformed

    private void sexComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sexComboBoxActionPerformed
        if(sexComboBox.getSelectedItem()==null) return;
        if(sexComboBox.getSelectedItem()==pot.getSex()) return;
        startEdit();
        pot.setSex((Pot.Sex)(sexComboBox.getSelectedItem()));
        endEdit();
    }//GEN-LAST:event_sexComboBoxActionPerformed
    Border selectedBorder=new EtchedBorder(), unselectedBorder=new EmptyBorder(1,1,1,1);
    
    private void formMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseExited
//        setBorder(unselectedBorder); // TODO add your handling code here:
    }//GEN-LAST:event_formMouseExited
    
    
    private void formMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseEntered
//        setBorder(selectedBorder);
    }//GEN-LAST:event_formMouseEntered
            
    private void biasSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_biasSliderStateChanged
        // we can get a double send here if user presses uparrow key,
        // resulting in new pot value,
        // which updates the slider position, which ends up with
        // a different bitvalue that makes a new
        // pot value.
        //See http://java.sun.com/docs/books/tutorial/uiswing/components/slider.html
        //        System.out.println("slider state changed");
        // slider is only source of ChangeEvents
        JSlider s = (JSlider)evt.getSource();
        //        System.out.println("slider state changed for "+pot);
        
        //        if(!s.getValueIsAdjusting()){
        //            startEdit();
        //        }
        int v = (int)s.getValue();
        if(v==0){
            pot.setBitValue(0); // these pot chanages will come back to us as Observer events
            // a problem because they will updateAappearance, which will change slider state
            // and generate possibly a new slider changeevent
        }else{
            v=sliderValueFromBitValue(s);
            pot.setBitValue(v);
        }
        //        if(!s.getValueIsAdjusting()){
        ////            System.out.println("slider done");
        //            endEdit();
        //        }
}//GEN-LAST:event_biasSliderStateChanged

    private void biasSliderMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_biasSliderMousePressed
        startEdit(); // start slider edit when mouse is clicked in it! not when dragging it
}//GEN-LAST:event_biasSliderMousePressed

    private void biasSliderMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_biasSliderMouseReleased
        endEdit();
}//GEN-LAST:event_biasSliderMouseReleased

    private void biasTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_biasTextFieldActionPerformed
        // new pots current value entered
        //        System.out.println("value field action performed");
        try{
            //            float v=Float.parseFloat(valueTextField.getText());
            float v=engFormat.parseFloat(biasTextField.getText());
            //            System.out.println("parsed "+valueTextField.getText()+" as "+v);
            startEdit();
            pot.setCurrent(v);
            endEdit();
        }catch(NumberFormatException e){
            Toolkit.getDefaultToolkit().beep();
            biasTextField.selectAll();
        }
}//GEN-LAST:event_biasTextFieldActionPerformed

    private void biasTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_biasTextFieldFocusGained
        biasTextField.setFont(new java.awt.Font("Courier New", 1, 11));
}//GEN-LAST:event_biasTextFieldFocusGained

    private void biasTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_biasTextFieldFocusLost
        biasTextField.setFont(new java.awt.Font("Courier New", 0, 11));
}//GEN-LAST:event_biasTextFieldFocusLost

    private void biasTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_biasTextFieldKeyPressed
        // key pressed in text field
        //        System.out.println("keyPressed evt "+evt);
        //        System.out.println("value field key pressed");
        String s=evt.getKeyText(evt.getKeyCode());
        int code=evt.getKeyCode();
        boolean shift=evt.isShiftDown();
        float byRatio=1.1f;
        if(shift) byRatio=10f;
        if(code==KeyEvent.VK_UP){
            startEdit();
            pot.changeByRatio(byRatio);
            endEdit();
        }else if(code==KeyEvent.VK_DOWN){
            startEdit();
            pot.changeByRatio(1f/byRatio);
            endEdit();
        }
}//GEN-LAST:event_biasTextFieldKeyPressed

    private void biasTextFieldMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_biasTextFieldMouseWheelMoved
        int clicks=evt.getWheelRotation();
        float ratio=(1-clicks*.1f);
        //        System.out.println("ratio="+ratio);
        startEdit();
        pot.changeByRatio(ratio);
        endEdit();
}//GEN-LAST:event_biasTextFieldMouseWheelMoved

    private void jPanel5formMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel5formMouseEntered
        //        setBorder(selectedBorder);
    }//GEN-LAST:event_jPanel5formMouseEntered

    private void jPanel5formMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel5formMouseExited
        //        setBorder(unselectedBorder); // TODO add your handling code here:
    }//GEN-LAST:event_jPanel5formMouseExited

    private void bufferBiasSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_bufferBiasSliderStateChanged
        // we can get a double send here if user presses uparrow key,
        // resulting in new pot value,
        // which updates the slider position, which ends up with
        // a different bitvalue that makes a new
        // pot value.
        //See http://java.sun.com/docs/books/tutorial/uiswing/components/slider.html
        //        System.out.println("slider state changed");
        // slider is only source of ChangeEvents
        JSlider s = (JSlider)evt.getSource();
        //        System.out.println("slider state changed for "+pot);
        
        //        if(!s.getValueIsAdjusting()){
        //            startEdit();
        //        }
        int v = (int)s.getValue();
        if(v==0){
            pot.setBufferBitValue(0); // these pot chanages will come back to us as Observer events
            // a problem because they will updateAappearance, which will change slider state
            // and generate possibly a new slider changeevent
        }else{
            v=bufferSliderValueFromBitValue(s);
            pot.setBufferBitValue(v);
        }
        //        if(!s.getValueIsAdjusting()){
        ////            System.out.println("slider done");
        //            endEdit();
        //        }
}//GEN-LAST:event_bufferBiasSliderStateChanged

    private void bufferBiasSliderMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bufferBiasSliderMousePressed
        startEdit(); // start slider edit when mouse is clicked in it! not when dragging it
}//GEN-LAST:event_bufferBiasSliderMousePressed

    private void bufferBiasSliderMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bufferBiasSliderMouseReleased
        endEdit();
}//GEN-LAST:event_bufferBiasSliderMouseReleased

    private void bufferBiasTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bufferBiasTextFieldActionPerformed
        try{
            float v=engFormat.parseFloat(bufferBiasTextField.getText());
            startEdit();
            pot.setBufferCurrent(v);
            endEdit();
        }catch(NumberFormatException e){
            Toolkit.getDefaultToolkit().beep();
            bufferBiasTextField.selectAll();
        }
}//GEN-LAST:event_bufferBiasTextFieldActionPerformed

    private void bufferBiasTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_bufferBiasTextFieldFocusGained
        bufferBiasTextField.setFont(new java.awt.Font("Courier New", 1, 11));
}//GEN-LAST:event_bufferBiasTextFieldFocusGained

    private void bufferBiasTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_bufferBiasTextFieldFocusLost
        bufferBiasTextField.setFont(new java.awt.Font("Courier New", 0, 11));
}//GEN-LAST:event_bufferBiasTextFieldFocusLost

    private void bufferBiasTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_bufferBiasTextFieldKeyPressed
        // key pressed in text field
        //        System.out.println("keyPressed evt "+evt);
        //        System.out.println("value field key pressed");
        String s=evt.getKeyText(evt.getKeyCode());
        int code=evt.getKeyCode();
        boolean shift=evt.isShiftDown();
        float byRatio=1.1f;
        if(shift) byRatio=10f;
        if(code==KeyEvent.VK_UP){
            startEdit();
            if(pot.getBufferBitValue()<8) {
                pot.setBufferBitValue(pot.getBufferBitValue()+1);
            }else{
                pot.setBufferCurrent(pot.getBufferCurrent()*byRatio);
            }
            endEdit();
        }else if(code==KeyEvent.VK_DOWN){
            startEdit();
            if(pot.getBufferBitValue()<8) {
                pot.setBufferBitValue(pot.getBufferBitValue()-1);
            }else{
                pot.setBufferCurrent(pot.getBufferCurrent()/byRatio);
            }
            endEdit();
        }
}//GEN-LAST:event_bufferBiasTextFieldKeyPressed

    private void bufferBiasTextFieldMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_bufferBiasTextFieldMouseWheelMoved
        int clicks=evt.getWheelRotation();
        pot.setBufferBitValue(pot.getBufferBitValue()-clicks); // rotating wheel away gives negative clicks (scrolling up) but should increase current
}//GEN-LAST:event_bufferBiasTextFieldMouseWheelMoved

    private void bufferBiasPanelformMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bufferBiasPanelformMouseEntered
        //        setBorder(selectedBorder);
}//GEN-LAST:event_bufferBiasPanelformMouseEntered

    private void bufferBiasPanelformMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bufferBiasPanelformMouseExited
        //        setBorder(unselectedBorder); // TODO add your handling code here:
}//GEN-LAST:event_bufferBiasPanelformMouseExited

    private void valueTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_valueTextFieldKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_valueTextFieldKeyPressed

    private void biasEnabledComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_biasEnabledComboBoxActionPerformed
        startEdit();
        pot.setEnabled(biasEnabledComboBox.getSelectedItem()==ConfigurableIPot.BiasEnabled.Enabled?true:false);
        endEdit();
}//GEN-LAST:event_biasEnabledComboBoxActionPerformed

private void formAncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_formAncestorAdded
            if (addedUndoListener) {
                return;
            }
            addedUndoListener = true;
            if (evt.getComponent() instanceof Container) {
                Container anc = (Container) evt.getComponent();
                while (anc != null && anc instanceof Container) {
                    if (anc instanceof UndoableEditListener) {
                        editSupport.addUndoableEditListener((UndoableEditListener) anc);
                        break;
                    }
                    anc = anc.getParent();
                }
            }
}//GEN-LAST:event_formAncestorAdded
    
    
//     private int oldPotValue=0;
     
    /** when slider is moved, event is sent here. The slider is the 'master' of the value in the text field.
     * Slider is log scale, from pot min to pot max with caveat that zero position is zero current (no current splitter
     * outputs switched on) and rest of values are log scale from pot.getCurrentResolution to pot.getMaxCurrent
     * @param e the ChangeEvent
     */   
     void startEdit(){
//        System.out.println("ipot start edit "+pot);
         edit=new MyStateEdit(this, "pot change");
//         oldPotValue=pot.getBitValue();
     }
     
     void endEdit(){
//         if(oldPotValue==pot.getBitValue()){
////            System.out.println("no edit, because no change in "+pot);
//             return;
//         }
//        System.out.println("ipot endEdit "+pot);
         if(edit!=null) edit.end();
//        System.out.println("ipot "+pot+" postEdit");
         editSupport.postEdit(edit);
     }
     
     final String KEY_BITVALUE="bitValue";
     final String KEY_BUFFERBITVALUE="bufferBitValue";
     final String KEY_SEX="sex";
     final String KEY_CASCODENORMALTYPE="cascodeNormalType";
     final String KEY_CURRENTLEVEL="currentLevel";
     final String KEY_ENABLED="enabled";
     
     public void restoreState(Hashtable<?,?> hashtable) {
//        System.out.println("restore state");
         if(hashtable==null) throw new RuntimeException("null hashtable");
         if(hashtable.get(KEY_BITVALUE)==null) {
             log.warning("pot "+pot+" not in hashtable "+hashtable+" with size="+hashtable.size());
//            Set s=hashtable.entrySet();
//            System.out.println("hashtable entries");
//            for(Iterator i=s.iterator();i.hasNext();){
//                Map.Entry me=(Map.Entry)i.next();
//                System.out.println(me);
//            }
             return;
         }
         pot.setBitValue((Integer)hashtable.get(KEY_BITVALUE));
         pot.setBufferBitValue((Integer)hashtable.get(KEY_BUFFERBITVALUE));
         pot.setSex((Sex)hashtable.get(KEY_SEX));
         pot.setType((Type)hashtable.get(KEY_CASCODENORMALTYPE));
         pot.setCurrentLevel((CurrentLevel)hashtable.get(KEY_CURRENTLEVEL));
         pot.setEnabled((Boolean)hashtable.get(KEY_ENABLED));
     }
     
     public void storeState(Hashtable<Object, Object> hashtable) {
//        System.out.println(" storeState "+pot);
         hashtable.put(KEY_BITVALUE, new Integer(pot.getBitValue()));
         hashtable.put(KEY_BUFFERBITVALUE, new Integer(pot.getBufferBitValue()));
         hashtable.put(KEY_SEX, pot.getSex()); // TODO assumes sex nonnull
         hashtable.put(KEY_CASCODENORMALTYPE, pot.getType());
         hashtable.put(KEY_CURRENTLEVEL, pot.getCurrentLevel());
         hashtable.put(KEY_ENABLED, new Boolean(pot.isEnabled()));

     }
     
     class MyStateEdit extends StateEdit{
         public MyStateEdit(StateEditable o, String s){
             super(o,s);
         }
         protected void removeRedundantState(){}; // override this to actually get a state stored!!
     }
 
    private static EngineeringFormat engFormat=new EngineeringFormat();
    
    /** updates the gui slider and text
     fields to match actual pot values. Neither of these trigger events.
     */
    protected void updateAppearance(){
        if(pot==null) return;
        if(biasSlider.isVisible()!=sliderEnabled){ biasSlider.setVisible(sliderEnabled); rr(); }
        if(biasTextField.isVisible()!=valueEnabled){ biasTextField.setVisible(valueEnabled); rr(); }
        
        biasSlider.setValue( bitValueFromSliderValue(biasSlider)) ;
        biasTextField.setText(engFormat.format(pot.getCurrent()));

        if(bitPatternTextField.isVisible()!=bitViewEnabled){ bitPatternTextField.setVisible(bitViewEnabled); rr(); }
        bitPatternTextField.setText(pot.toBitPatternString());
        
        bufferBiasSlider.setValue(bufferBitValueFromSliderValue(bufferBiasSlider));
        bufferBiasTextField.setText(engFormat.format(pot.getBufferCurrent()));
        
        if(sexComboBox.getSelectedItem()!=pot.getSex()) sexComboBox.setSelectedItem(pot.getSex());
        if(typeComboBox.getSelectedItem()!=pot.getType()) typeComboBox.setSelectedItem(pot.getType());
        if(currentLevelComboBox.getSelectedItem()!=pot.getCurrentLevel()) currentLevelComboBox.setSelectedItem(pot.getCurrentLevel());
        if(biasEnabledComboBox.getSelectedItem()!=pot.getBiasEnabled()) biasEnabledComboBox.setSelectedItem(pot.getBiasEnabled());
//        System.out.println(pot+" set combobox selected="+biasEnabledComboBox.getSelectedItem());
//        log.info("updateAppearance for "+pot);
    }
    
    // following two methods compute slider/bit value inverses
    
    private int sliderValueFromBitValue(JSlider s){
        double f=(double)s.getValue()/s.getMaximum(); // fraction of slider
        int v=(int)Math.round(Math.pow(2,f*pot.getNumBits())); // bit value is 2^(frac*Numbits)
        return v;
    }
    
    private int bitValueFromSliderValue(JSlider s){
        int v=(int)Math.round(s.getMaximum()/(double)pot.getNumBits()*Math.log((double)pot.getBitValue())/log2);
        return v;
    }
    
    private int bufferBitValueFromSliderValue(JSlider s){
        int v=(int)Math.round(s.getMaximum()/(double)ConfigurableIPot.numBufferBiasBits*Math.log((double)pot.getBufferBitValue())/log2);
        return v;
    }

    private int bufferSliderValueFromBitValue(JSlider s){
        double f=(double)s.getValue()/s.getMaximum(); // fraction of slider
        int v=(int)Math.round(Math.pow(2,f*ConfigurableIPot.numBufferBiasBits)); // bit value is 2^(frac*Numbits)
        return v;
    }

    /** called when Observable changes (pot changes) */
    public void update(Observable observable, Object obj) {
        if(observable instanceof IPot){
//            log.info("observable="+observable);
            SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    // don't do the following - it sometimes prevents display updates or results in double updates
//                        slider.setValueIsAdjusting(true); // try to prevent a new event from the slider
                    updateAppearance();
                }
            });
        }
    }

     
     
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox biasEnabledComboBox;
    private javax.swing.JSlider biasSlider;
    private javax.swing.JTextField biasTextField;
    private javax.swing.JTextField bitPatternTextField;
    private javax.swing.JPanel bufferBiasPanel;
    private javax.swing.JSlider bufferBiasSlider;
    private javax.swing.JTextField bufferBiasTextField;
    private javax.swing.JComboBox currentLevelComboBox;
    private javax.swing.JPanel flagsPanel;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JComboBox sexComboBox;
    private javax.swing.JPanel sliderAndValuePanel;
    private javax.swing.JComboBox typeComboBox;
    // End of variables declaration//GEN-END:variables
    
    
    public JTextField getBitPatternTextField() {
        return this.bitPatternTextField;
    }
    
    public static boolean isBitValueEnabled() {
        return ConfigurableIPotGUIControl.bitValueEnabled;
    }
    
    public static void setBitValueEnabled(final boolean bitValueEnabled) {
        ConfigurableIPotGUIControl.bitValueEnabled = bitValueEnabled;
        prefs.putBoolean("PotGUIControl.bitValueEnabled", bitValueEnabled);
    }
    
    public static boolean isBitViewEnabled() {
        return ConfigurableIPotGUIControl.bitViewEnabled;
    }
    
    public static void setBitViewEnabled(final boolean bitViewEnabled) {
        ConfigurableIPotGUIControl.bitViewEnabled = bitViewEnabled;
        prefs.putBoolean("PotGUIControl.bitViewEnabled", bitViewEnabled);
    }
    
    public static boolean isValueEnabled() {
        return ConfigurableIPotGUIControl.valueEnabled;
    }
    
    public static void setValueEnabled(final boolean valueEnabled) {
        ConfigurableIPotGUIControl.valueEnabled = valueEnabled;
        prefs.putBoolean("PotGUIControl.valueEnabled", valueEnabled);
    }
    
    public static boolean isSexEnabled() {
        return ConfigurableIPotGUIControl.sexEnabled;
    }
    
    public static void setSexEnabled(final boolean sexEnabled) {
        ConfigurableIPotGUIControl.sexEnabled = sexEnabled;
        prefs.putBoolean("PotGUIControl.sliderEnabled", sliderEnabled);
    }
    
    public static boolean isSliderEnabled() {
        return IPotSliderTextControl.sliderEnabled;
    }
    
    public static void setSliderEnabled(final boolean sliderEnabled) {
        ConfigurableIPotGUIControl.sliderEnabled = sliderEnabled;
        prefs.putBoolean("PotGUIControl.sliderEnabled", sliderEnabled);
    }
    
    public static boolean isTypeEnabled() {
        return ConfigurableIPotGUIControl.typeEnabled;
    }
    
    public static void setTypeEnabled(final boolean typeEnabled) {
        ConfigurableIPotGUIControl.typeEnabled = typeEnabled;
        prefs.putBoolean("PotGUIControl.typeEnabled", typeEnabled);
    }
    
    static ArrayList<ConfigurableIPotGUIControl> allInstances=new ArrayList<ConfigurableIPotGUIControl>();
    
    public static void revalidateAllInstances(){
        for(ConfigurableIPotGUIControl c:allInstances){
            c.updateAppearance();
            c.revalidate();
        }
    }
    
    
    static String[] controlNames={"Type","Sex","Slider","BitValue","BitView"};
    public static JMenu viewMenu;
    static {
        viewMenu=new JMenu("View options");
        viewMenu.setMnemonic('V');
        for(int i=0;i<controlNames.length;i++){
            viewMenu.add(new VisibleSetter(controlNames[i])); // add a menu item to enable view of this class of information
        }
    }
    
    /** this inner static class updates the appearance of all instances of the control 
     */
    static class VisibleSetter extends JCheckBoxMenuItem{
        public String myName;
        Method setMethod,isSetMethod;
        public VisibleSetter(String myName){
            super(myName);
            this.myName=myName;
            try{
                setMethod=ConfigurableIPotGUIControl.class.getMethod("set"+myName+"Enabled", Boolean.TYPE);
                isSetMethod=ConfigurableIPotGUIControl.class.getMethod("is"+myName+"Enabled");
                boolean isSel=(Boolean)isSetMethod.invoke(ConfigurableIPotGUIControl.class);
                setSelected(isSel);
            }catch(Exception e){
                e.printStackTrace();
            }
            addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    try{
                        setMethod.invoke(IPotSliderTextControl.class, new Boolean(isSelected()));
                        setSelected(isSelected());
                    }catch(Exception e2){
                        e2.printStackTrace();
                    }
                    ConfigurableIPotGUIControl.revalidateAllInstances();
                }
            });
        }
    }
    
    public javax.swing.JPanel getSliderAndValuePanel() {
        return sliderAndValuePanel;
    }
    
    public void setSliderAndValuePanel(javax.swing.JPanel sliderAndValuePanel) {
        this.sliderAndValuePanel = sliderAndValuePanel;
    }

    
    

}

