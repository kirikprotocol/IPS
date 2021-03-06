package mobi.eyeline.ips.utils

import groovy.transform.InheritConstructors
import mobi.eyeline.ips.model.*

import static mobi.eyeline.ips.utils.ModelBuilderUtils.Context
import static mobi.eyeline.ips.utils.ModelBuilderUtils.Deferred.DeferredReference
import static mobi.eyeline.ips.utils.ModelBuilderUtils.Deferred.resolve
import static mobi.eyeline.ips.utils.ModelBuilderUtils.ListContext

@SuppressWarnings("GrMethodMayBeStatic")
class SurveyBuilder {

  static Survey survey(Map _ = [:]) { new Survey(_) }

  static Survey survey(Map _, @DelegatesTo(SurveyContext) Closure closure) {
    new SurveyContext(survey(_)).invoke closure
  }

  static Survey survey(@DelegatesTo(SurveyContext) Closure closure) {
    new SurveyContext(null).invoke closure
  }

  static List<Survey> surveys(Map _, @DelegatesTo(SurveysContext) Closure closure) {
    new SurveysContext(_).invoke closure
  }

  static List<Answer> answers(Map _, @DelegatesTo(AnswersContext) Closure closure) {
    new AnswersContext(_).invoke closure
  }

  static Question question(Map _, @DelegatesTo(QuestionContext) Closure closure) {
    new PagesContext(new SurveyContext(null), [:]).question(_, closure)
  }

  @InheritConstructors
  static class SurveysContext extends ListContext<Survey> {

    Survey survey(Map _, @DelegatesTo(SurveyContext) Closure closure) {
      create(new Survey(common + _)) {
        new SurveyContext(it).with closure
      }
    }

    List<Survey> invoke(@DelegatesTo(SurveysContext) Closure closure) { super.invoke closure }
  }

  @InheritConstructors
  static class AnswersContext extends ListContext<Answer> {

    OptionAnswer optionAnswer(Map _) {
      add(new OptionAnswer(common + _))
    }

    TextAnswer textAnswer(Map _) {
      add(new TextAnswer(common + _))
    }

    List<Answer> invoke(@DelegatesTo(AnswersContext) Closure closure) { super.invoke closure }
  }

  @InheritConstructors
  static class PagesContext extends ListContext<Page> {
    protected SurveyContext surveyContext = new SurveyContext(null)

    PagesContext(SurveyContext surveyContext, Map commonArgs) {
      this(commonArgs)
      this.surveyContext = surveyContext
    }

    Question question(Map _) {
      surveyContext.bind(add(new Question(_))) {
        surveyContext.enclosing.pages << it
        it.survey = surveyContext.enclosing
      }
    }

    Question question(Map args, @DelegatesTo(QuestionContext) Closure closure) {
      question(question(args), closure)
    }

    ExtLinkPage extLink(Map _) {
      surveyContext.bind(add(new ExtLinkPage(_))) {
        surveyContext.enclosing.pages << it
        it.survey = surveyContext.enclosing
      }
    }

    ExtLinkPage extLink(Map args, @DelegatesTo(ExtLinkContext) Closure closure) {
      extLink extLink(args), closure
    }

    static Question question(Question question, @DelegatesTo(QuestionContext) Closure closure) {
      new QuestionContext(question).with closure
      question
    }

    static ExtLinkPage extLink(ExtLinkPage extLink, @DelegatesTo(ExtLinkContext) Closure closure) {
      new ExtLinkContext(extLink).with closure; extLink
    }

    List<Page> invoke(@DelegatesTo(PagesContext) Closure closure) {
      super.invoke closure

      final questions = list.findAll { it instanceof Question } as List<Question>

      // Resolve deferred references.
      questions.collectMany { it.options }
          .findAll { QuestionOption opt -> opt.nextPage instanceof DeferredReference }
          .each { QuestionOption opt ->
        opt.nextPage = (opt.nextPage as DeferredReference<Page>).resolve(list)
      }

      questions.findAll { Question q -> q.defaultPage instanceof DeferredReference }
          .each { Question q ->
        q.defaultPage = (q.defaultPage as DeferredReference<Page>).resolve(list)
      }

      list
    }

    /**
     * @return Page reference.
     */
    Page ref(Map classifier) { new DeferredPage(classifier) }
  }

  /**
   * Utilities for creation of {@link Survey} properties.
   */
  @InheritConstructors
  static class SurveyContext extends Context<Survey> {

    /**
     * Creates {@link SurveyDetails} and binds to the enclosing survey.
     */
    SurveyDetails details(Map args) {
      bind(new SurveyDetails(args)) {
        it.survey = enclosing
        enclosing.details = it
      }
    }

    /**
     * Creates {@link SurveyInvitation} and binds to the enclosing survey.
     */
    SurveyInvitation invitation(Map args) {
      bind(new SurveyInvitation(args)) {
        it.survey = enclosing
      }
    }

    /**
     * Creates a {@link User} and binds to the enclosing survey as an owner.
     */
    User owner(Map args) {
      bind(new User(args)) {
        it.createdSurveys = it.createdSurveys ?: []
        it.createdSurveys << enclosing
        enclosing.owner = it
      }
    }

    /**
     * Creates {@link SurveyStats} and binds to the enclosing survey.
     */
    SurveyStats statistics(Map args) {
      bind(new SurveyStats(args)) {
        it.survey = enclosing
        enclosing.statistics = it
      }
    }

    /**
     * Creates {@link SurveyStats} and binds to the enclosing survey.
     * @param closure Invoked in the corresponding {@link SurveyStatisticsContext}
     */
    SurveyStats statistics(Map args, @DelegatesTo(SurveyStatisticsContext) Closure closure) {
      SurveyStats statistics = statistics(args)
      new SurveyStatisticsContext(statistics).with closure
      statistics
    }

    /**
     * Allows to create a set of {@link Question questions} and binds them to the enclosing survey.
     */
    List<Question> pages(@DelegatesTo(PagesContext) Closure closure) {
      new PagesContext(this, [:]).invoke closure
    }

  }

  @InheritConstructors
  static class SurveyStatisticsContext extends Context<SurveyStats> {
  }

  @InheritConstructors
  static class QuestionContext extends Context<Question> {

    QuestionOption option(Map _) {
      bind(new QuestionOption(_)) {
        enclosing.options << it
        it.question = enclosing
      }
    }
  }

  @InheritConstructors
  static class ExtLinkContext extends Context<ExtLinkPage> {

  }

  static class DeferredPage extends Page implements DeferredReference<Page> {
    final Map classifier
    DeferredPage(Map classifier) { this.classifier = classifier }

    Page resolve(List<Page> others) { resolve(others, classifier) }

    @Override String getTitle() { '' }
    @Override int getActiveIndex() { 0 }
  }

}
