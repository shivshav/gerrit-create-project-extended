package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.gerrit.extensions.common.CommitInfo;
import com.google.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

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

        // Create the trash file
        writeTrashFile("testfile", "test file to create head reference");
        git.add().addFilepattern("testfile").call();
        git.commit().setMessage("Initial commit").call();

        // Test that the ref exists
        GitUtil.validateBranch(repo, existingBranch);

    }

    @Test(expected=RefNotFoundException.class)
    public void validateBranch_nonExistantBranch_exceptionThrown() throws Exception{
        String nonExistantBranch = "nonex";
        // Create the trash file
        writeTrashFile("testfile", "test file to create head reference");
        git.add().addFilepattern("testfile").call();
        git.commit().setMessage("Initial commit").call();

        GitUtil.validateBranch(repo, nonExistantBranch);
    }

//    @Test(expected = InvalidRefNameException.class)
//    public void validateBranch_invalidRef_exceptionThrown() throws Exception {
//        String invalidRef = "refs/invalid/hithere";
//        GitUtil.validateBranch(repo, invalidRef);
//    }

    @Test
    public void createFileCommit_simpleBareRepository_newFileCommitted() throws Exception {
        String commitBranch = "master";
        String fileContents = "file commit_test";
        db = createBareRepository();
        PersonIdent ident = new PersonIdent(db);

        Map<String, String> commitMap = new HashMap<>();

        // Create a basic commit
        commitMap.put("refName", commitBranch);
        commitMap.put("filename", "commit_test");
        commitMap.put("fileContents", fileContents);
        commitMap.put("commitMessage", "Committed test file");

        CommitInfo info = GitUtil.createFileCommit(db, ident, commitMap);

        // Ensure that the ref we asked to be updated was updated to the new commit
        assertEquals(info.commit, db.getRef(commitBranch).getObjectId().abbreviate(7).name());

        File tempWorkingTree = File.createTempFile("TestingWorkTree", "");
        tempWorkingTree.delete();

        // Clone the bare repo so we can examine it
        git = Git.cloneRepository()
                .setURI(db.getDirectory().getAbsolutePath())
                .setBranch(commitBranch)
                .setDirectory(tempWorkingTree)
                .call();

        // Ensure that the work tree is clean
        assertTrue(git.status().call().isClean());
        File file = new File(git.getRepository().getWorkTree(), "commit_test");

        // Ensure the file actually exists
        assertTrue(file.exists());

        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileChars = new byte[(int) file.length()];
        fileInputStream.read(fileChars);
        fileInputStream.close();

        // Ensure that the actual file contents are the same
        assertArrayEquals(fileChars, fileContents.getBytes());
    }

    @Test
    public void createFileCommit_bareWithCommits_newFileCommitted() throws Exception {
        String commitBranch = "master";
        String parentFilename = "commit_test";
        String fileContents = "file commit_test";
        db = createBareRepository();
        PersonIdent ident = new PersonIdent(db);

        Map<String, String> commitMap = new HashMap<>();

        // Create the first file commit
        commitMap.put("refName", commitBranch);
        commitMap.put("filename", parentFilename);
        commitMap.put("fileContents", fileContents);
        commitMap.put("commitMessage", "Committed test file");

        CommitInfo parent = GitUtil.createFileCommit(db, ident, commitMap);


        String parentCommitString = parent.commit;

        // Create another commit to see if there's anything janky going on in a non-empty repo commit
        String filename = "new_commit_test";
        String newFileContents = "new file new_commit_test";
        commitMap.put("filename", filename);
        commitMap.put("fileContents", newFileContents);
        commitMap.put("commitMessage", "Committed new test file");
        CommitInfo info = GitUtil.createFileCommit(db, ident, commitMap);

        // Ensure that the ref we asked to be updated was updated to the new commit
        ObjectId commitObjectId = db.getRef(commitBranch).getObjectId();
        assertEquals(info.commit, commitObjectId.abbreviate(7).name());

        // Ensure that the parent of the ref is our previous commit

        RevWalk rw = new RevWalk(db);
        RevCommit commit = rw.parseCommit(commitObjectId);
        RevCommit parentCommit = commit.getParent(0);
        assertEquals(parentCommitString, parentCommit.abbreviate(7).name());



        File tempWorkingTree = File.createTempFile("TestingWorkTree", "");
        tempWorkingTree.delete();

        // Clone the bare repo so we can examine it
        git = Git.cloneRepository()
                .setURI(db.getDirectory().getAbsolutePath())
                .setBranch(commitBranch)
                .setDirectory(tempWorkingTree)
                .call();

        // Ensure that the work tree is clean
        assert (git.status().call().isClean());

        File oldFile = new File(git.getRepository().getWorkTree(), parentFilename);
        File file = new File(git.getRepository().getWorkTree(), filename);

        // Ensure the file actually exists
        assertTrue(file.exists());
        assertTrue(oldFile.exists());

        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileChars = new byte[(int) file.length()];
        fileInputStream.read(fileChars);
        fileInputStream.close();

        // Ensure that the actual file contents are the same
        assertArrayEquals(fileChars, newFileContents.getBytes());

    }
    @Test
    public void normalizeBranchName_allValidVariations_hasRefsHeadsPrefix() throws Exception {
        String shortName = "master";
        String expectedName = Constants.R_HEADS + shortName;//refs/heads/master
        String longName = expectedName;
        String precedingSlashesName = "////" + longName;
//        String headShort = Constants.HEAD;
//        String headLong = Constants.R_HEADS + headShort;

        // Should prepend all strings given with correct refs/heads/ prefix
        assertEquals(expectedName, GitUtil.normalizeBranchName(shortName));
        assertEquals(expectedName, GitUtil.normalizeBranchName(longName));
        assertEquals(expectedName, GitUtil.normalizeBranchName(precedingSlashesName));

        // Should ignore any refs with HEAD by default
//        assertEquals(Constants.HEAD, GitUtil.normalizeBranchName(headShort));
//        assertEquals(Constants.HEAD, GitUtil.normalizeBranchName(headLong));
    }

    @Test
    public void normalizeBranchName_ignoreNormalizingHEAD_HeadNoPrefix() throws Exception {
        String expectedName = Constants.HEAD;
        String branchName = Constants.HEAD;

        // Should return the HEAD without altering the string
        assertEquals(expectedName, GitUtil.normalizeBranchName(branchName));
    }

    @Test(expected = InvalidRefNameException.class)
    public void normalizeBranchName_refsHeadsHEAD_throwInvalidRefNameException() throws Exception {
        String invalidRefName = Constants.R_HEADS + Constants.HEAD;

        // Make sure we throw an exception if we get refs/heads/HEAD as an argument
        GitUtil.normalizeBranchName(invalidRefName);
    }

    @Test
    public void normalizeBranchName_dontIgnoreHead_headNoPrefix() throws Exception {
        String headShort = Constants.HEAD;
        String headLong = Constants.R_HEADS + headShort;

        // Shouldn't ignore any refs with HEAD and return with HEAD
        assertEquals(Constants.HEAD, GitUtil.normalizeBranchName(headShort));
    }

    @Test
    public void denormalizeBranchName_allValidVariations_isShortName() throws Exception {
        String shortName = "master";
        String expectedName = shortName;
        String longName = Constants.R_HEADS + shortName;
        String precedingSlashesName = "////" + longName;

        String head = Constants.HEAD;

        // Should remove all prefixes and return short name for ref
        assertEquals(expectedName, GitUtil.denormalizeBranchName(shortName));
        assertEquals(expectedName, GitUtil.denormalizeBranchName(longName));
        assertEquals(expectedName, GitUtil.denormalizeBranchName(precedingSlashesName));

        // Should ignore any refs with HEAD by default
        assertEquals(Constants.HEAD, GitUtil.denormalizeBranchName(head));
    }

    @Test(expected = InvalidRefNameException.class)
    public void denormalizeBranchName_refsHeadsHEAD_throwsInvalidRefNameException() throws Exception {
        String invalidHeadRef = Constants.R_HEADS + Constants.HEAD;

        GitUtil.denormalizeBranchName(invalidHeadRef);
    }

    @Test
    public void denormalizeBranchName_dontModifyHEAD_isHeadShortName() throws Exception {
        String headShort = Constants.HEAD;

        // Shouldn't ignore any refs with HEAD and return with HEAD
        assertEquals(Constants.HEAD, GitUtil.denormalizeBranchName(headShort));
    }
}