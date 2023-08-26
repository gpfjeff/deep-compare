package com.gpfcomics.deepcompare.ui;

import com.gpfcomics.deepcompare.Main;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;

public class AboutDialog extends JDialog {
    private JPanel contentPane;
    private JButton btnOK;
    private JLabel lblVersion;
    private JLabel lblHyperlink;
    private JLabel lblCopyright;

    public AboutDialog(Frame owner) {

        super(owner, "About", true);

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(btnOK);

        lblVersion.setText("Deep Compare v" + Main.VERSION);
        lblCopyright.setText("Copyright " + Main.COPYRIGHT_YEAR + ", Jeffrey T. Darlington.");

        lblHyperlink.setText(Main.WEBSITE_URL);
        lblHyperlink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblHyperlink.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(Main.WEBSITE_URL));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Error launching Web browser!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        btnOK.addActionListener(e -> onOK());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onOK();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e ->
                        onOK(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
    }

    private void onOK() {
        dispose();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(0, 0));
        contentPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        contentPane.add(panel1, BorderLayout.SOUTH);
        btnOK = new JButton();
        btnOK.setText("OK");
        panel1.add(btnOK);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel2, BorderLayout.CENTER);
        lblHyperlink = new JLabel();
        lblHyperlink.setHorizontalAlignment(0);
        lblHyperlink.setText("URL goes here...");
        panel2.add(lblHyperlink, BorderLayout.SOUTH);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel2.add(panel3, BorderLayout.CENTER);
        lblCopyright = new JLabel();
        lblCopyright.setHorizontalAlignment(0);
        lblCopyright.setText("Copyright goes here...");
        panel3.add(lblCopyright, BorderLayout.SOUTH);
        lblVersion = new JLabel();
        lblVersion.setHorizontalAlignment(0);
        lblVersion.setText("Version info goes here...");
        panel3.add(lblVersion, BorderLayout.NORTH);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
