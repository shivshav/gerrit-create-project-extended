package com.spazz.shiv.gerrit.plugins.createprojectextended.rest;

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.GerritPersonIdent;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.jgit.lib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jgit.lib.Constants;

import java.io.IOException;

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

    private final String webUrl;
    private final GitRepositoryManager repoManager;
    private final Provider<CurrentUser> userProvider;
    private final MetaDataUpdate.User metaDataUpdateFactory;


    static class GitReviewInput {
        String branch;
    }

    @Inject
    AddGitReview(@CanonicalWebUrl String webUrl,
                 GitRepositoryManager repoManager,
                 Provider<CurrentUser> userProvider,
                 MetaDataUpdate.User metaDataUpdateFactory) {
        this.webUrl = webUrl;
        this.repoManager = repoManager;
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
    public Response<String> apply(ProjectResource projectResource, GitReviewInput gitReviewInput) throws RestApiException {
        log.info("Attempting to create gitreview file...");

        Project.NameKey name = new Project.NameKey(projectResource.getName());

        Repository repo = null;
        try {
            repo = repoManager.openRepository(name);

            String ref = denormalizeBranchName(gitReviewInput.branch);
            if(!Repository.isValidRefName(ref)) {
                throw new BadRequestException(ref + " is not a valid refname!");
            }

            ObjectId objId = repo.resolve(ref);
            if(objId == null) {
                throw new BadRequestException("branch " + ref + " does not exist");
            }

            createFileCommit(repo, name, ref);
        } catch (IOException ioe) {
            throw new RestApiException(ioe.getMessage());
        } finally {
            if(repo != null) {
                repo.close();
            }
        }
        log.info("gitreview file created");
        return Response.created("GitReview Response");
    }


    private void createFileCommit(Repository repo, Project.NameKey project, String refName) {
        log.info("Now entering the createFileCommit method");

        try(ObjectInserter oi = repo.newObjectInserter()) {

            ObjectId parent = repo.getRef(denormalizeBranchName(refName)).getObjectId();

            // Contents of the file becomes a blob
            byte[] grFile = ("[" + GITREVIEW_REMOTE_NAME + "]\n" +
                    GITREVIEW_HOST_KEY + webUrl + "\n" +
                    GITREVIEW_PORT_KEY + GITREVIEW_PORT_VALUE + "\n" +
                    GITREVIEW_PROJECT_KEY + project.get() + ".git" + "\n" +
                    GITREVIEW_DEFAULT_BRANCH_KEY + normalizeBranchName(refName) + "\n").getBytes();

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
            cb.setMessage("Initial .gitreview file");
            ObjectId commitId = oi.insert(cb);
            log.info("CommitID: " + commitId.getName());

            // Flush to inform the framework of the commit
            oi.flush();

            RefUpdate ru = repo.updateRef(denormalizeBranchName(refName));
//            ru.setForceUpdate(true);
            ru.setRefLogIdent(metaDataUpdateFactory.getUserPersonIdent());
            ru.setNewObjectId(commitId);
//            ru.setExpectedOldObjectId(ObjectId.zeroId());
            ru.setRefLogMessage("commit: Initial Hello", false);

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
    }

    private String normalizeBranchName(String refName) {
        refName = refName.replace(Constants.R_HEADS, "");
        while(refName.startsWith("/")) {
            refName = refName.substring(1);
        }

        log.info("normalizeBranchName::refname was " + refName);
        return refName;
    }

    private String denormalizeBranchName(String refname) {
        // remove all prepended slashes
        while (refname.startsWith("/")) {
            refname = refname.substring(1);
        }

        // If it doesn't begin with refs/heads/ make it so...
        if(!refname.startsWith(Constants.R_HEADS)) {
            refname = Constants.R_HEADS + refname;
        }
        log.info("denormalizeBranchName::refname was " + refname);
        return refname;
    }
}
