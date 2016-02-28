package com.spazz.shiv.gerrit.plugins.createprojectextended.client;

import com.google.gerrit.client.rpc.NativeMap;
//import com.google.gerrit.common.ProjectUtil;
//import com.google.gerrit.extensions.api.projects.ProjectApi;
//import com.google.gerrit.extensions.api.projects.Projects;
//import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
//import com.google.gerrit.reviewdb.client.Project;
//import com.google.gerrit.server.StringUtil;
//import com.google.gerrit.server.api.projects.ProjectApiImpl;
import com.google.gerrit.server.StringUtil;
import com.google.gson.JsonDeserializer;
import com.google.gwt.core.client.JavaScriptObject;
//import com.google.gwt.core.client.JsArray;
//import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Event;
//import com.google.gwt.user.client.History;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwtjsonrpc.client.JsonUtil;
import com.spazz.shiv.gerrit.plugins.createprojectextended.ExtendedProjectInfo;
//import com.google.gwtexpui.globalkey.client.NpTextBox;
//import com.google.gwtjsonrpc.common.VoidResult;
//import com.google.inject.Inject;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by shivneil on 11/7/15.
 */
public class CreateProjectExtendedScreen extends VerticalPanel {
//    private static final Logger log = LoggerFactory.getLogger(CreateProjectExtendedScreen.class);

    private final static String LABEL_PROJECT_NAME = "Project Name:";
    private final static String LABEL_PROJECT_PARENT = "Rights Inherit From:";
    private final static String LABEL_INITIAL_COMMIT = "Create initial commit";
    private final static String LABEL_CREATE_BRANCHES = "Initial Branches:";
    private final static String LABEL_DEFAULT_BRANCH = "Default Branch:";
    private final static String LABEL_GITIGNORE_ADD = "Add gitignore file";
    private final static String LABEL_GITIGNORE_TEMPLATES = "Gitignore Templates:";
    private final static String LABEL_GITREVIEW_ADD = "Add gitreview file";
    private final static String LABEL_ONLY_PARENT = "Only serve as parent for other projects";
    private final static String LABEL_CREATE_PROJECT = "Create Project";

    private Grid grid;
    private TextBox project;
    private Button create;
    private Button browse;
    private TextBox parent;
    private TextBox branches;
    private TextBox head;
    private TextBox gitignoreTemplates;
    private CheckBox addGitIgnore;
    private CheckBox addGitReview;
    private CheckBox emptyCommit;
    private CheckBox permissionsOnly;

    private FlexTable suggestedParentsTab;
    private PopupPanel projectsPopup;

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
        initBranchesTxt();
        initHeadText();

        initGitIgnore();
        initGitReview();
        initPermissionsOnly();
        initEmptyCommit();
        initSuggestedParents();

        addGrid(fp);

        boolean enable = emptyCommit.getValue() && !permissionsOnly.getValue();
        addGitIgnore.setEnabled(enable);
        gitignoreTemplates.setEnabled(enable);
        addGitReview.setEnabled(enable);

