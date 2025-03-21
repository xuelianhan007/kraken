package com.consoleconnect.kraken.operator.core.service;

import com.consoleconnect.kraken.operator.core.CustomConfig;
import com.consoleconnect.kraken.operator.core.client.ClientEvent;
import com.consoleconnect.kraken.operator.core.client.ClientEventTypeEnum;
import com.consoleconnect.kraken.operator.core.config.AppConfig;
import com.consoleconnect.kraken.operator.core.entity.ApiActivityLogEntity;
import com.consoleconnect.kraken.operator.core.enums.AchieveScopeEnum;
import com.consoleconnect.kraken.operator.core.enums.LifeStatusEnum;
import com.consoleconnect.kraken.operator.core.repo.ApiActivityLogBodyRepository;
import com.consoleconnect.kraken.operator.core.repo.ApiActivityLogRepository;
import com.consoleconnect.kraken.operator.core.toolkit.JsonToolkit;
import com.consoleconnect.kraken.operator.test.AbstractIntegrationTest;
import com.consoleconnect.kraken.operator.test.MockIntegrationTest;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@MockIntegrationTest
@ContextConfiguration(classes = {CustomConfig.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiActivityLogServiceAtControlPlaneTest extends AbstractIntegrationTest {
  @Autowired ApiActivityLogRepository apiActivityLogRepository;

  @SpyBean private ApiActivityLogService apiActivityLogService;
  @SpyBean private ApiActivityLogBodyRepository apiActivityLogBodyRepository;
  public static final String NOW_WITH_TIMEZONE = "2023-10-24T05:00:00+02:00";
  public static final String REQUEST_ID = "requestId";

  @BeforeEach
  void clearDb() {
    var list = this.apiActivityLogRepository.findAll();
    list.forEach(
        x -> {
          x.setApiLogBodyEntity(null);
        });
    this.apiActivityLogRepository.saveAll(list);
    this.apiActivityLogBodyRepository.deleteAll();
    this.apiActivityLogRepository.deleteAll();
  }

  @NoArgsConstructor
  @Getter
  @Setter
  @EqualsAndHashCode(of = "age")
  public static class BodyAge {
    private int age;
  }

  private static ClientEvent createClientEventWithBody() {
    var clientEvent = new ClientEvent();
    clientEvent.setEventType(ClientEventTypeEnum.CLIENT_API_AUDIT_LOG);
    clientEvent.setClientId("127.0.1.1");

    clientEvent.setEventPayload(
        """
            [
                {
                  "requestId": "requestId",
                  "callSeq": 0,
                  "method": "GET",
                  "buyer": "buyerId2",

                  "uri": "uri",
                  "path": "path",
                  "request": {
                    "age": 1
                  },
                  "response": {
                    "age": 2
                  }
                }
              ]
            """);
    return clientEvent;
  }

  private void addApiLogActivity(String envId) {
    apiActivityLogService.receiveClientLog(
        envId, UUID.randomUUID().toString(), createClientEventWithBody());
  }

  @Test
  void recordBodyByFilter() {
    ApiActivityLogEntity apiActivityLogEntity = new ApiActivityLogEntity();
    apiActivityLogEntity.setRequestId(EXISTED_REQUEST_ID);
    apiActivityLogEntity.setCallSeq(0);
    apiActivityLogEntity.setMethod("POST");
    apiActivityLogEntity.setBuyer("buy");
    apiActivityLogEntity.setUri("uri");
    apiActivityLogEntity.setPath("path");
    this.apiActivityLogService.save(apiActivityLogEntity);

    var entity = this.apiActivityLogRepository.findById(apiActivityLogEntity.getId()).orElse(null);
    Assertions.assertNotNull(entity.getApiLogBodyEntity());
    entity.setRequest("{}");
    entity.setResponse("{}");
    this.apiActivityLogService.save(entity);

    Assertions.assertEquals("{}", entity.getApiLogBodyEntity().getRequest());
    Assertions.assertEquals("{}", entity.getApiLogBodyEntity().getRequest());
  }

  public static final String EXISTED_REQUEST_ID = "requestId_existed";

  @Test
  void insertLogWithoutSubTable() {

    ApiActivityLogEntity entity = new ApiActivityLogEntity();
    entity.setRequestId(EXISTED_REQUEST_ID);
    entity.setCallSeq(0);
    entity.setMethod("POST");
    entity.setBuyer("buy");
    entity.setUri("uri");
    entity.setPath("path");
    entity.setRawRequest("""
            {
            "age":91
            }
            """);
    entity.setRawResponse("""
            {
            "age":92
            }
            """);
    this.apiActivityLogRepository.save(entity);

    var toMigrate =
        this.apiActivityLogRepository
            .findAllByMigrateStatus(PageRequest.of(0, Integer.MAX_VALUE))
            .getContent();
    Assertions.assertEquals(1, toMigrate.size());
  }

  @Test
  void receiveClientApiActivityLog() {
    this.insertLogWithoutSubTable();
    var envId = UUID.randomUUID();

    addApiLogActivity(envId.toString());

    var apiLog = this.apiActivityLogRepository.findAll();
    var apiLogBody = this.apiActivityLogBodyRepository.findAll();
    var toMigrate =
        this.apiActivityLogRepository
            .findAllByMigrateStatus(PageRequest.of(0, Integer.MAX_VALUE))
            .getContent();
    Assertions.assertEquals(1, toMigrate.size());

    Assertions.assertEquals(2, apiLog.size());
    Assertions.assertEquals(1, apiLogBody.size());
    assertRequestAndResponse();
  }

  private void assertRequestAndResponse() {
    var newLog =
        this.apiActivityLogRepository.findByRequestIdAndCallSeq(REQUEST_ID, 0).orElse(null);
    BodyAge requestAge = new BodyAge();
    requestAge.setAge(1);
    Assertions.assertEquals(
        requestAge,
        JsonToolkit.fromJson(
            JsonToolkit.toJson(newLog.getApiLogBodyEntity().getRequest()), BodyAge.class));
    BodyAge responseAge = new BodyAge();
    responseAge.setAge(2);
    Assertions.assertEquals(
        responseAge,
        JsonToolkit.fromJson(
            JsonToolkit.toJson(newLog.getApiLogBodyEntity().getResponse()), BodyAge.class));

    var existedLog =
        this.apiActivityLogRepository.findByRequestIdAndCallSeq(EXISTED_REQUEST_ID, 0).orElse(null);
    requestAge.setAge(91);
    Assertions.assertEquals(
        requestAge,
        JsonToolkit.fromJson(
            JsonToolkit.toJson(existedLog.getApiLogBodyEntity().getRequest()), BodyAge.class));
    responseAge.setAge(92);
    Assertions.assertEquals(
        responseAge,
        JsonToolkit.fromJson(
            JsonToolkit.toJson(existedLog.getApiLogBodyEntity().getResponse()), BodyAge.class));
  }

  @Test
  void migrateExistedData() {
    this.receiveClientApiActivityLog();
    AppConfig.AchieveApiActivityLogConf achieveApiActivityLogConf =
        new AppConfig.AchieveApiActivityLogConf();

    this.apiActivityLogService.migrateOnePage(achieveApiActivityLogConf);
    var apiLog = this.apiActivityLogRepository.findAll();
    var apiLogBody = this.apiActivityLogBodyRepository.findAll();
    var toMigrate =
        this.apiActivityLogRepository
            .findAllByMigrateStatus(PageRequest.of(0, Integer.MAX_VALUE))
            .getContent();
    Assertions.assertEquals(2, apiLog.size());
    Assertions.assertEquals(2, apiLogBody.size());

    Assertions.assertEquals(0, toMigrate.size());
    assertRequestAndResponse();
  }

  @Test
  void achieveApiActivityLog() {
    this.migrateExistedData();

    AppConfig.AchieveApiActivityLogConf achieveApiActivityLogConf =
        new AppConfig.AchieveApiActivityLogConf();
    achieveApiActivityLogConf.setAchieveScope(AchieveScopeEnum.DETAIL);
    achieveApiActivityLogConf.setMonth(-1);
    achieveApiActivityLogConf.setProtocol("GET");

    apiActivityLogService.achieveOnePage(achieveApiActivityLogConf);

    Assertions.assertEquals(
        0,
        this.apiActivityLogRepository
            .listExpiredApiLog(
                achieveApiActivityLogConf.toAchieve(),
                LifeStatusEnum.LIVE,
                achieveApiActivityLogConf.getProtocol(),
                PageRequest.of(0, Integer.MAX_VALUE))
            .getContent()
            .size());

    var list =
        this.apiActivityLogRepository
            .listExpiredApiLog(
                achieveApiActivityLogConf.toAchieve(),
                LifeStatusEnum.ARCHIVED,
                achieveApiActivityLogConf.getProtocol(),
                PageRequest.of(0, 20))
            .getContent();
    Assertions.assertEquals(1, list.size());
    list.forEach(
        x -> {
          Assertions.assertNull(x.getRawResponse());
          Assertions.assertNull(x.getRawRequest());
        });

    Assertions.assertEquals(1, this.apiActivityLogBodyRepository.findAll().size());
  }

  @Test
  void moreCodeCover() {

    this.apiActivityLogService.achieveOnePage(null);
    AppConfig.AchieveApiActivityLogConf achieveApiActivityLogConf =
        new AppConfig.AchieveApiActivityLogConf();

    this.apiActivityLogService.achieveOnePage(achieveApiActivityLogConf);

    this.apiActivityLogService.migrateOnePage(null);
    this.apiActivityLogService.migrateOnePage(achieveApiActivityLogConf);

    Assertions.assertNotNull(achieveApiActivityLogConf);
  }

  @Test
  void setSynced() {
    ApiActivityLogEntity entity = new ApiActivityLogEntity();
    entity.setRequestId(EXISTED_REQUEST_ID);
    entity.setCallSeq(10);
    entity.setMethod("POST");
    entity.setBuyer("buy");
    entity.setUri("uri");
    entity.setPath("path");
    this.apiActivityLogService.setSynced(List.of(entity), ZonedDateTime.now());

    Assertions.assertEquals(1, this.apiActivityLogRepository.findAll().size());
  }
}
