include_defs('//bucklets/gerrit_plugin.bucklet')

gerrit_plugin(
  name = 'create-project-extended',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: create-project-extended',
    'Gerrit-ApiType: extension',
    'Gerrit-ApiVersion: 2.11',
    'Gerrit-Module: com.spazz.shiv.gerrit.plugins.createprojectextended.Module',
  ],
)

# this is required for bucklets/tools/eclipse/project.py to work
java_library(
  name = 'classpath',
  deps = [':create-project-extended__plugin'],
)

