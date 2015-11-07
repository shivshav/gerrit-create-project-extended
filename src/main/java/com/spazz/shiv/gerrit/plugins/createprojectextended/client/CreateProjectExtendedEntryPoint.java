package com.spazz.shiv.gerrit.plugins.createprojectextended.client;

import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.PluginEntryPoint;


/**
 * Created by shivneil on 11/7/15.
 */
public class CreateProjectExtendedEntryPoint extends PluginEntryPoint {

    @Override
    public void onPluginLoad() {
        Plugin.get().screen("", new CreateProjectExtendedScreen.Factory());
    }
}


