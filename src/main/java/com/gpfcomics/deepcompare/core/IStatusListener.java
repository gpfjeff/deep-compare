/*
 * DEEP COMPARE: IStatusListener
 * AUTHOR: Jeffrey T. Darlington
 * URL: https://github.com/gpfjeff/deep-compare
 * Copyright 2023, Jeffrey T. Darlington.  All rights reserved.
 */
package com.gpfcomics.deepcompare.core;

/**
 * Classes that wish to be notified of the overall progress the Deep Compare's comparison process should implement this
 * interface and register themselves with the ComparisonEngine.
 */
public interface IStatusListener {

    /**
     * Inform the listener of the sum total number of files identified during the initial scan
     * @param fileCount A long representing the number of files found
     */
    void updateTotalFiles(long fileCount);

    /**
     * Inform the listener of the sum total number of bytes identified during the initial scan
     * @param totalBytes A long representing the number of bytes found
     */
    void updateTotalBytes(long totalBytes);

    /**
     * Inform the listener of the latest status message generated by the comparison engine.  This should be displayed
     * to the user through the UI.
     * @param message A String containing the status message to display
     */
    void updateStatusMessage(final String message);

    /**
     * Inform the listener of the latest error message generated by the comparison engine.  This should be displayed
     * to the user through the UI.
     * @param message A String containing the error message to display
     */
    void errorMessage(String message);

}
