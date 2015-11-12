package com.spazz.shiv.gerrit.plugins.createprojectextended;

import org.eclipse.jgit.lib.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivneil on 11/11/15.
 */
public class GitUtil {
    private static final Logger log = LoggerFactory.getLogger(GitUtil.class);

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
