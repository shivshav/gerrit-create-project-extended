package com.spazz.shiv.gerrit.plugins.createprojectextended.creategr;

import com.google.gerrit.extensions.events.LifecycleListener;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by shivneil on 5/22/15.
 */
@Singleton
public class CreateProjectExtendedManager implements LifecycleListener {
    private static final Logger log = LoggerFactory.getLogger(CreateProjectExtendedManager.class);
    private boolean started;
    private static Map<String, ProjectListenerTest> projectsInCreation;
    public static final String MAP_KEY_SEPARATOR = ":";

    @Override
    public void start() {
        if(!started) {
            log.info("CREATE_PROJECT_EXTENDED Plugin Loaded");
            started = true;
        }
    }

    @Override
    public void stop() {
        if (projectsInCreation != null) {
            projectsInCreation.clear();
            projectsInCreation = null;
        }
        log.info("CREATE_PROJECT_EXTENDED Plugin Unloaded");
    }

    public static Map<String, ProjectListenerTest> getProjectsInCreation() {
        if(projectsInCreation == null) {
            projectsInCreation = new HashMap<>();
        }
        return projectsInCreation;
    }
}
