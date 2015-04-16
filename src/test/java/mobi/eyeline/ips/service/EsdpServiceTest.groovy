package mobi.eyeline.ips.service

import mobi.eyeline.ips.external.esdp.EsdpServiceManager
import mobi.eyeline.ips.external.esdp.Service
import mobi.eyeline.ips.model.AccessNumber
import mobi.eyeline.ips.model.Survey
import mobi.eyeline.ips.model.User
import mobi.eyeline.ips.properties.Config
import mobi.eyeline.ips.properties.DefaultMockConfig

import static mobi.eyeline.ips.utils.SurveyBuilder.survey
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasSize

class EsdpServiceTest extends GroovyTestCase {

  UssdService ussdService

  MockEsdpServiceManager serviceManager
  EsdpService esdpService

  void setUp() {
    super.setUp()

    ussdService = new UssdService(
        new DefaultMockConfig(),
        null, null, null, null, null, null, null, null, null) {
      @Override
      String getSurveyUrl(Survey survey) { "http://surveys?id=$survey.id" }
    }

    serviceManager = new MockEsdpServiceManager()
    esdpService = new MockEsdpService(new DefaultMockConfig(), ussdService, serviceManager)
  }

  void testSave() {
    def survey = survey(id: 42) {
      details(title: 'Survey 1')
      owner(id: 53)
    }

    esdpService.save(new User(), survey)

    assertThat serviceManager.calledMethods, hasSize(1)

    Service service = serviceManager.calledMethods[0][1] as Service

    assertEquals 'ips-0053-0042', service.id
    assertEquals 'ips-0053-0042', service.tag
    assertEquals 'Survey 1', service.title
    assertEquals 'http://surveys?id=42',
        service.properties.entry.find { e -> e.key == 'start-page' }.value
  }

  void testDelete() {
    def survey = survey(id: 42) {
      owner(id: 53, esdpProvider: 'ips')
    }

    esdpService.delete(new User(), survey)

    assertThat serviceManager.calledMethods, hasSize(1)
    assertEquals 'ips.ips-0053-0042', serviceManager.calledMethods[0][1]
  }

  void testUpdate1() {

    serviceManager = new MockEsdpServiceManager() {
      @Override
      Service getService(String arg0) {
        new Service(properties: new Service.Properties())
      }
    }
    esdpService = new MockEsdpService(new DefaultMockConfig(), ussdService, serviceManager)

    def survey = survey(id: 42) {
      details(title: 'Survey 1')
      statistics(accessNumber: null)
      owner(id: 53)
    }

    esdpService.update(new User(), survey)

    assertThat serviceManager.calledMethods, hasSize(1)
    def service = serviceManager.calledMethods[0][1]

    assertEquals '', service.properties.entry.find { e -> e.key == 'sip-number' }.value
    assertEquals 'Survey 1', service.title
  }

  void testUpdate2() {

    serviceManager = new MockEsdpServiceManager() {
      @Override
      Service getService(String arg0) {
        new Service(properties: new Service.Properties())
      }
    }
    esdpService = new MockEsdpService(new DefaultMockConfig(), ussdService, serviceManager)

    def survey = survey(id: 42) {
      details(title: 'Survey 1')
      statistics(accessNumber: new AccessNumber(number: '123'))
      owner(id: 53)
    }

    esdpService.update(new User(), survey)

    assertThat serviceManager.calledMethods, hasSize(1)
    def service = serviceManager.calledMethods[0][1]

    assertEquals '123', service.properties.entry.find { e -> e.key == 'sip-number' }.value
    assertEquals 'Survey 1', service.title
  }

  static class MockEsdpService extends EsdpService {

    final EsdpServiceManager api

    MockEsdpService(Config config, UssdService ussdService, EsdpServiceManager api) {
      super(config, ussdService, createMockEsdp(), null)
      this.api = api
    }

    private static EsdpServiceSupport createMockEsdp() {
      new EsdpServiceSupport(null) {
        @Override
        String getServiceUrl(Survey survey) { "http://surveys?id=$survey.id" }
      }
    }

    @Override
    EsdpServiceManager getApi(String login, String passwordHash) { api }
  }

  static class MockEsdpServiceManager implements EsdpServiceManager {

    List<List> calledMethods = []

    @Override
    Service getService(String arg0) {
      calledMethods << ['getService', arg0]; null
    }

    @Override
    void createService(Service arg0) {
      calledMethods << ['createService', arg0]
    }

    @Override
    void updateService(Service arg0) {
      calledMethods << ['updateService', arg0]
    }

    @Override
    void activateCustomPackage(String arg0, int arg1, int arg2, int arg3) {
      calledMethods << ['createService', arg0, arg1, arg2, arg3]
    }

    @Override
    void deleteService(String arg0) {
      calledMethods << ['deleteService', arg0]
    }
  }
}
