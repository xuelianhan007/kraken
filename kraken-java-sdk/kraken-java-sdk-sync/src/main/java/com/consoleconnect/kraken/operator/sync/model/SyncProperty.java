package com.consoleconnect.kraken.operator.sync.model;

import com.consoleconnect.kraken.operator.core.config.AppConfig;
import java.util.List;
import lombok.Data;

@Data
public class SyncProperty {
  private ControlPlane controlPlane = new ControlPlane();
  private AppConfig.AchieveApiActivityLogConf achieveLogConf =
      new AppConfig.AchieveApiActivityLogConf();
  private MgmtPlane mgmtPlane = new MgmtPlane();
  private List<String> acceptAssetKinds = List.of();
  private boolean assetConfigOverwriteFlag = false;
  private long synDelaySeconds = 60;

  @Data
  public static class ControlPlane {
    private boolean enabled;
    private String url;
    private ExternalAuth auth;
    private String tokenHeader = "Authorization";
    private String retrieveProductReleaseDetailEndpoint =
        "/v2/callback/audits/releases/%s/components";
    protected String defaultProductId = "mef.sonata";

    private String latestDeploymentEndpoint = "/v2/callback/audits/deployments/latest";
    private String apiServerEndpoint = "/v2/callback/audits/api-servers";
    private String syncFromServerEndpoint = "/v2/callback/audits/sync-server-asset";
    private String scanEventEndpoint = "/v2/callback/event";

    private String pushEventEndpoint = "/client/events";

    private String triggerInstallationEndpoint = "/v2/callback/triggers/installation";

    private PushActivityLogExternal pushActivityLogExternal;
  }

  @Data
  public static class ExternalAuth {
    private String authMode;
    private InternalToken internalToken;
    private ClientCredentials clientCredentials;
  }

  @Data
  public static class InternalToken {
    private String accessToken;
  }

  private static final long EXPIRATION_BUFFER_IN_SECONDS = 30;
  private static final String ENDPOINT_AUTH_TOKEN = "/tenant/auth/token";

  @Data
  public static class ClientCredentials {
    private String authServerUrl;
    private String authTokenEndpoint = ENDPOINT_AUTH_TOKEN;
    private String clientId;
    private String clientSecret;
    private Long expirationBufferInSeconds = EXPIRATION_BUFFER_IN_SECONDS;
  }

  @Data
  public static class MgmtPlane {
    private String retrieveProductReleaseEndpoint =
        "/tenant/agent/callback/latest-release-subscription";
    private String downloadMappingTemplateEndpoint =
        "/tenant/agent/callback/mapping-template-download";
    private String mgmtPushEventEndpoint = "/tenant/agent/callback/events";
  }

  @Data
  public static class PushActivityLogExternal {
    private boolean enabled;
    private int batchSize = 200;
  }
}
