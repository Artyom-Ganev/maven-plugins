package ru.ganev.plugins.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import ru.ganev.plugins.ScmEnum;

import static ru.ganev.plugins.ScmEnum.GIT;
import static ru.ganev.plugins.ScmEnum.HG;

/**
 *
 */
public class RepositoryWrapper {

    private static final String SCM_URL_START = "scm";
    private MavenProject project;
    private ScmEnum scm;
    private URL scmUrl;
    private String version;

    private RepositoryWrapper(MavenProject project, ScmEnum scm, URL scmUrl, String version) {
        this.project = project;
        this.scm = scm;
        this.scmUrl = scmUrl;
        this.version = version;
    }

    public static MavenProjectWrapperBuilder builder() {
        return new MavenProjectWrapperBuilder();
    }

    public MavenProject getProject() {
        return project;
    }

    public ScmEnum getScm() {
        return scm;
    }

    public URL getScmUrl() {
        return scmUrl;
    }

    public String getVersion() {
        return version;
    }

    public static final class MavenProjectWrapperBuilder {
        private MavenProject project;
        private ScmEnum scm;
        private URL scmUrl;
        private String version;

        private MavenProjectWrapperBuilder() {
        }

        public MavenProjectWrapperBuilder project(MavenProject project) {
            this.project = project;
            return this;
        }

        public MavenProjectWrapperBuilder scm(ScmEnum scm) {
            this.scm = scm;
            return this;
        }

        public MavenProjectWrapperBuilder scmUrl(URL scmUrl) {
            this.scmUrl = scmUrl;
            return this;
        }

        public MavenProjectWrapperBuilder version(String version) {
            this.version = version;
            return this;
        }

        public RepositoryWrapper build() throws MojoFailureException {
            if (project == null) {
                throw new MojoFailureException("Can not create RepositoryWrapper with empty project");
            }
            if (scm == null) {
                scm = selectScm(project.getScm());
            }
            if (scmUrl == null) {
                scmUrl = createUrlFromScm(project.getScm());
            }
            return new RepositoryWrapper(project, scm, scmUrl, version);
        }

        private static ScmEnum selectScm(Scm scm) throws MojoFailureException {
            String scmName = scm.getConnection().split(":")[1];
            if (GIT.value().equals(scmName)) {
                return GIT;
            } else if (HG.value().equals(scmName)) {
                return HG;
            }
            throw new MojoFailureException("Unsupported version control system: " + scmName);
        }

        private static URL createUrlFromScm(Scm scm) throws MojoFailureException {
            String[] values = scm.getConnection().split(":");
            if (SCM_URL_START.equals(values[0])) {
                try {
                    int newLength = values.length - 2;
                    String[] url = new String[newLength];
                    System.arraycopy(values, 2, url, 0, newLength);
                    return new URL(Arrays.stream(url).collect(Collectors.joining(":")));
                } catch (MalformedURLException e) {
                    throw new MojoFailureException("Incorrect scm URL", e);
                }
            } else {
                throw new MojoFailureException("Incorrect scm connection settings: " + scm.getConnection());
            }
        }
    }
}
