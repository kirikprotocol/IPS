package mobi.eyeline.ips.service

import mobi.eyeline.ips.model.Survey
import mobi.eyeline.ips.repository.DbTestCase
import mobi.eyeline.ips.repository.InvitationDeliveryRepository
import mobi.eyeline.ips.repository.SurveyRepository

class SurveyServiceTest extends DbTestCase {

    Survey survey

    SurveyRepository surveyRepository
    InvitationDeliveryRepository invitationDeliveryRepository
    SurveyService surveyService

    void setUp() {
        super.setUp()

        survey = new Survey(id: 1)

        surveyRepository = new SurveyRepository(db)
        invitationDeliveryRepository = new InvitationDeliveryRepository(db)

        surveyService = new SurveyService(surveyRepository, null, invitationDeliveryRepository)
    }

    void test1() {
        assertNull surveyService.findSurvey(2, true)
        assertNull surveyService.findSurvey(2, false)
    }
}
