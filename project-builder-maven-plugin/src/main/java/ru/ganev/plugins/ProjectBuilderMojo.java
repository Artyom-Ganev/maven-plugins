package ru.ganev.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import ru.ganev.plugins.model.RepositoryWrapper;

import static java.lang.String.format;

/**
 * Build project with dependencies
 */
@Mojo(name = "build")
public class ProjectBuilderMojo extends AbstractMojo {

    private static final String SEPARATOR_LINE = "------------------------------------------------------------------------";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session.executionRootDirectory}", readonly = true, required = true)
    private File root;
    private Map<String, RepositoryWrapper> dependencyProjects = new HashMap<>();
    private MavenXpp3Reader xpp3Reader;
    private RepositoryWrapper currentRepo;

    @Override
    public void execute() throws MojoFailureException {
        if (checkRoot(project.getFile())) {
            printInfoWithSeparator("Project root found: " + root.getAbsolutePath());
            currentRepo = RepositoryWrapper.builder().project(project).build();
            getLog().info(currentRepo.getScmUrl().toString());
            for (Map.Entry<String, String> entry : getSnapshotProperties(project.getModel()).entrySet()) {
                addDependency(entry.getKey(), entry.getValue());
            }
        }
    }

    private static String getProjectNameFromProperty(String propName) {
        return propName.substring(0, propName.indexOf("-version"));
    }

    private void printInfoWithSeparator(String info) {
        getLog().info(SEPARATOR_LINE);
        getLog().info(info);
    }

    private boolean checkRoot(File pom) throws MojoFailureException {
        String rootPath = root.getAbsolutePath();
        if (!root.exists()) {
            throw new MojoFailureException(format("Project root with path %s not found", rootPath));
        }
        if (!pom.exists()) {
            throw new MojoFailureException(format("pom.xml with path %s doesn't exist", pom.getAbsolutePath()));
        }
        return Objects.equals(rootPath, pom.getParentFile().getAbsolutePath());
    }

    private Map<String, String> getSnapshotProperties(Model pomModel) {
        printInfoWithSeparator("SNAPSHOT properties:");
        return pomModel.getProperties().entrySet().stream()
                .filter(p -> p.getValue().toString().contains("SNAPSHOT"))
                .collect(Collectors.toMap(e -> getProjectNameFromProperty((String) e.getKey()), e -> (String) e.getValue()));
    }

    private void addDependency(String projectName, String version) throws MojoFailureException {
        if (xpp3Reader == null) {
            xpp3Reader = new MavenXpp3Reader();
        }
        File pom = new File(format("%s/%s/pom.xml", root.getParentFile().getAbsolutePath(), projectName));
        Model newModel;
        if (!pom.exists()) {
            Optional<URL> projectUrl = getProjectUrl(currentRepo.getScmUrl(), projectName);
        }
        try {
            newModel = xpp3Reader.read(new FileInputStream(pom));
        } catch (FileNotFoundException e) {
            throw new MojoFailureException("Can not find file " + pom.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new MojoFailureException("I/O problem", e);
        } catch (XmlPullParserException e) {
            throw new MojoFailureException("pom.xml parsing error " + pom.getAbsolutePath(), e);
        }
        MavenProject newProject = new MavenProject(newModel);
        dependencyProjects.put(projectName, RepositoryWrapper.builder().project(newProject).version(version).build());
    }

    private Optional<URL> getProjectUrl(URL currentUrl, String projectName) throws MojoFailureException {
        URI uri;
        URL newUrl;
        try {
            uri = currentUrl.toURI();
            URI parent = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
            URI newUri = parent.resolve(parent.getPath() + projectName);
            newUrl = newUri.toURL();
            HttpURLConnection huc = (HttpURLConnection) newUrl.openConnection();
            huc.setRequestMethod("HEAD");
            if (huc.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return Optional.of(newUrl);
            }
        } catch (URISyntaxException e) {
            throw new MojoFailureException("URISyntaxException", e);
        } catch (IOException e) {
            throw new MojoFailureException("URL problems", e);
        }
        return Optional.empty();
    }

}
