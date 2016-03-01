package com.spazz.shiv.gerrit.plugins.createprojectextended.rest;

import com.google.common.base.Strings;
import com.google.gerrit.extensions.common.CommitInfo;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.extensions.events.GitReferenceUpdated;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.spazz.shiv.gerrit.plugins.createprojectextended.GitUtil;
import com.spazz.shiv.gerrit.plugins.createprojectextended.rest.gitignore.GitignoreIoConnection;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by shivneil on 11/8/15.
 */
public class AddGitIgnore implements RestModifyView<ProjectResource, AddGitIgnore.GitIgnoreInput> {
    private static final Logger log = LoggerFactory.getLogger(AddGitIgnore.class);
    private static final String GITIGNORE_FILENAME = ".gitignore";
    private static final String GITIGNORE_DEFAULT_COMMIT_MESSAGE = "Added .gitignore file";
    private final GitReferenceUpdated referenceUpdated;

    static class GitIgnoreInput {
        String branch;
        List<String> gitignoreioTemplates;
        String commitMessage;
        boolean showFile;
    }

    public static class GitIgnoreInfo {
        String commitId;
        String commitMessage;
        String ignoreFile;
    }

    private final GitRepositoryManager repoManager;
    private final MetaDataUpdate.User metaDataUpdateFactory;
    @Inject
    AddGitIgnore(GitRepositoryManager repoManager,
                 GitReferenceUpdated referenceUpdated,
                 MetaDataUpdate.User metaDataUpdateFactory) {
        this.repoManager = repoManager;
        this.referenceUpdated = referenceUpdated;
        this.metaDataUpdateFactory = metaDataUpdateFactory;
    }

    @Override
    public Response<GitIgnoreInfo> apply(ProjectResource projectResource, GitIgnoreInput gitIgnoreInput) throws AuthException, BadRequestException, ResourceConflictException, ResourceNotFoundException, UnprocessableEntityException {

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

        //Check if templates given are valid against gitignore.io values
        // TODO: Keep a cache of gitignore templates to check against on requests
        try {
            HashSet<String> validTemplates = requestGitignoreIoTemplates();
            log.info("********BEGIN TEMPLATE SET*********");
            log.info(validTemplates.toString());
            log.info("********END TEMPLATE SET***********");
            for (String template :
                    gitIgnoreTemplates) {
                if(!validTemplates.contains(template)) {
                    throw new UnprocessableEntityException(template + " is not a valid template");
                }
            }
        } catch (IOException ioe) {
            throw new UnprocessableEntityException(ioe.getMessage());
        }

        // Check for custom commit commitMessage
        if(Strings.isNullOrEmpty(gitIgnoreInput.commitMessage)) {
            // Build default commitMessage with templates given
            StringBuilder sb = new StringBuilder(GITIGNORE_DEFAULT_COMMIT_MESSAGE).append(" with ");

            String template;
            ListIterator<String> it = gitIgnoreTemplates.listIterator();
            while(it.hasNext()) {
                template = it.next();
                sb.append(template);

                // Check if we are at the end of the template list for formatting;
                if(it.hasNext()) {
                    sb.append(", ");
                } else {
                    sb.append(" templates.");
                }
            }



            commitMessage = sb.toString();
        } else {// Use custom commit commitMessage
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

//            // TODO: Commit said file into git repo
            Map<String, String> ignoreMap = new HashMap<>();
            ignoreMap.put("refName", gitIgnoreBranch);
            ignoreMap.put("filename", GITIGNORE_FILENAME);
            ignoreMap.put("fileContents", ignoreFileContents);
            ignoreMap.put("commitMessage", commitMessage);
//            CommitInfo cInfo = GitUtil.createFileCommit(repo, metaDataUpdateFactory.getUserPersonIdent(), gitIgnoreBranch,
//                    GITIGNORE_FILENAME, ignoreFileContents, commitMessage, referenceUpdated, projectResource.getNameKey());
            CommitInfo cInfo = GitUtil.createFileCommit(repo, metaDataUpdateFactory.getUserPersonIdent(), ignoreMap);
//            CommitInfo cInfo = new CommitInfo();
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
        } catch (InvalidRefNameException | RefNotFoundException irne) {
            throw new BadRequestException(irne.getMessage());
        } catch (IOException ioe) {
            throw new UnprocessableEntityException(ioe.getMessage());
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

    private HashSet<String> requestGitignoreIoTemplates() throws IOException {
        GitignoreIoConnection connection = new GitignoreIoConnection();
        try {
            return connection.getTemplateList();
        } catch (IOException ioe) {
            throw new IOException("Error retrieving template list from gitignore.io\n" + ioe.getMessage());
        }
    }
}
