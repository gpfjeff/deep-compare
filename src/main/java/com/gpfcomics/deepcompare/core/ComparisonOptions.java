package com.gpfcomics.deepcompare.core;

import lombok.Getter;
import lombok.Setter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents a set of comparison options that allow the user to configure how they want the two directories
 * compared.  This includes a list of exclusion patterns (i.e., files to ignore) which can either use regular
 * expressions or simple DOS/UNIX file globs.  It also supports an optional log file, which is recommended for
 * command-line interface mode.
 */
public class ComparisonOptions {

    // Declare a list of hash algorithm names.  Not all of these will be available, but we'll try and be ambitious and
    // provide as many options as well can.  This first set should be available for at least Java 7 and up:
    public static final String HASH_MD5 = "MD5";
    public static final String HASH_SHA1 = "SHA-1";
    public static final String HASH_SHA256 = "SHA-256";
    public static final String HASH_SHA384 = "SHA-384";
    public static final String HASH_SHA512 = "SHA-512";
    // These should be available for Java 9 and up:
    public static final String HASH_SHA512_224 = "SHA-512/224";
    public static final String HASH_SHA512_256 = "SHA-512/256";
    public static final String HASH_SHA3_224 = "SHA3-224";
    public static final String HASH_SHA3_256 = "SHA3-256";
    public static final String HASH_SHA3_384 = "SHA3-384";
    public static final String HASH_SHA3_512 = "SHA3-512";

    /**
     * The list of available hashes.  These will be determined by checking the Java runtime during start-up.
     */
    public static final List<String> HASHES = new ArrayList<>();
    static {
        // Identify the actually available hashes and stuff those into a list.  We'll loop through all the algorithm
        // names set above and check to see what's available.  If it is (i.e., trying to instantiate it doesn't throw a
        // NoSuchAlgorithmException), add it to the list.
        for (String hash : Arrays.asList( HASH_MD5, HASH_SHA1, HASH_SHA256, HASH_SHA384, HASH_SHA512,
                HASH_SHA512_224, HASH_SHA512_256, HASH_SHA3_224, HASH_SHA3_256, HASH_SHA3_384, HASH_SHA3_512)) {
            try {
                MessageDigest.getInstance(hash);
                HASHES.add(hash);
            } catch (NoSuchAlgorithmException ignored) { }
        }
    }

    /**
     * The exclusion list.  Defaults to empty.
     */
    @Getter
    private final List<String> exclusions = new ArrayList<>();

    /**
     * Whether exclusions use regular expressions (true) or simply DOS/UNIX globs (false).  Defaults to false.
     */
    @Getter
    @Setter
    private boolean exclusionsRegex = false;

    /**
     * The name of the hash algorithm to use for comparisons.  Ideally, we want to use SHA-256 as the default, but if
     * that isn't available for some reason, fall back to SHA-1.  (SHA-1 is less secure, but it should be available
     * everywhere.)
     */
    @Getter
    private String hash = HASHES.contains(HASH_SHA256) ? HASH_SHA256 : HASH_SHA1;

    /**
     * Whether to check hidden files.  Defaults to false, i.e., hidden files will be skipped.
     */
    @Getter
    @Setter
    private boolean checkHiddenFiles = false;

    /**
     * The path to the output log file.  It is assumed the UI will validate this before setting it.  Defaults to null,
     * which means no log file will be written.
     */
    @Getter
    @Setter
    private String logFilePath = null;

    /**
     * Whether to log debug-level output.  Defaults to false.
     */
    @Getter
    @Setter
    private boolean debugMode = false;

    public ComparisonOptions() { }

    /**
     * Set the hash algorithm to use for comparisons
     * @param hash A String representing the cryptographic hash to use
     * @throws IllegalArgumentException Thrown if the string is not a recognized hash name
     */
    public void setHash(String hash) throws IllegalArgumentException {
        if (!HASHES.contains(hash)) {
            throw new IllegalArgumentException("Hash algorithm not recognized");
        }
        this.hash = hash;
    }

    /**
     * Convert simple DOS/UNIX wildcards to regular expressions.  This is a one-way conversion that should only be
     * performed right before running the comparison.  However, this will check the flag to make sure the "exclusions
     * use regex" flag isn't set before doing the conversion, so it should be safe to run even if the that flag is set.
     */
    public void convertSimpleWildcardsToRegex() {
        try {
            // Make sure the regex flag isn't set:
            if (!exclusionsRegex) {
                // Create a new list of converted strings:
                List<String> converted = new ArrayList<>();
                // Loop through the current list:
                for (String exclusion : exclusions) {
                    // Preserve any existing periods:
                    String newPattern = exclusion.replaceAll("\\.", "\\.");
                    // Convert single-character matches (question marks) to a dot:
                    newPattern = newPattern.replaceAll("\\?", ".");
                    // Convert multi-character matches (asterisks) to a "match anything" pattern:
                    newPattern = newPattern.replaceAll("\\*", ".*");
                    // TODO: What else should we convert?
                    // Add the converted pattern to the new list.
                    converted.add("^" + newPattern + "$");
                }
                // Clear the old list and replace it with the new list, then set the regex flag so this conversion
                // can't be run again:
                exclusions.clear();
                exclusions.addAll(converted);
                exclusionsRegex = true;
            }
        } catch (Exception ignored) {
            // TODO: What do we do if an exception is thrown?
        }
    }

}
