package com.spazz.shiv.gerrit.plugins.createprojectextended;

import autovalue.shaded.com.google.common.common.collect.Maps;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Singleton;
import org.apache.velocity.runtime.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by shivneil on 11/9/15.
 */
@Singleton
class ListCollectionTest implements RestReadView<ConfigResource> {
    private static final Logger log = LoggerFactory.getLogger(ListCollectionTest.class);
    @Override
    public Map<String, String> apply(ConfigResource projectResource)
            throws AuthException, BadRequestException, ResourceConflictException {
        log.info("apply::hey look it fired!");
        Map<String, String> collection = Maps.newTreeMap();
        for (int i = 0; i < 5; i++) {
            collection.put("Key" + i, "Value" + i);
        }
        return collection;
    }
}
