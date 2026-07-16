package top.egon.cola.component.bytecode.maven.mojo;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "generate-baseline",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        aggregator = true, threadSafe = true)
public final class GenerateBaselineMojo extends AbstractArchitectureMojo {

    @Override
    protected boolean reactor() {
        return true;
    }

    @Override
    protected boolean generateBaseline() {
        return true;
    }
}
