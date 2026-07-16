package top.egon.cola.component.bytecode.maven.scan;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MavenScanInputResolver {

    public List<ScanInput> resolve(
            MavenProject project,
            MavenSession session,
            boolean reactor,
            boolean scanTests,
            boolean scanDependencies,
            List<File> additionalClassDirectories
    ) {
        Map<String, ScanInput> inputs = new LinkedHashMap<>();
        Collection<MavenProject> projects = reactor ? session.getProjects() : List.of(project);
        for (MavenProject candidate : projects) {
            add(inputs, candidate.getArtifactId(), new File(candidate.getBuild().getOutputDirectory()));
            if (scanTests) {
                add(inputs, candidate.getArtifactId() + "-test",
                        new File(candidate.getBuild().getTestOutputDirectory()));
            }
            if (scanDependencies) {
                for (Artifact artifact : candidate.getArtifacts()) {
                    if (artifact.getFile() != null) {
                        add(inputs, artifact.getArtifactId(), artifact.getFile());
                    }
                }
            }
        }
        for (File additional : additionalClassDirectories) {
            add(inputs, project.getArtifactId() + "-additional", additional);
        }
        return new ArrayList<>(inputs.values());
    }

    private void add(Map<String, ScanInput> inputs, String module, File file) {
        inputs.putIfAbsent(file.getAbsolutePath(), new ScanInput(module, file.toPath()));
    }
}
