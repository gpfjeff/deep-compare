package com.gpfcomics.deepcompare.core;

import java.io.File;

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
        // For Windows, start the user in their Documents directory:
        // TODO: Should we check for older versions of Windows to account for older structures, or is the juice not
        //       worth the squeeze?
        if (System.getProperty("os.name").startsWith("Windows")) {
            return System.getProperty("user.home") + File.separator + "Documents";
        }
        // For everyone else (for now), start them in the Java user home directory:
        // TODO: This should work file for UN*X-y like OSes.  What about MacOS?  Should we customize for it?
        else return System.getProperty("user.home");
    }

}
