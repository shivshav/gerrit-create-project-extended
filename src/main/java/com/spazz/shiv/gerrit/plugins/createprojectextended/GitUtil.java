package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.server.git.GitRepositoryManager;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
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
