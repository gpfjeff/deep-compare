package com.gpfcomics.deepcompare.gui;

import com.gpfcomics.deepcompare.Main;
import com.gpfcomics.deepcompare.core.ComparisonOptions;
import com.gpfcomics.deepcompare.core.Utilities;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

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
            browseForPath(Main.RESOURCES.getString("source.label"), txtSource);
        });

        btnTargetBrowse.addActionListener(e -> {
            browseForPath(Main.RESOURCES.getString("target.label"), txtTarget);
        });

        // Add drag-and-drop support to the source and target text fields:
        txtSource.setTransferHandler(new FileDropHandler());
        txtTarget.setTransferHandler(new FileDropHandler());

        // The Comparison Options button launches the options dialog, which lets the user fine tune the comparison:
        btnOptions.addActionListener(e -> {
            OptionsDialog dialog = new OptionsDialog(parent, options);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        });

        // The About button launches the About Dialog.  (Funny, that.)
        btnAbout.addActionListener(e -> {
            AboutDialog dialog = new AboutDialog(parent);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
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
                    JOptionPane.showMessageDialog(
                            btnStart.getParent(),
                            Main.RESOURCES.getString("start.error.invalid.source.path"),
                            Main.RESOURCES.getString("dialog.title.error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                if (
                        targetString.isEmpty() ||
                                !Files.exists(targetPath) ||
                                !Files.isDirectory(targetPath)
                ) {
                    JOptionPane.showMessageDialog(
                            btnStart.getParent(),
                            Main.RESOURCES.getString("start.error.invalid.target.path"),
                            Main.RESOURCES.getString("dialog.title.error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                // If the source and target paths are equal or one is contained inside the other, abort:
                if (sourceString.equals(targetString) ||
                        sourcePath.startsWith(targetString) ||
                        targetPath.startsWith(sourceString)) {
                    JOptionPane.showMessageDialog(
                            btnStart.getParent(),
                            Main.RESOURCES.getString("start.error.source.target.same.path"),
                            Main.RESOURCES.getString("dialog.title.error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                // The Options dialog will take care of most of the option validation for us, but there is one thing
                // we need to check.  We don't want to let the user set the log folder into either the source or target
                // directories, as that will throw off the comparison.
                if (options.getLogFilePath() != null &&
                        (Paths.get(options.getLogFilePath()).startsWith(sourceString) ||
                                Paths.get(options.getLogFilePath()).startsWith(targetString))) {
                    JOptionPane.showMessageDialog(
                            btnStart.getParent(),
                            Main.RESOURCES.getString("start.error.log.file.in.path"),
                            Main.RESOURCES.getString("dialog.title.error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                // If the exclusions are not currently using regular expressions, convert them now.  Note that this is
                // is a one-way conversion, so from this point on we won't let the user edit it.
                if (!options.isExclusionsRegex()) {
                    options.convertSimpleWildcardsToRegex();
                }
                // At this point, we should be good to go.  Pass our parameters to the core comparison engine and
                // put it to work:
                ProgressDialog dialog = new ProgressDialog(parent, sourceString, targetString, options);
                dialog.pack();
                dialog.setLocationRelativeTo(parent);
                dialog.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        btnStart.getParent(),
                        Main.RESOURCES.getString("start.error.generic"),
                        Main.RESOURCES.getString("dialog.title.error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

    }

    /**
     * Allow the user to select the source or target folder via the system file selection dialog
     *
     * @param label       A String containing the type of folder we're searching for (source or target)
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
                    pathString = Utilities.defaultBrowsePath();
                }
            } catch (Exception ex) {
                targetField.setText("");
                pathString = Utilities.defaultBrowsePath();
            }
            // Open the file choose and configure it.  We'll start at the selected path above and restrict the user to
            // only choosing directories:
            JFileChooser chooser = new JFileChooser(pathString);
            chooser.setDialogTitle(
                    String.format(
                            Main.RESOURCES.getString("start.browse.file.chooser.title"),
                            label
                    )
            );
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            // If the user selects a valid path (which we'll assume the file chooser will validate), set the test field
            // to the absolute path of the selected directory:
            if (chooser.showOpenDialog(targetField.getParent()) == JFileChooser.APPROVE_OPTION) {
                targetField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        } catch (Exception ex) {
            // TODO: Catch explicit exceptions to make more useful error messages
            JOptionPane.showMessageDialog(
                    targetField.getParent(),
                    String.format(
                            Main.RESOURCES.getString("start.browse.generic.error"),
                            label
                    ),
                    Main.RESOURCES.getString("dialog.title.error"),
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
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        btnStart = new JButton();
        this.$$$loadButtonText$$$(btnStart, this.$$$getMessageFromBundle$$$("MessagesBundle", "start.start.button"));
        panel1.add(btnStart);
        btnAbout = new JButton();
        this.$$$loadButtonText$$$(btnAbout, this.$$$getMessageFromBundle$$$("MessagesBundle", "start.about.button"));
        panel1.add(btnAbout);
        btnClose = new JButton();
        this.$$$loadButtonText$$$(btnClose, this.$$$getMessageFromBundle$$$("MessagesBundle", "start.close.button"));
        panel1.add(btnClose);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        rootPanel.add(panel2, BorderLayout.CENTER);
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel2.add(panel3, BorderLayout.SOUTH);
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel3.add(panel4, BorderLayout.CENTER);
        btnOptions = new JButton();
        this.$$$loadButtonText$$$(btnOptions, this.$$$getMessageFromBundle$$$("MessagesBundle", "start.comparison.options.button"));
        panel4.add(btnOptions);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        panel2.add(panel5, BorderLayout.CENTER);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new BorderLayout(0, 0));
        panel5.add(panel6, BorderLayout.SOUTH);
        panel6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        btnTargetBrowse = new JButton();
        btnTargetBrowse.setLabel(this.$$$getMessageFromBundle$$$("MessagesBundle", "browse.button"));
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
        panel8.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("MessagesBundle", "start.target.folder"));
        panel8.add(label1, BorderLayout.WEST);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new BorderLayout(0, 0));
        panel7.add(panel9, BorderLayout.CENTER);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new BorderLayout(0, 0));
        panel9.add(panel10, BorderLayout.SOUTH);
        panel10.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        btnSourceBrowse = new JButton();
        btnSourceBrowse.setLabel(this.$$$getMessageFromBundle$$$("MessagesBundle", "browse.button"));
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
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("MessagesBundle", "start.source.folder"));
        panel12.add(label2, BorderLayout.SOUTH);
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
        return rootPanel;
    }

}
