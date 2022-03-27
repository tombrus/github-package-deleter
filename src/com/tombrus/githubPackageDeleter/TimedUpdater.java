package com.tombrus.githubPackageDeleter;

import javax.swing.*;

public class TimedUpdater {
    private Timer timer;

    public TimedUpdater(Runnable callback) {
        timer = new Timer(500, e -> {
            timer.stop();
            callback.run();
        });
    }

    public void update() {
        if (SwingUtilities.isEventDispatchThread()) {
            timer.restart();
        } else {
            SwingUtilities.invokeLater(() -> timer.restart());
        }
    }
}
