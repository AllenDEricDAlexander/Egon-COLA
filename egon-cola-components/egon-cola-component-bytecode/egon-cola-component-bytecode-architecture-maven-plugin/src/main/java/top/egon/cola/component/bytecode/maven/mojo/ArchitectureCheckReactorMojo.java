package top.egon.cola.component.bytecode.maven.mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "check-reactor", defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        aggregator = true, threadSafe = true)
public final class ArchitectureCheckReactorMojo extends AbstractArchitectureMojo {

    @Override
    protected boolean reactor() {
        return true;
    }
}
