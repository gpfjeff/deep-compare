package com.gpfcomics.deepcompare.gui;

import com.gpfcomics.deepcompare.Main;
import com.gpfcomics.deepcompare.core.ComparisonResult;
import com.gpfcomics.deepcompare.core.DCDirectory;
import com.gpfcomics.deepcompare.core.Utilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ResourceBundle;

/**
 * This dialog shows the comparison results if any discrepancies are found.
 */
public class ResultDialog extends JDialog {

    // GUI Builder controls:
    private JPanel contentPane;
    private JButton buttonOK;
    private JLabel lblResultStatus;
    private JLabel lblSource;
    private JLabel lblTarget;
    private JTree treeSource;
    private DefaultMutableTreeNode sourceTop;
    private JTree treeTarget;
    private JLabel lblSourceFiles;
    private JLabel lblTargetFiles;
    private DefaultMutableTreeNode targetTop;

    /**
     * Constructor
     *
     * @param owner  The Frame that owns this dialog
     * @param result The ComparisonResult object that contains the data to display
     */
    public ResultDialog(Frame owner, ComparisonResult result) {

        super(owner, Main.RESOURCES.getString("result.dialog.title"), true);

        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        // For now, this dialog only gets shown if the comparison found discrepancies.  (If both folders match, a simple
        // message dialog is displayed.)  As such, the status label should note that discrepancies were found.
        lblResultStatus.setText(Main.RESOURCES.getString("result.discrepancies.found"));

        // Populate the labels that show the number of files and total bytes for each directory:
        lblSourceFiles.setText(
                String.format(
                        Main.RESOURCES.getString("result.files.and.bytes"),
                        NumberFormat.getIntegerInstance().format(result.getSourceDirectory().getCount()),
                        Utilities.prettyPrintFileSize(result.getSourceDirectory().getSize())
                )
        );
        lblTargetFiles.setText(
                String.format(
                        Main.RESOURCES.getString("result.files.and.bytes"),
                        NumberFormat.getIntegerInstance().format(result.getTargetDirectory().getCount()),
                        Utilities.prettyPrintFileSize(result.getTargetDirectory().getSize())
                )
        );

        // Now build the two result trees, starting with the source tree.  The work is pretty much the same, so we'll
        // farm it out to a helper method.
        // TODO: Do we need to put the tree building portion in a separate thread to keep things responsive?  This
        //       could happen if there are a huge number of file to work through.
        buildResultTree(
                true,
                result.getSourceDirectory(),
                treeSource,
                sourceTop,
                !result.getSourceMissingFiles().isEmpty(),
                !result.getChangedFiles().isEmpty(),
                !result.getMatchingFiles().isEmpty()
        );

        // Now build the target tree:
        buildResultTree(
                false,
                result.getTargetDirectory(),
                treeTarget,
                targetTop,
                !result.getTargetMissingFiles().isEmpty(),
                !result.getChangedFiles().isEmpty(),
                !result.getMatchingFiles().isEmpty()
        );

    }

    private void onOK() {
        // The OK button simply closes this dialog:
        dispose();
    }

