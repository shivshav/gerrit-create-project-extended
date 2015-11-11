package com.spazz.shiv.gerrit.plugins.createprojectextended.rest;

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.project.ProjectResource;

/**
 * Created by shivneil on 11/8/15.
 */
public class AddGitIgnore implements RestModifyView<ProjectResource, AddGitIgnore.GitIgnoreInput> {
    static class GitIgnoreInput {

    }

    @Override
    public Object apply(ProjectResource projectResource, GitIgnoreInput gitIgnoreInput) throws AuthException, BadRequestException, ResourceConflictException, Exception {
        return Response.ok("Git Ignore Response");
    }
}
