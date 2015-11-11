package com.spazz.shiv.gerrit.plugins.createprojectextended.rest;

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.project.ProjectResource;

/**
 * Created by shivneil on 11/8/15.
 */
public class AddGitReview implements RestModifyView<ProjectResource, AddGitReview.GitReviewInput> {
    static class GitReviewInput {

    }

    @Override
    public Response<String> apply(ProjectResource projectResource, GitReviewInput gitReviewInput) throws AuthException, BadRequestException, ResourceConflictException {
        return Response.ok("Git Review Response");
    }
}
