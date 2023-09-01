package com.gpfcomics.deepcompare.gui;

import com.gpfcomics.deepcompare.Main;
import com.gpfcomics.deepcompare.core.ComparisonOptions;
import com.gpfcomics.deepcompare.core.ComparisonResult;
import com.gpfcomics.deepcompare.core.IHashProgressListener;
import com.gpfcomics.deepcompare.core.IStatusListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

public class ProgressDialog extends JDialog implements IStatusListener, IHashProgressListener, PropertyChangeListener {

    // GUI Builder controls:
    private JPanel contentPane;
    private JButton btnCancel;
    private JLabel lblStatus;
    private JProgressBar progressBar;

    private long totalBytes = 0L;

    private long processedBytes = 0L;

    private long currentFileBytes = 0L;

    private long totalFiles = 0L;

    //private ExecutorService executor;

    private final ComparisonWorker worker;

    public ProgressDialog(
            String sourcePath,
            String targetPath,
            ComparisonOptions options
    ) {

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(btnCancel);

        lblStatus.setText(Main.RESOURCES.getString("engine.status.startup"));

        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);

        btnCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(
                e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        worker = new ComparisonWorker(
                this,
                sourcePath,
                targetPath,
                options,
                this,
                this
        );

        worker.addPropertyChangeListener(this);

        worker.execute();

        /*try {

            executor = Executors.newSingleThreadExecutor();

            Future<ComparisonResult> future = executor.submit(
                    new ComparisonEngine(
                            sourcePath,
                            targetPath,
                            options,
                            this,
                            this
                    )
            );

            ComparisonResult result = future.get();

        } catch (InterruptedException ie) {

        } catch (ExecutionException ee) {

        } catch (Exception ex) {

        }*/

    }

    private void onCancel() {
        int result = JOptionPane.showConfirmDialog(
                this,
                Main.RESOURCES.getString("progress.cancel.prompt"),
                Main.RESOURCES.getString("progress.cancel.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (result == JOptionPane.YES_OPTION) {
            worker.cancel(true);
            dispose();
        }
    }

    // IStatusListener methods:
    @Override
    public void updateTotalFiles(long fileCount) {
        totalFiles = fileCount;
    }

    @Override
    public void updateTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    @Override
    public void updateStatusMessage(String message) {
        lblStatus.setText(message);
    }

    @Override
    public void errorMessage(String message) {
        lblStatus.setText(message);
    }

    // IHashProgressListener methods:
    @Override
    public void newFile() {
        processedBytes += currentFileBytes;
        currentFileBytes = 0L;
    }

    @Override
    public void updateProgress(long bytesRead) {
        currentFileBytes += bytesRead;
        int percent = (int) Math.floor((((double) processedBytes + (double) currentFileBytes) / (double) totalBytes) * 100);
        if (percent != progressBar.getValue())
            progressBar.setValue(percent);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (SwingWorker.StateValue.DONE == evt.getNewValue()) {
            ComparisonResult result = worker.getResult();
            if (result != null) {
                // TODO: Build result dialog
                JOptionPane.showMessageDialog(
                        this,
                        "Comparison was a success! Eventually, we'll show the result dialog here...",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Results were null. We may not need this message, but popping it up just in case...",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            dispose();
        }
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
        panel1.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel1, BorderLayout.CENTER);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        lblStatus = new JLabel();
        lblStatus.setText("Status info goes here...");
        panel1.add(lblStatus, BorderLayout.NORTH);
        progressBar = new JProgressBar();
        panel1.add(progressBar, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel2, BorderLayout.SOUTH);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel2.add(panel3, BorderLayout.CENTER);
        btnCancel = new JButton();
        this.$$$loadButtonText$$$(btnCancel, this.$$$getMessageFromBundle$$$("MessagesBundle", "cancel.button"));
        panel3.add(btnCancel);
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
