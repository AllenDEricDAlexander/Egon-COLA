package top.egon.cola.component.ddc.service;

import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;

public class DdcInstanceService {

    private static final String SDK_VERSION = "5.2.0-SNAPSHOT";

    private final DdcProperties properties;

    private final DdcAdminClient adminClient;

    private final String instanceId;

    public DdcInstanceService(DdcProperties properties, DdcAdminClient adminClient) {
        this.properties = properties;
        this.adminClient = adminClient;
        this.instanceId = buildInstanceId();
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void register() {
        DdcInstanceRegisterRequest request = new DdcInstanceRegisterRequest();
        fill(request);
        adminClient.register(request);
    }

    public void heartbeat() {
        DdcHeartbeatRequest request = new DdcHeartbeatRequest();
        fill(request);
        adminClient.heartbeat(request);
    }

    public void offline() {
        DdcHeartbeatRequest request = new DdcHeartbeatRequest();
        fill(request);
        adminClient.offline(request);
    }

    private void fill(DdcHeartbeatRequest request) {
        request.setInstanceId(instanceId);
        request.setAppCode(properties.getAppCode());
        request.setEnv(properties.getEnv());
        request.setNamespace(properties.getNamespace());
        request.setHost(host());
        request.setPid(pid());
        request.setSdkVersion(SDK_VERSION);
    }

    private String buildInstanceId() {
        return properties.getAppCode() + "-" + properties.getEnv() + "-" + host() + "-" + pid() + "-" + UuidV7.simpleString().substring(0, 8);
    }

    private String host() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private String pid() {
        return ManagementFactory.getRuntimeMXBean().getName();
    }
}
