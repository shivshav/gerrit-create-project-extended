package com.spazz.shiv.gerrit.plugins.createprojectextended.client;

import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;

/**
 * Created by shivneil on 11/7/15.
 */
public class CreateProjectExtendedScreen extends VerticalPanel {

    static class Factory implements Screen.EntryPoint {
        @Override
        public void onLoad(Screen screen) {
            screen.setPageTitle("Create Project Extended");
            screen.show(new CreateProjectExtendedScreen());
        }
    }

    CreateProjectExtendedScreen() {
        setStyleName("hello-panel");
        makeLegacyCreateProjectItems();

    }

    private void makeLegacyCreateProjectItems() {

        HorizontalPanel namePanel = new HorizontalPanel();
        namePanel.add(new InlineLabel("Project Name:"));
        namePanel.add(new TextBox());
        add(namePanel);

        HorizontalPanel rightsPanel = new HorizontalPanel();
        rightsPanel.add(new InlineLabel("Rights inherit from"));
        rightsPanel.add(new TextBox());
        rightsPanel.add(new Button("Browse"));
        add(rightsPanel);

        add(new CheckBox("Create initial empty commit"));
        add(new CheckBox("Only serve as parent for other projects"));
        add(new SubmitButton("Create Project"));
    }
}
