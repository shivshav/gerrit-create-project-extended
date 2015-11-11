package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.*;

import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;


/**
 * Created by shivneil on 11/9/15.
 */
@Singleton
class CollectionTest implements
        ChildCollection<ConfigResource, MyResource>,
        AcceptsCreate<ConfigResource> {
    private static final Logger log = LoggerFactory.getLogger(CollectionTest.class);

    private final DynamicMap<RestView<MyResource>> views;
    private final TestCreateRest.Factory createRestTestFactory;
    private final Provider<ListCollectionTest> list;

    private final String pluginName;

    @Inject
    CollectionTest(DynamicMap<RestView<MyResource>> views,
                   TestCreateRest.Factory createRestTestFactory,
                   Provider<ListCollectionTest> list,
                   @PluginName String pluginName) {
        log.info("********************BEGIN TRACE*********************");
        log.info("Constructor::hey it fired!");
//        System.out.println("CollectionTest::Constructor::Does this really fucking work!?!?");
        if (createRestTestFactory == null) {
            log.info("Constructor::createRestTestFactory was null");
        }
        else {
            log.info("Constructor::createRestTestFactory was not null!!!");
        }
        this.views = views;
        this.createRestTestFactory = createRestTestFactory;
        this.list = list;
        this.pluginName = pluginName;
    }

    @Override
    public RestView<ConfigResource> list() {
        log.info("list::hey it fired!");
        return list.get();
    }

    @Override
    public MyResource parse(ConfigResource configResource, IdString idString)
            throws ResourceNotFoundException {
        log.info("parse::hey it fired!");
//        System.out.println("CollectionTest::parse::Does this really fucking work!?!?");
//
//        log.info("parse::********************BEGINVIEWS*******************");
//        for (DynamicMap.Entry<RestView<MyResource>> entry: views) {
//            log.info("parse::entry is " + entry.toString());
//        }
//        log.info("parse::********************ENDVIEWS*******************");

        log.info("parse::idString is " + idString.get());
        boolean throwException = new Random().nextBoolean();
        if (throwException) {
            log.info("parse::we WILL be throwing the exception");
//            create(configResource, idString);
            throw new ResourceNotFoundException(idString);
        }

        return new MyResource(idString.get());
    }

    @Override
    public DynamicMap<RestView<MyResource>> views() {
        return views;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TestCreateRest create(ConfigResource projectResource, IdString idString) {
//        System.out.println("CollectionTest::create::Does this really fucking work!?!?");
        log.info("create::hey it fired!");
        return createRestTestFactory.create(idString.get());
    }
}
