package mobi.eyeline.ips.service

import mobi.eyeline.ips.model.Survey
import mobi.eyeline.ips.model.User

class MockMailService extends MailService {

  List<List> calledMethods = []

  MockMailService() {
    super(newTemplateService(), newSmtpSender())
  }

  private static TemplateService newTemplateService() {
    new TemplateService(null)
  }

  private static SmtpSender newSmtpSender() {
    new SmtpSender('', 0, '', '', '')
  }

  @Override
  void sendCouponRemaining(User user, Survey survey, int percent, long remainingNumber) {
    calledMethods << ['sendCouponRemaining', user, survey, percent, remainingNumber]
  }

  @Override
  void sendCouponExhaustion(User user, Survey survey) {
    calledMethods << ['sendCouponExhaustion', user, survey]
  }
}
