package com.gpfcomics.deepcompare.core;

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
