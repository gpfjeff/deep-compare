package com.gpfcomics.deepcompare.core;

public interface IHashProgressListener {

    /**
     * Inform the listener that a certain number of bytes have been hashed.
     * @param bytesRead A long listing the number of bytes processed.
     */
    void updateProgress(long bytesRead);

}
