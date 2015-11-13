package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.gerrit.extensions.common.ProjectInfo;
import com.spazz.shiv.gerrit.plugins.createprojectextended.rest.AddGitIgnore;
import com.spazz.shiv.gerrit.plugins.createprojectextended.rest.AddGitReview;

import java.lang.reflect.Field;

/**
 * Created by shivneil on 11/11/15.
 */
public class ExtendedProjectInfo {
    public ProjectInfo projectInfo;
    public AddGitReview.GitReviewInfo gitReviewInfo;
    public AddGitIgnore.GitIgnoreInfo gitignoreInfo;
    public String head;
}
