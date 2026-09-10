package top.egon.cola.archetype.source.agent.starter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The static half of the knowledge contract: what is checked by reading the sources instead of
 * running them (Spec B §6.2, §10, §15).
 *
 * <p>Every rule below is a sentence of the spec that would otherwise erode one commit at a time — a
 * tenant handed to a use case as an argument instead of read from the MDC, a document body
 * interpolated into a log line, a type whose name stops saying what it carries. Reading the
 * production sources of the knowledge packages turns each of them into a build failure rather than
 * a review finding.
 *
 * <p>Package documentation is the one rule this class does not restate:
 * {@code AgentSourceContractTest} already requires a package-info file for every populated package
 * of both source sets, so the gate here is the narrower question about the knowledge packages, that
 * their package-info describe them rather than merely exist.
 */
class KnowledgeContractTest {

    private static final Path SOURCE = Path.of(".").toAbsolutePath().normalize().getParent();

    /** One declared type of one source file, with the file it was declared in. */
    private record Declaration(Path file, String typeName) {
    }

    /**
     * The endings a knowledge type may carry: the vocabulary of the naming rule, plus the words the
     * plan's own file tree adds for the parts it names — an event, a domain service, a repository,
     * its implementation, a tenant filter, a channel or a metadata holder.
     *
     * <p>An ending outside this set is not wrong by itself, but it is a decision: a new kind of
     * carrier is worth naming as deliberately as the ones already here, and this gate is where that
     * happens.
     */
    private static final Set<String> TYPE_SUFFIXES = Set.of(
            "BO", "Channel", "Command", "Configuration", "Controller", "Converter", "DAO", "Deserializer",
            "Enum", "Event", "Exception", "Filter", "Gateway", "Handler", "Impl", "Lease", "Manage",
            "Metadata", "PO", "Properties", "Repository", "Request", "Runtime", "Service", "Tenant", "VO");

    /** Endings that name a carrier without saying what it carries. */
    private static final List<String> FORBIDDEN_SUFFIXES =
            List.of("Data", "Info", "Param", "Bean", "DTO", "Entity", "Helper", "Utils", "Util");

    /** One {@code class}/{@code interface}/{@code enum}/{@code record} declaration of a file. */
    private static final Pattern DECLARATION = Pattern.compile(
            "^\\s*(?:public\\s+|abstract\\s+|final\\s+|sealed\\s+|non-sealed\\s+|static\\s+)*"
                    + "(?:class|interface|enum|record|@interface)\\s+([A-Za-z0-9_]+)",
            Pattern.MULTILINE);

    /** The last word of a CamelCase name, which is the word the naming rule speaks about. */
    private static final Pattern TRAILING_WORD = Pattern.compile("([A-Z][a-z0-9]*|[A-Z]+)$");

    /** One logging statement, from its call to the semicolon that ends it. */
    private static final Pattern LOG_CALL = Pattern.compile(
            "log\\.(?:info|warn|error|debug|trace)\\(.*?\\);", Pattern.DOTALL);

    /**
     * A call that reads a text a document or an answer produced, unless the very next thing done to
     * it is asking how long it is: a length is what a log line may publish, not the text.
     */
    private static final Pattern CONTENT_ACCESSOR = Pattern.compile(
            "\\.(?:getContent|getText|getBody|getOriginalText|getOriginalFile|getDelta|getAnswer"
                    + "|content|text|body|originalText|originalFile|delta|answer)"
                    + "\\s*\\(\\s*\\)(?!\\s*\\.\\s*(?:length|size)\\b)");

    /** Labels that put a text into a log line by name. */
    private static final List<String> CONTENT_LABELS =
            List.of("content=", "text=", "body=", "question=", "query=", "delta=", "answer=", "original=");

    /** Wall-clock time sources the audit columns must not be read from. */
    private static final List<String> FORBIDDEN_TIME =
            List.of("java.util.Date", "java.sql.Date", "java.sql.Timestamp", "SimpleDateFormat",
                    "java.util.Calendar", "System.currentTimeMillis(", "new Date(");

    @Test
    void keeps_semantic_type_suffixes() throws IOException {
        TreeSet<String> types = new TreeSet<>();
        for (Declaration declaration : declarations()) {
            types.add(declaration.typeName() + " in " + declaration.file());
        }
        assertFalse(types.isEmpty(), "no knowledge type was found to check");
        for (String type : types) {
            String name = type.substring(0, type.indexOf(" in "));
            Matcher trailing = TRAILING_WORD.matcher(name);
            String suffix = trailing.find() ? trailing.group(1) : name;
            assertFalse(FORBIDDEN_SUFFIXES.contains(suffix),
                    () -> "a knowledge type carries a carrier-word name: " + type);
            assertTrue(TYPE_SUFFIXES.contains(suffix),
                    () -> "a knowledge type carries a suffix the naming rule does not name: " + type);
        }
    }

