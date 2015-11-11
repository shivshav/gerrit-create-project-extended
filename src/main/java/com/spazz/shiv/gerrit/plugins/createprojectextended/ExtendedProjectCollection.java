package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.*;

import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;


/**
 * Created by shivneil on 11/9/15.
 */
@Singleton
class ExtendedProjectCollection implements
        ChildCollection<ConfigResource, ExtendedProjectResource>,
        AcceptsCreate<ConfigResource> {
    private static final Logger log = LoggerFactory.getLogger(ExtendedProjectCollection.class);

    private final DynamicMap<RestView<ExtendedProjectResource>> views;
    private final CreateExtendedProject.Factory createExtendedProjectFactory;
    private final ProjectControl.GenericFactory controlFactory;
    private final Provider<CurrentUser> user;
    private final Provider<ListExtendedProjects> list;

    private final String pluginName;


    @Inject
    ExtendedProjectCollection(DynamicMap<RestView<ExtendedProjectResource>> views,
                              CreateExtendedProject.Factory createExtendedProjectFactory,
                              ProjectControl.GenericFactory controlFactory,
                              Provider<CurrentUser> user,
                              Provider<ListExtendedProjects> list,
                              @PluginName String pluginName) {
        log.info("********************BEGIN TRACE*********************");
        log.info("Constructor::hey it fired!");
//        System.out.println("ExtendedProjectCollection::Constructor::Does this really fucking work!?!?");
        if (createExtendedProjectFactory == null) {
            log.info("Constructor::createExtendedProjectFactory was null");
        }
        else {
            log.info("Constructor::createExtendedProjectFactory was not null!!!");
        }
        this.views = views;
        this.createExtendedProjectFactory = createExtendedProjectFactory;
        this.controlFactory = controlFactory;
        this.list = list;
        this.user = user;
        this.pluginName = pluginName;
    }

    @Override
    public RestView<ConfigResource> list() {
        log.info("list::hey it fired!");
        return list.get();
    }

    @Override
    public ExtendedProjectResource parse(ConfigResource configResource, IdString idString)
            throws ResourceNotFoundException, IOException {
        log.info("parse::hey it fired!");
//        System.out.println("ExtendedProjectCollection::parse::Does this really fucking work!?!?");
//
//        log.info("parse::********************BEGINVIEWS*******************");
//        for (DynamicMap.Entry<RestView<ExtendedProjectResource>> entry: views) {
//            log.info("parse::entry is " + entry.toString());
//        }
//        log.info("parse::********************ENDVIEWS*******************");

        log.info("parse::idString is " + idString.get());

        ProjectControl ctl;
        try {
            ctl = controlFactory.controlFor(new Project.NameKey(idString.get()), user.get());
        } catch (NoSuchProjectException nspe) {
            log.info("parse::we WILL be throwing the exception");
            throw new ResourceNotFoundException(idString);
        }

        return new ExtendedProjectResource(ctl, idString.get());
    }

    @Override
    public DynamicMap<RestView<ExtendedProjectResource>> views() {
        return views;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CreateExtendedProject create(ConfigResource projectResource, IdString idString) {
//        System.out.println("ExtendedProjectCollection::create::Does this really fucking work!?!?");
        log.info("create::hey it fired!");
        return createExtendedProjectFactory.create(idString.get());
    }
}
