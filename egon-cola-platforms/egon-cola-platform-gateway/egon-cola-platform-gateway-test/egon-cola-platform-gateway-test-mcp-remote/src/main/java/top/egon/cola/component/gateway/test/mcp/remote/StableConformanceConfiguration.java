package top.egon.cola.component.gateway.test.mcp.remote;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.AudioContent;
import io.modelcontextprotocol.spec.McpSchema.BlobResourceContents;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.ElicitRequest;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;
import io.modelcontextprotocol.spec.McpSchema.EmbeddedResource;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.PromptReference;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.SamplingMessage;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.modelcontextprotocol.spec.McpSchema.EnumSchemaOption;
import static io.modelcontextprotocol.spec.McpSchema.JSON_SCHEMA_DIALECT_2020_12;
import static io.modelcontextprotocol.spec.McpSchema.LegacyTitledEnumSchema;
import static io.modelcontextprotocol.spec.McpSchema.TitledMultiSelectEnumSchema;
import static io.modelcontextprotocol.spec.McpSchema.TitledMultiSelectItems;
import static io.modelcontextprotocol.spec.McpSchema.TitledSingleSelectEnumSchema;
import static io.modelcontextprotocol.spec.McpSchema.UntitledMultiSelectEnumSchema;
import static io.modelcontextprotocol.spec.McpSchema.UntitledMultiSelectItems;
import static io.modelcontextprotocol.spec.McpSchema.UntitledSingleSelectEnumSchema;

/**
 * Isolated official-SDK fixture for the Stable MCP conformance CLI.
 *
 * <p>The project federation endpoints remain under {@code /remote/**}; this
 * servlet exposes only the capability names and deterministic exchanges
 * mandated by the upstream conformance suite.</p>
 */
@Configuration(proxyBeanMethods = false)
class StableConformanceConfiguration {

    static final String MCP_ENDPOINT = "/conformance/stable";

    private static final Map<String, Object> EMPTY_JSON_SCHEMA = Map.of(
            "type", "object",
            "properties", Collections.emptyMap()
    );

    private static final String RED_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==";

    private static final String MINIMAL_WAV =
            "UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAAB9AAACABAAZGF0YQAAAAA=";

    private static final Set<String> TOOL_NAMES = Set.of(
            "test_simple_text",
            "test_image_content",
            "test_audio_content",
            "test_embedded_resource",
            "test_multiple_content_types",
            "test_tool_with_logging",
            "test_error_handling",
            "test_tool_with_progress",
            "test_sampling",
            "test_elicitation",
            "test_elicitation_sep1034_defaults",
            "json_schema_2020_12_tool",
            "test_elicitation_sep1330_enums"
    );

    static Set<String> toolNames() {
        return TOOL_NAMES;
    }

    @Bean
    ServletRegistrationBean<HttpServletStreamableServerTransportProvider>
            stableConformanceServlet() {
        var validator = DefaultServerTransportSecurityValidator.builder()
                .allowedOrigin("http://localhost:*")
                .allowedOrigin("http://127.0.0.1:*")
                .allowedHost("localhost:*")
                .allowedHost("127.0.0.1:*")
                .build();
        var transport = HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint(MCP_ENDPOINT)
                .keepAliveInterval(Duration.ofSeconds(30))
                .securityValidator(validator)
                .build();
        var registration = new ServletRegistrationBean<>(
                transport,
                MCP_ENDPOINT
        );
        registration.setName("stableMcpConformanceServlet");
        registration.setAsyncSupported(true);
        registration.setLoadOnStartup(1);
        return registration;
    }

    @Bean(destroyMethod = "closeGracefully")
    McpSyncServer stableConformanceServer(
            ServletRegistrationBean<HttpServletStreamableServerTransportProvider>
                    stableConformanceServlet) {
        var transport = stableConformanceServlet.getServlet();
        return McpServer.sync(transport)
                .serverInfo("egon-stable-conformance-fixture", "1.0.0")
                .capabilities(ServerCapabilities.builder()
                        .completions()
                        .resources(true, false)
                        .tools(false)
                        .prompts(false)
                        .build())
                .tools(createToolSpecs())
                .prompts(createPromptSpecs())
                .resources(createResourceSpecs())
                .resourceTemplates(createResourceTemplateSpecs())
                .completions(createCompletionSpecs())
                .requestTimeout(Duration.ofSeconds(30))
                .build();
    }

