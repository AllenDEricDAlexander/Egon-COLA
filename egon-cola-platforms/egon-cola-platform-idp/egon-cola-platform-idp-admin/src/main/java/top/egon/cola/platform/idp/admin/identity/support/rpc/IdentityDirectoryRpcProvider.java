package top.egon.cola.platform.idp.admin.identity.support.rpc;

import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.platform.idp.admin.identity.repo.IdentityUserDirectory;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.rpc.contract.IdentityDirectoryRpc;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesResponse;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IdentityProfile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Maps the IdP identity directory to the minimal read-only RPC projection consumed by RBAC.
 */
@EgonRpcProvider
public final class IdentityDirectoryRpcProvider
        implements IdentityDirectoryRpc {

    private static final int MIN_SUBJECTS = 1;
    private static final int MAX_SUBJECTS = 100;

    private final IdentityUserDirectory directory;

    public IdentityDirectoryRpcProvider(IdentityUserDirectory directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    @Override
    public BatchGetIdentityProfilesResponse batchGetIdentityProfiles(
            BatchGetIdentityProfilesRequest request) {
        Objects.requireNonNull(request, "request");
        List<String> subjects = normalizedSubjects(request.getSubjectsList());
        LinkedHashMap<String, IdentityUser> users = new LinkedHashMap<>();
        for (IdentityUser user : Objects.requireNonNull(
                directory.list(), "directory.list()")) {
            users.putIfAbsent(user.id(), user);
        }

        BatchGetIdentityProfilesResponse.Builder response =
                BatchGetIdentityProfilesResponse.newBuilder();
        for (String subject : subjects) {
            IdentityUser user = users.get(subject);
            if (user == null) {
                response.addMissingSubjects(subject);
                continue;
            }
            response.addProfiles(IdentityProfile.newBuilder()
                    .setSubject(user.id())
                    .setUsername(user.username())
                    .setDisplayName(user.displayName())
                    .setStatus(user.status().name())
                    .setVersion(user.version())
                    .build());
        }
        return response.build();
    }

    private List<String> normalizedSubjects(List<String> values) {
        if (values.size() < MIN_SUBJECTS || values.size() > MAX_SUBJECTS) {
            throw new IllegalArgumentException(
                    "subjects must contain between 1 and 100 values");
        }
        LinkedHashMap<String, Boolean> unique = new LinkedHashMap<>();
        for (String value : values) {
            if (value == null || value.isBlank()
                    || !value.equals(value.trim())) {
                throw new IllegalArgumentException(
                        "subjects must contain non-blank values");
            }
            if (unique.putIfAbsent(value, Boolean.TRUE) != null) {
                throw new IllegalArgumentException(
                        "subjects must not contain duplicates");
            }
        }
        return List.copyOf(unique.keySet());
    }
}
