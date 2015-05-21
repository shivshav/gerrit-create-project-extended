package com.spazz.shiv.gerrit.plugins.creategr;

import com.google.gerrit.extensions.events.NewProjectCreatedListener;

/**
 * Created by shivneil on 5/20/15.
 */
public class ProjectListenerTest implements NewProjectCreatedListener {
    //TODO: Get project name
    //TODO: Create develop branch
    //TODO: Update HEAD to develop
    //TODO: Create gitreview file
    //TODO: Commit to develop (bypass review?)


    @Override
    public void onNewProjectCreated(Event event) {
        String projName = event.getProjectName();
    }
}
