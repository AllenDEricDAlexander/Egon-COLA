package top.egon.cola.component.bytecode.maven.mojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRule;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureSeverity;
import top.egon.cola.component.bytecode.core.architecture.DefaultArchitectureRuleContext;
import top.egon.cola.component.bytecode.core.architecture.DefaultLayerResolver;
import top.egon.cola.component.bytecode.core.architecture.FindingFingerprint;
import top.egon.cola.component.bytecode.core.architecture.LayerMapping;
import top.egon.cola.component.bytecode.core.architecture.rule.ArchitectureRuleConfiguration;
import top.egon.cola.component.bytecode.maven.baseline.ArchitectureBaseline;
import top.egon.cola.component.bytecode.maven.baseline.ArchitectureBaselineRepository;
import top.egon.cola.component.bytecode.maven.baseline.BaselineComparator;
import top.egon.cola.component.bytecode.maven.config.ArchitectureFailurePolicy;
import top.egon.cola.component.bytecode.maven.config.ArchitecturePluginConfiguration;
import top.egon.cola.component.bytecode.maven.config.UnknownLayerPolicy;
import top.egon.cola.component.bytecode.maven.report.ConsoleReportWriter;
import top.egon.cola.component.bytecode.maven.report.HtmlReportWriter;
import top.egon.cola.component.bytecode.maven.report.JsonReportWriter;
import top.egon.cola.component.bytecode.maven.report.TextReportWriter;
import top.egon.cola.component.bytecode.maven.result.ArchitectureCheckResult;
import top.egon.cola.component.bytecode.maven.rule.ArchitectureRuleLoader;
import top.egon.cola.component.bytecode.maven.scan.ArchitectureScanResult;
import top.egon.cola.component.bytecode.maven.scan.ArchitectureScanner;
import top.egon.cola.component.bytecode.maven.scan.MavenScanInputResolver;
import top.egon.cola.component.bytecode.maven.scan.ScanRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractArchitectureMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project.build.directory}/egon-cola-architecture", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/.egon-cola/architecture-baseline.json")
    private File baselinePath;

    @Parameter(property = "egonArchitecture.overwrite", defaultValue = "false")
    private boolean overwrite;

    @Parameter(property = "egonArchitecture.scanTests", defaultValue = "false")
    private boolean scanTests;

    @Parameter(property = "egonArchitecture.scanDependencies", defaultValue = "false")
    private boolean scanDependencies;

    @Parameter
    private List<File> additionalClassDirectories = new ArrayList<>();

    @Parameter
    private Map<String, String> moduleMappings = Map.of();

    @Parameter
    private Map<String, String> packageMappings = Map.of();

    @Parameter
    private List<String> frameworkDenylist = new ArrayList<>();

    @Parameter
    private List<String> frameworkAllowlist = new ArrayList<>();

    @Parameter
    private List<String> facadeImplementationPackages = new ArrayList<>();

    @Parameter(property = "egonArchitecture.failurePolicy", defaultValue = "FAIL")
    private ArchitectureFailurePolicy failurePolicy;

    @Parameter(property = "egonArchitecture.unknownLayerPolicy", defaultValue = "WARN")
    private UnknownLayerPolicy unknownLayerPolicy;

    @Parameter(property = "egonArchitecture.cache.enabled", defaultValue = "true")
    private boolean cacheEnabled;

    @Parameter(defaultValue = "${project.build.directory}/egon-cola-architecture/cache")
    private File cacheDirectory;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        ArchitecturePluginConfiguration configuration = configuration();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            DefaultLayerResolver layerResolver = new DefaultLayerResolver(layerMapping(configuration));
            ArchitectureScanner scanner = new ArchitectureScanner(
                    objectMapper, message -> getLog().debug(message));
            var inputs = new MavenScanInputResolver().resolve(
                    project, session, reactor(), configuration.scanTests(),
                    configuration.scanDependencies(), configuration.additionalClassDirectories());
            ArchitectureScanResult scanResult = scanner.scan(new ScanRequest(
                    inputs,
                    layerResolver,
                    configuration.cacheEnabled(),
                    configuration.cacheDirectory().toPath(),
                    configurationDigest(configuration)
            ));
            getLog().info("Scanned architecture classes: parsed=" + scanResult.parsedClasses()
                    + ", cacheHits=" + scanResult.cacheHits());
            validateUnknownLayers(scanResult, configuration.unknownLayerPolicy());
            List<ArchitectureFinding> findings = evaluateRules(
                    scanResult, ruleConfiguration(configuration));
            if (generateBaseline()) {
                generateBaseline(findings, configuration, objectMapper);
                return;
            }
            check(findings, configuration, objectMapper);
        } catch (MojoFailureException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MojoExecutionException("Egon COLA architecture analysis failed", exception);
        }
    }

    protected boolean reactor() {
        return false;
    }

    protected boolean generateBaseline() {
        return false;
    }

    private ArchitecturePluginConfiguration configuration() {
        return new ArchitecturePluginConfiguration(
                outputDirectory, baselinePath, overwrite, scanTests, scanDependencies,
                safeList(additionalClassDirectories), safeMap(moduleMappings), safeMap(packageMappings),
                safeList(frameworkDenylist), safeList(frameworkAllowlist),
                safeList(facadeImplementationPackages), failurePolicy, unknownLayerPolicy,
                cacheEnabled, cacheDirectory
        );
    }

    private LayerMapping layerMapping(ArchitecturePluginConfiguration configuration) {
        Map<ArchitectureLayer, Set<String>> modules = invert(configuration.moduleMappings());
        Map<ArchitectureLayer, Set<String>> packages = invert(configuration.packageMappings());
        return new LayerMapping(modules, packages);
    }

    private Map<ArchitectureLayer, Set<String>> invert(Map<String, String> source) {
        Map<ArchitectureLayer, Set<String>> result = new EnumMap<>(ArchitectureLayer.class);
        source.forEach((value, layerName) -> result
                .computeIfAbsent(parseLayer(layerName), ignored -> new LinkedHashSet<>())
                .add(value));
        return result;
    }

    private ArchitectureLayer parseLayer(String value) {
        try {
            return ArchitectureLayer.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unknown architecture layer: " + value, exception);
        }
    }

    private ArchitectureRuleConfiguration ruleConfiguration(
            ArchitecturePluginConfiguration configuration
    ) {
        ArchitectureRuleConfiguration defaults = ArchitectureRuleConfiguration.defaults();
        Set<String> technical = configuration.frameworkDenylist().isEmpty()
                ? defaults.technicalFrameworkPrefixes()
                : Set.copyOf(configuration.frameworkDenylist());
        Set<String> facadePackages = configuration.facadeImplementationPackages().isEmpty()
                ? defaults.adapterPackagePatterns()
                : Set.copyOf(configuration.facadeImplementationPackages());
        return new ArchitectureRuleConfiguration(
                technical,
                defaults.persistencePrefixes(),
                facadePackages,
                Set.copyOf(configuration.frameworkAllowlist()));
    }

    private List<ArchitectureFinding> evaluateRules(
            ArchitectureScanResult scanResult,
            ArchitectureRuleConfiguration configuration
    ) throws MojoExecutionException {
        DefaultArchitectureRuleContext context = new DefaultArchitectureRuleContext(scanResult.graph());
        List<ArchitectureFinding> findings = new ArrayList<>();
        List<ArchitectureRule> rules;
        try {
            rules = new ArchitectureRuleLoader().load(getClass().getClassLoader(), configuration);
        } catch (RuntimeException exception) {
            throw new MojoExecutionException("Unable to load architecture rules", exception);
        }
        for (ArchitectureRule rule : rules) {
            try {
                findings.addAll(rule.evaluate(context));
            } catch (RuntimeException exception) {
                throw new MojoExecutionException("Architecture rule failed: " + rule.id(), exception);
            }
        }
        return findings;
    }

    private void validateUnknownLayers(
            ArchitectureScanResult scanResult,
            UnknownLayerPolicy policy
    ) throws MojoFailureException {
        List<String> unknown = scanResult.graph().types().stream()
                .filter(type -> type.layer() == ArchitectureLayer.UNKNOWN)
                .map(type -> type.module() + ":" + type.className())
                .toList();
        if (unknown.isEmpty() || policy == UnknownLayerPolicy.IGNORE) {
            return;
        }
        String message = "Unknown architecture layers: " + unknown;
        if (policy == UnknownLayerPolicy.FAIL) {
            throw new MojoFailureException(message);
        }
        getLog().warn(message);
    }

    private void check(
            List<ArchitectureFinding> findings,
            ArchitecturePluginConfiguration configuration,
            ObjectMapper objectMapper
    ) throws IOException, MojoFailureException {
        ArchitectureBaselineRepository baselineRepository = new ArchitectureBaselineRepository(
                configuration.baselinePath().toPath(), objectMapper);
        var comparison = new BaselineComparator().compare(findings, baselineRepository.load());
        ArchitectureCheckResult result = ArchitectureCheckResult.from(findings);
        writeReports(result, configuration.outputDirectory(), objectMapper);
        if (!comparison.staleFingerprints().isEmpty()) {
            getLog().warn("Stale architecture baseline entries: " + comparison.staleFingerprints().size());
        }
        long newErrors = comparison.newFindings().stream()
                .filter(finding -> finding.severity() == ArchitectureSeverity.ERROR)
                .count();
        if (newErrors == 0 || configuration.failurePolicy() == ArchitectureFailurePolicy.REPORT_ONLY) {
            return;
        }
        String message = "New architecture violations: " + newErrors;
        if (configuration.failurePolicy() == ArchitectureFailurePolicy.WARN) {
            getLog().warn(message);
        } else {
            throw new MojoFailureException(message);
        }
    }

    private void generateBaseline(
            List<ArchitectureFinding> findings,
            ArchitecturePluginConfiguration configuration,
            ObjectMapper objectMapper
    ) throws IOException {
        Set<String> fingerprints = findings.stream()
                .map(FindingFingerprint::of)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        new ArchitectureBaselineRepository(configuration.baselinePath().toPath(), objectMapper)
                .generate(new ArchitectureBaseline(
                        ArchitectureBaseline.CURRENT_SCHEMA, fingerprints), configuration.overwrite());
        getLog().info("Generated architecture baseline with " + fingerprints.size() + " entries at "
                + configuration.baselinePath());
    }

    private void writeReports(
            ArchitectureCheckResult result,
            File directory,
            ObjectMapper objectMapper
    ) throws IOException {
        new ConsoleReportWriter(getLog()).write(result, directory.toPath());
        new TextReportWriter().write(result, directory.toPath());
        new JsonReportWriter(objectMapper).write(result, directory.toPath());
        new HtmlReportWriter().write(result, directory.toPath());
    }

    private String configurationDigest(ArchitecturePluginConfiguration configuration) {
        return Integer.toHexString((configuration.moduleMappings().toString()
                + configuration.packageMappings()
                + configuration.frameworkDenylist()
                + configuration.frameworkAllowlist()
                + configuration.facadeImplementationPackages()).hashCode());
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private <K, V> Map<K, V> safeMap(Map<K, V> values) {
        return values == null ? Map.of() : values;
    }
}
