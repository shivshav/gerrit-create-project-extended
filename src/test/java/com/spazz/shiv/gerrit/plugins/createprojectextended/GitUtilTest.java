package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by shivneil on 11/22/15.
 */
public class GitUtilTest extends RepositoryTestCase{
    @Inject
//    private InMemoryRepositoryManager repoManager;
    private static final String TEST_FILE_NAME = "test-file";
    private Git git;
    private boolean isWindows = false;

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testValidBranchCheck() throws Exception {
        String existingBranch = "master";
        writeTrashFile("testfile", "test file to create head reference");
        Git git = new Git(db);
        Ref ref = git.branchCreate()
                .setName(existingBranch)
                .call();
        Repository repo = git.getRepository();

        GitUtil.validateBranch(repo, existingBranch);

    }

    @Test(expected=RefNotFoundException.class)
    public void testNonExistantBranchCheck() throws Exception{
        String nonExistantBranch = "nonex";
        String existingBranch = "master";

        Git git = new Git(db);
        Ref ref = git.branchCreate()
                .setName(existingBranch)
                .call();
        Repository repo = git.getRepository();

        GitUtil.validateBranch(repo, nonExistantBranch);
    }

    @Test
    public void testCreateFileCommit() throws Exception {
        assertTrue(false);
    }

    @Test
    public void testNormalizeBranchNameToRefsHeads() throws Exception {
        String shortName = "master";
        String expectedName = Constants.R_HEADS + shortName;
        String longName = expectedName;
        String precedingSlashesName = "////" + longName;
        String headShort = Constants.HEAD;
        String headLong = Constants.R_HEADS + headShort;

        assertEquals(expectedName, GitUtil.normalizeBranchName(shortName));
        assertEquals(expectedName, GitUtil.normalizeBranchName(longName));
        assertEquals(expectedName, GitUtil.normalizeBranchName(precedingSlashesName));
        assertEquals(headLong, GitUtil.normalizeBranchName(headLong));
        assertEquals(headShort, GitUtil.normalizeBranchName(headShort));
    }

    @Test
    public void testNormalizeBranchNameToRefsHeadsDontIgnoreHEAD() throws Exception {
        assertTrue(false);
    }

    @Test
    public void testDenormalizeBranchNameToShortName() throws Exception {
        String shortName = "master";
        String expectedName = shortName;
        String longName = Constants.R_HEADS + shortName;
        String precedingSlashesName = "////" + longName;

        assertEquals(expectedName, GitUtil.denormalizeBranchName(shortName));
        assertEquals(expectedName, GitUtil.denormalizeBranchName(longName));
        assertEquals(expectedName, GitUtil.denormalizeBranchName(precedingSlashesName));
    }

    @Test
    public void testDenormalizeBranchNameToShortNameDontIgnoreHEAD() throws Exception {
        assertTrue(false);
    }
}