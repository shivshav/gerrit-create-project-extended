package com.spazz.shiv.gerrit.plugins.createprojectextended.rest;

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Created by shivneil on 11/8/15.
 */
public class AddGitReview implements RestModifyView<ProjectResource, AddGitReview.GitReviewInput> {
    private final Provider<CurrentUser> userProvider;

    static class GitReviewInput {

    }

    @Inject
    AddGitReview(Provider<CurrentUser> userProvider) {
        this.userProvider = userProvider;
    }

    @Override
    public Response<String> apply(ProjectResource projectResource, GitReviewInput gitReviewInput) throws AuthException, BadRequestException, ResourceConflictException {
        return Response.created("GitReview Response");
    }
}
