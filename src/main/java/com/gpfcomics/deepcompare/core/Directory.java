package com.gpfcomics.deepcompare.core;

import lombok.Getter;
import lombok.Setter;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This class represents a directory or folder in the comparison tree.  It maintains the internal structure of our
 * results, associating files and sub-folders together.  Both the top-level source and target folders are Directory
 * objects, as are any sub-folders underneath them.
 */
public class Directory {

    /* MEMBER VARIABLES **********************************************************************************************/

    /**
     * The absolute path to this directory on the file system
     */
    @Getter
    private final String pathString;

    /**
     * Whether this directory matches its companion folder in the other path (source or target).  Defaults to true.
     * Will be set to true if and only if all files under this folder matches its counterpart.
     */
    @Getter
    @Setter
    private boolean match = false;

    /**
     * The list of sub-directories under this folder.  This should never be null, but may be empty if there are no
     * sub-directories under the current directory.
     */
    @Getter
    private final List<Directory> subdirectories = new ArrayList<>();

    /**
     * The list of files contained within this directory.  This should never be null, but may be empty if there are no
     * files contained within the directory.  Sub-directories will be in the subdirectories list.
     */
    @Getter
    private final List<File> files = new ArrayList<>();

    /**
     * The total size of all files and sub-directories under this directory.
     */
    @Getter
    private long size = 0;

    /**
     * The total count of all files under this directory.
     */
    @Getter
    private long count = 0;

    /* CONSTRUCTORS **************************************************************************************************/

    public Directory(String path) {
        this.pathString = path;
    }

    /* PUBLIC FUNCTIONS **********************************************************************************************/

    /**
     * Get the simple base name of this directory for display
     * @return A String containing the base directory name
     */
    public String getSimpleName() {
        return Paths.get(pathString).getFileName().toString();
    }

    /**
     * Recursively scan this directory for sub-directories and files, building the directory tree
     * @param options A ComparisonOptions object with our comparison options
     * @param log A BufferedWriter for our log file.  May be null if no log is to be written.
     */
    public void scan(ComparisonOptions options, BufferedWriter log) {
        // Make sure the subdirectory and file list are empty:
        subdirectories.clear();
        files.clear();
        // Asbestos underpants:
        try {
            // Get a sorted list of files and subdirectories under this path, the loop through them:
            try (Stream<Path> entries = Files.list(Paths.get(pathString)).sorted()) {
                entries.forEach(f -> {
                    try {
                        // If the file isn't hidden or we're supposed to check for hidden files, proceed:
                        if (!Files.isHidden(f) || options.isCheckHiddenFiles()) {
                            // Check to see if the file is in the exclusion list.  We'll start by assuming it's not,
                            // then check each exclusion in the list and see it's a match.  If it matches, flag the
                            // file so we'll skip it.  Note that if the pattern is invalid and won't compile, we'll
                            // silently skip the pattern and assume it's not a match.  Also note that if we're running
                            // on some flavor of Windows, we'll treat the regex as case-insensitive; otherwise, we'll
                            // assume it's case-sensitive.
                            boolean addToList = true;
                            String simpleName = getSimpleName();
                            boolean caseInsensitive = System.getProperty("os.name").startsWith("Windows");
                            for (String exclusion : options.getExclusions()) {
                                try {
                                    Pattern regex;
                                    if (caseInsensitive) regex = Pattern.compile(exclusion, Pattern.CASE_INSENSITIVE);
                                    else regex = Pattern.compile(exclusion);
                                    if (regex.matcher(simpleName).matches()) {
                                        addToList = false;
                                        break;
                                    }
                                } catch (Exception ignored) { }
                            }
                            // If we got through the exclusion check unscathed:
                            if (addToList) {
                                // If the "file" is a directory:
                                if (Files.isDirectory(f)) {
                                    // Create a Directory object and add it to the subdirectory list.  Then scan it,
                                    // which should recursively build the file list.
                                    Directory dir = new Directory(f.toAbsolutePath().toString());
                                    subdirectories.add(dir);
                                    dir.scan(options, log);
                                    // Update the total file size and count based on the subdirectory's own scan:
                                    size += dir.getSize();
                                    count += dir.getCount();
                                } else if (Files.isRegularFile(f)) {
                                    // If this is an actual file, create a File object and add it to the file list:
                                    File file = new File(f.toAbsolutePath().toString());
                                    // Tell the file to scan itself, then add its size to the total size and bump the
                                    // file count by one:
                                    file.scan();
                                    size += file.getSize();
                                    count++;
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // TODO: What to do if an exception is thrown?
                    }
                });
            }
        } catch (Exception ex) {
            // TODO: What to do if an exception is thrown?
        }
    }

    /**
     * Recursively generate the cryptographic hashes for all files under this directory
     * @param hasher A MessageDigest object that will be used to generate the hashes
     * @param listener An IHashProgressListener to report progress to
     */
    public void hash(MessageDigest hasher, IHashProgressListener listener) {
        // Tell all the files and subdirectories to do their own hashes:
        for (File file : files) {
            file.hash(hasher, listener);
        }
        for (Directory dir : subdirectories) {
            dir.hash(hasher, listener);
        }
    }

    public void compare(Directory companion) {
        match = true;
        for (File file : files) {
            File companionFile = companion.getFiles().stream()
                    .filter(f -> f.getSimpleName().equals(file.getSimpleName()))
                    .findFirst().orElse(null);
            if (companionFile != null) {
                file.setPathMatch(true);
                file.compare(companionFile);
            } else {
                file.setPathMatch(false);
                match = false;
            }
        }
        for (Directory dir : subdirectories) {
            Directory companionDir = companion.getSubdirectories().stream()
                    .filter(d -> d.getSimpleName().equals(dir.getSimpleName()))
                    .findFirst().orElse(null);
            if (companionDir != null) {
                dir.compare(companionDir);
            } else {
                dir.setMatch(false);
                match = false;
            }
        }
    }

}
