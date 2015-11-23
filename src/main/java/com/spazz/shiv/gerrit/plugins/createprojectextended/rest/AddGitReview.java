package com.spazz.shiv.gerrit.plugins.createprojectextended.rest;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.gerrit.extensions.common.CommitInfo;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.extensions.events.GitReferenceUpdated;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.spazz.shiv.gerrit.plugins.createprojectextended.GitUtil;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jgit.lib.Constants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by shivneil on 11/8/15.
 */
public class AddGitReview implements RestModifyView<ProjectResource, AddGitReview.GitReviewInput> {
    private final static Logger log = LoggerFactory.getLogger(AddGitReview.class);

    private final static String GITREVIEW_FILENAME = ".gitreview";

    private final static String GITREVIEW_REMOTE_NAME = "gerrit";

    private final static String GITREVIEW_HOST_KEY = "host=";
//    private final static String GITREVIEW_HOST_VALUE = "dev.randrweb.org";

    private final static String GITREVIEW_PORT_KEY = "port=";
    private final static String GITREVIEW_PORT_VALUE = "29418";

    private final static String GITREVIEW_PROJECT_KEY = "project=";
    private final static String GITREVIEW_PROJECT_VALUE = "TBD";

    private final static String GITREVIEW_DEFAULT_BRANCH_KEY = "defaultbranch=";
    private final static String GITREVIEW_DEFAULT_BRANCH_VALUE = "develop";

    private final static String GITREVIEW_DEFAULT_COMMIT_MESSAGE = "Added .gitreview file";

    private final String webUrl;
    private final GitRepositoryManager repoManager;
    private final Provider<CurrentUser> userProvider;
    private final MetaDataUpdate.User metaDataUpdateFactory;
    private final GitReferenceUpdated referenceUpdated;


    static class GitReviewInput {
        String branch;
        String commitMessage;
    }

    public static class GitReviewInfo {
        String commitId;
        String commitMessage;
    }

    @Inject
    AddGitReview(@CanonicalWebUrl String webUrl,
                 GitRepositoryManager repoManager,
                 GitReferenceUpdated referenceUpdated,
                 Provider<CurrentUser> userProvider,
                 MetaDataUpdate.User metaDataUpdateFactory) {
        this.webUrl = webUrl;
        this.repoManager = repoManager;
        this.referenceUpdated = referenceUpdated;
        this.userProvider = userProvider;
        this.metaDataUpdateFactory = metaDataUpdateFactory;

        if(userProvider == null) {
            log.error("userProvider was null!!!");
        }
        else {
            log.info("userProvider::" + userProvider.get().getRealUser());
        }
    }

    @Override
    public Response<GitReviewInfo> apply(ProjectResource projectResource, GitReviewInput gitReviewInput) throws RestApiException {

        log.info("Attempting to create gitreview file...");

        String ref;
        String message;

        // Check if branch parameter is null or empty
        if(Strings.isNullOrEmpty(gitReviewInput.branch)) {
            ref = Constants.HEAD;
            log.info("branch specification not found. Using " + Constants.HEAD);
        } else {
            ref = gitReviewInput.branch;
        }

        // Check if commit commitMessage is null or empty
        if(Strings.isNullOrEmpty(gitReviewInput.commitMessage)) {
            message = GITREVIEW_DEFAULT_COMMIT_MESSAGE;
            log.info("Commit commitMessage not found in data. Using default commitMessage");
        }
        else {
            message = gitReviewInput.commitMessage;
        }

        Project.NameKey name = new Project.NameKey(projectResource.getName());

        Repository repo = null;
        GitReviewInfo info = null;
        try {
            repo = repoManager.openRepository(name);

            GitUtil.validateBranch(repo, ref);

            ref = GitUtil.denormalizeBranchName(ref);

            // TODO: Use the method from GitUtil
            // Create the gitreview file
            Map<String, String> reviewMap = new HashMap<>();
            reviewMap.put("refName", ref);
            reviewMap.put("filename", GITREVIEW_FILENAME);
            reviewMap.put("fileContents", createReviewFile(name, GitUtil.normalizeBranchName(ref)));
            reviewMap.put("commitMessage", message);

            CommitInfo cInfo = new CommitInfo();
            cInfo = GitUtil.createFileCommit(repo, metaDataUpdateFactory.getUserPersonIdent(), referenceUpdated, name, reviewMap);

            info = new GitReviewInfo();
            info.commitId = cInfo.commit;
            info.commitMessage = cInfo.message;

//            info = createFileCommit(repo, name, ref, message);

        } catch (RefNotFoundException rnfe) {
            throw new BadRequestException(rnfe.getMessage());
        } catch (InvalidRefNameException irne) {
            throw new BadRequestException(irne.getMessage());
        } catch (IOException ioe) {
            throw new RestApiException(ioe.getMessage());
        } finally {
            if(repo != null) {
                repo.close();
            }
        }
        log.info("gitreview file created");
        return Response.created(info);
    }

