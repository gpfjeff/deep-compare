package com.gpfcomics.deepcompare.gui;

import com.gpfcomics.deepcompare.Main;
import com.gpfcomics.deepcompare.core.*;
import lombok.Getter;

import javax.swing.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * This subclass of SwingWorker wraps the ComparisonEngine to make it compatible with the Swing UI.
 */
public class ComparisonWorker extends SwingWorker<ComparisonResult, ComparisonStatus> {

    // Our parent ProgressDialog.  Primarily used to be the owner of any message dialogs we might pop.
    private final ProgressDialog parent;

    // The absolute path to the source directory
    private final String sourcePath;

    // The absolute path to the target directory
    private final String targetPath;

    // Our comparison options
    private final ComparisonOptions options;

    // The hash progress listener to notify of hash updates
    private final IHashProgressListener hashListener;

    // The status listener to notify of status updates
    private final IStatusListener statusListener;

    // Our result collection
    @Getter
    private ComparisonResult result;

    /**
     * Constructor
     * @param parent Our parent ProgressDialog
     * @param sourcePath A String containing the absolute path to the source directory
     * @param targetPath A String containing the absolute path to the target directory
     * @param options A ComparisonOptions object
     * @param hashListener An IHashProgressListener to notify of hash updates
     * @param statusListener An IStatusListener to notify of status updates
     */
    public ComparisonWorker(
            ProgressDialog parent,
            String sourcePath,
            String targetPath,
            ComparisonOptions options,
            IHashProgressListener hashListener,
            IStatusListener statusListener
    ) {
        this.parent = parent;
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.options = options;
        this.hashListener = hashListener;
        this.statusListener = statusListener;
    }

    @Override
    protected ComparisonResult doInBackground() throws Exception {
        // Super simple here.  Build the comparison engine and set it to work.  This will pass all of its status and
        // hash updates back to the progress dialog.
        ComparisonEngine engine = new ComparisonEngine(
                sourcePath,
                targetPath,
                options,
                hashListener,
                statusListener
        );
        return engine.call();
    }

    @Override
    public void done() {

        try {
            // Get and return: our result:
            result = get();
        } catch (InterruptedException | CancellationException ignored) {
            // Ignore interrupted and cancellation exceptions.  Odds are the user clicked Cancel here, so there's
            // nothing we need to do at this point.
        } catch (ExecutionException ee) {
            // All other exceptions should be logged to the log file if logging was enabled.  Show the user an
            // error box, then close this dialog:
            JOptionPane.showMessageDialog(
                    parent,
                    Main.RESOURCES.getString("progress.error.message"),
                    Main.RESOURCES.getString("dialog.title.error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }

    }

}
