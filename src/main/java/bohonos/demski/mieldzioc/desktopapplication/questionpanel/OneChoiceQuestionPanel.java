/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bohonos.demski.mieldzioc.desktopapplication.questionpanel;

import bohonos.demski.mieldzioc.desktopapplication.ApplicationLogic;
import bohonos.demski.mieldzioc.questions.OneChoiceQuestion;
import bohonos.demski.mieldzioc.survey.Survey;

/**
 *
 * @author Andrzej
 */
public class OneChoiceQuestionPanel extends QuestionPanel {

    public OneChoiceQuestionPanel(Survey survey, OneChoiceQuestion oneChoiceQuestion) {
        super(survey, oneChoiceQuestion);
        
        HEIGHT = 80;
        
    }
    
}
