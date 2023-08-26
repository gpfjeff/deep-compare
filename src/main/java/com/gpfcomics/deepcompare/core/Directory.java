package com.gpfcomics.deepcompare.core;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Directory {

    /* MEMBER VARIABLES **********************************************************************************************/

    @Getter
    private final String pathString;

    @Getter
    @Setter
    private boolean match = false;

    @Getter
    private final List<Directory> subdirectories = new ArrayList<>();

    @Getter
    private final List<File> files = new ArrayList<>();

    @Getter
    private long size = 0;

    @Getter
    private long count = 0;

    /* CONSTRUCTORS **************************************************************************************************/

    public Directory(String path) {
        this.pathString = path;
    }

    /* PUBLIC FUNCTIONS **********************************************************************************************/

    public String getSimpleName() {
        return Paths.get(pathString).getFileName().toString();
    }

    public void scan(ComparisonOptions options) {
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
                            // on some flavor of Windows, we'll treat the regex as case insensitive; otherwise, we'll
                            // assume it's case sensitive.
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
                                    dir.scan(options);
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

    public void hash(MessageDigest hasher, IHashProgressListener listener) {
        // Tell all the files and subdirectories to do their own hashes:
        for (File file : files) {
            file.hash(hasher, listener);
        }
        for (Directory dir : subdirectories) {
            dir.hash(hasher, listener);
        }
    }

}
