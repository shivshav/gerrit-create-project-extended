package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.gerrit.extensions.common.CommitInfo;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Created by shivneil on 11/11/15.
 */
public class GitUtil {


    private static final Logger log = LoggerFactory.getLogger(GitUtil.class);

    public static void validateBranch(Repository repo, String ref) throws InvalidRefNameException, RefNotFoundException, IOException {
        ref = GitUtil.normalizeBranchName(ref);
        if(!ref.matches(Constants.HEAD) && !Repository.isValidRefName(ref)) {
            throw new InvalidRefNameException(ref + " is not a valid refname!");
        }

        try {
            ObjectId objId = repo.resolve(ref);
            if (objId == null) {
                throw new RefNotFoundException("branch " + ref + " does not exist");
            }
        } catch (RefNotFoundException rnfe) {
            throw new RefNotFoundException("branch " + ref + " does not exist");
        } catch (IOException ioe) {
            throw new IOException("IOException" + ioe.getMessage());
        }
    }

    private static void createFileCommit(Repository repo, PersonIdent committer, String refName, String fileName, String fileContents) {

        try(ObjectInserter oi = repo.newObjectInserter()) {

            // Create an in memory index that's initially empty
            DirCache index = DirCache.newInCore();

//            DirCache index = new DirCache(repo.getDirectory(), repo.getFS());
            DirCacheBuilder builder = index.builder();

//            index.read();
//            boolean locked = index.lock();
//            DirCache index = repo.lockDirCache();

            refName = normalizeBranchName(refName);
            ObjectId branchRef = repo.resolve(refName);
            RevWalk rw = new RevWalk(repo);
            RevCommit parent = rw.parseCommit(branchRef);

            byte[] file = fileContents.getBytes();
//            byte[] second = "Hey its the ignore".getBytes("UTF-8");
            ObjectId fileId = oi.insert(Constants.OBJ_BLOB, file, 0, file.length);
//            ObjectId secondfileId = oi.insert(Constants.OBJ_BLOB, second, 0, second.length);

            RevTree commitTree = rw.parseTree(parent.getTree().getId());
            rw.dispose();

//            TreeFormatter formatter = new TreeFormatter();
            TreeWalk tw = new TreeWalk(repo);
            tw.addTree(commitTree);

            boolean fileAlreadyCreated = false;
            while(tw.next()) {
                if(tw.getObjectId(0) != ObjectId.zeroId()) {
                    if(tw.getNameString().matches(fileName)) {
//                        fileAlreadyCreated = true;
//                        fileId = tw.getObjectId(0);
//                        RevBlob blob;
                        log.info(tw.getNameString() + " found");
                        continue;
                    }

                    DirCacheEntry currentEntry = new DirCacheEntry(tw.getPathString());
                    currentEntry.setFileMode(tw.getFileMode(0));
                    currentEntry.setObjectId(tw.getObjectId(0));
                    builder.add(currentEntry);
//                    formatter.append(tw.getNameString(), tw.getFileMode(0), tw.getObjectId(0));
                }
                else {
                    log.info("Found Zero ID object");
                }
            }

            DirCacheEntry newEntry = new DirCacheEntry(fileName);
            newEntry.setFileMode(FileMode.REGULAR_FILE);
            newEntry.setObjectId(fileId);
            builder.add(newEntry);
//            formatter.append(fileName, FileMode.REGULAR_FILE, fileId);
//            formatter.append(".gitignore", FileMode.REGULAR_FILE, secondfileId);
//            ObjectId treeId = oi.insert(formatter);
            builder.finish();
            ObjectId treeId = index.writeTree(oi);
            tw.close();

            CommitBuilder cb = new CommitBuilder();
            cb.setTreeId(treeId);
            cb.setParentId(parent);
            cb.setAuthor(committer);
            cb.setCommitter(committer);
            cb.setMessage("Hererskadsk");
            ObjectId commitId = oi.insert(cb);

//            index.write();
//            index.commit();
//            index.writeTree(oi);

            oi.flush();


            RefUpdate ru = repo.updateRef(refName);
            ru.setForceUpdate(true);
            ru.setRefLogIdent(committer);
            ru.setNewObjectId(commitId);
//            ru.setExpectedOldObjectId(parent.toObjectId());
            ru.setRefLogMessage("commit: " + cb.getMessage(), false);

            RefUpdate.Result result = ru.update();
            log.info("Result: " + result.name());
            switch (result) {
                case NEW:
                case FAST_FORWARD:
                case FORCED:
                    break;
                default: {
                    throw new IOException(String.format("Failed to create ref: %s", result.name()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidRefNameException e) {
            e.printStackTrace();
        }
    }
    
    
    // Map:
    //  refToCommitTo
    //  filename
    //  filecontents
    //  commitMessage

    public static CommitInfo createFileCommit(Repository repo, PersonIdent committer, Map<String, String> commit) {
        log.info("Now entering the createFileCommit method");


        CommitInfo info = null;
        try(ObjectInserter oi = repo.newObjectInserter()) {
            
            // Create an in memory empty index and builder for index modification
            DirCache index = DirCache.newInCore();
            DirCacheBuilder idxBuilder = index.builder();
            
            String branchRefsHeads = Constants.R_HEADS;
            String branchAlone = Constants.R_HEADS;
//            StringBuilder commitMessageBuilder = new StringBuilder();

            String refName = commit.get("refName");
            String filename = commit.get("filename");
            String fileContents = commit.get("fileContents");
            String commitMessage = commit.get("commitMessage");

            branchRefsHeads = GitUtil.normalizeBranchName(refName);
//            branchAlone = GitUtil.normalizeBranchName(refName);

            // Contents of the file becomes a blob
            byte[] grFile = fileContents.getBytes();

            // Create the file blob reference on the Git tree
            ObjectId fileId = oi.insert(Constants.OBJ_BLOB, grFile, 0, grFile.length);
            log.info("FileID: " + fileId.getName());

            // Add a tree object that represents the filename and metadata
//                formatter.append(filename, FileMode.REGULAR_FILE, fileId);

//            commitMessageBuilder.append(commitMessage).append(System.lineSeparator());

            // Check if this is the first commit to the repo
            Ref headRef = repo.getRef( Constants.HEAD );

            ObjectId parent = null;
            // If there are previous commits, add all files previously in the repo to the index
            if( headRef != null && headRef.getObjectId() != null ) {
                // we have previous commits
                RevWalk rw = new RevWalk(repo);
                parent = repo.getRef(branchRefsHeads).getObjectId();
                RevCommit parentCommit = rw.parseCommit(parent);

                RevTree parentTree = parentCommit.getTree();
                TreeWalk tw = new TreeWalk(repo);
                tw.addTree(parentTree);
                rw.dispose();

                while(tw.next()) {
                    if(tw.getObjectId(0) != ObjectId.zeroId()) {
                        if(tw.getNameString().matches(filename)) {
//                        fileAlreadyCreated = true;
//                        fileId = tw.getObjectId(0);
//                        RevBlob blob;
                            log.info(tw.getNameString() + " found");
                            continue;
                        }

                        DirCacheEntry currentEntry = new DirCacheEntry(tw.getPathString());
                        currentEntry.setFileMode(tw.getFileMode(0));
                        currentEntry.setObjectId(tw.getObjectId(0));
                        idxBuilder.add(currentEntry);
//                    formatter.append(tw.getNameString(), tw.getFileMode(0), tw.getObjectId(0));
                    }
                    else {
                        log.info("Found Zero ID object");
                    }

                }
                tw.close();


            }
//            TreeFormatter formatter = new TreeFormatter();
//            for (Map<String, String> commit :
//                    commits) {
//
//            }

//            String commitMessage = commitMessageBuilder.toString();

//            ObjectId parent = repo.getRef(branchRefsHeads).getObjectId();

            info = new CommitInfo(); // info to return on success


//            ObjectId treeId = oi.insert(formatter);
            DirCacheEntry newEntry = new DirCacheEntry(filename);
            newEntry.setFileMode(FileMode.REGULAR_FILE);
            newEntry.setObjectId(fileId);
            idxBuilder.add(newEntry);
            idxBuilder.finish();
            ObjectId treeId = index.writeTree(oi);
            log.info("TreeID: " + treeId.getName());

            // Commit the changes to the repo i.e. attach our local leaf to the repo tree
            CommitBuilder cb = new CommitBuilder();
            if (parent != null) {
                cb.setParentId(parent);
            }
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
            ru.setForceUpdate(true);
            ru.setRefLogIdent(committer);
            ru.setNewObjectId(commitId);
            if (parent != null) {
                ru.setExpectedOldObjectId(parent.toObjectId());
            }
            ru.setRefLogMessage("commit: " + commitMessage, false);

            RefUpdate.Result result = ru.update();
            log.info("Result: " + result.name());
//            switch (result) {
//                case NEW:
//                case FAST_FORWARD:
//                case FORCED:
//                    referenceUpdated.fire(key, ru);
//                    break;
//                default: {
//                    throw new IOException(String.format("Failed to create ref: %s", result.name()));
//                }
//            }
        } catch(IOException ioe) {
            log.error("Unable to create commit", ioe);
        } catch (InvalidRefNameException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static String denormalizeBranchName(String refName) throws InvalidRefNameException {
        //TODO: I need to make sure this always spits back HEAD for those cases which apply
        if(refName.matches(Constants.R_HEADS + Constants.HEAD)) {
            throw new InvalidRefNameException(Constants.R_HEADS + Constants.HEAD + " is not a valid ref name");
        }
        if(refName.matches(Constants.HEAD)) {
            log.info("denormalizeBranchName::refName was " + refName);
            return refName;
        }

        refName = removePrecedingSlashes(refName);
        refName = refName.replace(Constants.R_HEADS, "");

        log.info("denormalizeBranchName::refname was " + refName);
        return refName;
    }

//    public static String denormalizeBranchName(String refName) throws InvalidRefNameException {
//        return GitUtil.denormalizeBranchName(refName, true);
//    }

    public static String normalizeBranchName(String refName) throws InvalidRefNameException {
        if(refName.matches(Constants.R_HEADS + Constants.HEAD)) {
            throw new InvalidRefNameException(Constants.R_HEADS + Constants.HEAD + " is not a valid reference");
        }
        //TODO: I need to make sure this always spits back HEAD for those cases
        if(refName.matches(Constants.HEAD)) {
            log.info("normalizeBranchName::refName was " + refName);
            return refName;
        }

        refName = removePrecedingSlashes(refName);

        // If it doesn't begin with refs/heads/ make it so...
        if(!refName.startsWith(Constants.R_HEADS)) {
            refName = Constants.R_HEADS + refName;
        }
        log.info("normalizeBranchName::refName was " + refName);
        return refName;
    }

//    public static String normalizeBranchName(String refName) throws InvalidRefNameException {
//        return GitUtil.normalizeBranchName(refName, true);
//    }

    private static String removePrecedingSlashes(String refName) {

        // remove all prepended slashes
        while (refName.startsWith("/")) {
            refName = refName.substring(1);
        }

        return refName;
    }
}
