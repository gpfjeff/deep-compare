/*
 * DEEP COMPARE: DCDirectory
 * AUTHOR: Jeffrey T. Darlington
 * URL: https://github.com/gpfjeff/deep-compare
 * Copyright 2023, Jeffrey T. Darlington.  All rights reserved.
 */
package com.gpfcomics.deepcompare.core;

import com.gpfcomics.deepcompare.Main;
import lombok.Getter;
import lombok.Setter;

import javax.swing.tree.DefaultMutableTreeNode;
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
public class DCDirectory {

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
    private final List<DCDirectory> subdirectories = new ArrayList<>();

    /**
     * The list of files contained within this directory.  This should never be null, but may be empty if there are no
     * files contained within the directory.  Sub-directories will be in the subdirectories list.
     */
    @Getter
    private final List<DCFile> files = new ArrayList<>();

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

    /**
     * Should our exclusion patterns be case-insensitive?  This is largely based on the operating system we are running
     * on.  Since this test only needs ot occur once, we'll do it as a private, static constant.
     */
    private static final boolean CASE_INSENSITIVE =
            // For now, only Windows and MacOS are case-insensitive; all others are case-sensitive:
            System.getProperty("os.name").startsWith("Windows") ||
            System.getProperty("os.name").startsWith("Mac");

    /* CONSTRUCTORS **************************************************************************************************/

