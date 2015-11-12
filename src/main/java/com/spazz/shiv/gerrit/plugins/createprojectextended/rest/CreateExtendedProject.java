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
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;

import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.spazz.shiv.gerrit.plugins.createprojectextended.GitUtil;
import com.spazz.shiv.gerrit.plugins.createprojectextended.rest.CreateExtendedProject.ExtendedProjectInput;
import com.spazz.shiv.gerrit.plugins.createprojectextended.ExtendedProjectInfo;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by shivneil on 11/8/15.
 */
@RequiresCapability(value = GlobalCapability.CREATE_PROJECT, scope = CapabilityScope.CORE)
public class CreateExtendedProject implements RestModifyView<ConfigResource, ExtendedProjectInput> {

    private final static Logger log = LoggerFactory.getLogger(CreateExtendedProject.class);
    private final GitRepositoryManager repositoryManager;

    static class ExtendedProjectInput extends ProjectInput {
        String head;
        boolean gitreview;
        List<String> gitignoreTemplates;
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
                          GerritApi api,
                          GitRepositoryManager repositoryManager) {
        log.info("Constructor::hey it fired!");

        this.pluginName = pluginName;
        this.name = name;
        this.currentUserProvider = currentUserProvider;
        this.api = api;
        this.repositoryManager = repositoryManager;
    }

    @Override
    public Response<ExtendedProjectInfo> apply(ConfigResource configResource, ExtendedProjectInput extendedProjectInput)
            throws RestApiException, OrmException {
        log.info("apply::hey it fired!");

        // Make sure we have a valid user
        CurrentUser user = currentUserProvider.get();
        if(user == null || !user.isIdentifiedUser()) {
            throw new AuthException("Authentication required");
        }

        // Let's not complicate things
        if(extendedProjectInput.name != null  && !extendedProjectInput.name.matches(name)) {
            throw new BadRequestException("name must match URL");
        }

        // Ensure that if HEAD is set, it is set to a branch inside of the branches input fields
        if(extendedProjectInput.head != null && !extendedProjectInput.branches.contains(extendedProjectInput.head)) {
            throw new BadRequestException("HEAD must be set to one of the values of the 'branches' key");
        }

        // Create the project through the Gerrit REST API
        ExtendedProjectInfo info = new ExtendedProjectInfo();
        try {
            info.projectInfo = api.projects().name(name).create(extendedProjectInput).get();

            if(extendedProjectInput.head != null) {
                log.info("Attempting to move HEAD to " + extendedProjectInput.head);
                Project.NameKey nameKey = new Project.NameKey(name);
                Repository repo = repositoryManager.openRepository(nameKey);
                info.head = updateHead(repo, extendedProjectInput.head, false, false);
            }
        } catch (RestApiException rae) {
            log.error(rae.getMessage());
            throw rae;
        } catch (RepositoryNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            throw new OrmException("Unable to read from the repository");
        }

        // Only check extended input params if user wants to create empty commits.
        // Should maintain compatibility with official API
        if(extendedProjectInput.createEmptyCommit) {

            // Do GitReview stuff
            StringBuilder sb = new StringBuilder("GitReview:")
                    .append(extendedProjectInput.gitreview);
            info.gitreviewCommit = "b2m3nc: Added default .gitreview file";

            // Do GitIgnore stuff
            if (extendedProjectInput.gitignoreTemplates != null) {
                sb.append(", GitIgnore Templates:");
                info.gitignoreCommit = "a09s8d: Added .gitignore file";

                for (String template : extendedProjectInput.gitignoreTemplates) {
                    sb.append(" ").append(template);
                }
            }
            log.info("Extended Project Arguments: " + sb.toString());
        }

        return Response.created(info);
    }

    private String updateHead(Repository repo, String newHead, boolean force, boolean detach) throws IOException {
        RefUpdate refUpdate = repo.getRefDatabase().newUpdate(Constants.HEAD, detach);
        refUpdate.setForceUpdate(force);

        newHead = GitUtil.denormalizeBranchName(newHead, false);
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
