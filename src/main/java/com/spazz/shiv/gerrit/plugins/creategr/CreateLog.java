package com.spazz.shiv.gerrit.plugins.creategr;

import com.google.gerrit.extensions.events.LifecycleListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by shivneil on 5/22/15.
 */
public class CreateLog implements LifecycleListener {
    private static final Logger log = LoggerFactory.getLogger(CreateLog.class);
    private boolean started;

    public CreateLog() {
    }

    @Override
    public void start() {
        if(!started) {
            log.info("CREATE_PROJECT_EXTENDED Plugin Loaded");
            started = true;
        }
    }

    @Override
    public void stop() {
        log.info("CREATE_PROJECT_EXTENDED Plugin Unloaded");
    }
}
