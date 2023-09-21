/*
 * DEEP COMPARE: ComparisonException
 * AUTHOR: Jeffrey T. Darlington
 * URL: https://github.com/gpfjeff/deep-compare
 * Copyright 2023, Jeffrey T. Darlington.  All rights reserved.
 */
package com.gpfcomics.deepcompare.core;

/**
 * A custom exception class for comparison exceptions
 */
public class ComparisonException extends Exception {

    public ComparisonException() {
        super();
    }

    public ComparisonException(String message) {
        super(message);
    }

    public ComparisonException(Throwable cause) {
        super(cause);
    }

    public ComparisonException(String message, Throwable cause) {
        super(message, cause);
    }

}
