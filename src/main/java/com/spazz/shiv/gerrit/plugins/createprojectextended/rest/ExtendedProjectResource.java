package com.spazz.shiv.gerrit.plugins.createprojectextended.rest;

import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.TypeLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivneil on 11/9/15.
 */
public class ExtendedProjectResource extends ProjectResource{
    public static final TypeLiteral<RestView<ExtendedProjectResource>> EXTENDED_PROJECT_KIND =
            new TypeLiteral<RestView<ExtendedProjectResource>>() {};

    private final static Logger log = LoggerFactory.getLogger(ExtendedProjectResource.class);
    private final String message;

    ExtendedProjectResource(ProjectControl control, String message) {
        super(control);
        log.info("Constructor::hey it fired!");
//        System.out.println("ExtendedProjectResource::Constructor::Does this really fucking work!?!?");
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
