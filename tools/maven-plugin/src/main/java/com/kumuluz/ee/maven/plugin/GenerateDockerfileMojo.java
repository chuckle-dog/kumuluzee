package com.kumuluz.ee.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(
        name = "generate-dockerfile",
        defaultPhase = LifecyclePhase.PACKAGE
)
public class GenerateDockerfileMojo extends AbstractDockerfileMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        generateDockerfile();
    }
}
