package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.projects.ProjectInput;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;

import com.google.inject.assistedinject.Assisted;
import com.spazz.shiv.gerrit.plugins.createprojectextended.CreateExtendedProject.ExtendedProjectInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by shivneil on 11/8/15.
 */
class CreateExtendedProject implements RestModifyView<ConfigResource, ExtendedProjectInput> {
    private final static Logger log = LoggerFactory.getLogger(CreateExtendedProject.class);

    static class ExtendedProjectInput extends ProjectInput {
        boolean gitreview;
        List<String> gitignoreTemplates;
    }

    interface Factory {
        CreateExtendedProject create(String projName);
    }

    private String pluginName;
    private String name;
    private final GerritApi api;

    @Inject
    CreateExtendedProject(@PluginName String pluginName,
                          @Assisted String name,
                          GerritApi api) {
        log.info("Constructor::hey it fired!");

        this.pluginName = pluginName;
        this.name = name;
        this.api = api;
    }

    @Override
    public Response<ExtendedProjectInfo> apply(ConfigResource configResource, ExtendedProjectInput extendedProjectInput)
            throws RestApiException {
        log.info("apply::hey it fired!");

        if(extendedProjectInput.name != null  && !extendedProjectInput.name.matches(name)) {
            throw new BadRequestException("name must match URL");
        }

        if(extendedProjectInput.createEmptyCommit) {
            // Do GitReview stuff
            StringBuilder sb = new StringBuilder("GitReview:")
                    .append(extendedProjectInput.gitreview);

            // Do GitIgnore stuff
            if (extendedProjectInput.gitignoreTemplates != null) {
                sb.append(", GitIgnore Templates:");
                for (String template : extendedProjectInput.gitignoreTemplates) {
                    sb.append(" ").append(template);
                }
            }
            log.info("Extended Project Arguments: " + sb.toString());
        }
        ExtendedProjectInfo info = new ExtendedProjectInfo();
        try {
            info.projectInfo = api.projects().name(name).create(extendedProjectInput).get();
            info.gitignoreCommit = "a09s8d: Added .gitignore file";
            info.gitreviewCommit = "b2m3nc: Added default .gitreview file";
        } catch (RestApiException rae) {
            log.error(rae.getMessage());
            throw rae;
        }

        return Response.created(info);
    }

}
