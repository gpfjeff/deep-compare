package com.gpfcomics.deepcompare.core;

import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;

public class File {

    /* MEMBER VARIABLES **********************************************************************************************/

    @Getter
    private final String pathString;

    @Getter
    private String hash;

    @Getter
    private long size;

    @Getter
    @Setter
    private boolean pathMatch = false;

    @Getter
    private boolean hashMatch = false;

    /* CONSTRUCTORS **************************************************************************************************/

    public File(String path) {
        this.pathString = path;
    }

    /* PUBLIC FUNCTIONS **********************************************************************************************/

    public String getSimpleName() {
        return Paths.get(pathString).getFileName().toString();
    }

    public void scan() {
        // This is seems pretty simple, but for now all we'll do is get our file size and keep track of it.  The parent
        // directory will reference this to get the total size of the directory.
        try {
            size = Files.size(Paths.get(pathString));
        } catch (Exception ex) {
            // TODO: What to do if an exception is thrown?
        }
    }

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

    public void compare(File companion) {
        hashMatch = hash.equals(companion.getHash());
    }

}

