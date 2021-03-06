// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.spazz.shiv.gerrit.plugins.createprojectextended;

import static com.google.gerrit.server.config.ConfigResource.CONFIG_KIND;
import static com.google.gerrit.server.project.ProjectResource.PROJECT_KIND;
import static com.spazz.shiv.gerrit.plugins.createprojectextended.rest.ExtendedProjectResource.EXTENDED_PROJECT_KIND;

import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.events.NewProjectCreatedListener;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.internal.UniqueAnnotations;
import com.spazz.shiv.gerrit.plugins.createprojectextended.client.CreateProjectExtendedMenuItem;
import com.spazz.shiv.gerrit.plugins.createprojectextended.creategr.CreateProjectExtendedManager;
import com.spazz.shiv.gerrit.plugins.createprojectextended.creategr.ProjectListenerTest;
import com.spazz.shiv.gerrit.plugins.createprojectextended.rest.AddGitIgnore;
import com.spazz.shiv.gerrit.plugins.createprojectextended.rest.AddGitReview;
import com.spazz.shiv.gerrit.plugins.createprojectextended.rest.CreateExtendedProject;
import com.spazz.shiv.gerrit.plugins.createprojectextended.rest.ExtendedProjectCollection;

class Module extends AbstractModule {
    @Override
    protected void configure() {
    // TODO
//        bind(ProjectListenerTest.class);
        bind(LifecycleListener.class)
                .annotatedWith(UniqueAnnotations.create())
                .to(CreateProjectExtendedManager.class);
//        bind(NewProjectCreatedListener.class)
//                .annotatedWith(UniqueAnnotations.create())
//                .to(ProjectListenerTest.class);
//        bind(GitReferenceUpdatedListener.class)
//                .annotatedWith(UniqueAnnotations.create())
//                .to(ProjectListenerTest.class);
        DynamicSet
                .bind(binder(), TopMenu.class)
                .to(CreateProjectExtendedMenuItem.class);

        install(new RestApiModule() {
            @Override
            protected void configure() {
                DynamicMap.mapOf(binder(), EXTENDED_PROJECT_KIND);
                bind(ExtendedProjectCollection.class);
                child(CONFIG_KIND, "projects").to(ExtendedProjectCollection.class);
                install(new FactoryModuleBuilder().build(CreateExtendedProject.Factory.class));

                put(PROJECT_KIND, "gitreview")
                        .to(AddGitReview.class);
                put(PROJECT_KIND, "gitignore")
                        .to(AddGitIgnore.class);
            }
        });
    }
}
