package com.gpfcomics.deepcompare.ui;

import com.gpfcomics.deepcompare.Main;
import com.gpfcomics.deepcompare.core.ComparisonOptions;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * The Comparison Options dialog allows the user to fine-tune the folder comparison by excluding files based on
 * patterns, to ignore hidden files, to change the hashing algorithm, and to determine where to store the log file, if
 * any.  These options are sent back to the Start Window once the user clicks the OK button, or abandoned if they click
 * the Cancel button.
 */
public class OptionsDialog extends JDialog {

    // GUI elements, mostly added automagically by the GUI builder:
    private JPanel contentPane;
    private JButton btnOk;
    private JButton btnCancel;
    private JCheckBox checkDebug;
    private JButton btnLogBrowse;
    private JTextField txtLogPath;
    private JCheckBox checkHidden;
    private JComboBox<String> comboHash;
    private JComboBox<String> comboRegex;
    private JButton btnAdd;
    private JButton btnRemove;
    private JList<String> listExclusions;
    private JButton btnMoveUp;
    private JButton btnMoveDown;

    // Our local copy of the shared comparison options object, set in the constructor:
    private final ComparisonOptions options;

    // Our exclusion list model, which will be used to represent the exclusions in the JList:
    private final DefaultListModel<String> exclusionListModel = new DefaultListModel<>();

