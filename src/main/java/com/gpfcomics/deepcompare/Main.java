package com.gpfcomics.deepcompare;

import com.gpfcomics.deepcompare.cli.CLIRunner;
import com.gpfcomics.deepcompare.gui.StartWindow;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Main application starting point.  This class controls whether we start in GUI or CLI mode, based on how the main
 * method is called.  It also hosts application-wide constants, such as version and copyright information.
 */
public class Main {

    /**
     * The current version string, which will be displayed in various output modes.  This should be incremented for
     * each new release.
     */
    public static final String VERSION = "1.0.0";

    /**
     * The current copyright year.  For now, this is just hard-coded to the year of our current release.  I suppose we
     * could calculate this from the current date, but that's overkill for our needs.
     */
    public static final int COPYRIGHT_YEAR = 2023;

    /**
     * A string containing our home website URL, which currently points to the Github project.  This will be displayed
     * in various output modes.
     */
    public static final String WEBSITE_URL = "https://github.com/gpfjeff/deep-compare";

    /**
     * A ResourceBundle to support internationalization, where available.  If no localized messages can be found for the
     * user's default locale, default to United States English.
     */
    public static final ResourceBundle RESOURCES;
    static {
        ResourceBundle temp;
        try {
            Locale locale = Locale.getDefault();
            temp = ResourceBundle.getBundle("MessagesBundle", locale);
        } catch (Exception ignored) {
            temp = ResourceBundle.getBundle("MessagesBundle", Locale.US);
        }
        RESOURCES = temp;
    }

    public static void main(String[] args) {

        // If we were provided with no command-line parameters and we know we're not running in a headless environment,
        // go ahead and launch the GUI.  We will try to set the UIManager to the default system look and feel, but if
        // that fails we'll fall back to the generic cross-platform look.
        if (args.length == 0 && !GraphicsEnvironment.isHeadless()) {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
                try {
                    UIManager.setLookAndFeel(
                            UIManager.getCrossPlatformLookAndFeelClassName()
                    );
                } catch (Exception ignored2) { }
            }
            JFrame mainWin = new JFrame("Deep Compare v" + VERSION);
            mainWin.setContentPane(new StartWindow(mainWin).getRootPanel());
            mainWin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainWin.pack();
            mainWin.setVisible(true);
        // Otherwise, pass our command-line arguments to the command-line runner:
        } else {
            CLIRunner runner = new CLIRunner(args);
            System.exit(runner.run());
        }

    }

}