        fp.add(emptyCommit);
        fp.add(permissionsOnly);
        fp.add(addGitIgnore);
        addGitIgnoreGrid(fp);
        fp.add(addGitReview);
        fp.add(create);
        VerticalPanel vp = new VerticalPanel();
        vp.add(fp);
        vp.add(suggestedParentsTab);
        add(vp);
    }

    private void initPermissionsOnly() {
        permissionsOnly = new CheckBox(LABEL_ONLY_PARENT);
        permissionsOnly.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                Boolean enabled = !valueChangeEvent.getValue();
                addGitIgnore.setEnabled(enabled);
                addGitReview.setEnabled(enabled);
            }
        });
    }

    private void initEmptyCommit() {
        emptyCommit = new CheckBox(LABEL_INITIAL_COMMIT);
        emptyCommit.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                // React for gitignore/review only if this isn't a rights project
                if(!permissionsOnly.getValue()) {
                    Boolean enabled = valueChangeEvent.getValue();
                    addGitIgnore.setEnabled(enabled);
                    addGitReview.setEnabled(enabled);
                }
            }
        });
    }

    private void addGitIgnoreGrid(VerticalPanel fp) {
        Grid gitignoreGrid = new Grid(1, 3);
        gitignoreGrid.setText(0, 0, LABEL_GITIGNORE_TEMPLATES);
        gitignoreGrid.setWidget(0, 1, gitignoreTemplates);
        fp.add(gitignoreGrid);
    }

    private void initGitReview() {
        addGitReview = new CheckBox(LABEL_GITREVIEW_ADD);
    }

    private void initGitIgnore() {
        gitignoreTemplates = new TextBox();
        gitignoreTemplates.setVisibleLength(50);
        addGitIgnore = new CheckBox(LABEL_GITIGNORE_ADD);
        addGitIgnore.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                gitignoreTemplates.setEnabled(valueChangeEvent.getValue());
            }
        });
    }

    private void initHeadText() {
        head = new TextBox();
        head.setVisibleLength(50);
    }

    private void initBranchesTxt() {
        branches = new TextBox();
        branches.setVisibleLength(50);
    }

    private void initCreateTxt() {
        project = new TextBox();// {
//            @Override
//            public void onBrowserEvent(Event event) {
//                super.onBrowserEvent(event);
//                if (event.getTypeInt() == Event.ONPASTE) {
//                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
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
        project.sinkEvents(Event.ONPASTE);
        project.setVisibleLength(50);
        project.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> valueChangeEvent) {
                String entered = valueChangeEvent.getValue();
                create.setEnabled(!entered.isEmpty());
            }
        });
        project.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
                    doCreateProject();
                }
            }
        });
    }

    private void initCreateButton() {
        create = new Button(LABEL_CREATE_PROJECT);
        create.setEnabled(false);
        create.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                doCreateProject();
            }
        });

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
//                projectsPopup.setPopupPosition(left, top);
////                projectsPopup.setPreferredCoordinates(top, left);
//                projectsPopup.show();
////                projectsPopup.displayPopup();
//            }
//        });
    }

    private void initParentBox() {
        parent = new TextBox();
        parent.setVisibleLength(50);
    }

    private void initSuggestedParents() {
//        projectsPopup = new PopupPanel(false);
        suggestedParentsTab = new FlexTable();
        FlexTable.FlexCellFormatter cellFormatter = suggestedParentsTab.getFlexCellFormatter();
        suggestedParentsTab.setWidth("32em");
        suggestedParentsTab.setCellSpacing(5);
        suggestedParentsTab.setCellPadding(3);
        suggestedParentsTab.setText(0, 0, "Parent Suggestions");
        cellFormatter.setStyleName(0, 0, "infoBlock");
        suggestedParentsTab.setText(0, 1, "Project Name");
        cellFormatter.setStyleName(0, 1, "infoBlock");
        suggestedParentsTab.setText(0, 2, "Project Description");
        cellFormatter.setStyleName(0, 2, "infoBlock");

        // Add some text

        cellFormatter.setHorizontalAlignment(
                0, 1, HasHorizontalAlignment.ALIGN_LEFT);
        cellFormatter.setColSpan(0, 0, 2);
        populateSuggestedParents(suggestedParentsTab);

        suggestedParentsTab.setVisible(true);
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

    private void populateSuggestedParents(final FlexTable table) {

//        new RestApi("projects")
//                .addParameter("type", "PERMISSIONS")
//                .addParameter("d", true)
//                .get(new AsyncCallback<NativeMap<JSExtendedProject>>() {
//                    @Override
//                    public void onFailure(Throwable throwable) {
//                        // never invoked
//                    }
//
//                    @Override
//                    public void onSuccess(NativeMap<JSExtendedProject> projectMap) {
////                        log.debug(projectMap.toString());
//                        int row = 0;
//                        for(String name: projectMap.keySet()) {
//                            JSExtendedProject proj = projectMap.get(name);
//
//                            final Anchor projectLink = new Anchor(name);
//                            projectLink.addClickHandler(new ClickHandler() {
//
//                                @Override
//                                public void onClick(ClickEvent event) {
//                                    parent.setText(getTitle());
//                                }
//                            });
//
//                            table.setWidget(row, 2, projectLink);
//                            table.setText(row, 3, proj.getDescription());
//
////                            setRowItem(i, k);
//
//                        }
//
//
//                    }
//                });
    }

    private static class JSExtendedProject extends JavaScriptObject {
        protected JSExtendedProject(){ }

        static JSExtendedProject create() {
            return (JSExtendedProject) createObject();
        }

        public final native void setName(String n) /*-{ this.name = n; }-*/;
        public final native void setParent(String p) /*-{ this.parent = p; }-*/;
        public final native void setPermissionsProject(boolean p) /*-{ this.permissions_only = p; }-*/;
        public final native void setInitialCommit(boolean c) /*-{ this.create_empty_commit = c; }-*/;
        public final native void setBranches(String[] b) /*-{ this.branches = b; }-*/;
        public final native void setHead(String h) /*-{ this.head = h; }-*/;

        public final native String getName() /*-{ return this.name; }-*/;
        public final native String getDescription() /*-{ return this.description; }-*/;
    }

    private void addGrid(final VerticalPanel fp) {
        grid = new Grid(4, 3);
        grid.setStyleName("infoBlock");
        grid.setText(0, 0, LABEL_PROJECT_NAME);
        grid.setWidget(0, 1, project);
        grid.setText(1, 0, LABEL_PROJECT_PARENT);
        grid.setWidget(1, 1, parent);
        grid.setWidget(1, 2, browse);
        grid.setText(2, 0, LABEL_CREATE_BRANCHES);
        grid.setWidget(2, 1, branches);
        grid.setText(3, 0, LABEL_DEFAULT_BRANCH);
        grid.setWidget(3, 1,  head);

        fp.add(grid);
    }
    private void doCreateProject() {
        final String projectName = project.getText().trim();
        final String parentName = parent.getText().trim();
        final String headName = head.getText().isEmpty()? null: head.getText().trim();
        if ("".equals(projectName)) {
            project.setFocus(true);
            return;
        }
//
//        enableForm(false);
        String[] branchList = branches.getText().isEmpty()? null:branches.getText().split(",");

        JSExtendedProject input = JSExtendedProject.create();
        input.setName(projectName);
        input.setParent(parentName);
        input.setBranches(branchList);
        input.setInitialCommit(emptyCommit.getValue());
        input.setPermissionsProject(permissionsOnly.getValue());
        input.setHead(headName);

        RestApi createCall = new RestApi("a").view("config").view("server").view("createprojectextended", "projects").id(projectName);
        DialogBox path = new DialogBox(true);
        path.setText(createCall.path());
        path.show();

        createCall.put(input, new AsyncCallback<NativeMap<JSExtendedProject>>() {
            @Override
            public void onFailure(Throwable throwable) {
            //never invoked
            }

            @Override
            public void onSuccess(NativeMap<JSExtendedProject> response) {
                String name = response.get("project_info").getName();
                History.newItem("/admin/projects/" + name);
            }
        });
//        ProjectApi.createProject(projectName, parentName, emptyCommit.getValue(),
//                permissionsOnly.getValue(), new AsyncCallback<VoidResult>() {
//                    @Override
//                    public void onSuccess(VoidResult result) {
//                        String nameWithoutSuffix = ProjectUtil.stripGitSuffix(projectName);
//                        History.newItem(Dispatcher.toProjectAdmin(new Project.NameKey(
//                                nameWithoutSuffix), ProjectScreen.INFO));
//                    }
//
//                    @Override
//                    public void onFailure(Throwable caught) {
//                        new ErrorDialog(caught.getMessage()).center();
//                        enableForm(true);
//                    }
//                });
    }

    private void enableForm(final boolean enabled) {
        project.setEnabled(enabled);
        create.setEnabled(enabled);
        parent.setEnabled(enabled);
        emptyCommit.setEnabled(enabled);
        permissionsOnly.setEnabled(enabled);
    }
}
