package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.gerrit.extensions.common.CommitInfo;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.extensions.events.GitReferenceUpdated;
import com.google.gerrit.server.git.GitRepositoryManager;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.lib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by shivneil on 11/11/15.
 */
public class GitUtil {
    private static final Logger log = LoggerFactory.getLogger(GitUtil.class);

    public static void validateBranch(Repository repo, String ref) throws InvalidRefNameException, IOException {
        ref = GitUtil.denormalizeBranchName(ref);
        if(!ref.matches(Constants.HEAD) && !Repository.isValidRefName(ref)) {
            throw new InvalidRefNameException(ref + " is not a valid refname!");
        }

        ObjectId objId = repo.resolve(ref);
        if(objId == null) {
            throw new InvalidRefNameException("branch " + ref + " does not exist");
        }
    }

    public static CommitInfo createFileCommit(Repository repo, PersonIdent committer, String refName, String filename, String fileContents, String commitMessage, GitReferenceUpdated referenceUpdated, Project.NameKey key) {
        log.info("Now entering the createFileCommit method");

        CommitInfo info = null;
        try(ObjectInserter oi = repo.newObjectInserter()) {

            String branchRefsHeads = GitUtil.denormalizeBranchName(refName);
            String branchAlone = GitUtil.normalizeBranchName(refName);

            ObjectId parent = repo.getRef(branchRefsHeads).getObjectId();

            info = new CommitInfo(); // info to return on success

            // Contents of the file becomes a blob
            byte[] grFile = fileContents.getBytes();

            // Create the file blob reference on the Git tree
            ObjectId fileId = oi.insert(Constants.OBJ_BLOB, grFile, 0, grFile.length);
            log.info("FileID: " + fileId.getName());

            // Add a tree object that represents the filename and metadata
            TreeFormatter formatter = new TreeFormatter();
            formatter.append(filename, FileMode.REGULAR_FILE, fileId);
            ObjectId treeId = oi.insert(formatter);
            log.info("TreeID: " + treeId.getName());

            // Commit the changes to the repo i.e. attach our local leaf to the repo tree
            PersonIdent person = new PersonIdent(repo);
            CommitBuilder cb = new CommitBuilder();
            cb.setParentId(parent);
            cb.setTreeId(treeId);
            cb.setAuthor(committer);
            cb.setCommitter(committer);
            cb.setMessage(commitMessage);
            ObjectId commitId = oi.insert(cb);
            log.info("CommitID: " + commitId.getName());

            // Get relevant info to send back to user
            info.commit = commitId.abbreviate(7).name();
            info.message = cb.getMessage();

            // Flush to inform the framework of the commit
            oi.flush();

            RefUpdate ru = repo.updateRef(branchRefsHeads);
//            ru.setForceUpdate(true);
            ru.setRefLogIdent(committer);
            ru.setNewObjectId(commitId);
//            ru.setExpectedOldObjectId(ObjectId.zeroId());
            ru.setRefLogMessage("commit: " + commitMessage, false);

            RefUpdate.Result result = ru.update();
            log.info("Result: " + result.name());
            switch (result) {
                case NEW:
                case FAST_FORWARD:
                case FORCED:
                    referenceUpdated.fire(key, ru);
                    break;
                default: {
                    throw new IOException(String.format("Failed to create ref: %s", result.name()));
                }
            }
        } catch(IOException ioe) {
            log.error("Unable to create commit", ioe);
        }

        return info;
    }

    public static String normalizeBranchName(String refName, boolean ignoreHead) {

        if(ignoreHead && refName.matches(Constants.HEAD)) {
            log.info("normalizeBranchName::refName was " + refName);
            return refName;
        }

        refName = removePrecedingSlashes(refName);
        refName = refName.replace(Constants.R_HEADS, "");

        log.info("normalizeBranchName::refname was " + refName);
        return refName;
    }

    public static String normalizeBranchName(String refName) {
        return GitUtil.normalizeBranchName(refName, true);
    }

    public static String denormalizeBranchName(String refName, boolean ignoreHead) {

        if(ignoreHead && refName.matches(Constants.HEAD)) {
            log.info("denormalizeBranchName::refName was " + refName);
            return refName;
        }

        refName = removePrecedingSlashes(refName);

        // If it doesn't begin with refs/heads/ make it so...
        if(!refName.startsWith(Constants.R_HEADS)) {
            refName = Constants.R_HEADS + refName;
        }
        log.info("denormalizeBranchName::refName was " + refName);
        return refName;
    }

    public static String denormalizeBranchName(String refName) {
        return GitUtil.denormalizeBranchName(refName, true);
    }

    private static String removePrecedingSlashes(String refName) {

        // remove all prepended slashes
        while (refName.startsWith("/")) {
            refName = refName.substring(1);
        }

        return refName;
    }
}
