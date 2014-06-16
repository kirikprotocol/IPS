package mobi.eyeline.ips.properties

import static junit.framework.TestCase.fail

@SuppressWarnings("GroovyMissingReturnStatement")
class FailingMockConfig implements Config {
    String getSmtpHost() { fail() }
    int getSmtpPort() { fail() }
    String getSmtpUsername() { fail() }
    String getSmtpPassword() { fail() }
    String getMailFrom() { fail() }
    String getLoginUrl() { fail() }

    Properties getDatabaseProperties() { fail() }

    boolean isMadvUpdateEnabled() { fail() }
    int getMadvUpdateDelayMinutes() { fail() }
    String getMadvUrl() { fail() }
    String getMadvUserLogin() { fail() }
    String getMadvUserPassword() { fail() }

    String getSadsPushUrl() { fail() }
    String getSadsSmsPushUrl() { fail() }
    int getSadsMaxSessions() { fail() }
    String getBaseSurveyUrl() { fail() }

    List<LocationProperties> getLocationProperties() { fail() }

    String getDeliveryUssdPushUrl() { fail() }
    String getDeliveryNIPushUrl() { fail() }

    int getPushThreadsNumber() { fail() }
    int getMessageQueueBaseline() { fail() }
    int getStateUpdateBatchSize() { fail() }
    int getRetryAttempts() { fail() }
    long getExpirationDelaySeconds() { fail() }

    boolean getExposeJmxBeans() { fail() }
}