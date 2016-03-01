package com.spazz.shiv.gerrit.plugins.createprojectextended.rest;

import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.CapabilityScope;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.projects.ProjectInput;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.extensions.events.GitReferenceUpdated;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.project.ProjectsCollection;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.spazz.shiv.gerrit.plugins.createprojectextended.GitUtil;
import com.spazz.shiv.gerrit.plugins.createprojectextended.rest.CreateExtendedProject.ExtendedProjectInput;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by shivneil on 11/8/15.
 */
@RequiresCapability(value = GlobalCapability.CREATE_PROJECT, scope = CapabilityScope.CORE)
public class CreateExtendedProject implements RestModifyView<ConfigResource, ExtendedProjectInput> {

    private final static Logger log = LoggerFactory.getLogger(CreateExtendedProject.class);
    private final GitRepositoryManager repositoryManager;
    private final Provider<AddGitReview> gitReviewProvider;
    private final Provider<AddGitIgnore> gitIgnoreProvider;
    private final Provider<ProjectsCollection> projectProvider;
    private final MetaDataUpdate.User userProvider;
    private final GitReferenceUpdated referenceUpdate;

    static class ExtendedProjectInput extends ProjectInput {
        String head;
        AddGitReview.GitReviewInput gitReview;
        AddGitIgnore.GitIgnoreInput gitIgnore;
    }

    public interface Factory {
        CreateExtendedProject create(String projName);
    }

    private String pluginName;
    private String name;
    private final Provider<CurrentUser> currentUserProvider;
    private final GerritApi api;

    @Inject
    CreateExtendedProject(@PluginName String pluginName,
                          @Assisted String name,
                          Provider<CurrentUser> currentUserProvider,
                          Provider<ProjectsCollection> projectsProvider,
                          MetaDataUpdate.User userProvider,
                          GerritApi api,
                          Provider<AddGitReview> gitReviewProvider,
                          Provider<AddGitIgnore> gitIgnoreProvider,
                          GitRepositoryManager repositoryManager,
                          GitReferenceUpdated referenceUpdated) {
//        log.info("Constructor::hey it fired!");

        this.pluginName = pluginName;
        this.name = name;
        this.currentUserProvider = currentUserProvider;
        if(projectsProvider == null) {
            log.error("Hey the projectsProvider was null!");
        }
        this.projectProvider = projectsProvider;
        this.userProvider = userProvider;
        this.api = api;
        this.gitReviewProvider = gitReviewProvider;
        this.gitIgnoreProvider = gitIgnoreProvider;
        this.repositoryManager = repositoryManager;
        this.referenceUpdate = referenceUpdated;
    }

