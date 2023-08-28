package com.gpfcomics.deepcompare.core;

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

}
