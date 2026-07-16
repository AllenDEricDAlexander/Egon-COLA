package top.egon.cola.component.bytecode.maven.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "check", threadSafe = true)
public final class ArchitectureCheckMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException {
        throw new MojoExecutionException("Architecture scan orchestration is not configured yet.");
    }
}
