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

import com.kumuluz.ee.common.utils.ResourceUtils;
import jdk.internal.loader.Resource;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @author Benjamin Kastelic
 * @since 2.4.0
 */
public abstract class AbstractPackageMojo extends AbstractCopyDependenciesMojo {

    private static final String LOADER_JAR = "META-INF/loader/kumuluzee-loader.jar";
    private static final String TEMP_DIR_NAME_PREFIX = "kumuluzee-loader";
    private static final String CLASS_SUFFIX = ".class";

    private static final String PACKAGING_TYPE_UBER = "uber";
    private static final String PACKAGING_TYPE_SMART = "smart";
    private static final String PACKAGING_TYPE_EXPLODED = "exploded";

    @Parameter(defaultValue = "com.kumuluz.ee.EeApplication")
    private String mainClass;

    @Parameter(defaultValue = PACKAGING_TYPE_UBER, property = "packagingType")
    private String packagingType;

    private String buildDirectory;
    private String outputDirectory;
    private String finalName;

    protected void repackage() throws MojoExecutionException {
        buildDirectory = project.getBuild().getDirectory();
        outputDirectory = project.getBuild().getOutputDirectory();
        finalName = project.getBuild().getFinalName();

        packagingType = packagingType.trim().toLowerCase();

        checkPrecoditions();
        if (packagingType.equals(PACKAGING_TYPE_UBER)) {
            copyDependencies("classes/lib");
            unpackDependencies();
            packageJar();
            renameJars();
        }
        else if (packagingType.equals(PACKAGING_TYPE_SMART)){
            copyDependencies("lib");
            unpackDependencies();
            packageSmartJar();
        }
        /*
        * Can add this to make packaging into exploded a bit more streamlined
        */
        else if (packagingType.equals(PACKAGING_TYPE_EXPLODED)){
            copyDependencies();
        }
        else {
            getLog().warn("Unknown packaging type. Skipping KumuluzEE packaging.");
        }
    }

    private void checkPrecoditions() throws MojoExecutionException {
        getLog().info("Checking if project meets the preconditions.");

        // only jar packagins if allowed
        if (!project.getPackaging().toLowerCase().equals("jar")) {
            throw new MojoExecutionException("Only projects of \"jar\" packaging can be repackaged into an Uber JAR.");
        }
    }

    private void unpackDependencies() throws MojoExecutionException {
        getLog().info("Unpacking kumuluzee-loader dependency.");

        try {
            // get plugin JAR
            URI pluginJarURI = getPluginJarPath();

            Path pluginJarFile = Paths.get(pluginJarURI);

            FileSystem pluginJarFs = FileSystems.newFileSystem(pluginJarFile, null);

            Path loaderJarFile = pluginJarFs.getPath(LOADER_JAR);
            Path tmpJar = Files.createTempFile(TEMP_DIR_NAME_PREFIX, ".tmp");

            Files.copy(loaderJarFile, tmpJar, StandardCopyOption.REPLACE_EXISTING);

            JarFile loaderJar = new JarFile(tmpJar.toFile());

            loaderJar.stream().parallel()
                    .filter(loaderJarEntry -> loaderJarEntry.getName().toLowerCase().endsWith(CLASS_SUFFIX) &&
                            loaderJarEntry.getName().toLowerCase().contains(packagingType))
                    .forEach(loaderJarEntry -> {
                        try {

                            Path outputPath = Paths.get(outputDirectory, loaderJarEntry.getName());

                            Path outputPathParent = outputPath.getParent();

                            if (outputPathParent != null) {

                                Files.createDirectories(outputPathParent);
                            }

                            InputStream inputStream = loaderJar.getInputStream(loaderJarEntry);

                            Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);

                            inputStream.close();
                        } catch (IOException ignored) {
                        }
                    });

            loaderJar.close();

            Files.delete(tmpJar);

            // Create the boot loader config file
            Path loaderConf = Paths.get(outputDirectory, "META-INF", "kumuluzee", "boot-loader.properties");

            Path loaderConfParent = loaderConf.getParent();

            if (!Files.exists(loaderConfParent)) {

                Files.createDirectories(loaderConfParent);
            }

            StringBuilder loaderConfContent = new StringBuilder("main-class=" + mainClass);

            if (packagingType.equals(PACKAGING_TYPE_SMART)){
                loaderConfContent.append("\nrepository-paths=" + String.join(",", getRepositoryPaths()));
                loaderConfContent.append("\ndependency-paths=" + String.join(",", getDependencyPaths()));
            }

            Files.write(loaderConf, loaderConfContent.toString().getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to unpack kumuluzee-loader dependency: " + e.getMessage() + ".");
        }
    }

