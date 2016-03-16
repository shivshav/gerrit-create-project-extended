# Gerrit Create Project Extended Plugin
A plugin for the [Gerrit Code Review System](https://www.gerritcodereview.com/) that adds functionality to project creation

This plugin can be used to aid in project creation. In addition to mimicking all parts of Gerrit's own Create Project functionality (REST API and UI currently), the user is also able to:
- Create a .[gitreview](https://github.com/openstack-infra/git-review)  file for the project
- Create a .gitignore file for the project with templates from [gitignore.io](https://www.gitignore.io)
- Specify which branch(es) will be initially created
- Specify which of the newly created branches Gerrit will treat as HEAD

## Building
1. Install maven `apt-get install mvn`
2. Clone the project `git clone https://github.com/shivshav/gerrit-create-project-extended`
3. `cd gerrit-create-project-extended`
4. edit `pom.xml` file with correct gerrit major.minor version 
5. Build with maven `mvn deploy`
6. Copy plugin jar from `build/` into `$GERRIT_SITE/plugins/`
7. Restart gerrit

## Usage
As of now, the plugin provides a UI interface and a REST API functionality

### UI
Under the `Projects` menu in Gerrit will be a new option called `Create Project Extended` next to the regular `Create Project` option

### REST API

#### Create gitReview file for existing project
**PUT** /a/projects/{PROJECT_NAME}/createprojectextended~gitreview/

#### Create gitIgnore file for existing project
**PUT** /a/projects/{PROJECT_NAME}/createprojectextended~gitignore/

#### Create new project w/ gitreview and/or gitignore
**PUT** /a/config/server/createprojectextended~projects/{PROJECT_NAME}
