package com.spazz.shiv.gerrit.plugins.creategr;

import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.events.NewProjectCreatedListener;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.CreateBranch;
import com.google.inject.Inject;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by shivneil on 5/20/15.
 */
public class ProjectListenerTest implements NewProjectCreatedListener, GitReferenceUpdatedListener {
    //TODO: Get project name
    //TODO: Create develop branch
    //TODO: Update HEAD to develop
    //TODO: Create gitreview file
    //TODO: Commit to develop (bypass review?)
    private final GitRepositoryManager repoManager;
    private static final Logger log = LoggerFactory.getLogger(ProjectListenerTest.class);

    private final static String NEW_REF_CREATED_ID = "0000000000000000000000000000000000000000";
    private final static String GITREVIEW_FILENAME = ".gitreview";
    private final String branchToCreate = "develop";
    private final String revToUse = "master";

    private CreateBranch.Factory createBranch;

    private boolean projectWasCreated = false;
    private String createdProjectName;
    private String createdProjectHead;


    //    @Inject
//    public ProjectListenerTest(GerritApi gerritApi) {
//        this.gApi = gerritApi;
//        log.info("The Listener was called!");
//    }
    @Inject
    public ProjectListenerTest(GitRepositoryManager repoManager) {
        this.repoManager = repoManager;
        log.info("New Project Creation Listener Fired");
    }

    @Override
    public void onNewProjectCreated(NewProjectCreatedListener.Event event) {
        log.info("Entered onNewProjectCreated");

        this.createdProjectName = event.getProjectName();
        this.createdProjectHead = event.getHeadName();
        if(this.createdProjectName != null && this.createdProjectHead != null) {
            this.projectWasCreated = true;
            log.info("New Project is: " + this.createdProjectName + " with HEAD@{" + this.createdProjectHead + "}");
            String key = this.createdProjectName + CreateProjectExtendedManager.MAP_KEY_SEPARATOR + this.createdProjectHead;
            CreateProjectExtendedManager.getProjectsInCreation().put(key, this);
        }

//
//
//        Project.NameKey newProjNameKey = new Project.NameKey(projName);
//        try {
//            newRepo = repoManager.openRepository(newProjNameKey);
//        } catch (RepositoryNotFoundException rnfe) {
//            System.out.println("Repository Not Found");
//            return;
//        } catch (IOException ioe) {
//            System.out.println("IO Exception");
//            return;
//        } finally {
//
//        }
//        Ref head;
//        try {
//            head = newRepo.getRef(projHead);
//            log.info("The HEAD is " + head.getName());
//        } catch (IOException e) {
//            log.info(e.getMessage());
////            e.printStackTrace();
//        }
//
//        newRepo.getP
//        try {
//            BranchInput bi = new BranchInput();
//            bi.revision = null;
//
//
//            gApi.projects().name(projName)
//                    .branch(branchToCreate).create(bi);
//        } catch (RestApiException rae) {
//            log.info(rae.getMessage());
//        }
//        CreateBranch.Input newBranch = new CreateBranch.Input();
//        newBranch.ref = branchToCreate;
//        newBranch.revision = projHead;
//
//        try {
//            createBranch.create(branchToCreate).apply(prsrc, newBranch);
//        } catch (BadRequestException e) {
//            log.info(e.getMessage());
//        } catch (AuthException e) {
//            log.info(e.getMessage());
//        } catch (ResourceConflictException e) {
//            log.info(e.getMessage());
//        } catch (IOException e) {
//            log.info(e.getMessage());
//        }
    }

    @Override
    public void onGitReferenceUpdated(GitReferenceUpdatedListener.Event event) {

        String newObj = event.getNewObjectId();
        String oldObj = event.getOldObjectId();
        String projName = event.getProjectName();
        String refName = event.getRefName();
        log.info("Project: " + projName + ", refName: " + refName + ", oldObj: " + oldObj + ", newObj: " + newObj);

        String key = projName + CreateProjectExtendedManager.MAP_KEY_SEPARATOR + refName;
        if (CreateProjectExtendedManager.getProjectsInCreation().containsKey(key)) {
            CreateProjectExtendedManager.getProjectsInCreation().remove(key);
            this.projectWasCreated = true;
            this.createdProjectName = projName;
            this.createdProjectHead = refName;

            Project.NameKey newProjNameKey = new Project.NameKey(projName);
            try {
                final Repository newRepo = repoManager.openRepository(newProjNameKey);
                createDevelopBranch(newRepo);
                addGitReviewFile(newRepo);

            } catch (IOException ioe) {
                log.error(ioe.getMessage());
            }
        }
    }
    // TODO: Add commitBuilder stuff to create the gitreview file (look at createemptycommits code in PerformCreateProject.java of Gerrit)
    private void createDevelopBranch(Repository repo) {
        Git git = new Git(repo);
        log.info("The repo dir is: " + repo.getDirectory().getName());
        try {
            Ref r = git.branchCreate()
                    .setName(branchToCreate)
                    .call();
            log.info("Branch " + r.getName() + " created.");
            updateHead(repo, r.getName(), true, false);
            log.info("The HEAD is at " + r.getName());

        } catch(RefAlreadyExistsException raee) {
            log.error("That branch already exists!");
        } catch(RefNotFoundException rnfe) {
            log.error("Ref wasnt found?");
        } catch(InvalidRefNameException irne) {
            log.error("That ref name is invalid");
        } catch(GitAPIException gapie) {
            log.error("Generic Git API Exception");
        } catch(IOException ioe) {
            log.error("General IO Exception from updateHead");
        } finally {
            git.close();
        }
    }

    private RefUpdate.Result updateHead(Repository repo, String newHead, boolean force, boolean detach) throws IOException {
        RefUpdate refUpdate = repo.getRefDatabase().newUpdate(Constants.HEAD, detach);
        refUpdate.setForceUpdate(force);
        return refUpdate.link(newHead);
    }

    private void addGitReviewFile(Repository repo) {
        BufferedWriter writer = null;

        try {
            String filename = repo.getDirectory() + "/" + GITREVIEW_FILENAME;
            File grFile = new File(filename);

            writer = new BufferedWriter(new FileWriter(grFile));
            writer.write("Hello world!");
            Git git = new Git(repo);

            git.add().addFilepattern(filename);
            git.commit().setMessage("Commit worked?").call();

        } catch (IOException ioe) {
            log.error(ioe.getMessage());
        } catch(GitAPIException gapie) {
            log.error(gapie.getMessage());
        } finally {
            try {
                if(writer != null) {
                    writer.close();
                }
            } catch(IOException ioe){
                log.error(ioe.getMessage());
            }
        }
        //RevBlob

//        try(ObjectInserter oi = repo.newObjectInserter()) {
//            CommitBuilder cb = new CommitBuilder();
//            cb.get;
//
//        } catch (IOException ioe) {
//            log.error("Failed to create gitreview commit");
//        }


    }



}
