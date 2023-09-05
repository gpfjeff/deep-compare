package com.gpfcomics.deepcompare.core;

import com.gpfcomics.deepcompare.Main;
import lombok.Getter;
import lombok.Setter;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

/**
 * This class represents an individual file in the comparison tree.  It maintains the file's own state (name, full
 * path, file size, etc.), as well as its cryptographic hash and whether it matches the path and hash of its companion
 * file (if it exists) in the other directory tree.
 */
public class DCFile {

    /* MEMBER VARIABLES **********************************************************************************************/

    /**
     * A string containing the full path to the file
     */
    @Getter
    private final String pathString;

    /**
     * The Base64-encoded cryptographic hash of the file
     */
    @Getter
    private String hash;

    /**
     * The size of the file in bytes
     */
    @Getter
    private long size;

    /**
     * Whether this file's path matches its companion file's path in the other tree
     */
    @Getter
    @Setter
    private boolean pathMatch = false;

    /**
     * Where this file's cryptographic hash matches its companion file's hash in the other tree
     */
    @Getter
    private boolean hashMatch = false;

    /* CONSTRUCTORS **************************************************************************************************/

    /**
     * Constructor
     * @param path A String containing the absolute path to the file
     */
    public DCFile(String path) {
        this.pathString = path;
    }

    /* PUBLIC FUNCTIONS **********************************************************************************************/

    /**
     * Get the simple base name of this file for display
     * @return A String containing the base file name
     */
    public String getSimpleName() {
        return Paths.get(pathString).getFileName().toString();
    }

    /**
     * Get the path name of the file relative to the specified root
     * @param root A String containing the root path to remove from the absolute path
     * @return A String containing the path to the file relative to the root
     */
    public String relativePath(String root) {
        // Take the substring of the absolute path, starting with the root.  Note that we'll add one to get the path
        // separator as well.  If the absolute path of file does *NOT* start with the specified root, that's a bug in
        // the calling code.  Instead of blowing up, just return the raw absolute path.
        if (pathString.startsWith(root))
            return pathString.substring( root.length() + 1 );
        else return pathString;
    }

    /**
     * Scan this individual file, taking note of its current file size
     * @throws IOException Thrown if anything blows up while scanning the file
     */
    public void scan() throws IOException {
        // This seems pretty simple, but for now all we'll do is get our file size and keep track of it.  The parent
        // directory will reference this to get the total size of the directory.
        size = Files.size(Paths.get(pathString));
    }

    /**
     * Generate the cryptographic hash of this file
     * @param hasher A MessageDigest object, which will perform the hash
     * @param listener The IHashProgressListener to report progress to
     * @param log An open BufferedWriter representing the log file
     */
    public void hash(MessageDigest hasher, IHashProgressListener listener, BufferedWriter log) {
        // Open the file and read in the raw bytes, feeding them to the hash algorithm.  As we update the hash, we'll
        // also send the number of bytes read to the listener to update our progress.  Once the file read is complete,
        // compute the final digest and Base64 encode it, storing the string in our local variable.
        try ( InputStream is = Files.newInputStream(Paths.get(pathString)) ) {
            hasher.reset();
            byte[] byteArray = new byte[4096]; // TODO: Tweak for performance
            int bytesCount = 0;
            while ((bytesCount = is.read(byteArray)) != -1) {
                hasher.update(byteArray, 0, bytesCount);
                listener.updateProgress(bytesCount);
            };
            hash = Base64.getEncoder().encodeToString(hasher.digest());
        } catch (Exception ex) {
            // If anything above blows up, log an error (if we're keeping a log) and set our hash to null:
            if (log != null) {
                try {
                    log.write(
                            String.format(
                                    Main.RESOURCES.getString("engine.log.hash.error"),
                                    pathString
                            )
                    );
                    log.newLine();
                    log.write(ex.toString());
                    log.newLine();
                } catch (Exception ignored) { }
            }
            hash = null;
        }
    }

    /**
     * Compare this file's cryptographic hash to its companion file in the opposite tree.  It is assumed that this
     * method will only be called after we confirm that both files exist within their respective trees.
     * @param companion The companion File
     */
    public void compare(DCFile companion) {
        // This should (hopefully) never happen, but if either our hash or the companion object are null, declare the
        // hash not a match.  Otherwise, compare the two hashes and return the result.  (If the companion's hash is
        // null, the comparison should still return false.  The null check is mostly to prevent NPEs.)
        if (hash == null || companion == null) hashMatch = false;
        else hashMatch = hash.equals(companion.getHash());
    }

    /**
     * Sort this file into the appropriate findings list based on the comparison results
     * @param missingFiles A List of Files containing all files present in this directory but missing from the other
     * @param changedFiles A List of Files containing all files that are present in both paths but have different
     *                     contents in the compared folders.  May be null if this list is not required.
     * @param matchingFiles A List of Files containing all files that match in the comparison.  May be null if this list
     *                     is not required.
     */
    public void compileResults(List<DCFile> missingFiles, List<DCFile> changedFiles, List<DCFile> matchingFiles) {
        // Easy peasy.  Sort the file into the appropriate list, based on our findings.  Check the path flag first, then
        // the hash flag.  If both flags are true, the file matches and goes into the matching list.  Note that the
        // changed and missing lists may be null if we're not collecting that info, so check for nulls there first.
        if (!pathMatch) {
            missingFiles.add(this);
        } else if (!hashMatch) {
            if (changedFiles != null) changedFiles.add(this);
        } else {
            if (matchingFiles != null) matchingFiles.add(this);
        }
    }

    /**
     * Sort this file into its appropriate category tree for the GUI.  The tree nodes passed in represent our parent
     * directory's node.  This will attach our file node to the relevant parent.
     * @param missingNode A DefaultMutableTreeNode representing our missing files node
     * @param changedNode A DefaultMutableTreeNode representing our changed files node
     * @param matchingNode A DefaultMutableTreeNode representing our matching files node
     */
    public void buildTree(
            DefaultMutableTreeNode missingNode,
            DefaultMutableTreeNode changedNode,
            DefaultMutableTreeNode matchingNode
    ) {
        // Create a node for ourselves.  Note that we won't allow this node to have any children.
        DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(getSimpleName(), false);
        // Sort ourselves into the correct bucket.  As with compileResults() above, path mismatches override hash
        // mismatches, which in turn overrides matches.
        if (!pathMatch) missingNode.add(fileNode);
        else if (!hashMatch) changedNode.add(fileNode);
        else matchingNode.add(fileNode);
    }

}

