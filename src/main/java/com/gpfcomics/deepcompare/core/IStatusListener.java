package com.gpfcomics.deepcompare.core;

public interface IStatusListener {

    void updateTotalFiles(long fileCount);

    void updateTotalBytes(long totalBytes);

    void updateStatusMessage(final String message);

    void errorMessage(String message);

}
