package top.egon.cola.platform.idp.rpc.contract;

import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.BatchGetIdentityProfilesResponse;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.GetTenantMembershipRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.GetTenantMembershipResponse;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IdentityDirectoryServiceGrpc;

/**
 * Read-only IdP identity directory contract for RBAC profile enrichment.
 */
@EgonRpcService(
        grpcClass = IdentityDirectoryServiceGrpc.class,
        group = IdentityDirectoryRpc.GROUP,
        version = IdentityDirectoryRpc.VERSION
)
public interface IdentityDirectoryRpc {

    String GROUP = "idp";

    String VERSION = "1.0.0";

    /**
     * Resolves the minimal display profile for a bounded subject batch.
     */
    @EgonRpcMethod(name = "BatchGetIdentityProfiles")
    BatchGetIdentityProfilesResponse batchGetIdentityProfiles(
            BatchGetIdentityProfilesRequest request);

    /**
     * Resolves one tenant membership fact without exposing RBAC identifiers or policy data.
     */
    @EgonRpcMethod(name = "GetTenantMembership")
    GetTenantMembershipResponse getTenantMembership(
            GetTenantMembershipRequest request);
}
