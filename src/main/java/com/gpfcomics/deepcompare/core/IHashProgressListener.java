/*
 * DEEP COMPARE: IHashProgressListener
 * AUTHOR: Jeffrey T. Darlington
 * URL: https://github.com/gpfjeff/deep-compare
 * Copyright 2023, Jeffrey T. Darlington.  All rights reserved.
 */
package com.gpfcomics.deepcompare.core;

/**
 * Classes that wish to be notified of the progress the Deep Compare's file hashing step should implement this interface
 * and register themselves with the ComparisonEngine.
 */
public interface IHashProgressListener {

    /**
     * Inform the listener that we starting to hash a new file.  The listener should make whatever changes necessary
     * on its end to adjust any running counts.
     */
    void newFile();

    /**
     * Inform the listener that a certain number of bytes have been hashed from the current file.
     * @param bytesRead A long listing the number of bytes processed
     */
    void updateProgress(long bytesRead);

}