    public OptionsDialog(Frame owner, ComparisonOptions options) {

        super(owner, Main.RESOURCES.getString("options.dialog.title"), true);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(btnOk);

        // Copy over the options object to our local variable:
        this.options = options;

        btnOk.addActionListener(e -> onOK());

        btnCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e ->
                        onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        // Set up the exclusions UI.  Start by populating the exclusion model with the exclusion list from the options
        // object, then associate the model with the list UI:
        for (String exclusion : options.getExclusions()) {
            exclusionListModel.addElement(exclusion);
        }
        listExclusions.setModel(exclusionListModel);
        // Only allow a single item to be selected at a time:
        listExclusions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Default the Remove, Up, and Down buttons to disabled, since no items have been selected yet:
        btnRemove.setEnabled(false);
        btnMoveUp.setEnabled(false);
        btnMoveDown.setEnabled(false);

        // Set up the selection listener on the list box.  Essentially, this enables or disables the Remove, Up, and
        // Down buttons based on where the selection is in the list.  The Remove button should always be enabled if an
        // item is selected.  The Up button should be enabled for all items except the first one in the list, while the
        // Down button should be enabled for all items except the last one.  If nothing is selected, all three buttons
        // should be disabled.  The Add button should always be enabled.
        listExclusions.addListSelectionListener(e -> {
            btnRemove.setEnabled(listExclusions.getSelectedIndex() != -1);
            btnMoveUp.setEnabled(listExclusions.getSelectedIndex() > 0);
            btnMoveDown.setEnabled(
                    listExclusions.getSelectedIndex() != -1 &&
                            listExclusions.getSelectedIndex() != exclusionListModel.getSize() - 1
            );
        });

        // Make the Add button useful:
        btnAdd.addActionListener(e -> {
            // Prompt the user for a new exclusion pattern.  Note that we're not going to do any validation here to see
            // if the pattern is valid.  The comparison engine will simply ignore the pattern if it doesn't parse.
            String newExclusion = JOptionPane.showInputDialog(
                    Main.RESOURCES.getString("options.exclusions.dialog.prompt")
            );
            // If the string is non-empty:
            if (newExclusion != null && !newExclusion.trim().isEmpty()) {
                // Get the current selection index.  If something is already selected, we'll insert the new item at
                // that location:
                int insertIndex = listExclusions.getSelectedIndex();
                if (insertIndex > -1) {
                    exclusionListModel.add(insertIndex, newExclusion);
                    listExclusions.setSelectedIndex(insertIndex);
                    btnRemove.setEnabled(true);
                    btnMoveUp.setEnabled(insertIndex > 0);
                    btnMoveDown.setEnabled(insertIndex != exclusionListModel.getSize() - 1);
                } else {
                    // If nothing is selected, put the new item at the bottom of the list:
                    exclusionListModel.addElement(newExclusion);
                }
            }
        });

        // Make the Remove button useful:
        btnRemove.addActionListener(e -> {
            // If something is selected, remove that item from the list.  Deselect any selected item and disable the
            // Remove, Up, and Down buttons:
            if (listExclusions.getSelectedIndex() != -1) {
                exclusionListModel.remove(listExclusions.getSelectedIndex());
                listExclusions.setSelectedIndex(-1);
                btnRemove.setEnabled(false);
                btnMoveUp.setEnabled(false);
                btnMoveDown.setEnabled(false);
            }
        });

        // Make the Move Up button useful:
        btnMoveUp.addActionListener(e -> {
            // If something is selected, move that item one notch up the list.  Enable or disable the Up button based
            // on where the item now is.
            if (listExclusions.getSelectedIndex() > 0) {
                int index = listExclusions.getSelectedIndex();
                String valueToMove = exclusionListModel.remove(index);
                exclusionListModel.add(--index, valueToMove);
                listExclusions.setSelectedIndex(index);
                btnMoveUp.setEnabled(index > 0);
            }
        });

        // Make the Move Down button useful:
        btnMoveDown.addActionListener(e -> {
            // If something is selected, move that item one notch down the list.  Enable or disable the Down button
            // based on where the item now is.
            if (listExclusions.getSelectedIndex() != -1 &&
                    listExclusions.getSelectedIndex() != exclusionListModel.getSize() - 1) {
                int index = listExclusions.getSelectedIndex();
                String valueToMove = exclusionListModel.remove(index);
                exclusionListModel.add(++index, valueToMove);
                listExclusions.setSelectedIndex(index);
                btnMoveDown.setEnabled(
                        listExclusions.getSelectedIndex() != exclusionListModel.getSize() - 1
                );
            }
        });

        // Build the regex drop-down.  Rather than a checkbox or set of radio buttons, we're opting for a drop-down
        // that toggles between the two options.  (I think it's easier to understand this way and it takes up less
        // space.)  Put the two options into the list, then select the appropriate item based on the value in the
        // options.
        comboRegex.setEditable(false);
        comboRegex.addItem(Main.RESOURCES.getString("options.exclusions.option.dos"));
        comboRegex.addItem(Main.RESOURCES.getString("options.exclusions.option.regex"));
        comboRegex.setSelectedIndex(this.options.isExclusionsRegex() ? 1 : 0);

        // Build the hash algorithm drop-down.  Grab the list of available hashes from the options class and add them
        // to the combo box, then select the one that's currently set in the options.
        comboHash.setEditable(false);
        for (String hash : ComparisonOptions.HASHES) {
            comboHash.addItem(hash);
        }
        comboHash.setSelectedItem(options.getHash());

        // Set the hidden files checkbox value from the options:
        checkHidden.setSelected(this.options.isCheckHiddenFiles());

        // Set the log path text box based on the options.  If the value is null, set it to an empty string:
        txtLogPath.setText(
                this.options.getLogFilePath() == null ? "" : this.options.getLogFilePath()
        );

        // Make the Browse button useful:
        btnLogBrowse.addActionListener(e -> {
            try {
                // Get the current value of the log path text box.  If it's non-empty and a valid directory, set that
                // as the start path for the file chooser dialog.  If this isn't a valid directory for any reason,
                // default to the user's home directory.
                String currentPath = txtLogPath.getText().trim();
                try {
                    Path currentPathPath = Paths.get(currentPath);
                    if (
                            currentPath.isEmpty() ||
                                    !Files.exists(currentPathPath) ||
                                    !Files.isDirectory(currentPathPath)
                    ) {
                        txtLogPath.setText("");
                        currentPath = System.getProperty("user.home");
                    }
                } catch (Exception ex) {
                    txtLogPath.setText("");
                    currentPath = System.getProperty("user.home");
                }
                // Open the file chooser to the starting path and only let the user select directories:
                JFileChooser chooser = new JFileChooser(currentPath);
                chooser.setDialogTitle(Main.RESOURCES.getString("options.log.directory.prompt"));
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                // If the user selects a directory, update the log path text box:
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    txtLogPath.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        null,
                        Main.RESOURCES.getString("options.error.log.path.generic"),
                        Main.RESOURCES.getString("dialog.title.error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // Set the debug checkbox:
        checkDebug.setSelected(this.options.isDebugMode());

        // The OK and Cancel buttons will be delegated to dedicated functions:
        btnOk.addActionListener(e -> onOK());
        btnCancel.addActionListener(e -> onCancel());

        // The system close button (usually the "X" in the upper right of the dialog) should do the same thing as
        // the Cancel button:
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // Similarly, hitting the Escape key also mimics Cancel:
        contentPane.registerKeyboardAction(
                e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
    }

    private void onOK() {
        // Validate the log path.  If the user used the Browse button, this should be redundant, but technically they
        // can type in any path directly.  If this is not a valid directory, show an error and abort.
        String logPath = txtLogPath.getText().trim();
        if (!logPath.isEmpty()) {
            try {
                Path logPathPath = Paths.get(logPath);
                if (!Files.exists(logPathPath) || !Files.isDirectory(logPathPath)) {
                    JOptionPane.showMessageDialog(
                            null,
                            Main.RESOURCES.getString("options.error.log.path.invalid"),
                            Main.RESOURCES.getString("dialog.title.error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        null,
                        Main.RESOURCES.getString("options.error.log.path.invalid"),
                        Main.RESOURCES.getString("dialog.title.error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }
        // If any exclusions have been provided and the user has specified that they are regular expressions, go ahead
        // and validate that the regexes actually compile.  If any of them do not, throw an error and abort:
        // TODO: Can we do any validation for simple DOS/UNIX file wildcards?
        if (listExclusions.getModel().getSize() > 0 && comboRegex.getSelectedIndex() == 1) {
            for (int i = 0; i < listExclusions.getModel().getSize(); i++) {
                try {
                    Pattern.compile(exclusionListModel.getElementAt(i));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            null,
                            String.format(
                                    Main.RESOURCES.getString("options.error.invalid.regex"),
                                    exclusionListModel.getElementAt(i)
                            ),
                            Main.RESOURCES.getString("dialog.title.error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
            }
        }
        // If we reached this point, we're good to go.  Start updating the options object with the values from the UI.
        // We'll start with the exclusions.  Clear out the current list, then loop through the values in the list box
        // and add them in order.
        options.getExclusions().clear();
        for (int i = 0; i < listExclusions.getModel().getSize(); i++) {
            options.getExclusions().add(exclusionListModel.getElementAt(i));
        }
        // Copy over the other values.  Most of these are straight forward.  Since this object is shared between this
        // dialog and the parent, updating this copy also updates the parent's copy.
        options.setExclusionsRegex(comboRegex.getSelectedIndex() == 1);
        options.setHash((String) comboHash.getSelectedItem());
        options.setCheckHiddenFiles(checkHidden.isSelected());
        options.setLogFilePath(logPath.isEmpty() ? null : logPath);
        options.setDebugMode(checkDebug.isSelected());
        // Close the dialog:
        dispose();
    }

    private void onCancel() {
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
        panel1.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel1, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel1.add(panel2, BorderLayout.SOUTH);
        checkDebug = new JCheckBox();
        this.$$$loadButtonText$$$(checkDebug, this.$$$getMessageFromBundle$$$("MessagesBundle", "options.debug.checkbox"));
        panel2.add(checkDebug, BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel1.add(panel3, BorderLayout.CENTER);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        panel3.add(panel4, BorderLayout.SOUTH);
        btnLogBrowse = new JButton();
        this.$$$loadButtonText$$$(btnLogBrowse, this.$$$getMessageFromBundle$$$("MessagesBundle", "browse.button"));
        panel4.add(btnLogBrowse, BorderLayout.EAST);
        txtLogPath = new JTextField();
        panel4.add(txtLogPath, BorderLayout.CENTER);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        panel3.add(panel5, BorderLayout.CENTER);
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("MessagesBundle", "options.log.file.label"));
        panel5.add(label1, BorderLayout.SOUTH);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new BorderLayout(0, 0));
        panel5.add(panel6, BorderLayout.CENTER);
        checkHidden = new JCheckBox();
        this.$$$loadButtonText$$$(checkHidden, this.$$$getMessageFromBundle$$$("MessagesBundle", "options.hidden.files.checkbox"));
        panel6.add(checkHidden, BorderLayout.SOUTH);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new BorderLayout(0, 0));
        panel6.add(panel7, BorderLayout.CENTER);
        comboHash = new JComboBox();
        panel7.add(comboHash, BorderLayout.SOUTH);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new BorderLayout(0, 0));
        panel7.add(panel8, BorderLayout.CENTER);
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("MessagesBundle", "options.hash.label"));
        panel8.add(label2, BorderLayout.SOUTH);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new BorderLayout(0, 0));
        panel8.add(panel9, BorderLayout.CENTER);
        comboRegex = new JComboBox();
        panel9.add(comboRegex, BorderLayout.SOUTH);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new BorderLayout(0, 0));
        panel9.add(panel10, BorderLayout.CENTER);
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("MessagesBundle", "options.exclusions.label"));
        panel10.add(label3, BorderLayout.NORTH);
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new BorderLayout(0, 0));
        panel10.add(panel11, BorderLayout.EAST);
        btnMoveDown = new JButton();
        this.$$$loadButtonText$$$(btnMoveDown, this.$$$getMessageFromBundle$$$("MessagesBundle", "options.down.button"));
        panel11.add(btnMoveDown, BorderLayout.SOUTH);
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new BorderLayout(0, 0));
        panel11.add(panel12, BorderLayout.NORTH);
        btnMoveUp = new JButton();
        this.$$$loadButtonText$$$(btnMoveUp, this.$$$getMessageFromBundle$$$("MessagesBundle", "options.up.button"));
        panel12.add(btnMoveUp, BorderLayout.SOUTH);
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new BorderLayout(0, 0));
        panel12.add(panel13, BorderLayout.CENTER);
        btnRemove = new JButton();
        this.$$$loadButtonText$$$(btnRemove, this.$$$getMessageFromBundle$$$("MessagesBundle", "options.remove.button"));
        panel13.add(btnRemove, BorderLayout.SOUTH);
        btnAdd = new JButton();
        this.$$$loadButtonText$$$(btnAdd, this.$$$getMessageFromBundle$$$("MessagesBundle", "options.add.button"));
        panel13.add(btnAdd, BorderLayout.NORTH);
        listExclusions = new JList();
        panel10.add(listExclusions, BorderLayout.CENTER);
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        contentPane.add(panel14, BorderLayout.SOUTH);
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel14.add(panel15);
        btnOk = new JButton();
        btnOk.setText("OK");
        panel15.add(btnOk);
        btnCancel = new JButton();
        this.$$$loadButtonText$$$(btnCancel, this.$$$getMessageFromBundle$$$("MessagesBundle", "cancel.button"));
        panel15.add(btnCancel);
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
