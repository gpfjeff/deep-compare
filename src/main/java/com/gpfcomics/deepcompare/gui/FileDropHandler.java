package com.gpfcomics.deepcompare.gui;

import com.gpfcomics.deepcompare.Main;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A TransferHandler subclass for allowing directories to be dropped on text boxes.  This should not be added to
 * any Components besides JTextFields.
 */
public class FileDropHandler extends TransferHandler {

    // Based loosely on: https://stackoverflow.com/a/39415436

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {

        // Ignore non-drop requests:
        if (!support.isDrop()) return false;

        // Ignore anything that isn't a file drop:
        for (DataFlavor flavor : support.getDataFlavors()) {
            if (flavor.isFlavorJavaFileListType()) {
                return true;
            }
        }
        return false;

    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(TransferHandler.TransferSupport support) {

        // This has probably already been done, but double-check anyway:
        if (!this.canImport(support))
            return false;

        // Look for any dropped files.  We're only interested in a single dropped directory, but we have to start
        // somewhere...
        List<File> files;
        try {
            files = (List<File>) support.getTransferable()
                    .getTransferData(DataFlavor.javaFileListFlavor);
        } catch (UnsupportedFlavorException | IOException ex) {
            // This should never happen; could be a JDK bug:
            return false;
        }

        // Get the text field we're dropping on.  We will only ever use this on the "Browse" text fields, but I'm
        // not sure how to really enforce this.
        JTextField field = (JTextField)support.getComponent();

        try {
            // Loop through the files.  If we do get more than one, we'll only consider the first one.
            for (File file : files) {
                // Only allow directories to be dropped:
                if (file.isDirectory()) {
                    // Copy the absolute to the file into the text field and return success:
                    field.setText(file.getAbsolutePath());
                    return true;
                } else {
                    // If the user tries to drop a file, complain:
                    JOptionPane.showMessageDialog(
                            field.getParent(),
                            Main.RESOURCES.getString("error.drop.file"),
                            Main.RESOURCES.getString("dialog.title.error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        } catch (Exception ex) {
            // If any file operation fails, show an error:
            JOptionPane.showMessageDialog(
                    field.getParent(),
                    Main.RESOURCES.getString("error.drop.generic"),
                    Main.RESOURCES.getString("dialog.title.error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
        return false;
    }

}
