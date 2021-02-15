/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.maven.plugin;

import com.kumuluz.ee.common.utils.KumuluzProject;
import com.kumuluz.ee.common.utils.MustacheWriter;
import com.kumuluz.ee.common.utils.Prompter;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractGenerateProjectMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "outputDirectory", defaultValue = "${basedir}")
    private File outputDirectory;

    @Parameter( defaultValue = "${project.resources}", required = true, readonly = true )
    private List<Resource> resources;

    private static final String SIMPLE_POM_TEMPLATE = "project-generation/simple/simplePom.mustache";
    private static final String MAIN_POM_TEMPLATE = "project-generation/modular/pom/mainPom.mustache";
    private static final String API_POM_TEMPLATE = "project-generation/modular/pom/apiPom.mustache";
    private static final String SERVICES_POM_TEMPLATE = "project-generation/modular/pom/servicesPom.mustache";
    private static final String ENTITIES_POM_TEMPLATE = "project-generation/modular/pom/entitiesPom.mustache";
    private static final String CONFIG_TEMPLATE = "project-generation/config.mustache";

    private static final String APPLICATION_TEMPLATE = "project-generation/application.mustache";
    private static final String SIMPLE_HELLO_ENDPOINT_TEMPLATE = "project-generation/simple/helloEndpoint.mustache";
    private static final String MODULAR_HELLO_ENDPOINT_TEMPLATE = "project-generation/modular/application/helloEndpoint.mustache";
    private static final String HELLO_SERVICE_TEMPLATE = "project-generation/modular/application/helloService.mustache";
    private static final String BEANS_TEMPLATE = "project-generation/modular/meta-inf/beans.mustache";

    private static final String DEFAULT_DESCRIPTION = "KumuluzEE project generated by kumuluzee-maven-plugin";


    private static final String DEFAULT_PORT = "8080";

    private KumuluzProject kumuluzProject;

    private boolean modularProject;

    protected void generate(){

        kumuluzProject = new KumuluzProject();
        kumuluzProject.setPort(DEFAULT_PORT);
        kumuluzProject.setDescription(DEFAULT_DESCRIPTION);

        File pom = new File(outputDirectory + File.separator + "pom.xml");

        // Generate or modify pom.xml
        if (pom.exists()){
            getLog().info("Found Maven POM.");
            readFromExistingPom();
        }
        else {
            getLog().info("No Maven POM detected. Input required.");
            Prompter.promptUserForProjectInfo(kumuluzProject);
            outputDirectory = new File (outputDirectory + File.separator + kumuluzProject.getName());
        }

        modularProject = Prompter.promptUserForBoolean("Generate default modules?");

        if (modularProject){
            // Generate modules
            generateDefaultModules();

            // Generate modular HelloApplication
            generateModularHelloApplication();
        }
        else {
            // Generate directory structure
            generateDirectoryStructure(kumuluzProject, outputDirectory.getAbsolutePath());

            // Generate config.yaml
            File resourcesDir = new File(kumuluzProject.getDirectoryPaths().get("resourcesDir"));
            MustacheWriter.writeFileFromTemplate(CONFIG_TEMPLATE, "config.yaml", kumuluzProject, resourcesDir);

            // Generate HelloApplication
            generateSimpleHelloApplication();
        }

        generateMainPom();
    }

    private void readFromExistingPom(){

        String groupId = project.getGroupId();
        String artifactId = project.getArtifactId();
        String version = project.getVersion();
        String name = outputDirectory.getName();

        kumuluzProject.setGroupId(groupId);
        kumuluzProject.setArtifactId(artifactId);
        kumuluzProject.setVersion(version);
        kumuluzProject.setName(name);

        String kumuluzVersion = (String)project.getProperties().get("kumuluz.version");
        if (kumuluzVersion == null || kumuluzVersion.isBlank()) {
            kumuluzVersion = Prompter.promptUserForInputWithDefault("Enter KumuluzEE version", "3.10.0-SNAPSHOT");
        }
        kumuluzProject.setKumuluzVersion(kumuluzVersion);

    }

    private void generateMainPom(){

        if (modularProject){
            getLog().info("Generating modular application");
            MustacheWriter.writeFileFromTemplate(MAIN_POM_TEMPLATE, "pom.xml", kumuluzProject, outputDirectory);
        }
        else {
            getLog().info("Generating simple application");
            MustacheWriter.writeFileFromTemplate(SIMPLE_POM_TEMPLATE, "pom.xml", kumuluzProject, outputDirectory);
        }
    }

    private void generateDirectoryStructure(KumuluzProject kumuluzProject, String baseDirPath){

        HashMap<String, String> dirPaths = new HashMap<>();

        String sourceDirPath = baseDirPath + File.separator + "src" + File.separator;

        File javaTestDir = new File(sourceDirPath + "test" + File.separator + "java");

        if (javaTestDir.mkdirs()){
            getLog().info("Generated java test directory");
        }
        dirPaths.put("javaTestDir", javaTestDir.getAbsolutePath());

        String mainDirPath = sourceDirPath + "main" + File.separator;

        String javaSourceCodeDirPath = mainDirPath + "java" + File.separator;
        String packageName =
                kumuluzProject.getGroupId().replaceAll("(-)+", ".") + "." +
                kumuluzProject.getArtifactId().replaceAll("(-)+", ".");
        javaSourceCodeDirPath += packageName;
        String resourcesDirPath = mainDirPath + "resources";

        File javaSourceCodeDir = new File (javaSourceCodeDirPath);
        File resourcesDir = new File(resourcesDirPath);

        if (javaSourceCodeDir.mkdirs()){
            getLog().info("Generated java source code directory");
        }
        dirPaths.put("javaSourceCodeDir", javaSourceCodeDir.getAbsolutePath());
        if (resourcesDir.mkdirs()){
            getLog().info("Generated resource directory");
        }
        dirPaths.put("resourcesDir", resourcesDir.getAbsolutePath());

        File metaInfDir = new File(resourcesDirPath + File.separator + "META-INF");
        if (metaInfDir.mkdirs()){
            getLog().info("Generated META-INF directory");
        }
        dirPaths.put("metaInfDir", metaInfDir.getAbsolutePath());

        kumuluzProject.setPackageName(packageName);
        kumuluzProject.setDirectoryPaths(dirPaths);
    }

    private void generateSimpleHelloApplication(){

        File javaSourceCodeDir = new File(kumuluzProject.getDirectoryPaths().get("javaSourceCodeDir"));
        MustacheWriter.writeFileFromTemplate(APPLICATION_TEMPLATE, "HelloApplication.java", kumuluzProject, javaSourceCodeDir);
        MustacheWriter.writeFileFromTemplate(SIMPLE_HELLO_ENDPOINT_TEMPLATE, "HelloEndpoint.java", kumuluzProject, javaSourceCodeDir);

    }

    private void generateModularHelloApplication(){

        for (KumuluzProject module : kumuluzProject.getModules()){

            File javaSourceCodeDir = new File(module.getDirectoryPaths().get("javaSourceCodeDir"));
            File resourcesDir = new File(module.getDirectoryPaths().get("resourcesDir"));
            File metaInfDir = new File(module.getDirectoryPaths().get("metaInfDir"));

            if (module.getArtifactId().equals("api")){
                MustacheWriter.writeFileFromTemplate(APPLICATION_TEMPLATE, "HelloApplication.java", module, javaSourceCodeDir);
                MustacheWriter.writeFileFromTemplate(MODULAR_HELLO_ENDPOINT_TEMPLATE, "HelloEndpoint.java", module, javaSourceCodeDir);
                MustacheWriter.writeFileFromTemplate(CONFIG_TEMPLATE, "config.yaml", module, resourcesDir);
                MustacheWriter.writeFileFromTemplate(BEANS_TEMPLATE, "beans.xml", module, metaInfDir);
            }
            else if (module.getArtifactId().equals("services")){
                MustacheWriter.writeFileFromTemplate(HELLO_SERVICE_TEMPLATE, "HelloService.java", module, javaSourceCodeDir);
                MustacheWriter.writeFileFromTemplate(BEANS_TEMPLATE, "beans.xml", module, metaInfDir);
            }
        }

    }

    private void generateModule(String moduleId, String modulePomTemplate){

        File moduleDir = new File(outputDirectory.getAbsolutePath() + File.separator + moduleId);
        if (moduleDir.mkdirs()){
            getLog().info(String.format("Generated module directory: %s", moduleId));
        }

        KumuluzProject module = kumuluzProject.newModule(moduleId);

        generateDirectoryStructure(module, moduleDir.getAbsolutePath());

        MustacheWriter.writeFileFromTemplate(modulePomTemplate, "pom.xml", module, moduleDir);

    }

    private void generateDefaultModules(){
        generateModule("api", API_POM_TEMPLATE);
        generateModule("services", SERVICES_POM_TEMPLATE);
        generateModule("entities", ENTITIES_POM_TEMPLATE);
    }

}