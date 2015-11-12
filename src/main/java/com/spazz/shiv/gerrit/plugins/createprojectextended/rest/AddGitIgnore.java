package com.spazz.shiv.gerrit.plugins.createprojectextended.rest;

import com.google.common.base.Strings;
import com.google.gerrit.extensions.common.CommitInfo;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.mail.EmailHeader;
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
import java.util.ListIterator;

/**
 * Created by shivneil on 11/8/15.
 */
public class AddGitIgnore implements RestModifyView<ProjectResource, AddGitIgnore.GitIgnoreInput> {
    private static final Logger log = LoggerFactory.getLogger(AddGitIgnore.class);
    private static final String GITIGNORE_FILENAME = ".gitignore";
    private static final String GITIGNORE_DEFAULT_COMMIT_MESSAGE = "Added .gitignore file";
    static class GitIgnoreInput {
        String branch;
        List<String> gitignoreioTemplates;
        String commitMessage;
        boolean showFile;
    }

    static class GitIgnoreInfo {
        String commitId;
        String commitMessage;
        String ignoreFile;
    }

    private final GitRepositoryManager repoManager;
    private final MetaDataUpdate.User metaDataUpdateFactory;
    @Inject
    AddGitIgnore(GitRepositoryManager repoManager,
                 MetaDataUpdate.User metaDataUpdateFactory) {
        this.repoManager = repoManager;
        this.metaDataUpdateFactory = metaDataUpdateFactory;
    }

    @Override
    public Object apply(ProjectResource projectResource, GitIgnoreInput gitIgnoreInput) throws AuthException, BadRequestException, ResourceConflictException, ResourceNotFoundException, UnprocessableEntityException {

        String gitIgnoreBranch;
        List<String> gitIgnoreTemplates;
        String commitMessage;
        boolean sendFile = false;

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

        // TODO: Keep a cache of gitignore templates to check against on requests

        // Check for custom commit message
        if(Strings.isNullOrEmpty(gitIgnoreInput.commitMessage)) {
            // Build default message with templates given
            StringBuilder sb = new StringBuilder(GITIGNORE_DEFAULT_COMMIT_MESSAGE).append(" with ");
            String template = null;
            for (ListIterator<String> it = gitIgnoreTemplates.listIterator(); it.hasNext(); template = it.next()) {
                sb.append(template);
                if(it.hasNext()) {
                    sb.append(", ");
                } else {
                    sb.append(" templates");
                }
            }
            commitMessage = sb.toString();
        } else {// Use custom commit message
            commitMessage = gitIgnoreInput.commitMessage;
        }

        if(gitIgnoreInput.showFile) {
            sendFile = true;
        }

        Repository repo = null;
        String ignoreFileContents;
        GitIgnoreInfo info = null;
        try {
            repo = repoManager.openRepository(projectResource.getNameKey());

            // Make sure the given ref is valid and the branch exists
            GitUtil.validateBranch(repo, gitIgnoreBranch);

            // Request file from gitignore.io
            ignoreFileContents = requestFromGitignoreIO(gitIgnoreTemplates);

            // TODO: Commit said file into git repo
            CommitInfo cInfo = GitUtil.createFileCommit(repo, metaDataUpdateFactory.getUserPersonIdent(), gitIgnoreBranch,
                    GITIGNORE_FILENAME, ignoreFileContents, commitMessage);

            // Build our response
            info = new GitIgnoreInfo();
            info.commitId = cInfo.commit;
            info.commitMessage = cInfo.message;
            if(sendFile) {
                info.ignoreFile = ignoreFileContents;
            }

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
        return Response.created(info);
    }

    private String requestFromGitignoreIO(List<String> templates) throws IOException {
        GitignoreIoConnection connection = new GitignoreIoConnection();
        try {
            return connection.getGitIgnoreFile(templates);
        } catch (IOException ioe) {
            throw new IOException("Error accessing gitignore.io\n" + ioe.getMessage());
        }
    }
}