    private List<String> getRepositoryPaths(){

        List<Repository> repositories = project.getRepositories();

        // Move maven central repo to first position
        for (Repository repository : repositories) {
            if (repository.getId().equals("central")) {
                repositories.remove(repository);
                repositories.add(0, repository);
            }
        }

        List<String> repoPaths = new LinkedList<>();
        for (Repository repository : repositories){
            repoPaths.add(repository.getUrl());
        }

        return repoPaths;
    }

    private List<String> getDependencyPaths(){

        List<String> depPaths = new LinkedList<>();
        for (Artifact artifact : (Set<Artifact>)project.getArtifacts()){
            String filename = String.format("%s-%s.jar", artifact.getArtifactId(), artifact.getVersion());
            String filepath = String.join("/",
                    artifact.getGroupId().replaceAll("\\.", "/"),
                    artifact.getArtifactId(),
                    artifact.getVersion(),
                    filename
            );

            depPaths.add(filepath);
        }

        return depPaths;
    }

    private URI getPluginJarPath() throws MojoExecutionException {
        try {
            ProtectionDomain protectionDomain = RepackageMojo.class.getProtectionDomain();
            CodeSource codeSource = protectionDomain.getCodeSource();

            if (codeSource == null) {
                throw new MojoExecutionException("Failed to retrieve plugin JAR file path. Unobtainable Code Source.");
            }

            return codeSource.getLocation().toURI();
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("Failed to retrieve plugin JAR file path.", e);
        }
    }

    private void packageJar() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-jar-plugin"),
                        version(MojoConstants.MAVEN_JAR_PLUGIN_VERSION)
                ),
                goal("jar"),
                configuration(
                        element("finalName", finalName),
                        element("outputDirectory", buildDirectory),
                        element("classifier", "uber"),
                        element("forceCreation", "true"),
                        element("archive",
                                element("manifest",
                                        element("mainClass", "com.kumuluz.ee.loader.uber.EeBootLoader")
                                ),
                                element("manifestEntries",
                                        element("packagingType", packagingType)
                                )
                        )
                ),
                executionEnvironment(project, session, buildPluginManager)
        );
    }

    private void packageSmartJar() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-jar-plugin"),
                        version(MojoConstants.MAVEN_JAR_PLUGIN_VERSION)
                ),
                goal("jar"),
                configuration(
                        element("finalName", finalName),
                        element("outputDirectory", buildDirectory),
                        element("classifier", "smart"),
                        element("forceCreation", "true"),
                        element("archive",
                                element("manifest",
                                        element("addClasspath", "true"),
                                        element("classpathPrefix", "lib/"),
                                        element("mainClass", "com.kumuluz.ee.loader.smart.EeSmartLoader")
                                ),
                                element("manifestEntries",
                                        element("packagingType", packagingType)
                                )
                        )
                ),
                executionEnvironment(project, session, buildPluginManager)
        );
    }

    private void renameJars() throws MojoExecutionException {
        try {
            Path sourcePath1 = Paths.get(buildDirectory, finalName + ".jar");

            getLog().info("Repackaging jar: " + sourcePath1.toAbsolutePath());

            if (Files.exists(sourcePath1)) {
                Files.move(
                        sourcePath1,
                        sourcePath1.resolveSibling(finalName + ".jar.original"),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            Path sourcePath2 = Paths.get(buildDirectory, finalName + "-uber.jar");

            if (Files.exists(sourcePath2)) {
                Files.move(
                        sourcePath2,
                        sourcePath2.resolveSibling(finalName + ".jar"),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            List<Artifact> artifacts = project.getAttachedArtifacts();
            for (Artifact artifact : project.getAttachedArtifacts()) {
                if (artifact.hasClassifier() && artifact.getClassifier().equals("uber")) {
                    artifacts.remove(artifact);
                    break;
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to rename the final build artifact.");
        }
    }

    private void copySkimmedModules(){

        String modulesDirPath = outputDirectory + File.separator + "modules";

        File modulesDir = new File(modulesDirPath);

        if (!modulesDir.exists()){
            if (!modulesDir.mkdir()){
                getLog().warn("Could not create modules directory.");
            }
        }

    }

}