    @SuppressWarnings("deprecation")
    private static List<McpServerFeatures.SyncToolSpecification>
            createToolSpecs() {
        return List.of(
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_simple_text",
                                "Returns simple text content for testing",
                                EMPTY_JSON_SCHEMA
                        ))
                        .callHandler((exchange, request) -> callResult(
                                TextContent.builder(
                                        "This is a simple text response for testing."
                                ).build()
                        ))
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_image_content",
                                "Returns image content for testing",
                                EMPTY_JSON_SCHEMA
                        ))
                        .callHandler((exchange, request) -> callResult(
                                ImageContent.builder(
                                        RED_PIXEL_PNG,
                                        "image/png"
                                ).build()
                        ))
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_audio_content",
                                "Returns audio content for testing",
                                EMPTY_JSON_SCHEMA
                        ))
                        .callHandler((exchange, request) -> callResult(
                                AudioContent.builder(
                                        MINIMAL_WAV,
                                        "audio/wav"
                                ).build()
                        ))
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_embedded_resource",
                                "Returns embedded resource content for testing",
                                EMPTY_JSON_SCHEMA
                        ))
                        .callHandler((exchange, request) -> {
                            var resource = TextResourceContents.builder(
                                            "test://embedded-resource",
                                            "This is an embedded resource content."
                                    )
                                    .mimeType("text/plain")
                                    .build();
                            return callResult(
                                    EmbeddedResource.builder(resource).build()
                            );
                        })
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_multiple_content_types",
                                "Returns multiple content types for testing",
                                EMPTY_JSON_SCHEMA
                        ))
                        .callHandler((exchange, request) -> {
                            var resource = TextResourceContents.builder(
                                            "test://mixed-content-resource",
                                            "{\"test\":\"data\",\"value\":123}"
                                    )
                                    .mimeType("application/json")
                                    .build();
                            return CallToolResult.builder()
                                    .content(List.of(
                                            TextContent.builder(
                                                    "Multiple content types test:"
                                            ).build(),
                                            ImageContent.builder(
                                                    RED_PIXEL_PNG,
                                                    "image/png"
                                            ).build(),
                                            EmbeddedResource.builder(resource).build()
                                    ))
                                    .isError(false)
                                    .build();
                        })
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_tool_with_logging",
                                "Sends log messages during execution",
                                EMPTY_JSON_SCHEMA
                        ))
                        .callHandler((exchange, request) -> {
                            exchange.loggingNotification(
                                    LoggingMessageNotification.builder(
                                            LoggingLevel.INFO,
                                            "Tool execution started"
                                    ).build()
                            );
                            exchange.loggingNotification(
                                    LoggingMessageNotification.builder(
                                            LoggingLevel.INFO,
                                            "Tool processing data"
                                    ).build()
                            );
                            exchange.loggingNotification(
                                    LoggingMessageNotification.builder(
                                            LoggingLevel.INFO,
                                            "Tool execution completed"
                                    ).build()
                            );
                            return callResult(TextContent.builder(
                                    "Tool execution completed with logging"
                            ).build());
                        })
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_error_handling",
                                "Returns an error for testing",
                                EMPTY_JSON_SCHEMA
                        ))
                        .callHandler((exchange, request) -> CallToolResult.builder()
                                .content(List.of(TextContent.builder(
                                        "This tool intentionally returns an error for testing"
                                ).build()))
                                .isError(true)
                                .build())
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_tool_with_progress",
                                "Reports progress notifications",
                                EMPTY_JSON_SCHEMA
                        ))
                        .callHandler((exchange, request) -> {
                            Object progressToken = request.meta()
                                    .get("progressToken");
                            if (progressToken != null) {
                                exchange.progressNotification(
                                        ProgressNotification.builder(
                                                        progressToken,
                                                        0.0
                                                )
                                                .total(100.0)
                                                .build()
                                );
                                exchange.progressNotification(
                                        ProgressNotification.builder(
                                                        progressToken,
                                                        50.0
                                                )
                                                .total(100.0)
                                                .build()
                                );
                                exchange.progressNotification(
                                        ProgressNotification.builder(
                                                        progressToken,
                                                        100.0
                                                )
                                                .total(100.0)
                                                .build()
                                );
                            }
                            return callResult(TextContent.builder(
                                    "Tool execution completed with progress"
                            ).build());
                        })
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_sampling",
                                "Requests LLM sampling from the client",
                                Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "prompt", Map.of(
                                                        "type", "string",
                                                        "description",
                                                        "The prompt to sample"
                                                )
                                        ),
                                        "required", List.of("prompt")
                                )
                        ))
                        .callHandler((exchange, request) -> {
                            String prompt = (String) request.arguments()
                                    .get("prompt");
                            var samplingRequest = CreateMessageRequest.builder(
                                            List.of(SamplingMessage.builder(
                                                    Role.USER,
                                                    TextContent.builder(prompt)
                                                            .build()
                                            ).build()),
                                            100
                                    )
                                    .build();
                            CreateMessageResult response = exchange.createMessage(
                                    samplingRequest
                            );
                            String responseText = "LLM response: "
                                    + ((TextContent) response.content()).text();
                            return callResult(
                                    TextContent.builder(responseText).build()
                            );
                        })
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_elicitation",
                                "Requests user input from the client",
                                Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "message", Map.of(
                                                        "type", "string",
                                                        "description",
                                                        "The message to show"
                                                )
                                        ),
                                        "required", List.of("message")
                                )
                        ))
                        .callHandler((exchange, request) -> {
                            String message = (String) request.arguments()
                                    .get("message");
                            var requestedSchema = Map.<String, Object>of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "username", Map.of(
                                                    "type", "string",
                                                    "description", "User response"
                                            ),
                                            "email", Map.of(
                                                    "type", "string",
                                                    "description", "Email address"
                                            )
                                    ),
                                    "required", List.of("username", "email")
                            );
                            ElicitResult response = exchange.createElicitation(
                                    ElicitRequest.builder(
                                            message,
                                            requestedSchema
                                    ).build()
                            );
                            return callResult(TextContent.builder(
                                    "User response: action=" + response.action()
                                            + ", content=" + response.content()
                            ).build());
                        })
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_elicitation_sep1034_defaults",
                                "Requests elicitation with primitive defaults",
                                EMPTY_JSON_SCHEMA
                        ))
                        .callHandler((exchange, request) -> {
                            var requestedSchema = Map.<String, Object>of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "name", Map.of(
                                                    "type", "string",
                                                    "default", "John Doe"
                                            ),
                                            "age", Map.of(
                                                    "type", "integer",
                                                    "default", 30
                                            ),
                                            "score", Map.of(
                                                    "type", "number",
                                                    "default", 95.5
                                            ),
                                            "status", Map.of(
                                                    "type", "string",
                                                    "enum", List.of(
                                                            "active",
                                                            "inactive",
                                                            "pending"
                                                    ),
                                                    "default", "active"
                                            ),
                                            "verified", Map.of(
                                                    "type", "boolean",
                                                    "default", true
                                            )
                                    ),
                                    "required", List.of(
                                            "name",
                                            "age",
                                            "score",
                                            "status",
                                            "verified"
                                    )
                            );
                            ElicitResult response = exchange.createElicitation(
                                    ElicitRequest.builder(
                                            "Please provide information with defaults",
                                            requestedSchema
                                    ).build()
                            );
                            return callResult(TextContent.builder(
                                    "Elicitation completed: action="
                                            + response.action()
                                            + ", content=" + response.content()
                            ).build());
                        })
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "json_schema_2020_12_tool",
                                "Uses JSON Schema 2020-12 features",
                                Map.of(
                                        "$schema", JSON_SCHEMA_DIALECT_2020_12,
                                        "type", "object",
                                        "$defs", Map.of(
                                                "address", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "street", Map.of(
                                                                        "type",
                                                                        "string"
                                                                ),
                                                                "city", Map.of(
                                                                        "type",
                                                                        "string"
                                                                )
                                                        )
                                                )
                                        ),
                                        "properties", Map.of(
                                                "name", Map.of(
                                                        "type", "string"
                                                ),
                                                "address", Map.of(
                                                        "$ref",
                                                        "#/$defs/address"
                                                )
                                        ),
                                        "additionalProperties", false
                                )
                        ))
                        .callHandler((exchange, request) -> callResult(
                                TextContent.builder("ok").build()
                        ))
                        .build(),
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(tool(
                                "test_elicitation_sep1330_enums",
                                "Requests every elicitation enum schema form",
                                EMPTY_JSON_SCHEMA
                        ))
                        .callHandler((exchange, request) -> {
                            TypeRef<Map<String, Object>> mapType =
                                    new TypeRef<>() {
                                    };
                            var mapper = McpJsonDefaults.getMapper();
                            var untitledSingle =
                                    UntitledSingleSelectEnumSchema.builder()
                                            .enumValues(
                                                    "option1",
                                                    "option2",
                                                    "option3"
                                            )
                                            .build();
                            var titledSingle =
                                    TitledSingleSelectEnumSchema.builder()
                                            .oneOf(
                                                    new EnumSchemaOption(
                                                            "value1",
                                                            "First Option"
                                                    ),
                                                    new EnumSchemaOption(
                                                            "value2",
                                                            "Second Option"
                                                    ),
                                                    new EnumSchemaOption(
                                                            "value3",
                                                            "Third Option"
                                                    )
                                            )
                                            .build();
                            var legacyEnum = LegacyTitledEnumSchema.builder()
                                    .enumValues("opt1", "opt2", "opt3")
                                    .enumNames(
                                            "Option One",
                                            "Option Two",
                                            "Option Three"
                                    )
                                    .build();
                            var untitledMulti =
                                    UntitledMultiSelectEnumSchema.builder(
                                            UntitledMultiSelectItems.builder()
                                                    .enumValues(
                                                            "option1",
                                                            "option2",
                                                            "option3"
                                                    )
                                                    .build()
                                    ).build();
                            var titledMulti =
                                    TitledMultiSelectEnumSchema.builder(
                                            TitledMultiSelectItems.builder()
                                                    .anyOf(
                                                            new EnumSchemaOption(
                                                                    "value1",
                                                                    "First Choice"
                                                            ),
                                                            new EnumSchemaOption(
                                                                    "value2",
                                                                    "Second Choice"
                                                            ),
                                                            new EnumSchemaOption(
                                                                    "value3",
                                                                    "Third Choice"
                                                            )
                                                    )
                                                    .build()
                                    ).build();
                            var requestedSchema = Map.<String, Object>of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "untitledSingle", mapper.convertValue(
                                                    untitledSingle,
                                                    mapType
                                            ),
                                            "titledSingle", mapper.convertValue(
                                                    titledSingle,
                                                    mapType
                                            ),
                                            "legacyEnum", mapper.convertValue(
                                                    legacyEnum,
                                                    mapType
                                            ),
                                            "untitledMulti", mapper.convertValue(
                                                    untitledMulti,
                                                    mapType
                                            ),
                                            "titledMulti", mapper.convertValue(
                                                    titledMulti,
                                                    mapType
                                            )
                                    ),
                                    "required", List.of(
                                            "untitledSingle",
                                            "titledSingle",
                                            "legacyEnum",
                                            "untitledMulti",
                                            "titledMulti"
                                    )
                            );
                            ElicitResult response = exchange.createElicitation(
                                    ElicitRequest.builder(
                                            "Select preferences",
                                            requestedSchema
                                    ).build()
                            );
                            return callResult(TextContent.builder(
                                    "Elicitation completed: action="
                                            + response.action()
                                            + ", content=" + response.content()
                            ).build());
                        })
                        .build()
        );
    }

    private static List<McpServerFeatures.SyncPromptSpecification>
            createPromptSpecs() {
        return List.of(
                new McpServerFeatures.SyncPromptSpecification(
                        Prompt.builder("test_simple_prompt")
                                .description("A simple prompt for testing")
                                .arguments(List.of())
                                .build(),
                        (exchange, request) -> GetPromptResult.builder(
                                List.of(PromptMessage.builder(
                                        Role.USER,
                                        TextContent.builder(
                                                "This is a simple prompt for testing."
                                        ).build()
                                ).build())
                        ).build()
                ),
                new McpServerFeatures.SyncPromptSpecification(
                        Prompt.builder("test_prompt_with_arguments")
                                .description("A prompt with arguments")
                                .arguments(List.of(
                                        PromptArgument.builder("arg1")
                                                .description("First argument")
                                                .required(true)
                                                .build(),
                                        PromptArgument.builder("arg2")
                                                .description("Second argument")
                                                .required(true)
                                                .build()
                                ))
                                .build(),
                        (exchange, request) -> {
                            String text = "Prompt with arguments: arg1='"
                                    + request.arguments().get("arg1")
                                    + "', arg2='"
                                    + request.arguments().get("arg2") + "'";
                            return GetPromptResult.builder(List.of(
                                    PromptMessage.builder(
                                            Role.USER,
                                            TextContent.builder(text).build()
                                    ).build()
                            )).build();
                        }
                ),
                new McpServerFeatures.SyncPromptSpecification(
                        Prompt.builder("test_prompt_with_embedded_resource")
                                .description("A prompt with an embedded resource")
                                .arguments(List.of(
                                        PromptArgument.builder("resourceUri")
                                                .description("Resource URI")
                                                .required(true)
                                                .build()
                                ))
                                .build(),
                        (exchange, request) -> {
                            String uri = (String) request.arguments()
                                    .get("resourceUri");
                            var resource = TextResourceContents.builder(
                                            uri,
                                            "Embedded resource content for testing."
                                    )
                                    .mimeType("text/plain")
                                    .build();
                            return GetPromptResult.builder(List.of(
                                    PromptMessage.builder(
                                            Role.USER,
                                            EmbeddedResource.builder(resource).build()
                                    ).build(),
                                    PromptMessage.builder(
                                            Role.USER,
                                            TextContent.builder(
                                                    "Please process the embedded resource above."
                                            ).build()
                                    ).build()
                            )).build();
                        }
                ),
                new McpServerFeatures.SyncPromptSpecification(
                        Prompt.builder("test_prompt_with_image")
                                .description("A prompt with image content")
                                .arguments(List.of())
                                .build(),
                        (exchange, request) -> GetPromptResult.builder(List.of(
                                PromptMessage.builder(
                                        Role.USER,
                                        ImageContent.builder(
                                                RED_PIXEL_PNG,
                                                "image/png"
                                        ).build()
                                ).build(),
                                PromptMessage.builder(
                                        Role.USER,
                                        TextContent.builder(
                                                "Please analyze the image above."
                                        ).build()
                                ).build()
                        )).build()
                )
        );
    }

    private static List<McpServerFeatures.SyncResourceSpecification>
            createResourceSpecs() {
        return List.of(
                new McpServerFeatures.SyncResourceSpecification(
                        Resource.builder(
                                        "test://static-text",
                                        "Static Text Resource"
                                )
                                .description("A static text resource")
                                .mimeType("text/plain")
                                .build(),
                        (exchange, request) -> ReadResourceResult.builder(
                                List.of(TextResourceContents.builder(
                                                "test://static-text",
                                                "This is the content of the static text resource."
                                        )
                                        .mimeType("text/plain")
                                        .build())
                        ).build()
                ),
                new McpServerFeatures.SyncResourceSpecification(
                        Resource.builder(
                                        "test://static-binary",
                                        "Static Binary Resource"
                                )
                                .description("A static binary resource")
                                .mimeType("image/png")
                                .build(),
                        (exchange, request) -> ReadResourceResult.builder(
                                List.of(BlobResourceContents.builder(
                                                "test://static-binary",
                                                RED_PIXEL_PNG
                                        )
                                        .mimeType("image/png")
                                        .build())
                        ).build()
                ),
                new McpServerFeatures.SyncResourceSpecification(
                        Resource.builder(
                                        "test://watched-resource",
                                        "Watched Resource"
                                )
                                .description("A subscribable resource")
                                .mimeType("text/plain")
                                .build(),
                        (exchange, request) -> ReadResourceResult.builder(
                                List.of(TextResourceContents.builder(
                                                "test://watched-resource",
                                                "This is a watched resource content."
                                        )
                                        .mimeType("text/plain")
                                        .build())
                        ).build()
                )
        );
    }

    private static List<McpServerFeatures.SyncResourceTemplateSpecification>
            createResourceTemplateSpecs() {
        return List.of(
                new McpServerFeatures.SyncResourceTemplateSpecification(
                        ResourceTemplate.builder(
                                        "test://template/{id}/data",
                                        "Template Resource"
                                )
                                .description("A parameterized resource")
                                .mimeType("application/json")
                                .build(),
                        (exchange, request) -> {
                            String uri = request.uri();
                            String id = uri.replaceAll(
                                    "test://template/(.+)/data",
                                    "$1"
                            );
                            String text = "{\"id\":\"" + id
                                    + "\",\"templateTest\":true,"
                                    + "\"data\":\"Data for ID: "
                                    + id + "\"}";
                            return ReadResourceResult.builder(List.of(
                                    TextResourceContents.builder(uri, text)
                                            .mimeType("application/json")
                                            .build()
                            )).build();
                        }
                )
        );
    }

    private static List<McpServerFeatures.SyncCompletionSpecification>
            createCompletionSpecs() {
        return List.of(
                new McpServerFeatures.SyncCompletionSpecification(
                        new PromptReference("test_prompt_with_arguments"),
                        (exchange, request) -> new CompleteResult(
                                new CompleteResult.CompleteCompletion(
                                        List.of(),
                                        0,
                                        false
                                )
                        )
                )
        );
    }

    private static Tool tool(
            String name,
            String description,
            Map<String, Object> inputSchema) {
        return Tool.builder(name, inputSchema)
                .description(description)
                .build();
    }

    private static CallToolResult callResult(
            io.modelcontextprotocol.spec.McpSchema.Content content) {
        return CallToolResult.builder()
                .content(List.of(content))
                .isError(false)
                .build();
    }
}
