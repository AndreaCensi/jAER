/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PanTiltFrame.java
 *
 * Created on 27.05.2009, 13:08:56
 */
package ch.unizh.ini.jaer.projects.holger;

import java.util.ListIterator;
import javax.swing.JOptionPane;

/**
 *
 * @author Holger
 */
public class PanTiltFrame extends javax.swing.JFrame {

    public PanTiltControl panTiltControl = null;

    /** Creates new form PanTiltFrame */
    public PanTiltFrame() {
        initComponents();
        panTiltControl = new PanTiltControl();
        java.util.List<String> ports = panTiltControl.getPortList();
        ListIterator i = ports.listIterator();
        while (i.hasNext()) {
            cbxComPort.addItem(i.next());
        }
        cbxComPort.setSelectedIndex(-1);
        this.pack();
        this.setSize(40, 100);
    }

    public void setRetinaPanOffset(float retinaPanOffset) {
        txtRetinaPanOffset.setText(String.format("%.1f", retinaPanOffset));
        repaint();
    }

    public void setCochleaPanOffset(float cochleaPanOffset) {
        txtCochleaPanOffset.setText(String.format("%.1f", cochleaPanOffset));
        repaint();
    }

    public void setRetinaTiltOffset(float retinaTiltOffset) {
        txtRetinaTiltOffset.setText(String.format("%.1f", retinaTiltOffset));
        repaint();
    }

    public void setCochleaTiltOffset(float cochleaTiltOffset) {
        txtCochleaTiltOffset.setText(String.format("%.1f", cochleaTiltOffset));
        repaint();
    }

    public void setRetinaConfidence(float retinaConfidence) {
        txtRetinaConfidence.setText(String.format("%.1f", retinaConfidence));
        repaint();
    }

    public void setCochleaConfidence(float cochleaConfidence) {
        txtCochleaConfidence.setText(String.format("%.1f", cochleaConfidence));
        repaint();
    }

    public boolean isLogResponse() {
        return cbxLogResponse.isSelected();
    }

    public boolean isUseCochlea() {
        return cbxUseCochlea.isSelected();
    }

    public boolean isUseRetina() {
        return cbxUseRetina.isSelected();
    }

    public int getCochleaThreshold() {
        return Integer.parseInt(txtCochleaThreshold.getText());
    }

    public int getRetinaThreshold() {
        return Integer.parseInt(txtRetinaThreshold.getText());
    }

    public void setPanPos(int pos) {
        sldPanPos.setValue(pos);
        this.setPanPos();
    }