    /**
     * Build the result tree
     *
     * @param isSource         A boolean indicating if the current tree is the source (TRUE) or the target (FALSE) folder
     * @param directory        The DCDirectory tree behind this tree
     * @param tree             The JTree parent
     * @param topNode          A DefaultMutableTreeNode that is the parent of all nodes in the tree
     * @param hasMissingFiles  A boolean indicating whether any missing files were found
     * @param hasChangedFiles  A boolean indicating whether any changed files were found
     * @param hasMatchingFiles A boolean indicating whether any matching files were found
     */
    private void buildResultTree(
            boolean isSource,
            DCDirectory directory,
            JTree tree,
            DefaultMutableTreeNode topNode,
            boolean hasMissingFiles,
            boolean hasChangedFiles,
            boolean hasMatchingFiles
    ) {

        // Rename the top node with the path string from the directory:
        topNode.setUserObject(directory.getPathString());

        // Build sub-nodes for missing, changed, and matching files, then add those to the root.  Note that missing file
        // node prints a slightly different message depending on whether this is the source or target tree.
        DefaultMutableTreeNode missingNode = new DefaultMutableTreeNode(
                isSource ?
                        Main.RESOURCES.getString("result.source.missing.files") :
                        Main.RESOURCES.getString("result.target.missing.files"),
                true
        );
        topNode.add(missingNode);
        DefaultMutableTreeNode changedNode = new DefaultMutableTreeNode(
                Main.RESOURCES.getString("result.changed.files"),
                true
        );
        topNode.add(changedNode);
        DefaultMutableTreeNode matchingNode = new DefaultMutableTreeNode(
                Main.RESOURCES.getString("result.matching.files"),
                true
        );
        topNode.add(matchingNode);

        // Pass the buck to the directory object to have it recursively build each node:
        directory.buildTree(missingNode, changedNode, matchingNode);

        // If there are missing files, expand the first level of the missing file node.  If there were not missing
        // files, simply remove and discard the missing file node.
        TreePath missingPath = new TreePath(missingNode.getPath());
        if (hasMissingFiles) tree.expandPath(missingPath);
        else topNode.remove(missingNode);

        // Do the same with the changed file node:
        TreePath changedPath = new TreePath(changedNode.getPath());
        if (hasChangedFiles) tree.expandPath(changedPath);
        else topNode.remove(changedNode);

        // The matching file node is slightly different.  We want to default this node to being collapsed, so we
        // highlight the discrepancies.  Only expand this node if there are no missing or changed files.  If there are
        // no matching files at all, remove the node entirely.
        TreePath matchingPath = new TreePath(matchingNode.getPath());
        if (hasMatchingFiles && !hasMissingFiles && !hasChangedFiles)
            tree.expandPath(matchingPath);
        else if (!hasMatchingFiles) topNode.remove(matchingNode);

    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(0, 0));
        contentPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel1, BorderLayout.CENTER);
        lblResultStatus = new JLabel();
        lblResultStatus.setText("Result status goes here...");
        panel1.add(lblResultStatus, BorderLayout.NORTH);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel1.add(panel2, BorderLayout.CENTER);
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel2.add(panel3, BorderLayout.WEST);
        lblSource = new JLabel();
        this.$$$loadLabelText$$$(lblSource, this.$$$getMessageFromBundle$$$("MessagesBundle", "source.label"));
        panel3.add(lblSource, BorderLayout.NORTH);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, BorderLayout.CENTER);
        scrollPane1.setViewportView(treeSource);
        lblSourceFiles = new JLabel();
        lblSourceFiles.setText("Files and Bytes Here");
        panel3.add(lblSourceFiles, BorderLayout.SOUTH);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        panel2.add(panel4, BorderLayout.EAST);
        lblTarget = new JLabel();
        this.$$$loadLabelText$$$(lblTarget, this.$$$getMessageFromBundle$$$("MessagesBundle", "target.label"));
        panel4.add(lblTarget, BorderLayout.NORTH);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel4.add(scrollPane2, BorderLayout.CENTER);
        scrollPane2.setViewportView(treeTarget);
        lblTargetFiles = new JLabel();
        lblTargetFiles.setHorizontalAlignment(4);
        lblTargetFiles.setHorizontalTextPosition(4);
        lblTargetFiles.setText("Files and Bytes Here");
        panel4.add(lblTargetFiles, BorderLayout.SOUTH);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel5, BorderLayout.SOUTH);
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel5.add(panel6, BorderLayout.CENTER);
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel6.add(buttonOK);
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
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    private void createUIComponents() {
        // Override the IntelliJ code to create a dedicated root node for each tree, since the root needs to be defined
        // when the tree is declared.  For now, set the label to the tree type (source vs. target); this will get
        // overwritten once we have our result data.
        sourceTop = new DefaultMutableTreeNode(Main.RESOURCES.getString("source.label"));
        treeSource = new JTree(sourceTop);
        targetTop = new DefaultMutableTreeNode(Main.RESOURCES.getString("target.label"));
        treeTarget = new JTree(targetTop);
    }

}
