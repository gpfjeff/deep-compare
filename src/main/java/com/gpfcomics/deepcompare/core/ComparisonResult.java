package com.gpfcomics.deepcompare.core;

import lombok.Getter;
import lombok.Setter;

/**
 * This class services as a container for our comparison results
 */
@Getter
@Setter
public class ComparisonResult {

    /**
     * The mapped source directory
     */
    private Directory sourceDirectory;

    /**
     * The mapped target directory
     */
    private Directory targetDirectory;

    /**
     * The total number of files discovered during the comparison
     */
    private long totalFiles;

    /**
     * The total number of bytes processed during the comparison
     */
    private long totalBytes;

    /**
     * The input comparison options
     */
    private ComparisonOptions options;

}