    @Test
    void documents_every_new_package() throws IOException {
        for (Path directory : knowledgePackages()) {
            Path packageInfo = directory.resolve("package-info.java");
            assertTrue(Files.exists(packageInfo), () -> "missing package-info.java: " + directory);
            Matcher javadoc = Pattern.compile("/\\*\\*(.*?)\\*/", Pattern.DOTALL)
                    .matcher(Files.readString(packageInfo));
            assertTrue(javadoc.find() && !javadoc.group(1).isBlank(),
                    () -> "the package-info of " + directory + " does not describe the package");
        }
    }

    @Test
    void uses_java_time_only() throws IOException {
        for (Path file : knowledgeSources()) {
            String source = Files.readString(file);
            for (String forbidden : FORBIDDEN_TIME) {
                assertFalse(source.contains(forbidden),
                        () -> "a knowledge source measures time with " + forbidden + ": " + file);
            }
        }
    }

    @Test
    void never_logs_document_content() throws IOException {
        for (Path file : knowledgeSources()) {
            Matcher calls = LOG_CALL.matcher(Files.readString(file));
            while (calls.find()) {
                String call = calls.group(0);
                assertFalse(CONTENT_ACCESSOR.matcher(call).find(),
                        () -> "a knowledge log line publishes a text: " + file + " " + firstLine(call));
                for (String label : CONTENT_LABELS) {
                    assertFalse(call.contains(label),
                            () -> "a knowledge log line names a text field: " + file + " " + firstLine(call));
                }
            }
        }
    }

    @Test
    void signs_no_tenant_parameter_in_services() throws IOException {
        for (Path file : serviceSources()) {
            String source = Files.readString(file);
            assertFalse(Pattern.compile("[(,]\\s*(?:final\\s+)?(?:Long|long|String)\\s+tenantId\\s*[,)]")
                            .matcher(source).find(),
                    () -> "a knowledge service is handed the tenant instead of reading it from the MDC: " + file);
        }
    }

    /** Every declared type of the production sources of the knowledge packages. */
    private static List<Declaration> declarations() throws IOException {
        return knowledgeSources().stream()
                .flatMap(file -> declared(file))
                .toList();
    }

    private static Stream<Declaration> declared(Path file) {
        try {
            Matcher matcher = DECLARATION.matcher(Files.readString(file));
            return matcher.results().map(found -> new Declaration(file, found.group(1))).toList().stream();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read " + file, failure);
        }
    }

    /**
     * The production sources of the knowledge packages, wherever they live.
     *
     * <p>A package counts as knowledge when its path has that segment: the domain owns the
     * vocabulary, the application the use cases, the infrastructure the adapters, the adapter the
     * boundary, and the shared channel the message contract between them.
     */
    private static List<Path> knowledgeSources() throws IOException {
        try (Stream<Path> files = Files.walk(SOURCE)) {
            return files.filter(path -> path.toString().contains("/src/main/java/"))
                    .filter(path -> path.toString().contains("/knowledge/"))
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                    .sorted()
                    .toList();
        }
    }

    /**
     * The sources that carry signatures, which is where a tenant must not appear.
     *
     * <p>The domain model is left out on purpose: the migrated tables have a tenant column, so the
     * carriers of those rows hold it as a value — that is what the column is for. The rule is about
     * a use case or an adapter being handed a tenant to act on, which is the signature of everything
     * else.
     */
    private static List<Path> serviceSources() throws IOException {
        return knowledgeSources().stream()
                .filter(path -> !path.toString().contains("/knowledge/model/"))
                .toList();
    }

    /** Every knowledge package with production or test sources under it. */
    private static List<Path> knowledgePackages() throws IOException {
        try (Stream<Path> files = Files.walk(SOURCE)) {
            return files.filter(path -> path.toString().contains("/src/test/java/")
                            || path.toString().contains("/src/main/java/"))
                    .filter(path -> path.toString().contains("/knowledge/"))
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(Path::getParent)
                    .distinct()
                    .sorted()
                    .toList();
        }
    }

    private static String firstLine(String call) {
        return call.split("\n", 2)[0].trim();
    }
}
