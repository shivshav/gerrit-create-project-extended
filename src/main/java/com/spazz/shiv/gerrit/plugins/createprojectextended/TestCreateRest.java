package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;

import com.google.inject.assistedinject.Assisted;
import com.spazz.shiv.gerrit.plugins.createprojectextended.TestCreateRest.Input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivneil on 11/8/15.
 */
class TestCreateRest implements RestModifyView<ConfigResource, Input> {
    private final static Logger log = LoggerFactory.getLogger(TestCreateRest.class);

    static class Input {
        String name;
        String message;
    }

    interface Factory {
        TestCreateRest create(String projName);
    }

    private String pluginName;
    private String name;

    @Inject
    TestCreateRest(@PluginName String pluginName,
                   @Assisted String name) {
        log.info("Constructor::hey it fired!");
//        System.out.println("TestCreateRest::Constructor::Does this really fucking work!?!?");

        this.pluginName = pluginName;
        this.name = name;
    }


    @Override
    public Response<String> apply(ConfigResource projectResource, Input createProjectExtendedInput)
            throws AuthException, BadRequestException, ResourceConflictException {
        log.info("apply::hey it fired!");
        System.out.println("TestCreateRest::apply::Does this really fucking work!?!?");

        if(createProjectExtendedInput.name != null  && !createProjectExtendedInput.name.matches(name)) {
            throw new BadRequestException("name must match URL");
        }

        return Response.created("Heeeyyyy yo " + name + ", " + createProjectExtendedInput.message);
    }

}
