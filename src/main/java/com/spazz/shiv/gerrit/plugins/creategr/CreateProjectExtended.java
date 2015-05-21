package com.spazz.shiv.gerrit.plugins.creategr;

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.project.CreateProject;

import com.spazz.shiv.gerrit.plugins.creategr.CreateProjectExtended.Input;

import java.util.List;

/**
 * Created by shivneil on 5/20/15.
 */
public class CreateProjectExtended implements RestModifyView<TopLevelResource, Input>{
    @Override
    public Object apply(TopLevelResource topLevelResource, Input input) throws AuthException, BadRequestException, ResourceConflictException, Exception {
        return null;
    }

    static class Input {

        List<String> branches;
    }

}
