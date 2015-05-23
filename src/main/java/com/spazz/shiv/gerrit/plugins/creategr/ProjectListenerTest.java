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
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final String branchToCreate = "develop";
    private final String revToUse = "master";

    private CreateBranch.Factory createBranch;

    private static boolean projectWasCreated = false;
    private static String createdProjectName;
    private static String createdProjectHead;


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

        createdProjectName = event.getProjectName();
        createdProjectHead = event.getHeadName();
        if(createdProjectName != null && createdProjectHead != null) {
            projectWasCreated = true;
            log.info("New Project is: " + createdProjectName + " with HEAD@{" + createdProjectHead + "}");
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
        log.info("Git Ref Updated Listener Fired! with createdProjectName: " + createdProjectName + " And projectWasCreated=" + projectWasCreated);


        if(projectWasCreated) {
            String newObj = event.getNewObjectId();
            String oldObj = event.getOldObjectId();
            String projName = event.getProjectName();
            String refName = event.getRefName();
            log.info("Project: " + projName + ", refName: " + refName + ", oldObj: " + oldObj + ", newObj: " + newObj);

            if (projName.matches(createdProjectName) && refName.matches(createdProjectHead)
                    && oldObj.matches(NEW_REF_CREATED_ID)) {

                Project.NameKey newProjNameKey = new Project.NameKey(projName);
                try {
                    final Repository newRepo = repoManager.openRepository(newProjNameKey);


                    try{
                        final Ref head = newRepo.getRef(createdProjectHead);
                        log.info("The HEAD is " + head.getName());
                    } catch (RepositoryNotFoundException rnfe) {
                        log.error("Repository Not Found");
                    } catch (IOException ioe) {
                        log.error("IO Exception");
                    } finally {
                        newRepo.close();
                        createdProjectHead = null;
                        createdProjectName = null;
                        projectWasCreated = false;
                    }
                } catch (IOException ioe) {
                    log.error(ioe.getMessage());
                }
            }
        }


    }
    // TODO: Add commitBuilder stuff to create the gitreview file (look at createemptycommits code in PerformCreateProject.java of Gerrit)
    private void createDevelopBranch(Repository repo) {
        Git git = new Git(repo);

        try {
            git.branchCreate()
                    .setName(branchToCreate)
                    .call();
        } catch(RefAlreadyExistsException raee) {
            log.error("That branch already exists!");
        }
    }
}
