package mobi.eyeline.ips.service;

import mobi.eyeline.ips.messages.AnswerOption;
import mobi.eyeline.ips.messages.MessageHandler;
import mobi.eyeline.ips.messages.MissingParameterException;
import mobi.eyeline.ips.util.RequestParseUtils;
import mobi.eyeline.ips.messages.UssdModel;
import mobi.eyeline.ips.messages.UssdOption;
import mobi.eyeline.ips.model.Answer;
import mobi.eyeline.ips.model.Question;
import mobi.eyeline.ips.model.QuestionOption;
import mobi.eyeline.ips.model.Respondent;
import mobi.eyeline.ips.model.Survey;
import mobi.eyeline.ips.repository.AnswerRepository;
import mobi.eyeline.ips.repository.QuestionOptionRepository;
import mobi.eyeline.ips.repository.RespondentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static mobi.eyeline.ips.util.RequestParseUtils.getInt;
import static mobi.eyeline.ips.util.RequestParseUtils.getString;

/**
 * Mobilizer landing page rendering.
 */
public class UssdService implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(UssdService.class);

    private final RespondentRepository respondentRepository;
    private final AnswerRepository answerRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final SurveyService surveyService;

    /**
     * Generic non-localized messages.
     */
    private final ResourceBundle bundle = ResourceBundle.getBundle("messages");

    public UssdService(SurveyService surveyService,
                       RespondentRepository respondentRepository,
                       AnswerRepository answerRepository,
                       QuestionOptionRepository questionOptionRepository) {

        this.respondentRepository = respondentRepository;
        this.answerRepository = answerRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.surveyService = surveyService;
    }

    public UssdModel handle(Map<String, String[]> parameters)
            throws MissingParameterException {

        try {
            return handle0(parameters);

        } catch (RuntimeException e) {
            logger.error("Error processing USSD request, parameters: " +
                    RequestParseUtils.toString(parameters), e);
            return fatalError();
        }
    }

    private UssdModel handle0(Map<String, String[]> parameters)
            throws MissingParameterException {

        logger.debug("Handling request: " + RequestParseUtils.toString(parameters));

        final String msisdn = getString(parameters, "abonent");

        final UssdOption request = UssdOption.parse(parameters);

        if (request == null) {
            // Respondent just loaded the start page.
            // It might be either an unregistered msisdn (new respondent),
            // survey surveyStart or resumption.
            final int surveyId = getInt(parameters, "survey_id");
            return handleStartPage(msisdn, surveyId);

        } else {
            return request.handle(msisdn, this);
        }
    }

    private UssdModel handleStartPage(String msisdn, int surveyId) {
        final Survey survey = surveyService.findSurvey(surveyId);
        if (survey == null) {
            return surveyNotFound();
        }

        // Ensure we've got an entry in `respondents' for this survey.
        final Respondent respondent =
                respondentRepository.findOrCreate(msisdn, survey);

        final Answer lastAnswer =
                answerRepository.getLast(survey, respondent);

        if (!respondent.isFinished() && lastAnswer != null) {
            final Question next = lastAnswer.getQuestion().getNext();
            if (next != null) {
                // There are unanswered questions, so render the next one.
                return question(next);
            }
        }

        return surveyStart(survey, respondent);
    }

    @Override
    public UssdModel handle(String msisdn, AnswerOption request) {
        final Survey survey = surveyService.findSurvey(request.getSurveyId());
        if (survey == null) {
            return surveyNotFound();
        }

        final Respondent respondent =
                respondentRepository.findOrCreate(msisdn, survey);
        final QuestionOption option =
                questionOptionRepository.load(request.getAnswerId());

        answerRepository.save(respondent, option);

        if (option.isTerminal()) {
            return surveyFinish(respondent, survey);
        }

        final Question next = option.getQuestion().getNext();
        if (next != null) {
            return question(next);

        } else {
            // All the questions are answered.
            return surveyFinish(respondent, option.getQuestion().getSurvey());
        }
    }

    @Override
    public UssdModel handle(String msisdn, UssdOption request) {
        throw new AssertionError("Unsupported request type: " + request.getClass());
    }


    //
    //  Message generators.
    //

    private UssdModel surveyNotFound() {
        return new UssdModel(bundle.getString("ussd.survey.not.available"));
    }

    private UssdModel fatalError() {
        return new UssdModel(bundle.getString("ussd.error"));
    }

    private UssdModel surveyFinish(Respondent respondent, Survey survey) {
        respondent.setFinished(true);
        respondentRepository.update(respondent);

        return new UssdModel(survey.getDetails().getEndText());
    }

    private UssdModel surveyStart(Survey survey, Respondent respondent) {
        respondent.setFinished(false);
        respondentRepository.update(respondent);

        answerRepository.clear(survey, respondent);

        final Iterator<Question> questions = survey.getQuestions().iterator();
        if (questions.hasNext()) {
            return question(questions.next());

        } else {
            // This survey has no questions (if it's even allowed), so just end it.
            return surveyFinish(respondent, survey);
        }
    }

    /**
     * @return Form for the specified question.
     */
    private UssdModel question(Question question) {
        final List<AnswerOption> renderedOptions = new ArrayList<>();
        {
            List<QuestionOption> questionOptions = question.getOptions();
            for (int i = 0; i < questionOptions.size(); i++) {
                renderedOptions.add(
                        new AnswerOption(i + 1, questionOptions.get(i))
                );
            }
        }

        return new UssdModel(question.getTitle(), renderedOptions);
    }
}