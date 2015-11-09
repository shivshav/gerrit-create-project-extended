package com.spazz.shiv.gerrit.plugins.createprojectextended.client;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.client.GerritTopMenu;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by shivneil on 11/7/15.
 */
public class CreateProjectExtendedMenuItem implements TopMenu {
    private final List<MenuEntry> menuEntries;
    @Inject
    public CreateProjectExtendedMenuItem(@PluginName String name) {
        menuEntries = new ArrayList<>();
        menuEntries.add(new MenuEntry(GerritTopMenu.PROJECTS, Collections
                .singletonList(new MenuItem("Create Project Extended", "#/x/" + name + "/admin/create-project", ""))));
    }

    @Override
    public List<MenuEntry> getEntries() {
        return menuEntries;
    }
}
//"My Screen", "#/x/" + name + "/my-screen", ""