package com.gpfcomics.deepcompare.core;

import javax.swing.filechooser.FileSystemView;

/**
 * A catch-all Swiss Army knife of generic static methods that might be of use throughout the application.  Essentially,
 * if might be used anywhere but doesn't fit into any particular class, these methods will go here.
 */
public class Utilities {

    private Utilities() { }

    /**
     * Given a long containing the supposed size of a file (or cumulative collection of files), return a formatted
     * string summarizing the size in a recognizable format
     * @param bytes A long containing the raw cumulative file size
     * @return A String containing the formatted output
     */
    public static String prettyPrintFileSize(long bytes) {
        // We won't bother going any higher than terabytes (for now):
        if (bytes > 1099511627776L) {
            return bytes / 1099511627776L + " TiB";
        }
        // Gigabytes:
        if (bytes > 1073741824L) {
            return bytes / 1073741824L + " GiB";
        }
        // Megabytes:
        if (bytes > 1048576L) {
            return bytes / 1048576L + " MiB";
        }
        // Kilobytes:
        if (bytes > 1024L) {
            return bytes / 1024L + " kiB";
        }
        // Plain ol' bytes:
        return bytes + " B";
    }

    /**
     * Get the default browse starting path.  This may return different values depending on the operating system.
     * @return A String containing the default path to use for all file/directory Browse buttons
     */
    public static String defaultBrowsePath() {
        // For Windows, start the user in their Documents directory.  The "easiest" way to get this to work is to use
        // the Swing file chooser FileSystemView to get what it thinks is the user's default directory.  (The system
        // property "user.home" puts us at the root of the user's personal folders, but most Windows users will probably
        // expect to start in their Documents directory.)
        if (System.getProperty("os.name").startsWith("Windows")) {
            return FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
        }
        // For everyone else, start them in the Java user home directory.  This should work for MacOS and anything
        // UN*X-y.
        else return System.getProperty("user.home");
    }

}
