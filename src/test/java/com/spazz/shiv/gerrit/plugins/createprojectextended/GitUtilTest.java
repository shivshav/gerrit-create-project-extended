package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.junit.TestRepository;
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
    private Repository repo;
    private boolean isWindows = false;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        git = new Git(db);
        repo = git.getRepository();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void validateBranch_shortRef_validated() throws Exception {
        String existingBranch = "master";
        writeTrashFile("testfile", "test file to create head reference");

        Ref ref = git.branchCreate()
                .setName(existingBranch)
                .call();
        GitUtil.validateBranch(repo, existingBranch);

    }

    @Test(expected=RefNotFoundException.class)
    public void validateBranch_nonExistantBranch_exceptionThrown() throws Exception{
        String nonExistantBranch = "nonex";
        String existingBranch = "master";
        writeTrashFile("testfile", "test file to create head reference");

        Ref ref = git.branchCreate()
                .setName(existingBranch)
                .call();

        GitUtil.validateBranch(repo, nonExistantBranch);
    }

    @Test
    public void validateBranch_invalidRef_exceptionThrown() throws Exception {

        assertTrue(false);
    }

    @Test
    public void createFileCommit_simpleBareRepository_newFileCommitted() throws Exception {
        assertTrue(false);
    }

    @Test
    public void normalizeBranchName_allValidVariations_hasRefsHeadsPrefix() throws Exception {
        String shortName = "master";
        String expectedName = Constants.R_HEADS + shortName;
        String longName = expectedName;
        String precedingSlashesName = "////" + longName;
        String headShort = Constants.HEAD;
        String headLong = Constants.R_HEADS + headShort;

        // Should prepend all strings given with correct refs/heads/ prefix
        assertEquals(expectedName, GitUtil.normalizeBranchName(shortName));
        assertEquals(expectedName, GitUtil.normalizeBranchName(longName));
        assertEquals(expectedName, GitUtil.normalizeBranchName(precedingSlashesName));

        // Should ignore any refs with HEAD by default
        assertEquals(Constants.HEAD, GitUtil.denormalizeBranchName(headShort));
        assertEquals(Constants.HEAD, GitUtil.denormalizeBranchName(headLong));
    }

    @Test
    public void normalizeBranchName_dontIgnoreHead_headNoPrefix() throws Exception {
        String headShort = Constants.HEAD;
        String headLong = Constants.R_HEADS + headShort;

        // Shouldn't ignore any refs with HEAD and return with refs/heads/HEAD
        assertEquals(Constants.HEAD, GitUtil.denormalizeBranchName(headShort, false));
        assertEquals(Constants.HEAD, GitUtil.denormalizeBranchName(headLong, false));
    }

    @Test
    public void denormalizeBranchName_allValidVariations_isShortName() throws Exception {
        String shortName = "master";
        String expectedName = shortName;
        String longName = Constants.R_HEADS + shortName;
        String precedingSlashesName = "////" + longName;

        String head = Constants.HEAD;
        String longHead = Constants.R_HEADS + head;

        // Should remove all prefixes and return short name for ref
        assertEquals(expectedName, GitUtil.denormalizeBranchName(shortName));
        assertEquals(expectedName, GitUtil.denormalizeBranchName(longName));
        assertEquals(expectedName, GitUtil.denormalizeBranchName(precedingSlashesName));

        // Should ignore any refs with HEAD by default
        assertEquals(Constants.HEAD, GitUtil.denormalizeBranchName(head));
        assertEquals(Constants.R_HEADS + Constants.HEAD, GitUtil.denormalizeBranchName(longHead));
    }

    @Test
    public void denormalizeBranchName_dontIgnoreHead_isHeadShortName() throws Exception {
        String headShort = Constants.HEAD;
        String headLong = Constants.R_HEADS + headShort;

        // Shouldn't ignore any refs with HEAD and return with HEAD
        assertEquals(Constants.HEAD, GitUtil.denormalizeBranchName(headShort, false));
        assertEquals(Constants.HEAD, GitUtil.denormalizeBranchName(headLong, false));
    }
}