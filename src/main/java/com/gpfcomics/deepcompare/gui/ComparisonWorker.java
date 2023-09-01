package com.gpfcomics.deepcompare.gui;

import com.gpfcomics.deepcompare.Main;
import com.gpfcomics.deepcompare.core.*;
import lombok.Getter;

import javax.swing.*;
import java.util.concurrent.ExecutionException;

public class ComparisonWorker extends SwingWorker<ComparisonResult, ComparisonStatus> {

    private final ProgressDialog parent;

    private final String sourcePath;

    private final String targetPath;

    private final ComparisonOptions options;

    private final IHashProgressListener hashListener;

    private final IStatusListener statusListener;

    @Getter
    private ComparisonResult result;

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
            // Get our result:
            result = get();
            // TODO: Launch the result dialog, feeding it the result for display, then close this dialog
        } catch (InterruptedException ignored) {
            // Ignore interrupted exceptions.  Odds are the user clicked Cancel here, so there's nothing we
            // need to do at this point.
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
