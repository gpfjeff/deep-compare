package com.gpfcomics.deepcompare.gui;

import com.gpfcomics.deepcompare.Main;
import com.gpfcomics.deepcompare.core.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ResourceBundle;

/**
 * This progress dialog displays in GUI mode as the ComparisonEngine does the actual work.  It displays various status
 * messages, as well as a progress bar during the lengthiest and most complex part of the work:  hashing the files.  A
 * cancel button allows the user to cancel the long-running process if they choose to abort.
 */
public class ProgressDialog extends JDialog implements IStatusListener, IHashProgressListener, PropertyChangeListener {

    // GUI Builder controls:
    private JPanel contentPane;
    private JButton btnCancel;
    private JLabel lblStatus;
    private JProgressBar progressBar;
    private JLabel lblFilesAndBytes;

    private final Frame owner;

    // The total number of bytes for all files that need to be hashed
    private long totalBytes = 0L;

    // The cumulative number of bytes hashed up to the last fully-hashed file
    private long processedBytes = 0L;

    // The cumulative number of bytes hashed on the current file being hashed
    private long currentFileBytes = 0L;

    // The total number of files to be hashed
    private long totalFiles = 0L;

    // The workhorse for the GUI.  This subclass of SwingWorker runs the ComparisonEngine in a separate thread to keep
    // the GUI responsive.
    private final ComparisonWorker worker;

    /**
     * Constructor
     *
     * @param sourcePath A String containing the absolute path to the source directory
     * @param targetPath A String containing the absolute path to the target directory
     * @param options    A ComparisonOptions object
     */
    public ProgressDialog(
            Frame owner,
            String sourcePath,
            String targetPath,
            ComparisonOptions options
    ) {

        super(owner, Main.RESOURCES.getString("progress.dialog.title"), true);
        this.owner = owner;

        // Set up our content pane and dialog.
        setContentPane(contentPane);
        setModal(true);

        // There will be only one button on this dialog:  the Cancel button.  So make it the default:
        getRootPane().setDefaultButton(btnCancel);

        // Set the status label to our "starting up" message:
        lblStatus.setText(Main.RESOURCES.getString("engine.status.startup"));

        // Set our progress bar minimum and maximum values, and initialize it to zero.  (These are probably the defaults
        // anyway, but I'd rather be explicit.)  Note that the progress bar will show a percentage of the number of
        // bytes that have been hashed so far.
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);

        // Wire up the cancel button:
        btnCancel.addActionListener(e -> onCancel());

        // If the user clicks the close button in the upper right corner, treat it like clicking Cancel:
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // Do the same with the Escape button:
        contentPane.registerKeyboardAction(
                e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        // Set up the comparison worker, feeding it our inputs.  We'll also pass in ourselves as the hash and status
        // listeners so we'll know what the comparison engine is up to.
        worker = new ComparisonWorker(
                this,
                sourcePath,
                targetPath,
                options,
                this,
                this
        );

        // As a SwingWorker, the comparison working also looks for a property change listener.  We'll play that role
        // as well.
        worker.addPropertyChangeListener(this);

        // Tell the worker to get to work:
        worker.execute();

    }

    // If the user clicks the Cancel button, hits Escape, or attempts to close the dialog, prompt the user to see if
    // they really want to cancel the job.  If they do, tell the worker to stop and close the dialog.
    private void onCancel() {
        int result = JOptionPane.showConfirmDialog(
                btnCancel.getParent(),
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

    // IStatusListener methods.  These receive status from the comparison engine, which we'll want to pass along to the
    // user.  This mostly involves taking note of the total files and bytes, as well as displaying status messages.

    @Override
    public void updateTotalFiles(long fileCount) {
        totalFiles = fileCount;
        updateFilesAndBytesLabel();
    }

    @Override
    public void updateTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
        updateFilesAndBytesLabel();
    }

    @Override
    public void updateStatusMessage(String message) {
        lblStatus.setText(message);
    }

    @Override
    public void errorMessage(String message) {
        lblStatus.setText(message);
    }

    // IHashProgressListener methods.  These inform us of the status of the hashing job.  This mostly drives the
    // progress bar.

    @Override
    public void newFile() {
        // Whenever the engine starts a new file, add the previous file's byte count to the running total of processed
        // bytes, then reset the current file to zero:
        processedBytes += currentFileBytes;
        currentFileBytes = 0L;
    }

    @Override
    public void updateProgress(long bytesRead) {
        // Add the number of bytes read to the current file's running total:
        currentFileBytes += bytesRead;
        // Calculate the percentage of the total number of bytes hashed.  For this, we'll add the current file's bytes
        // to the previous files' total, then divide by the total number of bytes.  There's a lot of type conversion
        // here, but the goal is to get to a round integer between 0 and 100.
        int percent = (int) Math.floor(
                (((double) processedBytes + (double) currentFileBytes) / (double) totalBytes) * 100.0d
        );
        // Update the progress bar.  To avoid flicker, we'll only update the display if the percentage has changed from
        // the previous value.
        if (percent != progressBar.getValue()) {
            progressBar.setValue(percent);
            progressBar.setToolTipText(percent + "%");
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // If the engine finishes its job, close this dialog and show either the result dialog or an error, depending
        // on whether we get any useful results:
        if (SwingWorker.StateValue.DONE == evt.getNewValue()) {
            ComparisonResult result = worker.getResult();
            if (result != null) {
                // If both directories match, there's no point showing the full result dialog.  Just show a quick
                // message dialog with a success message.  (This is a compromise between some of the logic.  If both
                // sides match, there's not a lot of point giving the user a complex GUI of identical file trees to
                // scroll through.)
                if (result.getSourceDirectory().isMatch() && result.getTargetDirectory().isMatch()) {
                    JOptionPane.showMessageDialog(
                            btnCancel.getParent(),
                            Main.RESOURCES.getString("result.all.match"),
                            Main.RESOURCES.getString("result.dialog.title"),
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } else {
                    // Looks like we've got to show our work.  Launch the result dialog to show what's different:
                    ResultDialog dialog = new ResultDialog(owner, result);
                    dialog.pack();
                    dialog.setLocationRelativeTo(owner);
                    dialog.setVisible(true);
                }
            // TODO: Commenting this out for now.  This else block was being hit if the process was cancelled, which
            //       for some reason returns DONE.  Naturally, the result is null in this case, which was showing this
            //       error dialog.  Need to differentiate between done-done and cancelled-done.
            /*} else {
                // This *SHOULDN'T* happen, but if it does, show an error box if the result is null:
                JOptionPane.showMessageDialog(
                        btnCancel.getParent(),
                        Main.RESOURCES.getString("progress.error.message"),
                        Main.RESOURCES.getString("dialog.title.error"),
                        JOptionPane.ERROR_MESSAGE
                );*/
            }
            dispose();
        }
    }

    /**
     * Update the display to show the number of files and bytes once both values are available
     */
    private void updateFilesAndBytesLabel() {
        if (totalFiles > 0L && totalBytes > 0L) {
            lblFilesAndBytes.setText(
                    String.format(
                            Main.RESOURCES.getString("progress.files.label.display"),
                            NumberFormat.getIntegerInstance().format(totalFiles),
                            Utilities.prettyPrintFileSize(totalBytes)
                    )
            );
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
        lblFilesAndBytes = new JLabel();
        this.$$$loadLabelText$$$(lblFilesAndBytes, this.$$$getMessageFromBundle$$$("MessagesBundle", "progress.files.label.wait"));
        panel1.add(lblFilesAndBytes, BorderLayout.SOUTH);
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
    private void $$$loadLabelText$$$(JLabel component, String text) {
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
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
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
