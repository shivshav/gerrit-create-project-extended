package com.spazz.shiv.gerrit.plugins.createprojectextended;

import com.google.gerrit.extensions.restapi.RestResource;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.inject.TypeLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivneil on 11/9/15.
 */
class MyResource implements RestResource{
    public static final TypeLiteral<RestView<MyResource>> MY_RESOURCE_KIND =
            new TypeLiteral<RestView<MyResource>>() {};

    private final static Logger log = LoggerFactory.getLogger(MyResource.class);
    private final String message;

    MyResource(String message) {
        log.info("Constructor::hey it fired!");
//        System.out.println("MyResource::Constructor::Does this really fucking work!?!?");
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