    @Override
    public Response<ExtendedProjectInfo> apply(ConfigResource configResource, ExtendedProjectInput extendedProjectInput)
            throws RestApiException, OrmException {

        // Make sure we have a valid user
        CurrentUser user = currentUserProvider.get();
        if(user == null || !user.isIdentifiedUser()) {
            throw new AuthException("Authentication required");
        }

        // Let's not complicate things
        if(extendedProjectInput.name != null  && !extendedProjectInput.name.matches(name)) {
            throw new BadRequestException("name must match URL");
        }

        if(extendedProjectInput.branches == null) {
            extendedProjectInput.branches = new ArrayList<>();
            extendedProjectInput.branches.add("master");
        }

        // Ensure that if HEAD is set, it is set to a branch inside of the branches input fields
        if(extendedProjectInput.head != null && !extendedProjectInput.branches.contains(extendedProjectInput.head)) {
            throw new BadRequestException("HEAD must be set to one of the values of the 'branches' key");
        }

        // Create the project through the Gerrit REST API
        ExtendedProjectInfo info = new ExtendedProjectInfo();

        Project.NameKey nameKey = new Project.NameKey(name);
        try {
            log.info("Creating new extended project " + name);
            info.projectInfo = api.projects().name(name).create(extendedProjectInput).get();
            log.info("Project Created");

        } catch (RestApiException rae) {
            log.error(rae.getMessage());
            throw rae;
        }

        // Only check extended input params if user wants to create empty commits.
        // Should maintain compatibility with official API
        log.info("Create Empty Commit is..." + extendedProjectInput.createEmptyCommit);

        if(extendedProjectInput.createEmptyCommit) {
            log.info("Empty commit was created");

            // Move head before we make any commits
            if(extendedProjectInput.head != null) {
                try {
                    log.info("Attempting to move HEAD to " + extendedProjectInput.head);
                    Repository repo = repositoryManager.openRepository(nameKey);
                    info.head = updateHead(repo, extendedProjectInput.head, false, false);
                } catch (RepositoryNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new OrmException("Unable to read from the repository");
                }
            }

            ProjectResource createdProject = null;
            // Do GitReview stuff
            if(extendedProjectInput.gitReview != null) {
                log.info("Adding gitreview to " + name);
                try {
                    createdProject = projectProvider.get().parse(name);
                    Response<AddGitReview.GitReviewInfo> reviewInfoResponse = gitReviewProvider.get().apply(createdProject, extendedProjectInput.gitReview);
                    info.gitReviewInfo = reviewInfoResponse.value();
                } catch (IOException ioe) {
                    log.error("GitReview IOException::" + ioe.getMessage());
                }
            }

            // Do GitIgnore stuff
            if (extendedProjectInput.gitIgnore != null) {
                log.info("Adding gitreview to " + name);
                if(createdProject == null) {
                    try {
                        createdProject = projectProvider.get().parse(name);
                    } catch (IOException ioe) {
                        log.error("GitIgnore IOException::" + ioe.getMessage());
                    }
                }
                Response<AddGitIgnore.GitIgnoreInfo> gitIgnoreInfoResponse = gitIgnoreProvider.get().apply(createdProject, extendedProjectInput.gitIgnore);
                info.gitignoreInfo = gitIgnoreInfoResponse.value();

            }

//            try {
//                createdProject = projectProvider.get().parse(name);
//                Repository repo = repositoryManager.openRepository(nameKey);
////                List<Map<String, String>> commitMaps = new ArrayList<>(2);
//                if(extendedProjectInput.gitReview != null) {
//                    log.info("Found gitreview arguments");
//                    // Map:
//                    //  refToCommitTo
//                    //  filename
//                    //  filecontents
//                    //  commitMessage
//                    Map<String, String> reviewMap = new HashMap<>();
//                    reviewMap.put("refName", extendedProjectInput.gitReview.branch);
//                    reviewMap.put("filename", ".gitreview");
//                    reviewMap.put("fileContents", "Heres what goes in the gitreview file");
//                    reviewMap.put("commitMessage", extendedProjectInput.gitReview.commitMessage);
//
//                    log.info(reviewMap.toString());
//                    log.info("creating review file commit");
//                    CommitInfo cInfo = GitUtil.createFileCommit(repo, userProvider.getUserPersonIdent(), referenceUpdate, nameKey, reviewMap);
//                    info.gitReviewInfo = new AddGitReview.GitReviewInfo();
//                    info.gitReviewInfo.commitId = cInfo.commit;
//                    info.gitReviewInfo.commitMessage = cInfo.message;
//
////                    commitMaps.add(reviewMap);
//                }
//
//                if(extendedProjectInput.gitIgnore != null) {
//                    log.info("Found gitignore arguments");
//                    Map<String, String> ignoreMap = new HashMap<>();
//                    ignoreMap.put("refName", extendedProjectInput.gitIgnore.branch);
//                    ignoreMap.put("filename", ".gitignore");
//                    ignoreMap.put("fileContents", "Here's what goes in the gitignore file");
//                    ignoreMap.put("commitMessage", extendedProjectInput.gitIgnore.commitMessage);
//                    log.info(ignoreMap.toString());
//                    log.info("creating ignore file commit");
//                    CommitInfo cInfo = GitUtil.createFileCommit(repo, userProvider.getUserPersonIdent(), referenceUpdate, nameKey, ignoreMap);
//                    info.gitignoreInfo = new AddGitIgnore.GitIgnoreInfo();
//                    info.gitignoreInfo.commitId = cInfo.commit;
//                    info.gitignoreInfo.commitMessage = cInfo.message;
////                    commitMaps.add(ignoreMap);
//                }
//
//
////                if(commitMaps.size() > 0) {
////                    log.info("creating file commit");
////                    CommitInfo cInfo = GitUtil.createFileCommit(repo, userProvider.getUserPersonIdent(), referenceUpdate, nameKey, commitMaps);
////                    info.gitignoreInfo = new AddGitIgnore.GitIgnoreInfo();
////                    info.gitignoreInfo.commitId = cInfo.commit;
////                    info.gitignoreInfo.commitMessage = cInfo.message;
////                    info.gitReviewInfo = new AddGitReview.GitReviewInfo();
////                    info.gitReviewInfo.commitId = cInfo.commit;
////                    info.gitReviewInfo.commitMessage = cInfo.message;
////                }
//            } catch (RepositoryNotFoundException rne) {
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        return Response.created(info);
    }

    private String updateHead(Repository repo, String newHead, boolean force, boolean detach) throws IOException {
        RefUpdate refUpdate = repo.getRefDatabase().newUpdate(Constants.HEAD, detach);
        refUpdate.setForceUpdate(force);

        // TODO: Handle this differently with the exception. Should we be normalizing earlier in the process?
        try {
            newHead = GitUtil.denormalizeBranchName(newHead);
        } catch (InvalidRefNameException e) {
            e.printStackTrace();
        }
        RefUpdate.Result res = refUpdate.link(newHead);

        String resStr;
        switch (res){
            case FAST_FORWARD:
            case NEW:
                resStr = "Success:" + res.name();
                break;
            default:
                resStr = "Error:" + res.name();
                break;
        }
        log.info("Result of updating the HEAD was " + resStr);
        return resStr;
    }

}
