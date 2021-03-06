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

import com.google.common.collect.Sets;
import com.kumuluz.ee.common.utils.KumuluzProject;
import com.kumuluz.ee.common.utils.MustacheWriter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public abstract class AbstractDockerfileMojo extends AbstractMojo {

    private static final Logger log = Logger.getLogger(AbstractDockerfileMojo.class.getName());

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
    private List<Resource> resources;

    @Parameter(defaultValue = "uber", required = true)
    private String packagingType;

    @Parameter(defaultValue = "false", required = true)
    private String windowsOS;

    @Parameter(property = "appModules")
    private String[] appModules;

    @Parameter(property = "port", defaultValue = "-1")
    private int port;

    private static final String DOCKERFILE_SMART_TEMPLATE = "dockerfile-generation/dockerfileSmart.mustache";
    private static final String DOCKERFILE_EXPLODED_TEMPLATE = "dockerfile-generation/dockerfileExploded.mustache";
    private static final String DOCKERFILE_UBER_TEMPLATE = "dockerfile-generation/dockerfileUber.mustache";
    private static final String DOCKERIGNORE_TEMPLATE = "dockerfile-generation/dockerignore.mustache";

    private static final int DEFAULT_PORT = 8080;

    private static final String DEFAULT_JAVA_VERSION = "11";

    private KumuluzProject kumuluzProject;

    private boolean generateDockerfile = true;

    public void generateDockerfile() throws MojoExecutionException, MojoFailureException {

        kumuluzProject = new KumuluzProject();

        packagingType = packagingType.toLowerCase().trim();

        String executableName = String.format("%s-%s.jar", project.getName(), project.getVersion());

        String dockerfileTemplate;
        if (packagingType.equals("uber")){
            dockerfileTemplate = DOCKERFILE_UBER_TEMPLATE;
        }
        else if (packagingType.equals("smart")){
            dockerfileTemplate = DOCKERFILE_SMART_TEMPLATE;
            executableName = executableName.replace(".jar", "-smart.jar");
        }
        else if (packagingType.equals("exploded")){
            String OS = System.getProperty("os.name").toLowerCase();
            if (windowsOS.equals("true")){
                kumuluzProject.setWindowsOS(true);
            }
            else {
                kumuluzProject.setWindowsOS(false);
            }
            dockerfileTemplate = DOCKERFILE_EXPLODED_TEMPLATE;
        }
        else {
            getLog().warn("Unknown packaging type. Skipping KumuluzEE Dockerfile generation.");
            return;
        }

        List<String> modules = project.getModules();
        HashSet<String> appModulesSet = Sets.newHashSet(appModules);
        List<Dependency> dependencies = project.getDependencies();
        LinkedList<String> moduleFileNames = new LinkedList<>();

        if (port == -1){
            port = readPortFromConfig();
        }
        if (port <= 0 || port >= 65536){
            getLog().warn("Invalid port value in plugin config. Port must be an integer between 1 and 65535.");
            port = readPortFromConfig();
        }

        kumuluzProject.setPort(Integer.toString(port));
        kumuluzProject.setExecutableName(executableName);

        getLog().info(project.getArtifactId());

        // Single-module project
        if (project.getParent() == null && modules.size() == 0) {
            kumuluzProject.setName(project.getName());
            kumuluzProject.setDescription(project.getDescription());

            MustacheWriter.writeFileFromTemplate(dockerfileTemplate, "Dockerfile", kumuluzProject, outputDirectory);
        }
        // Multi-module project
        else if (project.getParent() != null && modules.size() == 0){
            // Collect module JAR names
            MavenProject parent = project.getParent();

            String javaVersion;
            if (parent.getProperties().get("maven.compiler.target") != null){
                javaVersion = parent.getProperties().get("maven.compiler.target").toString();
            }
            else {
                javaVersion = DEFAULT_JAVA_VERSION;
            }
            kumuluzProject.setJavaVersion(javaVersion);

            for (Object module : parent.getModules()){
                if (!project.getArtifactId().equals(module)) {
                    String moduleExecutableName = String.format("%s-%s.jar", module.toString(), parent.getVersion());
                    moduleFileNames.add(moduleExecutableName);

                    if (appModulesSet.contains(module.toString())){
                        KumuluzProject kumuluzModule = kumuluzProject.newAppModule(module.toString());
                        kumuluzModule.setExecutableName(moduleExecutableName);
                    }
                    else {
                        KumuluzProject kumuluzModule = kumuluzProject.newModule(module.toString());
                        kumuluzModule.setExecutableName(moduleExecutableName);
                    }
                }
            }

            if (packagingType.equals("smart")){
                copySmartModules(moduleFileNames);
            }

            kumuluzProject.setName(parent.getName());
            kumuluzProject.setDescription(parent.getDescription());

            MustacheWriter.writeFileFromTemplate(dockerfileTemplate, "Dockerfile", kumuluzProject, outputDirectory);
            MustacheWriter.writeFileFromTemplate(DOCKERIGNORE_TEMPLATE, ".dockerignore", kumuluzProject, outputDirectory);

        }

    }

    /**
     * Can cause problem if "port:" appears in any line before the correct one.
     * Needs more robust implementation.
     *
     * @return
     */
    private int readPortFromConfig(){

        try {
            File configYaml = new File(resources.get(0).getDirectory() + "/config.yaml");

            Yaml yaml = new Yaml();
            InputStream inputStream = new FileInputStream(configYaml);
            Map<String, Object> config = yaml.load(inputStream);

            Object kumuluzee = config.get("kumuluzee");
            if (kumuluzee instanceof HashMap){
                Object server = ((HashMap) kumuluzee).get("server");
                if (server instanceof HashMap){
                    Object http = ((HashMap) server).get("http");
                    if (http instanceof HashMap){
                        Object port = ((HashMap) http).get("port");
                        if (port != null){
                            return Integer.parseInt(port.toString());
                        }
                    }
                }
            }
        }
        catch (NumberFormatException e){
            getLog().warn("Invalid port value in config.yaml.");
        }
        catch (IOException e){
            e.printStackTrace();
        }

        return DEFAULT_PORT;
    }

    /**
     * To take advantage of docker layering, the module JARs must be kept in a separate directory
     * A copy of the module JARs must remain in the LIB directory for the app to run outside of Docker
     * The copies in the LIB directory are excluded from the Docker build via .dockerignore
     *
     * @param moduleFileNames names of files to copy
     */
    private void copySmartModules(List<String> moduleFileNames){

        File modulesDir = new File(outputDirectory.getAbsolutePath() + "/modules");

        if (!modulesDir.exists()){
            if (!modulesDir.mkdir()){
                getLog().warn("Could not create modules directory.");
            }
        }

        for (String moduleFileName : moduleFileNames){
            File moduleJAR = new File(outputDirectory.getAbsolutePath() + "/lib/" + moduleFileName);
            File copiedModuleJAR = new File(outputDirectory.getAbsolutePath() + "/modules/" + moduleFileName);

            try {
                FileUtils.copyFile(moduleJAR, copiedModuleJAR);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

}
