package com.gpfcomics.deepcompare;

import com.gpfcomics.deepcompare.cli.CLIRunner;
import com.gpfcomics.deepcompare.ui.StartWindow;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static final String VERSION = "1.0.0";

    public static final int COPYRIGHT_YEAR = 2023;

    public static final String WEBSITE_URL = "https://github.com/gpfjeff/deep-compare";

    public static void main(String[] args) {

        // If we were provided with no command-line parameters and we know we're not running in a headless environment,
        // go ahead and launch the GUI:
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
