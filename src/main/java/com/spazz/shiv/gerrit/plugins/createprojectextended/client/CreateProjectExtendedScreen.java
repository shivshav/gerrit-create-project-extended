package com.spazz.shiv.gerrit.plugins.createprojectextended.client;

import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Created by shivneil on 11/7/15.
 */
public class CreateProjectExtendedScreen extends VerticalPanel {

    static class Factory implements Screen.EntryPoint {
        @Override
        public void onLoad(Screen screen) {
            screen.setPageTitle("Hello");
            screen.show(new CreateProjectExtendedScreen());
        }
    }

    CreateProjectExtendedScreen() {
        setStyleName("hello-panel");
        add(new Label("Hello World Screen"));
    }
}
