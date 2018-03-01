package org.feedhenry

class PomModifier implements Serializable {

    /**
     * This is an org.apache.maven.model.Model
     */
    final def model

    PomModifier(model) {
        this.model = model.clone()
    }

    /**
     * Updates component names and values in frontend-maven-plugin section
     *
     * @param componentData shape: ['componentName': ['version': '1', 'release']]
     * @return an instance of org.apache.maven.model.Model
     */
    def updateFrontendPluginExecutions(Map<String, Map<String, String>> componentData) {
        def build = model.getBuild()
        List buildPlugins = build.getPlugins()
        def frontEndPlugin = buildPlugins.find({it.getArtifactId() == 'frontend-maven-plugin'})
        List executions = frontEndPlugin.getExecutions()

        componentData.each { name, data ->
            def imEx = executions.find { it.id.matches(/npm-patch-${name}-image/) }
            def valEx = executions.find { it.id.matches(/npm-patch-${name}-value/) }

            if (imEx) {
                imEx.getConfiguration().getChild('arguments').setValue("run update-param -- -p ${name.toUpperCase().replace('-', '_')}_IMAGE -v rhmap45/${name}")
            }

            if (valEx) {
                valEx.getConfiguration().getChild('arguments').setValue("run update-param -- -p ${name.toUpperCase().replace('-', '_')}_VERSION -v ${data.version}-${data.release}")
            }
        }

        return model
    }

}
