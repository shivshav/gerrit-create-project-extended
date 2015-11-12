package com.spazz.shiv.gerrit.plugins.createprojectextended.rest;

import com.google.common.base.Strings;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gwt.http.client.*;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.inject.Inject;
import com.spazz.shiv.gerrit.plugins.createprojectextended.GitUtil;
import com.spazz.shiv.gerrit.plugins.createprojectextended.rest.gitignore.GitignoreIoConnection;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by shivneil on 11/8/15.
 */
public class AddGitIgnore implements RestModifyView<ProjectResource, AddGitIgnore.GitIgnoreInput> {
    private static final Logger log = LoggerFactory.getLogger(AddGitIgnore.class);

    static class GitIgnoreInput {
        String branch;
        List<String> gitignoreioTemplates;
    }

    static class GitIgnoreInfo {
        String message;
        String ignoreFile;
    }

    private final GitRepositoryManager repoManager;

    @Inject
    AddGitIgnore(GitRepositoryManager repoManager) {
        this.repoManager = repoManager;
    }

    @Override
    public Object apply(ProjectResource projectResource, GitIgnoreInput gitIgnoreInput) throws AuthException, BadRequestException, ResourceConflictException, ResourceNotFoundException, UnprocessableEntityException {

        String gitIgnoreBranch;
        List<String> gitIgnoreTemplates;

        // Validate arguments
        // Check if branch is null or empty
        if(Strings.isNullOrEmpty(gitIgnoreInput.branch)) {
            gitIgnoreBranch = Constants.HEAD;
        } else {
            gitIgnoreBranch = gitIgnoreInput.branch;
        }

        // Check if template list is null or empty
        if(gitIgnoreInput.gitignoreioTemplates == null || gitIgnoreInput.gitignoreioTemplates.isEmpty()) {
            log.info("Templates list from request was empty");
            throw new BadRequestException("You must specify a list of templates to use for the gitignore");
        } else {
            gitIgnoreTemplates = gitIgnoreInput.gitignoreioTemplates;
        }

        Repository repo = null;
        String ignoreFile;
        try {
            repo = repoManager.openRepository(projectResource.getNameKey());

            // Make sure the given ref is valid and the branch exists
            GitUtil.validateBranch(repo, gitIgnoreBranch);

            // Request file from gitignore.io
            // TODO: Keep a cache of gitignore templates to check against on requests
            ignoreFile = requestFromGitignoreIO(gitIgnoreTemplates);
            // Commit said file into git repo


        } catch (RepositoryNotFoundException e) {
            log.error("Repository not found for " + projectResource.getName());
            throw new ResourceConflictException("Repository not found for " + projectResource.getName());
        } catch (IOException ioe) {
            throw new UnprocessableEntityException(ioe.getMessage());
        } catch (InvalidRefNameException irne) {
            throw new BadRequestException(irne.getMessage());
        } finally {
            if(repo != null) {
                repo.close();
            }
        }

        // Return results of request
        GitIgnoreInfo info = new GitIgnoreInfo();
        info.message = "GitIgnore Response";
        info.ignoreFile = ignoreFile;

        return Response.created(info);
    }

    private String requestFromGitignoreIO(List<String> templates) throws IOException {
        GitignoreIoConnection connection = new GitignoreIoConnection();
        try {
            return connection.getGitIgnoreFile(templates);
        } catch (IOException ioe) {
            throw new IOException("Error accessing gitignore.io");
        }
    }
}
