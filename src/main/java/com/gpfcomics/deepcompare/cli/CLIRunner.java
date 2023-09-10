package com.gpfcomics.deepcompare.cli;

import com.gpfcomics.deepcompare.Main;
import com.gpfcomics.deepcompare.core.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class CLIRunner implements IHashProgressListener, IStatusListener {

    // The command line arguments, passed in from the Main class:
    private final String[] args;

    // Our copy of the comparison options class.  This instantiation will set the defaults, which the user can override
    // with command line parameters.
    private final ComparisonOptions options = new ComparisonOptions();

    // Our source path.  This isn't part of the options object, but is separate.
    private String sourcePath = null;

    // The target path:
    private String targetPath = null;

    // The total number of bytes for all files that need to be hashed
    private long totalBytes = 0L;

    // The cumulative number of bytes hashed up to the last fully-hashed file
    private long processedBytes = 0L;

    // The cumulative number of bytes hashed on the current file being hashed
    private long currentFileBytes = 0L;

    private final List<Integer> percentsShown = new ArrayList<>();

    public CLIRunner(String[] args) {
        this.args = args;
    }

    /* CORE FUNCTIONS ************************************************************************************************/

    /**
     * Start the CLI runner, first parsing the command-line arguments and returning any errors if necessary.  If
     * everything looks good, the comparison engine will run and the results will be printed to the specified log file.
     * If there are errors or not command line arguments are provided, this will print a usage message to the console.
     * @return An integer return code.  If zero, the program ran without errors.  If non-zero, the program turned an
     * error and did not complete successfully.
     */
    public int run() {
        // Print out a starting banner:
        System.out.println();
        System.out.println("Deep Compare v" + Main.VERSION);
        System.out.println("Copyright " + Main.COPYRIGHT_YEAR + ", Jeffrey T. Darlington");
        System.out.println(Main.WEBSITE_URL);
        String translator = Main.RESOURCES.getString("about.translation.attribution");
        if (!translator.isEmpty()) System.out.println(translator);
        System.out.println();
        // Check the command-line arguments.  If none were provided, print the use message and exit:
        if (args == null || args.length == 0) {
            printUsage();;
            return 0;
        }
        // Parse the command-line arguments.  This returns a list of error strings.  If this list is non-empty, print
        // the errors to the console in order and exit with a non-zero return code.
        List<String> errors = parseCommandLineArgs();
        if (!errors.isEmpty()) {
            for (String error : errors) {
                System.out.println(
                        Main.RESOURCES.getString("cli.error.prefix") + " " + error
                );
            }
            System.out.println();
            printUsage();
            return 1;
        }
        // If the exclusions are not currently using regular expressions, convert them now.  Note that this is
        // is a one-way conversion.
        if (!options.isExclusionsRegex()) { options.convertSimpleWildcardsToRegex(); }
        // All our command-line arguments look good.  Time to get to work:
        try {
            // We're going to take advantage of the fact that the comparison engine runs on a different thread.  Create
            // a new executor, create an instance of our comparison engine, submit the engine to the executor, and sit
            // back and wait.  (The listeners will feed back progress information to this thread so we can update the
            // UI.)
            ExecutorService executor = Executors.newSingleThreadExecutor();
            ComparisonEngine engine = new ComparisonEngine(
                    sourcePath,
                    targetPath,
                    options,
                    this,
                    this
            );
            Future<ComparisonResult> worker = executor.submit(engine);
            ComparisonResult result = worker.get();
            // Check the result.  It shouldn't be null, but we'll add a check for that, just in case.  Do a simple check
            // to see if the two directories match, then print either the confirmation of that or the warning that
            // discrepancies were found.  For the CLI, that's all we're going to do on the screen.  For the full
            // results, we'll direct the user to the mandatory log file.
            if (result != null) {
                if (result.getSourceDirectory().isMatch() && result.getTargetDirectory().isMatch()) {
                    System.out.println(Main.RESOURCES.getString("cli.all.match"));
                } else {
                    System.out.println(Main.RESOURCES.getString("cli.discrepancies.found"));
                }
                System.out.println(Main.RESOURCES.getString("cli.see.log"));
                return 0;
            } else {
                System.err.println(Main.RESOURCES.getString("cli.error.generic"));
                return 1;
            }
        } catch (Exception ex) {
            // There's a chance exceptions could be thrown here.  Catch them and warn the user that something went
            // wrong:
            // TODO: For now, I'm having this dump the exceptions to STDERR, since we don't really have a log here.
            //       (The comparison engine "owns" the log file, which would already be closed at this point.)
            System.err.println(Main.RESOURCES.getString("cli.error.generic"));
            ex.printStackTrace();
            return 1;
        }
    }

    /**
     * Print the usage text to the console.
     */
    private void printUsage() {
        // The usage statement is one bit property in the resources files:
        System.out.println(Main.RESOURCES.getString("cli.usage"));
        System.out.println();
    }

    /**
     * Parses the command-line arguments and updates the comparison options with the provided data
     * @return A List of Strings containing any error messages generated.  If this list is empty, no errors were
     * generated.  If non-empty, the program should printed the errors to the console and exit.
     */
    private List<String> parseCommandLineArgs() {
        // Create a return list of error strings:
        List<String> errors = new ArrayList<>();
        // Loop through the list of arguments.  Before this gets called, we should already know that the list is
        // populated and non-empty.
        for (String arg : args) {
            // If the individual argument is null or empty, skip it:
            if (arg == null || arg.trim().isEmpty()) continue;
            // If the argument doesn't start with two dashes, it's invalid.  Add an error and skip processing it:
            if (!arg.startsWith("--")) {
                errors.add(
                        String.format(
                                Main.RESOURCES.getString("cli.error.unrecognized.parameter"),
                                arg
                        )
                );
                continue;
            }
            // Strip the two dashes from the start of the argument string and split it on the first equals sign.  We
            // should get either one or two parts, depending on the argument.
            String[] argParts = arg.replace("--", "").split("\\=", 2);
            // Doing a case-insensitive compare, check the first part of the argument:
            // TODO: Not sure if we can internationalize the argument names here.  The existing switch statement
            //       obviously won't work, since we can't guarantee unique paths.  Kicking this down the road as a
            //       potential future enhancement.
            switch (argParts[0].toLowerCase()) {
                // Set the source path:
                case "source":
                    // Make sure the second part of the argument is present and populated:
                    if (argParts.length < 2 || argParts[1] == null || argParts[1].trim().isEmpty()) {
                        errors.add(Main.RESOURCES.getString("cli.error.source.path.not.found"));
                    } else {
                        // Check to see if the second part is a valid directory.  If it is, take note of it.  If not,
                        // generate an error:
                        try {
                            String pathString = argParts[1].trim();
                            Path path = Paths.get(pathString);
                            if (Files.exists(path) && Files.isDirectory(path)) {
                                sourcePath = pathString;
                            } else {
                                errors.add(Main.RESOURCES.getString("cli.error.source.path.not.valid"));
                            }
                        } catch (Exception ex) {
                            errors.add(Main.RESOURCES.getString("cli.error.source.path.not.valid"));
                        }
                    }
                    break;
                // Set the target path:
                case "target":
                    // This is pretty much exactly the same as the source path argument, just for the target:
                    if (argParts.length < 2 || argParts[1] == null || argParts[1].trim().isEmpty()) {
                        errors.add(Main.RESOURCES.getString("cli.error.target.path.not.found"));
                    } else {
                        try {
                            String pathString = argParts[1].trim();
                            Path path = Paths.get(pathString);
                            if (Files.exists(path) && Files.isDirectory(path)) {
                                targetPath = pathString;
                            } else {
                                errors.add(Main.RESOURCES.getString("cli.error.target.path.not.valid"));
                            }
                        } catch (Exception ex) {
                            errors.add(Main.RESOURCES.getString("cli.error.target.path.not.valid"));
                        }
                    }
                    break;
                // Set the exclusions from the specified file:
                case "exclusions":
                    // Again, make sure the second part of the parameter is populated:
                    if (argParts.length < 2 || argParts[1] == null || argParts[1].trim().isEmpty()) {
                        errors.add(Main.RESOURCES.getString("cli.error.exclusions.file.not.found"));
                    } else {
                        try {
                            // This should be a simple text file.  If it exists and is readable, slurp up the file and
                            // add each line to the exclusion list.  Ignore blank lines and any line starting with a
                            // hash or pound character (#).  Note that we're not trying to validate that these are
                            // valid patterns here; we're just slurping in the data for now.  If a pattern ends up being
                            // invalid, the comparison engine will ignore it.
                            Path path = Paths.get(argParts[1].trim());
                            if (Files.exists(path) && Files.isRegularFile(path) &&
                                    Files.isReadable(path)) {
                                try (Stream<String> stream = Files.lines(path)) {
                                    stream.forEach(s -> {
                                        if (!s.startsWith("#") && !s.trim().isEmpty()) {
                                            options.getExclusions().add(s.trim());
                                        }
                                    });
                                }
                            } else {
                                errors.add(Main.RESOURCES.getString("cli.error.exclusions.not.valid.file"));
                            }
                        } catch (Exception ex) {
                            errors.add(Main.RESOURCES.getString("cli.error.exclusions.cannot.read"));
                        }
                    }
                    break;
                // Set the regex flag if this is set (the default will be false):
                case "use-regex":
                    options.setExclusionsRegex(true);
                    break;
                // Set the hash algorithm:
                case "hash":
                    // If this is populated and in the list of recognized hashes, great!  Override the default with the
                    // provided value.  If it's not valid, print an error.  Note that we're forcing this to upper case,
                    // since all the algorithms use upper case letters.  This way, the user can use either case and
                    // we'll still recognize it.
                    if (argParts.length < 2 || argParts[1] == null || argParts[1].trim().isEmpty()) {
                        errors.add(Main.RESOURCES.getString("cli.error.hash.not.found"));
                    } else if (!ComparisonOptions.HASHES.contains(argParts[1].trim().toUpperCase())) {
                        errors.add(Main.RESOURCES.getString("cli.error.hash.not.supported"));
                    } else {
                        options.setHash(argParts[1].trim().toUpperCase());
                    }
                    break;
                // Set the "check hidden files" flag if this is set (the default will be false):
                case "hidden":
                    options.setCheckHiddenFiles(true);
                    break;
                // Set the log file directory:
                case "log":
                    // This is pretty much the same as the source and target directories:
                    if (argParts.length < 2 || argParts[1] == null || argParts[1].trim().isEmpty()) {
                        errors.add(Main.RESOURCES.getString("cli.error.log.path.not.found"));
                    } else {
                        try {
                            String pathString = argParts[1].trim();
                            Path path = Paths.get(pathString);
                            if (Files.exists(path) && Files.isDirectory(path)) {
                                options.setLogFilePath(pathString);
                            } else {
                                errors.add(Main.RESOURCES.getString("cli.error.log.path.not.valid"));
                            }
                        } catch (Exception ex) {
                            errors.add(Main.RESOURCES.getString("cli.error.log.path.not.valid"));
                        }
                    }
                    break;
                // Set the debug flag (default is false):
                case "debug":
                    options.setDebugMode(true);
                    break;
                // Print a list of hash algorithms available to Deep Compare and exit:
                case "show-hashes":
                    System.out.println(Main.RESOURCES.getString("cli.available.hashes"));
                    for (String hash : ComparisonOptions.HASHES) {
                        System.out.println("\t" + hash);
                    }
                    System.exit(0);
                // Print the usage message:
                case "help":
                    printUsage();
                    System.exit(0);
                // If we get anything else, tell the use we don't recognize the parameter:
                default:
                    errors.add(
                            String.format(
                                    Main.RESOURCES.getString("cli.error.unrecognized.parameter"),
                                    argParts[0]
                            )
                    );
            }
        }
        // By this point, we should have pulled out our required parameters.  If we couldn't find them, explicitly print
        // errors for them.  We'll do this in reverse order of what's in the usage list, inserting them into the
        // beginning of the list.  The log file, the target path, and the source path  are all required.
        if (options.getLogFilePath() == null) {
            errors.add(0, Main.RESOURCES.getString("cli.error.log.path.not.specified"));
        }
        if (targetPath == null) {
            errors.add(0, Main.RESOURCES.getString("cli.error.target.path.not.specified"));
        }
        if (sourcePath == null) {
            errors.add(0,Main.RESOURCES.getString("cli.error.source.path.not.specified"));
        }
        // Don't let the user compare the same path to itself, or let the source path contain the target path or vice
        // versa:
        if (sourcePath != null && targetPath != null && (
                sourcePath.equals(targetPath) ||
                        Paths.get(sourcePath).startsWith(targetPath) ||
                        Paths.get(targetPath).startsWith(sourcePath)
                )) {
            errors.add(Main.RESOURCES.getString("cli.error.source.target.same.path"));

        }
        // There's one last thing that needs to be checked.  If all three paths are set (source, target, and log), make
        // sure the log isn't being written to either the source or target path, as this could throw off the comparison.
        if (sourcePath != null && targetPath != null && options.getLogFilePath() != null && (
            Paths.get(options.getLogFilePath()).startsWith(sourcePath) ||
            Paths.get(options.getLogFilePath()).startsWith(targetPath))) {
            errors.add(Main.RESOURCES.getString("cli.error.log.file.in.path"));
        }
        // Return the final error list:
        return errors;
    }

    // IStatusListener methods:
    @Override
    public void updateStatusMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void errorMessage(String message) {
        System.err.println(message);
    }

    @Override
    public void updateTotalFiles(long fileCount) {
        System.out.println(
                String.format(
                        Main.RESOURCES.getString("cli.files.discovered"),
                        fileCount
                )
        );
    }

    @Override
    public void updateTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
        System.out.println(
                String.format(
                        Main.RESOURCES.getString("cli.bytes.discovered"),
                        Utilities.prettyPrintFileSize(totalBytes)
                )
        );
    }

    // IHashProgressListener methods:
    @Override
    public void newFile() {
        // Whenever the engine starts a new file, add the previous file's byte count to the running total of processed
        // bytes, then reset the current file to zero:
        processedBytes += currentFileBytes;
        currentFileBytes = 0L;
    }

    @Override
    public void updateProgress(long bytesRead) {
        // Add the number of bytes read to the current file's running total:
        currentFileBytes += bytesRead;
        // Calculate the percentage of the total number of bytes hashed.  For this, we'll add the current file's bytes
        // to the previous files' total, then divide by the total number of bytes.  There's a lot of type conversion
        // here, but the goal is to get to a round integer between 0 and 100.
        int percent = (int) Math.floor(
                (((double) processedBytes + (double) currentFileBytes) / (double) totalBytes) * 100.0d
        );
        // Report our progress at approximately 10% intervals.  If the percentage is evenly divisible by ten, check to
        // see if we've already reported our progress.  If not, do so now.  The "percents shown" list will keep us from
        // reporting the same percentage multiple times, on the off-chance that the calculation above resolves to the
        // same number multiple times.
        if (percent % 10 == 0 && !percentsShown.contains(percent)) {
            System.out.println(
                    String.format(
                            Main.RESOURCES.getString("cli.hash.progress"),
                            percent + "%"
                    )
            );
            percentsShown.add(percent);
        }
    }

}
