/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bohonos.demski.mieldzioc.desktopapplication;

import bohonos.demski.mieldzioc.survey.SurveyHandler;
import bohonos.demski.mieldzioc.survey.Survey;
import bohonos.demski.mieldzioc.interviewer.Interviewer;
import bohonos.demski.mieldzioc.questions.Question;
import java.util.GregorianCalendar;

/**
 *
 * @author Andrzej
 */
public class CreatorLogic {
    
    private SurveyHandler surveyHandler;
    private Interviewer interviewer;
    
    /**
     * adds new survey template to survey handler
     * @return id of new survey template
     */
    public String newSurvey() {
        String idOfSurvey = surveyHandler.addNewSurveyTemplate(new Survey(interviewer));
        return idOfSurvey;
    }
    
    /**
     * returns title of survey template with given id
     * @param idOfSurvey id of survey template, we are interested in
     * @return title of survey template
     */
    public String getSurveyTitle(String idOfSurvey) {
        return surveyHandler.getSurvey(idOfSurvey).getTitle();
    }
    
    public void setSurveyTitleAndDescription(String idOfSurvey, String title, String description) throws CloneNotSupportedException {
        Survey survey = surveyHandler.provideSurvey(idOfSurvey);
        survey.setTitle(title);
        survey.setDescription(description);
    }
    
    public String getSurveyDescription(String idOfSurvey) {
        return surveyHandler.getSurvey(idOfSurvey).getDescription();
    }
    
    public CreatorLogic() {
        surveyHandler = new SurveyHandler(0);
        interviewer = new Interviewer("Imi�", "Nazwisko", "PESEL000000", new GregorianCalendar()); //to do
    }
    
}