    /**
     * Constructor
     * @param path A String containing the absolute path to the directory
     */
    public DCDirectory(String path) {
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
                            // on an operating system where files names are not case-sensitive, we'll treat the regex as
                            // case-insensitive; otherwise, we'll assume it's case-sensitive.
                            boolean addToList = true;
                            String simpleName = f.getFileName().toString();
                            for (String exclusion : options.getExclusions()) {
                                try {
                                    Pattern regex;
                                    if (CASE_INSENSITIVE)
                                        regex = Pattern.compile(exclusion, Pattern.CASE_INSENSITIVE);
                                    else
                                        regex = Pattern.compile(exclusion);
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
                                    DCDirectory dir = new DCDirectory(f.toAbsolutePath().toString());
                                    subdirectories.add(dir);
                                    dir.scan(options, log);
                                    // Update the total file size and count based on the subdirectory's own scan:
                                    size += dir.getSize();
                                    count += dir.getCount();
                                } else if (Files.isRegularFile(f)) {
                                    // If this is an actual file, create a File object and add it to the file list:
                                    DCFile file = new DCFile(f.toAbsolutePath().toString());
                                    files.add(file);
                                    // Tell the file to scan itself, then add its size to the total size and bump the
                                    // file count by one:
                                    file.scan();
                                    size += file.getSize();
                                    count++;
                                }
                            }
                        }
                    // Inner exception catch (at the current file/directory).  If logging is turned on, log the error.
                    // If debugging is turned on, include the full exception.
                    } catch (Exception ex) {
                        if (log != null) {
                            try {
                                log.write(
                                        String.format(
                                                Main.RESOURCES.getString("engine.log.scan.error"),
                                                f.toAbsolutePath()
                                        )
                                );
                                log.newLine();
                                if (options.isDebugMode()) {
                                    log.write(ex.toString());
                                    log.newLine();
                                }
                            } catch (Exception ignored) { }
                        }
                    }
                });
            }
        // Outer exception catch, if things go horribly wrong.  As above, log an error message if logging is enabled,
        // and include the exception if debugging is on.
        } catch (Exception ex) {
            if (log != null) {
                try {
                    log.write(
                            String.format(
                                    Main.RESOURCES.getString("engine.log.scan.error"),
                                    pathString
                            )
                    );
                    log.newLine();
                    if (options.isDebugMode()) {
                        log.write(ex.toString());
                        log.newLine();
                    }
                } catch (Exception ignored) { }
            }
        }
    }

    /**
     * Recursively generate the cryptographic hashes for all files under this directory
     * @param hasher A MessageDigest object that will be used to generate the hashes
     * @param listener An IHashProgressListener to report progress to
     * @param log An open BufferedWriter representing the log file
     */
    public void hash(MessageDigest hasher, IHashProgressListener listener, BufferedWriter log) {
        // Tell all the files and subdirectories to do their own hashes:
        for (DCFile file : files) {
            file.hash(hasher, listener, log);
        }
        for (DCDirectory dir : subdirectories) {
            dir.hash(hasher, listener, log);
        }
    }

    /**
     * Compare this directory with its companion directory in the opposite tree
     * @param companion The companion Directory
     */
    public void compare(DCDirectory companion) {
        // We'll be optimistic and assume for now that the two directories match.  If this proves false, we'll flip
        // this bit.
        match = true;
        // Loop through our own files:
        for (DCFile file : files) {
            // Try to find the companion file in the opposite tree with the same name as this file.  If we fail to find
            // the file, this will return null.
            DCFile companionFile = companion.getFiles().stream()
                    .filter(f -> f.getSimpleName().equals(file.getSimpleName()))
                    .findFirst().orElse(null);
            // If we find the companion file, mark our path as a match, then compare the two file hashes.  This will
            // set our file's hash match flag.  If the hashes do not match, that means our directories don't match
            // either.
            if (companionFile != null) {
                file.setPathMatch(true);
                file.compare(companionFile);
                if (!file.isHashMatch()) match = false;
            } else {
                // If we couldn't find the companion file, the path doesn't match.  That also means the directories
                // don't match.
                file.setPathMatch(false);
                match = false;
            }
        }
        // Now loop through our subdirectories:
        for (DCDirectory dir : subdirectories) {
            // Just as above, look for the subdirectory in the companion folder that matches this directory's name:
            DCDirectory companionDir = companion.getSubdirectories().stream()
                    .filter(d -> d.getSimpleName().equals(dir.getSimpleName()))
                    .findFirst().orElse(null);
            // If we found the same subfolder, run it through the same comparison process we did here, then compare its
            // flag.  If they don't match, we don't match.
            if (companionDir != null) {
                dir.compare(companionDir);
                if (!dir.isMatch()) match = false;
            } else {
                // We didn't find the subfolder, so we don't match:
                dir.setMatch(false);
                match = false;
            }
        }
    }

    /**
     * Sort this directory's files into the appropriate findings list based on the comparison results
     * @param missingFiles A List of Files containing all files present in this directory but missing from the other
     * @param changedFiles A List of Files containing all files that are present in both paths but have different
     *                     contents in the compared folders.  May be null if this list is not required.
     * @param matchingFiles A List of Files containing all files that match in the comparison.  May be null if this list
     *                     is not required.
     */
    public void compileResults(List<DCFile> missingFiles, List<DCFile> changedFiles, List<DCFile> matchingFiles) {
        // Ask our files to compare themselves first, then ask all subdirectories to do the same:
        for (DCFile file : files) {
            file.compileResults(missingFiles, changedFiles, matchingFiles);
        }
        for (DCDirectory dir : subdirectories) {
            dir.compileResults(missingFiles, changedFiles, matchingFiles);
        }
    }

    /**
     * Sort this directory's files into the appropriate findings list based on the comparison results
     * @param missingFiles A List of Files containing all files present in this directory but missing from the other
     * @param changedFiles A List of Files containing all files that are present in both paths but have different
     *                     contents in the compared folders
     */
    public void compileResults(List<DCFile> missingFiles, List<DCFile> changedFiles) {
        // A convenience wrapper for the above method that sets the matching file list to null:
        compileResults(missingFiles, changedFiles, null);
    }

    /**
     * Sort this directory's files into the appropriate findings list based on the comparison results
     * @param missingFiles A List of Files containing all files present in this directory but missing from the other
     */
    public void compileResults(List<DCFile> missingFiles) {
        // A convenience wrapper for the above method that sets the changed and matching file lists to null:
        compileResults(missingFiles, null, null);
    }

    /**
     * Recursively build the GUI result tree.  The input tree nodes passed in represent "our" (i.e., the current level
     * in the directory tree) nodes, to which we will add files and directories as needed.
     * @param missingNode A DefaultMutableTreeNode representing our missing files node
     * @param changedNode A DefaultMutableTreeNode representing our changed files node
     * @param matchingNode A DefaultMutableTreeNode representing our matching files node
     */
    public void buildTree(
            DefaultMutableTreeNode missingNode,
            DefaultMutableTreeNode changedNode,
            DefaultMutableTreeNode matchingNode
    ) {
        // Loop through the files that are direct children to this directory and ask them to sort themselves.  Note that
        // if we have no direct file children, nothing happens here.
        for (DCFile file : files) {
            file.buildTree(missingNode, changedNode, matchingNode);
        }
        // Now loop through our subdirectories.  Build child nodes for each type (missing, changed, and matching), then
        // recursively as each directory to sort themselves.
        for (DCDirectory dir : subdirectories) {
            DefaultMutableTreeNode myMissingNode = new DefaultMutableTreeNode(dir.getSimpleName(), true);
            DefaultMutableTreeNode myChangedNode = new DefaultMutableTreeNode(dir.getSimpleName(), true);
            DefaultMutableTreeNode myMatchingNode = new DefaultMutableTreeNode(dir.getSimpleName(), true);
            dir.buildTree(myMissingNode, myChangedNode, myMatchingNode);
            // If our child nodes have their own children, add them to our tree.  Otherwise, discard the node.  This
            // way, only relevant nodes will be created.
            if (myMissingNode.getChildCount() > 0) missingNode.add(myMissingNode);
            if (myChangedNode.getChildCount() > 0) changedNode.add(myChangedNode);
            if (myMatchingNode.getChildCount() > 0) matchingNode.add(myMatchingNode);
        }
    }

}
