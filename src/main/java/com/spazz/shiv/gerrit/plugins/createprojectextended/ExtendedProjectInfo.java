package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.gerrit.extensions.common.ProjectInfo;

import java.lang.reflect.Field;

/**
 * Created by shivneil on 11/11/15.
 */
public class ExtendedProjectInfo {
    public ProjectInfo projectInfo;
    public String gitreviewCommit;
    public String gitignoreCommit;
}
