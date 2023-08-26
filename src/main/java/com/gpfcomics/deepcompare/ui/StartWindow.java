package com.gpfcomics.deepcompare.ui;

import com.gpfcomics.deepcompare.core.ComparisonOptions;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The main start window for the GUI app.  This exposes the main parameters and gives the user a chance to set
 * additional options.
 */
public class StartWindow {

    // GUI elements, mostly added automagically by the GUI builder:
    @Getter
    private JPanel rootPanel;
    private JButton btnStart;
    private JButton btnAbout;
    private JButton btnClose;
    private JButton btnOptions;
    private JButton btnTargetBrowse;
    private JTextField txtTarget;
    private JButton btnSourceBrowse;
    private JTextField txtSource;

    // Our comparison options.  This instantiation sets the defaults, which the user can override via various GUI
    // controls.
    private final ComparisonOptions options = new ComparisonOptions();

    /**
     * The main start-up window, which prompts the user for their initial inputs
     *
     * @param parent The JFrame parent our root panel will be attached to
     */
    public StartWindow(JFrame parent) {

        // The source and target Browse buttons will allow the user to select these folders graphically:
        btnSourceBrowse.addActionListener(e -> {
            browseForPath("Source", txtSource);
        });

        btnTargetBrowse.addActionListener(e -> {
            browseForPath("Target", txtTarget);
        });

        // The Comparison Options button launches the options dialog, which lets the user fine tune the comparison:
        btnOptions.addActionListener(e -> {
            OptionsDialog dialog = new OptionsDialog(parent, options);
            dialog.pack();
            dialog.setVisible(true);
        });

        // The About button launches the About Dialog.  (Funny, that.)
        btnAbout.addActionListener(e -> {
            AboutDialog dialog = new AboutDialog(parent);
            dialog.pack();
            dialog.setVisible(true);
        });

        // The Close button (wait for it...) closes the window:
        btnClose.addActionListener(e -> {
            System.exit(0);
        });

        // Where the rubber meets the road.  The Start button validates our inputs and launches the comparison engine
        // to perform the actual comparison.
        btnStart.addActionListener(e -> {
            // Asbestos underpants:
            try {
                // Get the source and target paths from the text boxes and validate that they are populated and valid
                // directory paths.  If not, abort:
                String sourceString = txtSource.getText().trim();
                String targetString = txtTarget.getText().trim();
                Path sourcePath = Paths.get(sourceString);
                Path targetPath = Paths.get(targetString);
                if (
                        sourceString.isEmpty() ||
                                !Files.exists(sourcePath) ||
                                !Files.isDirectory(sourcePath)
                ) {
                    JOptionPane.showMessageDialog(null, "Source path is not a valid directory!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (
                        targetString.isEmpty() ||
                                !Files.exists(targetPath) ||
                                !Files.isDirectory(targetPath)
                ) {
                    JOptionPane.showMessageDialog(null, "Target path is not a valid directory!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // If the source and target paths are equal or one is contained inside the other, abort:
                if (sourceString.equals(targetString) ||
                        sourcePath.startsWith(targetString) ||
                        targetPath.startsWith(sourceString)) {
                    JOptionPane.showMessageDialog(null, "Portions of the source and target " +
                            "paths refer to the same path!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // The Options dialog will take care of most of the option validation for us, but there is one thing
                // we need to check.  We don't want to let the user set the log folder into either the source or target
                // directories, as that will throw off the comparison.
                if (options.getLogFilePath() != null &&
                        (Paths.get(options.getLogFilePath()).startsWith(sourceString) ||
                                Paths.get(options.getLogFilePath()).startsWith(targetString))) {
                    JOptionPane.showMessageDialog(null, "The log file cannot be written to " +
                            "either the source or target paths!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // If the exclusions are not currently using regular expressions, convert them now.  Note that this is
                // is a one-way conversion, so from this point on we won't let the user edit it.
                if (!options.isExclusionsRegex()) { options.convertSimpleWildcardsToRegex(); }
                // At this point, we should be good to go.  Pass our parameters to the core comparison engine and
                // put it to work:
                // TODO: Obviously, we haven't gotten that far yet...
                JOptionPane.showMessageDialog(null,
                        "Everything looks good! Here's where the fun begins...");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error validating inputs!", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

    }

    /**
     * Allow the user to select the source or target folder via the system file selection dialog
     * @param label A String containing the type of folder we're searching for (source or target)
     * @param targetField The JTextField containing the currently selected path, if any.  The user's selection will be
     *                    added to this box.
     */
    private void browseForPath(String label, JTextField targetField) {
        // Asbestos underpants:
        try {
            // Get the current value of the text field and try to find the actual path on the file system.  If the value
            // is valid, we'll use that as the starting point for the search.  Otherwise, if the path is empty or
            // doesn't point to a valid folder, reset search to the user's home directory.
            String pathString = targetField.getText().trim();
            Path startPath = Paths.get(pathString);
            try {
                if (
                        pathString.isEmpty() ||
                        !Files.exists(startPath) ||
                        !Files.isDirectory(startPath)
                ) {
                    targetField.setText("");
                    pathString = System.getProperty("user.home");
                }
            } catch (Exception ex) {
                targetField.setText("");
                pathString = System.getProperty("user.home");
            }
            // Open the file choose and configure it.  We'll start at the selected path above and restrict the user to
            // only choosing directories:
            JFileChooser chooser = new JFileChooser(pathString);
            chooser.setDialogTitle("Select " + label + " Folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            // If the user selects a valid path (which we'll assume the file chooser will validate), set the test field
            // to the absolute path of the selected directory:
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                targetField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        } catch (Exception ex) {
            // TODO: Catch explicit exceptions to make more useful error messages
            JOptionPane.showMessageDialog(
                    null,
                    "Error trying to find the " + label.toLowerCase() + " path!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Yes, I'm using IntelliJ's GUI Designer.  So sue me.  I remember designing Java GUIs in code way back in the Dark
    // Ages when Java was first introduced.  I'm not going through that pain and punishment again.

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
        rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout(0, 0));
        rootPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        rootPanel.add(panel1, BorderLayout.SOUTH);
        btnStart = new JButton();
        btnStart.setText("Start");
        panel1.add(btnStart);
        btnAbout = new JButton();
        btnAbout.setText("About...");
        panel1.add(btnAbout);
        btnClose = new JButton();
        btnClose.setText("Close");
        panel1.add(btnClose);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        rootPanel.add(panel2, BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel2.add(panel3, BorderLayout.SOUTH);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel3.add(panel4, BorderLayout.CENTER);
        btnOptions = new JButton();
        btnOptions.setText("Comparison Options...");
        panel4.add(btnOptions);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        panel2.add(panel5, BorderLayout.CENTER);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new BorderLayout(0, 0));
        panel5.add(panel6, BorderLayout.SOUTH);
        btnTargetBrowse = new JButton();
        btnTargetBrowse.setLabel("Browse...");
        btnTargetBrowse.setText("Browse...");
        panel6.add(btnTargetBrowse, BorderLayout.EAST);
        txtTarget = new JTextField();
        panel6.add(txtTarget, BorderLayout.CENTER);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new BorderLayout(0, 0));
        panel5.add(panel7, BorderLayout.CENTER);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new BorderLayout(0, 0));
        panel7.add(panel8, BorderLayout.SOUTH);
        final JLabel label1 = new JLabel();
        label1.setText("Target Folder:");
        panel8.add(label1, BorderLayout.WEST);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new BorderLayout(0, 0));
        panel7.add(panel9, BorderLayout.CENTER);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new BorderLayout(0, 0));
        panel9.add(panel10, BorderLayout.SOUTH);
        btnSourceBrowse = new JButton();
        btnSourceBrowse.setLabel("Browse...");
        btnSourceBrowse.setText("Browse...");
        panel10.add(btnSourceBrowse, BorderLayout.EAST);
        txtSource = new JTextField();
        panel10.add(txtSource, BorderLayout.CENTER);
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new BorderLayout(0, 0));
        panel9.add(panel11, BorderLayout.CENTER);
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new BorderLayout(0, 0));
        panel11.add(panel12, BorderLayout.SOUTH);
        final JLabel label2 = new JLabel();
        label2.setText("Source Folder:");
        panel11.add(label2, BorderLayout.WEST);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
