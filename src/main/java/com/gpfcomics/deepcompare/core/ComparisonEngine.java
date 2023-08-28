package com.gpfcomics.deepcompare.core;

import lombok.Getter;

import java.security.MessageDigest;

public class ComparisonEngine implements Runnable {

    private final String sourcePath;

    private final String targetPath;

    private final ComparisonOptions options;

    @Getter
    private Directory sourceDirectory;

    @Getter
    private Directory targetDirectory;

    @Getter
    private long totalFiles;

    @Getter
    private long totalBytes;

    private final IHashProgressListener hashListener;

    private final IStatusListener statusListener;

    public ComparisonEngine(String sourcePath, String targetPath, ComparisonOptions options,
                            IHashProgressListener hashListener, IStatusListener statusListener) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.options = options;
        this.hashListener = hashListener;
        this.statusListener = statusListener;
    }

    @Override
    public void run() {
        try {
            statusListener.updateStatusMessage("Starting up...");
            sourceDirectory = new Directory(sourcePath);
            targetDirectory = new Directory(targetPath);
            statusListener.updateStatusMessage("Building source directory map...");
            sourceDirectory.scan(options);
            statusListener.updateStatusMessage("Building target directory map...");
            targetDirectory.scan(options);
            totalFiles = sourceDirectory.getCount() + targetDirectory.getCount();
            totalBytes = sourceDirectory.getSize() + targetDirectory.getSize();
            statusListener.updateTotalFiles(totalFiles);
            statusListener.updateTotalBytes(totalBytes);
            statusListener.updateStatusMessage("Generating source hashes...");
            MessageDigest hash = MessageDigest.getInstance(options.getHash());
            sourceDirectory.hash(hash, hashListener);
            statusListener.updateStatusMessage("Generating target hashes...");
            sourceDirectory.hash(hash, hashListener);
            statusListener.updateStatusMessage("Generating final report...");
        } catch (Exception ex) {
            statusListener.errorMessage("Error generated while processing");
        }

    }

}
