/**
 * 
 */
package bohonos.demski.mieldzioc.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import bohonos.demski.mieldzioc.survey.Survey;
import bohonos.demski.mieldzioc.survey.SurveyHandler;

/**
 * @author Dominik Demski
 * Klasa b�d�ca kontrolerem. Dotyczy obs�ugi ankiet (z wyj�tkiem tworzenia nowej i odpowiadania 
 * na istniej�ce ankiety).
 *
 */
public class SurveysTemplateControl {

	private SurveyHandler surveyHandler;

	public SurveysTemplateControl(SurveyHandler surveyHandler) {
		this.surveyHandler = surveyHandler;
	}
	
	/**
	 * Zwraca list� wszystkich szablon�w ankiet znajduj�cych si� w surveyHandler
	 * o zadanym id. Lista jest posortowana w porz�dku
	 * alfabetycznym wzgl�dem tytu��w ankiet (bez znaczenia jest wielko�� znak�w).
	 * @param surveysId id grupy ankiet.
	 * @return lista wszystkich szablon�w ankiet znajduj�cych si� w surveyHandler
	 * o zadanym id w porz�dku alfabetycznym wzgl�dem tytu�u ankiet.
	 */
	public List<Survey> getSurveysWithId(int surveysId){
		Set<Survey> set = surveyHandler.getStatusSurveys(surveysId).keySet();
		List<Survey> list = new ArrayList<Survey>();
		list.addAll(set);
		Collections.sort(list, new Comparator<Survey>() {

			public int compare(Survey o1, Survey o2) {
				return o1.getTitle().compareToIgnoreCase(o2.getTitle());
			}
		});
		return list;
	}
	
	
	
}
