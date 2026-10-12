package top.egon.cola.component.yuheng.edge.config.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Fixed deployment resource and its finite issuer-to-registration selection. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class EdgeRegistrationResourceBindingBO {
    @NotBlank @Size(max = 128) private String resourceServerId;
    @NotNull private URI resourceUri;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") private String defaultRegistrationId;
    @NotEmpty @Size(max = 64)
    @Builder.Default
    private Map<@NotBlank @Size(max = 2048) String, @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String> issuerRegistrationIds = new LinkedHashMap<>();
    @NotNull @Size(max = 64)
    @Builder.Default private Set<@NotBlank @Pattern(regexp = "[A-Za-z0-9._~-]{1,128}") String> routeIds = new LinkedHashSet<>();
    @NotNull @Size(max = 64)
    @Builder.Default private List<@NotBlank @Size(max = 2048) String> requestPaths = new ArrayList<>();
    @NotEmpty @Size(max = 64)
    @Builder.Default private Set<@Pattern(regexp = "[A-Za-z0-9:._-]{1,128}") String> scopes = new LinkedHashSet<>();
    @NotBlank @Size(max = 2048) private String returnPath;

    @JsonIgnore
    @AssertTrue(message = "Edge resource, issuer, path and scope bindings must be canonical and bounded")
    public boolean isBindingValid() {
        return canonicalHttps(resourceUri) && issuerRegistrationIds != null
                && issuerRegistrationIds.containsValue(defaultRegistrationId)
                && issuerRegistrationIds.keySet().stream().allMatch(value -> {
                    try { return canonicalHttps(URI.create(value)); }
                    catch (IllegalArgumentException | NullPointerException invalid) { return false; }
                })
                && requestPaths != null && requestPaths.size() == new LinkedHashSet<>(requestPaths).size()
                && requestPaths.stream().allMatch(value -> value != null && value.matches("/(?:[A-Za-z0-9._~-]+/)*[A-Za-z0-9._~-]+(?:/\\*\\*)?")
                        && java.util.Arrays.stream(value.split("/")).noneMatch(segment -> segment.equals(".") || segment.equals("..")))
                && scopes != null && scopes.contains("openid") && scopes.contains("offline_access")
                && scopes.stream().noneMatch(value -> value == null || value.contains("*"))
                && returnPath != null && returnPath.startsWith("/") && !returnPath.startsWith("//")
                && !returnPath.contains("\\") && !returnPath.contains("%")
                && !returnPath.contains("?") && !returnPath.contains("#")
                && java.util.Arrays.stream(returnPath.split("/", -1)).noneMatch(segment -> segment.equals(".") || segment.equals(".."))
                && returnPath.indexOf('\r') < 0 && returnPath.indexOf('\n') < 0;
    }

    public EdgeRegistrationResourceBindingBO setIssuerRegistrationIds(Map<String, String> values) {
        issuerRegistrationIds = values == null ? null : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(values)); return this;
    }

    public EdgeRegistrationResourceBindingBO setRouteIds(Set<String> values) {
        routeIds = values == null ? null : java.util.Collections.unmodifiableSet(new LinkedHashSet<>(values)); return this;
    }

    public EdgeRegistrationResourceBindingBO setRequestPaths(List<String> values) {
        requestPaths = values == null ? null : java.util.Collections.unmodifiableList(new ArrayList<>(values)); return this;
    }

    public EdgeRegistrationResourceBindingBO setScopes(Set<String> values) {
        scopes = values == null ? null : java.util.Collections.unmodifiableSet(new LinkedHashSet<>(values)); return this;
    }

    private static boolean canonicalHttps(URI value) {
        return value != null && value.toASCIIString().length() <= 2048 && "https".equals(value.getScheme())
                && value.getHost() != null && value.getUserInfo() == null && value.getQuery() == null
                && value.getFragment() == null && value.equals(value.normalize()) && !value.toASCIIString().contains("%");
    }
}