    private String createReviewFile(Project.NameKey project, String branchAlone){
        String reviewFile = "[" + GITREVIEW_REMOTE_NAME + "]\n" +
                GITREVIEW_HOST_KEY + webUrl + "\n" +
                GITREVIEW_PORT_KEY + GITREVIEW_PORT_VALUE + "\n" +
                GITREVIEW_PROJECT_KEY + project.get() + ".git\n" +
                GITREVIEW_DEFAULT_BRANCH_KEY + branchAlone + "\n";
        //        // Contents of the file becomes a blob
//        byte[] grFile = ("[" + GITREVIEW_REMOTE_NAME + "]\n" +
//                GITREVIEW_HOST_KEY + webUrl + "\n" +
//                GITREVIEW_PORT_KEY + GITREVIEW_PORT_VALUE + "\n" +
//                GITREVIEW_PROJECT_KEY + project.get() + ".git" + "\n" +
//                GITREVIEW_DEFAULT_BRANCH_KEY + branchAlone + "\n").getBytes();
        return reviewFile;
    }

    private GitReviewInfo createFileCommit(Repository repo, Project.NameKey project, String refName, String message) {
        log.info("Now entering the createFileCommit method");

        GitReviewInfo info = null;

        try(ObjectInserter oi = repo.newObjectInserter()) {

            String branchRefsHeads = GitUtil.denormalizeBranchName(refName);
            String branchAlone = GitUtil.normalizeBranchName(refName);

            ObjectId parent = repo.getRef(branchRefsHeads).getObjectId();

            info = new GitReviewInfo(); // info to return on success

            // Contents of the file becomes a blob
            byte[] grFile = ("[" + GITREVIEW_REMOTE_NAME + "]\n" +
                    GITREVIEW_HOST_KEY + webUrl + "\n" +
                    GITREVIEW_PORT_KEY + GITREVIEW_PORT_VALUE + "\n" +
                    GITREVIEW_PROJECT_KEY + project.get() + ".git" + "\n" +
                    GITREVIEW_DEFAULT_BRANCH_KEY + branchAlone + "\n").getBytes();

            // Create the file blob for the Git tree
            ObjectId fileId = oi.insert(Constants.OBJ_BLOB, grFile, 0, grFile.length);
            log.info("FileID: " + fileId.getName());

            // Add a tree object that represents the filename and metadata
            TreeFormatter formatter = new TreeFormatter();
            formatter.append(GITREVIEW_FILENAME, FileMode.REGULAR_FILE, fileId);
            ObjectId treeId = oi.insert(formatter);
            log.info("TreeID: " + treeId.getName());

            // Commit the changes to the repo
            PersonIdent person = new PersonIdent(repo);
            CommitBuilder cb = new CommitBuilder();
            cb.setParentId(parent);
            cb.setTreeId(treeId);
            cb.setAuthor(metaDataUpdateFactory.getUserPersonIdent());
            cb.setCommitter(metaDataUpdateFactory.getUserPersonIdent());
            cb.setMessage(message);
            ObjectId commitId = oi.insert(cb);
            log.info("CommitID: " + commitId.getName());

            // Get relevant info to send back to user
            info.commitId = commitId.abbreviate(7).name();
            info.commitMessage = cb.getMessage();

            // Flush to inform the framework of the commit
            oi.flush();

            RefUpdate ru = repo.updateRef(branchRefsHeads);
//            ru.setForceUpdate(true);
            ru.setRefLogIdent(metaDataUpdateFactory.getUserPersonIdent());
            ru.setNewObjectId(commitId);
//            ru.setExpectedOldObjectId(ObjectId.zeroId());
            ru.setRefLogMessage("commit: " + message, false);

            RefUpdate.Result result = ru.update();
            log.info("Result: " + result.name());
            switch (result) {
                case NEW:
//                    referenceUpdated.fire(project, ru);
                    break;
                case FAST_FORWARD:
                    break;
                default: {
                    throw new IOException(String.format("Failed to create ref: %s", result.name()));
                }
            }
//
//            for (String ref : refs) {
//                RefUpdate ru = repo.updateRef(ref);
//                ru.setNewObjectId(commitId);
//                final RefUpdate.Result result = ru.update();
//                switch (result) {
//                    case NEW:
//                        referenceUpdated.fire(project, ru);
//                        break;
//                    default: {
//                        throw new IOException(String.format("Failed to create ref \"%s\": %s", ref, result.name()));
//                    }
//                }
//            }
        } catch(IOException ioe) {
            log.error("Cannot create hello world commit", ioe);
        }

        return info;
    }
}
