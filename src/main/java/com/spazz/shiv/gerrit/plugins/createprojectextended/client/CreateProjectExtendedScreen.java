package com.spazz.shiv.gerrit.plugins.createprojectextended.client;

import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import com.google.gwtexpui.globalkey.client.NpTextBox;



/**
 * Created by shivneil on 11/7/15.
 */
public class CreateProjectExtendedScreen extends VerticalPanel {
    private final static String LABEL_PROJECT_NAME = "Project Name:";
    private final static String LABEL_PROJECT_PARENT = "Rights inherit from:";
    private final static String LABEL_INITIAL_COMMIT = "Create initial commit";
    private final static String LABEL_ONLY_PARENT = "Only serve as parent for other projects";
    private final static String LABEL_CREATE_PROJECT = "Create Project";


    private Grid grid;
    private TextBox project;
    private Button create;
    private Button browse;
    private TextBox parent;
    private CheckBox emptyCommit;
    private CheckBox permissionsOnly;
//    private ProjectsTable suggestedParentsTab;
//    private ProjectListPopup projectsPopup;

    static class Factory implements Screen.EntryPoint {
        @Override
        public void onLoad(Screen screen) {
            screen.setPageTitle("Create Project Extended");
            screen.show(new CreateProjectExtendedScreen());
        }
    }

    CreateProjectExtendedScreen() {
//        makeLegacyCreateProjectItems();
        addCreateProjectPanel();

    }

    private void makeLegacyCreateProjectItems() {

        setStyleName("createProjectPanel");
        HorizontalPanel namePanel = new HorizontalPanel();
        namePanel.setStyleName("hello-panel");
        namePanel.add(new InlineLabel(LABEL_PROJECT_NAME));
        namePanel.add(new TextBox());
        add(namePanel);

        HorizontalPanel rightsPanel = new HorizontalPanel();
        rightsPanel.add(new InlineLabel(LABEL_PROJECT_PARENT));
        rightsPanel.add(new TextBox());
        rightsPanel.add(new Button("Browse"));
        add(rightsPanel);

        add(new CheckBox(LABEL_INITIAL_COMMIT));
        add(new CheckBox(LABEL_ONLY_PARENT));
        add(new SubmitButton(LABEL_CREATE_PROJECT));

    }

    private void addCreateProjectPanel() {
        final VerticalPanel fp = new VerticalPanel();
        fp.setStyleName("createProjectPanel");

        initCreateButton();
        initCreateTxt();
        initParentBox();

        addGrid(fp);

        emptyCommit = new CheckBox(LABEL_INITIAL_COMMIT);
        permissionsOnly = new CheckBox(LABEL_ONLY_PARENT);
        fp.add(emptyCommit);
        fp.add(permissionsOnly);
        fp.add(create);
        VerticalPanel vp = new VerticalPanel();
        vp.add(fp);
//        initSuggestedParents();
//        vp.add(suggestedParentsTab);
        add(vp);
    }

    private void initCreateTxt() {
        project = new TextBox();
//        project = new NpTextBox() {
//            @Override
//            public void onBrowserEvent(Event event) {
//                super.onBrowserEvent(event);
//                if (event.getTypeInt() == Event.ONPASTE) {
//                    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
//                        @Override
//                        public void execute() {
//                            if (project.getValue().trim().length() != 0) {
//                                create.setEnabled(true);
//                            }
//                        }
//                    });
//                }
//            }
//        };
//        project.sinkEvents(Event.ONPASTE);
        project.setVisibleLength(50);
//        project.addKeyPressHandler(new KeyPressHandler() {
//            @Override
//            public void onKeyPress(KeyPressEvent event) {
//                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
//                    doCreateProject();
//                }
//            }
//        });
//        new OnEditEnabler(create, project);
    }

    private void initCreateButton() {
        create = new Button(LABEL_CREATE_PROJECT);
        create.setEnabled(false);
//        create.addClickHandler(new ClickHandler() {
//            @Override
//            public void onClick(final ClickEvent event) {
//                doCreateProject();
//            }
//        });

        browse = new Button("Browse");
//        browse.addClickHandler(new ClickHandler() {
//            @Override
//            public void onClick(final ClickEvent event) {
//                int top = grid.getAbsoluteTop() - 50; // under page header
//                // Try to place it to the right of everything else, but not
//                // right justified
//                int left =
//                        5 + Math.max(
//                                grid.getAbsoluteLeft() + grid.getOffsetWidth(),
//                                suggestedParentsTab.getAbsoluteLeft()
//                                        + suggestedParentsTab.getOffsetWidth());
//                projectsPopup.setPreferredCoordinates(top, left);
//                projectsPopup.displayPopup();
//            }
//        });
    }

    private void initParentBox() {
        parent = new TextBox();
        parent.setVisibleLength(50);
    }

    private void initSuggestedParents() {
//        suggestedParentsTab = new ProjectsTable() {
//            {
//                table.setText(0, 1, Util.C.parentSuggestions());
//            }
//
//            @Override
//            protected void populate(final int row, final ProjectInfo k) {
//                final Anchor projectLink = new Anchor(k.name());
//                projectLink.addClickHandler(new ClickHandler() {
//
//                    @Override
//                    public void onClick(ClickEvent event) {
//                        parent.setText(getRowItem(row).name());
//                    }
//                });
//
//                table.setWidget(row, 2, projectLink);
//                table.setText(row, 3, k.description());
//
//                setRowItem(row, k);
//            }
//        };
//        suggestedParentsTab.setVisible(false);
//
//        ProjectMap.parentCandidates(new GerritCallback<ProjectMap>() {
//            @Override
//            public void onSuccess(ProjectMap list) {
//                if (!list.isEmpty()) {
//                    suggestedParentsTab.setVisible(true);
//                    suggestedParentsTab.display(list);
//                    suggestedParentsTab.finishDisplay();
//                }
//            }
//        });
    }

    private void addGrid(final VerticalPanel fp) {
        grid = new Grid(2, 3);
        grid.setStyleName("infoBlock");
        grid.setText(0, 0, LABEL_PROJECT_NAME);
        grid.setWidget(0, 1, project);
        grid.setText(1, 0, LABEL_PROJECT_PARENT);
        grid.setWidget(1, 1, parent);
        grid.setWidget(1, 2, browse);
        fp.add(grid);
    }
}
