package udman;

import java.util.StringTokenizer;
import javax.swing.JOptionPane;

public class RenameDialog extends javax.swing.JDialog {

    
    private int[] updatedNameChars;
    
    public RenameDialog(java.awt.Frame parent, boolean modal,int[] oldNameChars) {
        super(parent, modal);
        initComponents();
        this.updatedNameChars=null;
        
        int[] nameChars = oldNameChars;
        
        StringBuilder sbText = new StringBuilder();
        StringBuilder sbHex = new StringBuilder();
        
        for(int i=0;i<nameChars.length;i++) {
            sbText.append((char)nameChars[i]);
            sbHex.append(String.format("%02X ",nameChars[i]));
        }
        
        jtfText.setText(sbText.toString().trim());
        jtfHex.setText(sbHex.toString().trim());
        
        getRootPane().setDefaultButton(jbtConfirm);
        jtfText.grabFocus();
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        bgTextHex = new javax.swing.ButtonGroup();
        jrbText = new javax.swing.JRadioButton();
        jrbHex = new javax.swing.JRadioButton();
        jtfText = new javax.swing.JTextField();
        jtfHex = new javax.swing.JTextField();
        pCommands = new javax.swing.JPanel();
        jbtConfirm = new javax.swing.JButton();
        jbtCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Rename");
        setModal(true);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        bgTextHex.add(jrbText);
        jrbText.setMnemonic('T');
        jrbText.setSelected(true);
        jrbText.setText("Text:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jrbText, gridBagConstraints);

        bgTextHex.add(jrbHex);
        jrbHex.setMnemonic('H');
        jrbHex.setText("Hex:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jrbHex, gridBagConstraints);

        jtfText.setColumns(12);
        jtfText.setFont(new java.awt.Font("DialogInput", 0, 12)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jtfText, gridBagConstraints);

        jtfHex.setColumns(34);
        jtfHex.setFont(new java.awt.Font("DialogInput", 0, 12)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(jtfHex, gridBagConstraints);

        jbtConfirm.setMnemonic('O');
        jbtConfirm.setText("OK");
        jbtConfirm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onRenameConfirmed(evt);
            }
        });
        pCommands.add(jbtConfirm);

        jbtCancel.setMnemonic('C');
        jbtCancel.setText("Cancel");
        jbtCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onCancel(evt);
            }
        });
        pCommands.add(jbtCancel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(pCommands, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void onRenameConfirmed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onRenameConfirmed
        
        if (jrbText.isSelected()) {
            String nm = jtfText.getText();
            int numChars = Math.min(10, nm.length());
            
            updatedNameChars = new int[10];
            
            for(int i=0;i<10;i++) {
                updatedNameChars[i]=0x20;
            }
            for(int i=0;i<numChars;i++) {
                updatedNameChars[i]=nm.charAt(i);
            }
        }
        else {
            try {
                updatedNameChars = new int[10];
                StringTokenizer tk = new StringTokenizer(jtfHex.getText(), " ");
                int numTokens = tk.countTokens();
                
                if (numTokens != 10) {
                    throw new NumberFormatException("Not enough hexadecimal values. Expected 10, found "+numTokens+".");
                }
                for (int i = 0;i<numTokens;i++) {
                    int oneNumber = Integer.parseInt(tk.nextToken(), 16);
                    if (oneNumber<0x00 || oneNumber>0xFF) throw new NumberFormatException("Hexadecimal value not between $00 and $FF");
                    updatedNameChars[i]=oneNumber;
                }
            
            }
            catch (NumberFormatException nfe) {
                updatedNameChars = null;
                JOptionPane.showMessageDialog(this, nfe.getMessage(), "Invalid input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
        }
        
        setVisible(false);
        
        
    }//GEN-LAST:event_onRenameConfirmed

    private void onCancel(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onCancel
        setVisible(false);
    }//GEN-LAST:event_onCancel

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(RenameDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(RenameDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(RenameDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(RenameDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                RenameDialog dialog = new RenameDialog(new javax.swing.JFrame(), true,null);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgTextHex;
    private javax.swing.JButton jbtCancel;
    private javax.swing.JButton jbtConfirm;
    private javax.swing.JRadioButton jrbHex;
    private javax.swing.JRadioButton jrbText;
    private javax.swing.JTextField jtfHex;
    private javax.swing.JTextField jtfText;
    private javax.swing.JPanel pCommands;
    // End of variables declaration//GEN-END:variables

    int[] getUpdatedNameChars() {
        return updatedNameChars;
    }
}