    void setTiltPos(int pos) {
        sldTiltPos.setValue(pos);
        this.setTiltPos();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        btnConnect = new javax.swing.JButton();
        cbxComPort = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        sldPanPos = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        txtCommand = new javax.swing.JTextField();
        btnExecuteCommand = new javax.swing.JButton();
        cbxLogResponse = new javax.swing.JCheckBox();
        txtCochleaPanOffset = new javax.swing.JTextField();
        txtRetinaPanOffset = new javax.swing.JTextField();
        txtRetinaTiltOffset = new javax.swing.JTextField();
        txtRetinaConfidence = new javax.swing.JTextField();
        txtCochleaConfidence = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtCochleaThreshold = new javax.swing.JTextField();
        txtRetinaThreshold = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        btnSetSpeed = new javax.swing.JButton();
        txtPanPos = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        txtSpeed = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        sldTiltPos = new javax.swing.JSlider();
        txtTiltPos = new javax.swing.JTextField();
        cbxUseCochlea = new javax.swing.JCheckBox();
        cbxUseRetina = new javax.swing.JCheckBox();
        btnHalt = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        txtCochleaTiltOffset = new javax.swing.JTextField();
        btnSetPanPos = new javax.swing.JButton();
        btnSetTiltPos = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        txtTiltPosMin = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        txtTiltPosMax = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        txtPanPosMin = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        txtPanPosMax = new javax.swing.JTextField();
        txtWaitPeriod = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Pan-TIlt");
        setResizable(false);

        jLabel1.setText("Cochlea:");

        jLabel2.setText("Retina:");

        btnConnect.setText("Connect to Pan-Tilt-Unit");
        btnConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectActionPerformed(evt);
            }
        });

        cbxComPort.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "COM10", "COM11", "COM12" }));
        cbxComPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxComPortActionPerformed(evt);
            }
        });

        jLabel3.setText("COM-Port:");

        sldPanPos.setMaximum(1000);
        sldPanPos.setMinimum(-1000);
        sldPanPos.setValue(0);
        sldPanPos.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldPanPosStateChanged(evt);
            }
        });

        jLabel4.setText("Set Pan Position:");

        txtCommand.setText("PP100");

        btnExecuteCommand.setText("Execute Command");
        btnExecuteCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExecuteCommandActionPerformed(evt);
            }
        });

        cbxLogResponse.setText("Log Pan-Tilt Responses");
        cbxLogResponse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxLogResponseActionPerformed(evt);
            }
        });

        txtCochleaPanOffset.setEditable(false);
        txtCochleaPanOffset.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtCochleaPanOffset.setText("1");

        txtRetinaPanOffset.setEditable(false);
        txtRetinaPanOffset.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtRetinaPanOffset.setText("1");

        txtRetinaTiltOffset.setEditable(false);
        txtRetinaTiltOffset.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtRetinaTiltOffset.setText("1");

        txtRetinaConfidence.setEditable(false);
        txtRetinaConfidence.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtRetinaConfidence.setText("1");

        txtCochleaConfidence.setEditable(false);
        txtCochleaConfidence.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtCochleaConfidence.setText("1");

        jLabel5.setText("Pan-Offset");

        jLabel6.setText("Confidence");

        txtCochleaThreshold.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtCochleaThreshold.setText("1");

        txtRetinaThreshold.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtRetinaThreshold.setText("1");

        jLabel7.setText("Threshold");

        btnSetSpeed.setText("Set Pan-Tilt-Speed");
        btnSetSpeed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetSpeedActionPerformed(evt);
            }
        });

        txtPanPos.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtPanPos.setText("0");
        txtPanPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPanPosActionPerformed(evt);
            }
        });

        jLabel8.setText("Tilt-Offset");

        txtSpeed.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtSpeed.setText("1000");

        jLabel9.setText("Set Tilt Position:");

        sldTiltPos.setMaximum(500);
        sldTiltPos.setMinimum(-500);
        sldTiltPos.setValue(0);
        sldTiltPos.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldTiltPosStateChanged(evt);
            }
        });

        txtTiltPos.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtTiltPos.setText("0");
        txtTiltPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTiltPosActionPerformed(evt);
            }
        });

        btnHalt.setText("Halt Pan-Tilt");
        btnHalt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHaltActionPerformed(evt);
            }
        });

        jLabel10.setText("Use");

        txtCochleaTiltOffset.setEditable(false);
        txtCochleaTiltOffset.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtCochleaTiltOffset.setText("1");

        btnSetPanPos.setText("Set");
        btnSetPanPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetPanPosActionPerformed(evt);
            }
        });

        btnSetTiltPos.setText("Set");
        btnSetTiltPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetTiltPosActionPerformed(evt);
            }
        });

        jLabel11.setText("Min. Tilt Position:");

        txtTiltPosMin.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtTiltPosMin.setText("-500");
        txtTiltPosMin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTiltPosMinActionPerformed(evt);
            }
        });

        jLabel12.setText("Max. Tilt Position:");

        txtTiltPosMax.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtTiltPosMax.setText("500");
        txtTiltPosMax.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTiltPosMaxActionPerformed(evt);
            }
        });

        jLabel13.setText("Min. Pan Position:");

        txtPanPosMin.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtPanPosMin.setText("-1000");
        txtPanPosMin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPanPosMinActionPerformed(evt);
            }
        });

        jLabel14.setText("Max. Pan  Position:");

        txtPanPosMax.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtPanPosMax.setText("1000");
        txtPanPosMax.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPanPosMaxActionPerformed(evt);
            }
        });

        txtWaitPeriod.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtWaitPeriod.setText("500");
        txtWaitPeriod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtWaitPeriodActionPerformed(evt);
            }
        });

        jLabel15.setText("Command:");

        jLabel16.setText("Speed:");

        jLabel17.setText("Wait Period after Movement:");

        jLabel18.setText("ms");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel9))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sldPanPos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sldTiltPos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtPanPos, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtTiltPos, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnSetPanPos)
                            .addComponent(btnSetTiltPos)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addComponent(jLabel11))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtTiltPosMin, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtPanPosMin, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14)
                            .addComponent(jLabel12))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtPanPosMax, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtTiltPosMax, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(cbxLogResponse, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(txtWaitPeriod)
                                    .addComponent(cbxComPort, 0, 0, Short.MAX_VALUE)
                                    .addComponent(txtSpeed)
                                    .addComponent(txtCommand, javax.swing.GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE))
                                .addGap(8, 8, 8)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(btnConnect, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnExecuteCommand, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnSetSpeed, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnHalt, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel18)))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtCochleaPanOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRetinaPanOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addGap(19, 19, 19))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(txtRetinaTiltOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtCochleaTiltOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtCochleaConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6)
                            .addComponent(txtRetinaConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtCochleaThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRetinaThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cbxUseRetina)
                            .addComponent(cbxUseCochlea)
                            .addComponent(jLabel10))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7)
                    .addComponent(jLabel10)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(txtCochleaPanOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtCochleaConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtCochleaThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtCochleaTiltOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(txtRetinaPanOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRetinaConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRetinaThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRetinaTiltOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cbxUseCochlea)
                        .addGap(18, 18, 18)
                        .addComponent(cbxUseRetina)))
                .addGap(33, 33, 33)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                    .addComponent(btnConnect)
                    .addComponent(cbxComPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                    .addComponent(txtCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnExecuteCommand)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                    .addComponent(btnSetSpeed)
                    .addComponent(txtSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16))
                .addGap(9, 9, 9)
                .addComponent(btnHalt, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(txtWaitPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cbxLogResponse, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(28, 28, 28)
                        .addComponent(jLabel9))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sldPanPos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(txtPanPos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btnSetPanPos)))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sldTiltPos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(txtTiltPos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btnSetTiltPos)))))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel13)
                    .addComponent(txtPanPosMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14)
                    .addComponent(txtPanPosMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel11)
                    .addComponent(txtTiltPosMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12)
                    .addComponent(txtTiltPosMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cbxComPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxComPortActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_cbxComPortActionPerformed

    private void btnConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConnectActionPerformed
        if (panTiltControl == null) {
            panTiltControl = new PanTiltControl();
        }
        PanTiltControl.setWaitPeriod(Integer.parseInt(txtWaitPeriod.getText()));
        if (panTiltControl.isConnected() == false) {
            try {
                panTiltControl.connect((String) this.cbxComPort.getSelectedItem());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_btnConnectActionPerformed

    private void btnExecuteCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExecuteCommandActionPerformed
        if (panTiltControl == null || panTiltControl.isConnected() == false) {
            JOptionPane.showMessageDialog(null, "Not Connected to Pan-Tilt-Unit", "Not Connected", JOptionPane.OK_CANCEL_OPTION);
        } else {
            panTiltControl.executeCommand(this.txtCommand.getText());
        }
    }//GEN-LAST:event_btnExecuteCommandActionPerformed

    private void btnSetSpeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetSpeedActionPerformed
        if (panTiltControl == null || panTiltControl.isConnected() == false) {
            JOptionPane.showMessageDialog(null, "Not Connected to Pan-Tilt-Unit", "Not Connected", JOptionPane.OK_CANCEL_OPTION);
        } else {
            panTiltControl.setPanSpeed(Integer.parseInt(this.txtSpeed.getText()));
        }
    }//GEN-LAST:event_btnSetSpeedActionPerformed

    private void btnHaltActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHaltActionPerformed
        if (panTiltControl == null || panTiltControl.isConnected() == false) {
            JOptionPane.showMessageDialog(null, "Not Connected to Pan-Tilt-Unit", "Not Connected", JOptionPane.OK_CANCEL_OPTION);
        } else {
            panTiltControl.halt();
        }
    }//GEN-LAST:event_btnHaltActionPerformed

    private void sldPanPosStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldPanPosStateChanged
        this.txtPanPos.setText(Integer.toString(sldPanPos.getValue()));
    }//GEN-LAST:event_sldPanPosStateChanged

    private void sldTiltPosStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldTiltPosStateChanged
        this.txtTiltPos.setText(Integer.toString(sldTiltPos.getValue()));
    }//GEN-LAST:event_sldTiltPosStateChanged

    private void btnSetPanPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetPanPosActionPerformed
        setPanPos();
    }//GEN-LAST:event_btnSetPanPosActionPerformed

    private void setPanPos() {
        if (panTiltControl == null || panTiltControl.isConnected() == false) {
            JOptionPane.showMessageDialog(null, "Not Connected to Pan-Tilt-Unit", "Not Connected", JOptionPane.OK_CANCEL_OPTION);
        } else {
            panTiltControl.setPanPos(sldPanPos.getValue());
        }
    }

    private void btnSetTiltPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetTiltPosActionPerformed
        setTiltPos();
    }//GEN-LAST:event_btnSetTiltPosActionPerformed

    private void setTiltPos() {
    if (panTiltControl == null || panTiltControl.isConnected() == false) {
            JOptionPane.showMessageDialog(null, "Not Connected to Pan-Tilt-Unit", "Not Connected", JOptionPane.OK_CANCEL_OPTION);
        } else {
            panTiltControl.setTiltPos(sldTiltPos.getValue());
        }
    }

    private void txtPanPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPanPosActionPerformed
        sldPanPos.setValue(Integer.parseInt(txtPanPos.getText()));
    }//GEN-LAST:event_txtPanPosActionPerformed

    private void txtTiltPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTiltPosActionPerformed
        sldTiltPos.setValue(Integer.parseInt(txtTiltPos.getText()));
    }//GEN-LAST:event_txtTiltPosActionPerformed

    private void txtTiltPosMinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTiltPosMinActionPerformed
        sldTiltPos.setMinimum(Integer.parseInt(txtTiltPosMin.getText()));
        updateValuesToBoundaries();
}//GEN-LAST:event_txtTiltPosMinActionPerformed

    private void txtTiltPosMaxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTiltPosMaxActionPerformed
        sldTiltPos.setMaximum(Integer.parseInt(txtTiltPosMax.getText()));
        updateValuesToBoundaries();
}//GEN-LAST:event_txtTiltPosMaxActionPerformed

    private void txtPanPosMinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPanPosMinActionPerformed
        sldPanPos.setMinimum(Integer.parseInt(txtPanPosMin.getText()));
        updateValuesToBoundaries();
}//GEN-LAST:event_txtPanPosMinActionPerformed

    private void txtPanPosMaxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPanPosMaxActionPerformed
        sldPanPos.setMaximum(Integer.parseInt(txtPanPosMax.getText()));
        updateValuesToBoundaries();
}//GEN-LAST:event_txtPanPosMaxActionPerformed

    private void cbxLogResponseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxLogResponseActionPerformed
        PanTiltControl.setLogResponses(cbxLogResponse.isSelected());
    }//GEN-LAST:event_cbxLogResponseActionPerformed

    private void txtWaitPeriodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtWaitPeriodActionPerformed
        PanTiltControl.setWaitPeriod(Integer.parseInt(txtWaitPeriod.getText()));
    }//GEN-LAST:event_txtWaitPeriodActionPerformed

    private void updateValuesToBoundaries() {
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new PanTiltFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConnect;
    private javax.swing.JButton btnExecuteCommand;
    private javax.swing.JButton btnHalt;
    private javax.swing.JButton btnSetPanPos;
    private javax.swing.JButton btnSetSpeed;
    private javax.swing.JButton btnSetTiltPos;
    private javax.swing.JComboBox cbxComPort;
    private javax.swing.JCheckBox cbxLogResponse;
    private javax.swing.JCheckBox cbxUseCochlea;
    private javax.swing.JCheckBox cbxUseRetina;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSlider sldPanPos;
    private javax.swing.JSlider sldTiltPos;
    private javax.swing.JTextField txtCochleaConfidence;
    private javax.swing.JTextField txtCochleaPanOffset;
    private javax.swing.JTextField txtCochleaThreshold;
    private javax.swing.JTextField txtCochleaTiltOffset;
    private javax.swing.JTextField txtCommand;
    private javax.swing.JTextField txtPanPos;
    private javax.swing.JTextField txtPanPosMax;
    private javax.swing.JTextField txtPanPosMin;
    private javax.swing.JTextField txtRetinaConfidence;
    private javax.swing.JTextField txtRetinaPanOffset;
    private javax.swing.JTextField txtRetinaThreshold;
    private javax.swing.JTextField txtRetinaTiltOffset;
    private javax.swing.JTextField txtSpeed;
    private javax.swing.JTextField txtTiltPos;
    private javax.swing.JTextField txtTiltPosMax;
    private javax.swing.JTextField txtTiltPosMin;
    private javax.swing.JTextField txtWaitPeriod;
    // End of variables declaration//GEN-END:variables
}