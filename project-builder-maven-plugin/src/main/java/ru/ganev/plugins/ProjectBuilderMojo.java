package ru.ganev.plugins;

import java.io.File;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static java.lang.String.format;

/**
 * Build project with dependencies
 */
@Mojo(name = "build")
public class ProjectBuilderMojo extends AbstractMojo {

    private static final String SEPARATOR_LINE = "------------------------------------------------------------------------";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        File currentProjectPom = project.getFile();
        if (currentProjectPom.exists()) {
            printInfoWithSeparator("Project found: " + currentProjectPom.getAbsolutePath());
        } else {
            throw new MojoExecutionException("Project not found");
        }
        printSnapshotProperties();
        printSnapshotDependencies();

    }

    private void printSnapshotProperties() {
        Model pomModel = project.getModel();
        printInfoWithSeparator("SNAPSHOT properties:");
        pomModel.getProperties().entrySet().stream()
                .filter(p -> p.getValue().toString().contains("SNAPSHOT"))
                .forEach(e -> getLog().info(format("%s:%s", e.getKey(), e.getValue())));
    }

    private void printSnapshotDependencies() {
        Model pomModel = project.getModel();
        printInfoWithSeparator("SNAPSHOT dependencies:");
        pomModel.getDependencies().stream()
                .filter(dependency -> dependency.getVersion().contains("SNAPSHOT"))
                .forEach(d -> getLog().info(format("%s:%s:%s", d.getGroupId(), d.getArtifactId(), d.getVersion())));
    }

    private void printInfoWithSeparator(String info) {
        getLog().info(SEPARATOR_LINE);
        getLog().info(info);
    }
}
