package com.spazz.shiv.gerrit.plugins.createprojectextended;

import autovalue.shaded.com.google.common.common.collect.Maps;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.project.ProjectJson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by shivneil on 11/9/15.
 */
@Singleton
class ListExtendedProjects implements RestReadView<ConfigResource> {
    private static final Logger log = LoggerFactory.getLogger(ListExtendedProjects.class);
    private final GerritApi api;

    @Inject
    ListExtendedProjects(GerritApi api) {
        this.api = api;
    }

    @Override
    public Map<String, ProjectInfo> apply(ConfigResource projectResource)
            throws RestApiException {
        log.info("apply::hey look it fired!");
        Map<String, ProjectInfo> collection = Maps.newTreeMap();

        List<ProjectInfo> list = api.projects().list().withDescription(true).get();


        for (ProjectInfo project : list) {

            log.info("apply::project_list::" + project.id);
            collection.put(project.id, project);
        }

        return collection;
    }
}
