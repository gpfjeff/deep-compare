package com.gpfcomics.deepcompare.core;

import com.gpfcomics.deepcompare.Main;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * The comparison engine is the main workhorse of this application.  It searches the input source and target
 * directories, mapping the current folder structure and hashing the contained files to create our final result.  This
 * class implements Callable, which allows it to be run on a separate thread, which is important for allowing the user
 * to receive feedback while it is working and to allow it to be interrupted and cancelled.  It returns a
 * ComparisonResult object.
 */
public class ComparisonEngine implements Callable<ComparisonResult> {

    private final String sourcePath;

    private final String targetPath;

    private final ComparisonOptions options;

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
    public ComparisonResult call() throws ComparisonException {

        // Declare a buffered writer for our log file, but initialize it to null.  We will set this up later if needed,
        // but the null default will let us shortcut the logging steps if we don't need them.
        BufferedWriter log = null;

        ComparisonResult result = new ComparisonResult();
        result.setOptions(options);

        // Asbestos underpants:
        try {

            // Get things started.  Send our start-up message to our listener:
            statusListener.updateStatusMessage(Main.RESOURCES.getString("engine.status.startup"));

            // Set up the log file if directed to do so:
            if (!options.getLogFilePath().isEmpty()) {

                // Create the writer, overwriting any existing file by the same name that may already exist in that
                // location:
                log = Files.newBufferedWriter(
                        Paths.get(options.getLogFilePath(), "deep-compare.log").toAbsolutePath(),
                        StandardOpenOption.TRUNCATE_EXISTING
                );

                // Write our preamble, with a header that includes the start time and the source and target directories:
                log.write("Deep Compare v" + Main.VERSION);
                log.newLine();
                log.write("Begin comparison at " + (new Date()));
                log.newLine();
                log.write("Source directory: " + sourcePath);
                log.newLine();
                log.write("Target directory: " + targetPath);
                log.newLine();

                // If we're in debug mode, dump a bunch of extra input information:
                if (options.isDebugMode()) {
                    // TODO: Log debug options...
                }

            }

            // Declare our source and target trees, initializing them to the input paths:
            Directory sourceDirectory = new Directory(sourcePath);
            Directory targetDirectory = new Directory(targetPath);
            result.setSourceDirectory(sourceDirectory);
            result.setTargetDirectory(targetDirectory);

            // Time to start building our maps.  We'll start with the source:
            statusListener.updateStatusMessage(Main.RESOURCES.getString("engine.status.build.source.map"));
            if (log != null) {
                log.write(Main.RESOURCES.getString("engine.status.build.source.map"));
                log.newLine();
            }
            sourceDirectory.scan(options, log);

            // Next, build the target map:
            statusListener.updateStatusMessage(Main.RESOURCES.getString("engine.status.build.target.map"));
            if (log != null) {
                log.write(Main.RESOURCES.getString("engine.status.build.target.map"));
                log.newLine();
            }
            targetDirectory.scan(options, log);

            // Get the total number of files and bytes from the two directory maps and report them, first to our status
            // listener, then to the log file (if necessary):
            long totalFiles = sourceDirectory.getCount() + targetDirectory.getCount();
            long totalBytes = sourceDirectory.getSize() + targetDirectory.getSize();
            result.setTotalFiles(totalFiles);
            result.setTotalBytes(totalBytes);
            statusListener.updateTotalFiles(totalFiles);
            statusListener.updateTotalBytes(totalBytes);
            if (log != null) {
                log.write("Total number of files discovered: " + totalFiles);
                log.newLine();
                log.write("Total file size to process: " + Utilities.prettyPrintFileSize(totalBytes));
                log.newLine();
            }

            // Now for the real work.  Starting with the source tree, start recursively hashing files:
            statusListener.updateStatusMessage(Main.RESOURCES.getString("engine.status.hash.source"));
            if (log != null) {
                log.write(Main.RESOURCES.getString("engine.status.hash.source"));
                log.newLine();
            }
            MessageDigest hash = MessageDigest.getInstance(options.getHash());
            sourceDirectory.hash(hash, hashListener);

            // Do the same for the target tree:
            statusListener.updateStatusMessage(Main.RESOURCES.getString("engine.status.hash.target"));
            if (log != null) {
                log.write(Main.RESOURCES.getString("engine.status.hash.target"));
                log.newLine();
            }
            sourceDirectory.hash(hash, hashListener);

            // Generate the final report.  Tell the source folder to compare itself against the target folder and vice
            // versa.  This has to be done from both sides, because a file may be missing from one tree and not the
            // other, and that's the best way to identify this.
            // TODO: One efficiency shortcut might be to skip comparing two files' hashes if we've already compared them
            //       (i.e., skip target to source if we've already checked source to target).  Then again, this is just
            //       a simple string compare at this point, so this might not buy us much.
            statusListener.updateStatusMessage(Main.RESOURCES.getString("engine.status.generate.report"));
            sourceDirectory.compare(targetDirectory);
            targetDirectory.compare(sourceDirectory);

            // Log our results to the log file.  For CLI mode, this is our only useful output, while for GUI mode its
            // an added bonus.  For the log file, we'll only be concerned with logging discrepancies; we don't need an
            // exhaustive list of all files.  If the two folders match, a simple message stating that they match will
            // suffice.
            if (log != null) {
                // TODO: Write results to log file
            }

        // If anything blows up, catch the exception and write the exception to the log, if we're writing one.  Note
        // that this ignores the debug flag; we will *ALWAYS* log the exception here.
        } catch (Exception ex) {
            statusListener.errorMessage("Error generated while performing comparison");
            if (log != null) {
                try {
                    log.write("Error generated while performing comparison:");
                    log.newLine();
                    log.write(ex.toString());
                    log.newLine();
                } catch (Exception ignored) { }
            }
            throw new ComparisonException();

        // Finally, flush and close the log if necessary:
        } finally {
            if (log != null) {
                try {
                    log.write("End comparison at " + (new Date()));
                    log.newLine();
                    log.flush();
                    log.close();
                } catch (IOException ignored) {
                    statusListener.errorMessage("Error generated while performing comparison");
                }
            }
        }

        return result;

    }

}
