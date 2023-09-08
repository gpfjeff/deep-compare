package com.gpfcomics.deepcompare.core;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * This class services as a container for our comparison results
 */
@Getter
public class ComparisonResult {

    /**
     * The mapped source directory
     */
    @Setter
    private DCDirectory sourceDirectory;

    /**
     * The mapped target directory
     */
    @Setter
    private DCDirectory targetDirectory;

    /**
     * The total number of files discovered during the comparison
     */
    @Setter
    private long totalFiles;

    /**
     * The total number of bytes processed during the comparison
     */
    @Setter
    private long totalBytes;

    /**
     * The input comparison options
     */
    @Setter
    private ComparisonOptions options;

    // For the file discrepancy lists, we'll initialize this as empty here, then remove the setter, forcing the caller
    // to use the lists as-is.

    /**
     * A list of all files that match in both directories
     */
    private final List<DCFile> matchingFiles = new ArrayList<>();

    /**
     * A list of all files in the source directory that are missing from the target directory
     */
    private final List<DCFile> sourceMissingFiles = new ArrayList<>();

    /**
     * A list of all files in the target directory that are missing from the source directory
     */
    private final List<DCFile> targetMissingFiles = new ArrayList<>();

    /**
     * A list of all files that are in both directories but whose contents are different
     */
    private final List<DCFile> changedFiles = new ArrayList<>();

}
