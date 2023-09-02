package com.gpfcomics.deepcompare.core;

import com.gpfcomics.deepcompare.Main;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * This class represents an individual file in the comparison tree.  It maintains the file's own state (name, full
 * path, file size, etc.), as well as its cryptographic hash and whether it matches the path and hash of its companion
 * file (if it exists) in the other directory tree.
 */
public class File {

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

    public File(String path) {
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
     * Scan this individual file, taking note of its current file size
     */
    public void scan() {
        // This is seems pretty simple, but for now all we'll do is get our file size and keep track of it.  The parent
        // directory will reference this to get the total size of the directory.
        try {
            size = Files.size(Paths.get(pathString));
        } catch (Exception ex) {
            // TODO: What to do if an exception is thrown?
        }
    }

    /**
     * Generate the cryptographic hash of this file
     * @param hasher A MessageDigest object, which will perform the hash
     * @param listener The IHashProgressListener to report progress to
     */
    public void hash(MessageDigest hasher, IHashProgressListener listener) {
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
            // TODO: What to do if an exception is thrown?
            hash = null;
        }
    }

    /**
     * Compare this file's cryptographic hash to its companion file in the opposite tree.  It is assumed that this
     * method will only be called after we confirm that both files exist within their respective trees.
     * @param companion The companion File
     */
    public void compare(File companion) {
        hashMatch = hash.equals(companion.getHash());
    }

    /**
     * Log discrepancies to the specified log file
     * @param log An open BufferedWriter that writes to the log file
     * @throws IOException Thrown if any file errors occur while writing
     */
    public void logResult(BufferedWriter log) throws IOException {
        if (!pathMatch) {
            log.write(
                    String.format(
                            Main.RESOURCES.getString("engine.log.discrepancies.file.not.found"),
                            pathString
                    )
            );
            log.newLine();
        } else if (!hashMatch) {
            log.write(
                    String.format(
                            Main.RESOURCES.getString("engine.log.discrepancies.file.hash.not.match"),
                            pathString
                    )
            );
            log.newLine();
        }
    }

}

