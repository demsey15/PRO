/**
 * 
 */
package bohonos.demski.mieldzioc.networkConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import bohonos.demski.mieldzioc.constraints.IConstraint;
import bohonos.demski.mieldzioc.constraints.NumberConstraint;
import bohonos.demski.mieldzioc.constraints.TextConstraint;
import bohonos.demski.mieldzioc.interviewer.Interviewer;
import bohonos.demski.mieldzioc.interviewer.InterviewerSurveyPrivileges;
import bohonos.demski.mieldzioc.questions.DateTimeQuestion;
import bohonos.demski.mieldzioc.questions.GridQuestion;
import bohonos.demski.mieldzioc.questions.MultipleChoiceQuestion;
import bohonos.demski.mieldzioc.questions.OneChoiceQuestion;
import bohonos.demski.mieldzioc.questions.Question;
import bohonos.demski.mieldzioc.questions.ScaleQuestion;
import bohonos.demski.mieldzioc.questions.TextQuestion;
import bohonos.demski.mieldzioc.survey.Survey;

import com.google.gson.Gson;

/**
 * @author Dominik Demski
 *
 */
public class ServerConnectionFacade {
	
	public final static int PORT = 8046;
//	public final static String HOST = "192.168.0.104";
	public final  String HOST;// = "150.254.79.9";
	
	public final static int BAD_DATA_FORMAT = -2;
	public final static int UNKNOWN_FAIL = -1;
	public final static int LOGIN_OK = 1;
	public final static int BAD_PASSWORD = 2;
	public final static int AUTHORIZATION_FAILED = 3;
	public final static int AUTHORIZATION_OK = 4;
	
	public final static int OPERATION_OK = 5;
	
	public final static int SEND_NEW_TEMPLATE = 10;
	public final static int TEMPLATE_ALREADY_EXISTS = 11;
	
	public final static int CHANGE_SURVEY_STATUS = 12;
	
	public final static int UPDATE_SURVEY_TEMPLATE = 13;
	public final static int SURVEY_UNEDITABLE = 14;
	public final static int LACK_OF_SURVEY_TEMPLATE_WITH_ID = 15;
	
	public final static int SEND_FILLED_SURVEYS = 16;
	public final static int SURVEY_INACTIVE = 17;
	
	public final static int GET_ACTIVE_SURVEY_TEMPLATE = 18;
	public final static int GET_INACTIVE_SURVEY_TEMPLATE = 19;
	public final static int GET_IN_PROGRESS_SURVEY_TEMPLATE = 20;
	
	public final static int ADD_NEW_INTERVIEWER = 21;
	public final static int ADD_NEW_ADMINISTRATOR = 22;
	
	public final static int GET_FILLED_SURVEYS = 23;
	public final static int SENDING_FILLED_SURVEYS = 24;
	
	public final static int GET_ALL_INTERVIEWERS = 25;
	public final static int GET_INTERVIEWER = 26;
	public final static int AUTHENTICATION = 27;
	public final static int DISMISS_INTERVIEWER = 28;
	public final static int BACK_TO_WORK_INTERVIEWER = 29;
	
	public final static int GET_SURVEYS_FILLED_BY_INTERVIEWER = 30;
	
	public final static int SEND_INTERVIEWER_PRIVILAGES = 31;
	public final static int GET_INTERVIEWER_PRIVILAGES = 32;
	public final static int GET_INTERVIEWER_CREATING_PRIVILIGES = 33;
	public final static int SET_INTERVIEWER_CREATING_PRIVILIGES = 34;
	

	public final static int GET_ACTIVE_TEMPLATES_ID_FOR_INTERVIEWER = 35; //pobierz ankiety, kt�re ankieter mo�e wype�nia�
	public final static int GET_EDITABLE_TEMPLATES_ID_FOR_INTERVIEWER = 36; //pobierz ankiety, kt�re ankieter mo�e edytowa�
	public final static int GET_SURVEY_TEMPLATE = 37;
	
	public final static int SERVER_UNAVAILABLE = 38;
	
//	private SocketChannel socketChannel;
	private Socket socketChannel;
	private Scanner in;
	private PrintWriter out;
	//private Gson gson = new Gson();
	
	
	
	public ServerConnectionFacade(String host) {
		this.HOST = host;
	}

	/**
	 * Wysy�a nowy szablon ankiety na serwer.
	 * @param survey szablon.
	 * @param usersId id przesy�aj�cego u�ytkownika (musi mie� uprawnienia do tworzenia ankiet).
	 * @param password has�o u�ytkownika.
	 * @return zwraca BAD_PASSWORD, je�li podane has�o jest b��dne lub nie ma u�ytkownika o podanym id,
	 * AUTHORIZATION_FAILED, je�li u�ytkownik nie ma odpowienich uprawnie�, TEMPLATE_ALREADY_EXISTS,
	 * je�li szablon o podanym id ju� istnieje, OPERATION_OK, je�li operacja przebieg�a pomy�lnie.
	 * SERVER_UNAVAILABLE, je�li nie mo�na po��czy� z serwerem.
	 */
	public int sendSurveyTemplate(Survey survey, String usersId, char[] password){
		if(survey == null ||usersId == null || password == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		System.out.println("��cz�");
		if(!connect())
			return SERVER_UNAVAILABLE;
		
		System.out.println("��cz� po");
		if(!login(usersId, password)){
			disconnect();
			System.out.println("Z�e has�o");
			return BAD_PASSWORD;
		}
		System.out.println("Wysy�am");
		sendInt(SEND_NEW_TEMPLATE);
		int authorization = readInt();
		System.out.println("Otrzyma�em " + authorization);
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return AUTHORIZATION_FAILED; 
		}
		//sendObject(survey);
		sendSurveyTemplate(survey);
		int status = readInt();
		disconnect();
		return status; 
	}
	
	/**
	 * Zmienia status szablonu ankiet.
	 * @param idOfSurveys id grupy ankiet.
	 * @param status status (patrz sta�e w klasie SurevyHandler).
	 * @param usersId id u�ytkownika zmianiaj�cego status (status mo�e zmieni� tylko administrator).
	 * @param password has�o u�ytkownika.
	 * @return BAD_PASSWORD, je�li podano b��dne has�o lub nie ma u�ytkownika o podanym id,
	 * AUTHORIZATION_FAILED, je�li u�ytkownik nie jest administratorem, BAD_DATA_FORMAT, je�li nie ma
	 * ankiety o zadanym id, OPERATION_OK, je�li wszystko przebieg�o pomy�lnie.
	 * SERVER_UNAVAILABLE, je�li nie mo�na po��czy� z serwerem.
	 */
	public int changeSurveyStatus(String idOfSurveys, int status, String usersId, char[] password){
		if(idOfSurveys == null ||usersId == null || password == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return SERVER_UNAVAILABLE;
		
		if(!login(usersId, password)){
			disconnect();
			return BAD_PASSWORD;
		}
		sendInt(CHANGE_SURVEY_STATUS);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return AUTHORIZATION_FAILED;
		}
		sendString(idOfSurveys);
		sendInt(status);
		int operationStatus = readInt();
		disconnect();
		return operationStatus; 
	}
	
	/**
	 * Aktualizuj szablon ankiet.
	 * @param survey szablon.
	 * @param usersId id u�ytkownika.
	 * @param password has�o u�ytwkonika.
	 * @return BAD_PASSWORD, je�li podano b��dne has�o lub nie ma u�ytkownika o podanym id,
	 * AUTHORIZATION_FAILED, je�li u�ytkownik nie ma dopowiednich uprawnie� (uprawnienia do tworzenia
	 * ankiet), BAD_DATA_FORMAT, je�li nie ma
	 * ankiety o zadanym id lub ankieta nie ma statusu IN_PROGRESS,
	 *  OPERATION_OK, je�li wszystko przebieg�o pomy�lnie.
	 *  SERVER_UNAVAILABLE, je�li nie mo�na po��czy� z serwerem.
	 */
	public int updateSurveyTemplate(Survey survey, String usersId, char[] password){
		if(survey == null ||usersId == null || password == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return SERVER_UNAVAILABLE;
		
		if(!login(usersId, password)){
			disconnect();
			return BAD_PASSWORD;
		}
		sendInt(UPDATE_SURVEY_TEMPLATE);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return AUTHORIZATION_FAILED; 
		}
		sendObject(survey);
		int status = readInt();
		disconnect();
		return status; 	
	}
	
	/**
	 * Wysy�a wype�nion� ankiet�.
	 * @param survey wype�niona ankieta (nie mo�e by� r�wna null).
	 * @param usersId id ankietera.
	 * @param password has�o ankietera.
	 * @return kodem: BAD_PASSWORD, je�li podano b��dne has�o lub nie ma u�ytkownika o podanym id,
	 * AUTHORIZATION_FAILED, je�li u�ytkownik nie ma dopowiednich uprawnie� (musi by� ankieterem),
	 * albo lista z kodami (dla ka�dej ankiety odpowiednio): 
	 * BAD_DATA_FORMAT, je�li ankieta o zadanym numerze znajduje si� ju� w repozytorium,
	 *  OPERATION_OK, je�li wszystko przebieg�o pomy�lnie, SURVEY_INACTIVE, je�li ankieta nie ma
	 *  statusu "aktywna".
	 *  SERVER_UNAVAILABLE, je�li nie mo�na po��czy� z serwerem.
	 */
	public int sendFilledSurvey(Survey survey, String usersId, char[] password){
		if(survey == null ||usersId == null || password == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return SERVER_UNAVAILABLE;
	
		if(!login(usersId, password)){
			disconnect();
			return BAD_PASSWORD;
		}
		sendInt(SEND_FILLED_SURVEYS);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return AUTHORIZATION_FAILED;
		}
		sendFilledSurvey(survey);
		int result = readInt();
		disconnect();
		return result;
	}
	
	public List<Survey> getActiveSurveyTemplates(String usersId, char[] password){
		if(usersId == null || password == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return null;
		
		if(!login(usersId, password)){
			disconnect();
			return null;
		}
		sendInt(GET_ACTIVE_SURVEY_TEMPLATE);
		List<Survey> surveys = new ArrayList<Survey>();
		int size = readInt();
		for(int i = 0; i < size; i++){
			surveys.add((Survey) readObject());
		}
		disconnect();
		return surveys;
	}
	
	public List<Survey> getInactiveSurveyTemplates(String usersId, char[] password){
		if(usersId == null || password == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return null;
		
		if(!login(usersId, password)){
			disconnect();
			return null;
		}
		sendInt(GET_INACTIVE_SURVEY_TEMPLATE);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return null; 
		}
		List<Survey> surveys = new ArrayList<Survey>();
		int size = readInt();
		for(int i = 0; i < size; i++){
			surveys.add((Survey) readObject());
		}
		disconnect();
		return surveys;
	}
	
	public List<Survey> getInProgressSurveyTemplates(String usersId, char[] password){
		if(usersId == null || password == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return null;
		
		if(!login(usersId, password)){
			disconnect();
			return null;
		}
		sendInt(GET_IN_PROGRESS_SURVEY_TEMPLATE);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return null; 
		}
		List<Survey> surveys = new ArrayList<Survey>();
		int size = readInt();
		for(int i = 0; i < size; i++){
			surveys.add((Survey) readObject());
		}
		disconnect();
		return surveys;
	}
	
	public int addNewInterviewer(Interviewer interviewerToAdd, char[] passwordToAdd,
									String usersId, char[] password){
		if(interviewerToAdd == null || password == null || usersId == null ||
				passwordToAdd == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return SERVER_UNAVAILABLE;
		
		if(!login(usersId, password)){
			disconnect();
			return BAD_PASSWORD;
		}
		sendInt(ADD_NEW_INTERVIEWER);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return AUTHORIZATION_FAILED; 
		}
		sendObject(interviewerToAdd);
		sendString(new String(passwordToAdd));
		int status = readInt();
		disconnect();
		return status; 	
	}
	
	public int addNewAdministrator(String adminId, char[] adminPassword,
			String usersId, char[] password){
		if(adminId == null || password == null || usersId == null ||
				adminPassword == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return SERVER_UNAVAILABLE;
		
		if(!login(usersId, password)){
		disconnect();
		return BAD_PASSWORD;
		}
		sendInt(ADD_NEW_ADMINISTRATOR);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return AUTHORIZATION_FAILED; 
		}
		sendString(adminId);
		sendString(new String(adminPassword));
		int status = readInt();
		disconnect();
		return status; 	
	}
	
	/**
	 * Pobierz list� wype�nionych ankiet (mo�e to zrobi� tylko administrator).
	 * @param idOfSurveys id grupy ankiet do pobrania.
	 * @param usersId id administratora.
	 * @param password has�o administratora.
	 * @return list� wype�nionych ankiet dla danej grupy ankiet lub null, je�li: podano b��dne
	 * dane logowania, loguj�cy si� u�ytkownik nie jest administratorem, nie ma grupy ankiet o
	 * podanym id lub nie przes�ano jeszcze �adnego wyniku, wyst�pi� nieznany b��d.
	 * null, je�li nie mo�na po��czy� z serwerem.
	 */
	public List<Survey> getFilledSurveys(String idOfSurveys, String usersId, char[] password){
		if(idOfSurveys == null || password == null || usersId == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return null;
		
		if(!login(usersId, password)){
		disconnect();
		return null;
		}
		sendInt(GET_FILLED_SURVEYS);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return null; 
		}
		else{
			int result = readInt();
			if(result == BAD_DATA_FORMAT){
				disconnect();
				return null;
			}
			else{
				int size = readInt();
				List<Survey> list = new ArrayList<Survey>(size);
				for(int i = 0; i < size; i++){
					Survey survey = receiveFilledSurvey();
					list.add(survey);
				}
				if(readInt() == OPERATION_OK){
					disconnect();
					return list;
				}
				else{
					disconnect();
					return null;   //nie powinno si� zdarzy�
				}
			}
		}
	}
	
	/**
	 * Pobierz list� ankieter�w (mo�e to zrobi� tylko administrator).
	 * @param usersId id administratora.
	 * @param password has�o administratora.
	 * @return list� ankieter�w lub null, je�li: podano b��dne
	 * dane logowania, loguj�cy si� u�ytkownik nie jest administratorem, wyst�pi� nieznany b��d.
	 * null, je�li nie mo�na po��czy� z serwerem.
	 */
	public List<Interviewer> getAllInterviewers(String usersId, char[] password){
		if(password == null || usersId == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return null;
		
		if(!login(usersId, password)){
		disconnect();
		return null;
		}
		sendInt(GET_ALL_INTERVIEWERS);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return null; 
		}
		else{
			int size = readInt();
			List<Interviewer> list = new ArrayList<Interviewer>(size);
			for(int i = 0; i < size; i++){
				list.add((Interviewer) readObject());
			}
			if(readInt() == OPERATION_OK){
				disconnect();
				return list;
			}
			else{
				disconnect();
				return null;   //nie powinno si� zdarzy�
			}
			
		}
	}
	
	/**
	 * Pobierz ankietera (mo�e to zrobi� tylko administrator lub ankieter sam siebie).
	 * @param interviewerId id ankietera do pobrania.
	 * @param usersId id pobieraj�cego u�ytkownika.
	 * @param password has�o pobieraj�cego u�ytkownika.
	 * @return ankieter lub null, je�li: podano b��dne
	 * dane logowania, loguj�cy si� u�ytkownik nie ma odpowiednich uprawnie�, brak
	 * ankietera o zadanym id.
	 * null, je�li nie mo�na po��czy� z serwerem.
	 */
	public Interviewer getInterviewer(String interviewerId, String usersId, char[] password){
		if(password == null || usersId == null || interviewerId == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return null;
		
		if(!login(usersId, password)){
		disconnect();
		return null;
		}
		sendInt(GET_INTERVIEWER);
		sendString(interviewerId);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return null; 
		}
		else{
			if(readInt() == OPERATION_OK){
				Interviewer interviewer = (Interviewer) readObject();
				disconnect();
				return interviewer;
			}
			else{
				disconnect();
				return null;   //nie ma takiego interviewera.
			}
			
		}
	}
	
	/**
	 * Sprawd� poprawno�� has�a i loginu.
	 * @param usersId login u�ytkownika.
	 * @param password has�o.
	 * @return true, je�li dane s� poprawne, false w przeciwnym przypadku.
	 */
	public boolean authenticate(String usersId, char[] password){
		if(password == null || usersId == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return false;
		if(!login(usersId, password)){
		disconnect();
		return false;
		}
		else{
			sendInt(AUTHENTICATION);
			disconnect();
			return true;
		}
	}
	
	/**
	 * Zwalnia ankietera o zadanym id.
	 * @param interviewerId id ankietera do zwolnienia.
	 * @param relieveDay data zwolnienia.
	 * @param usersId id admnistratora, kt�ry chce zwolni� pracownika.
	 * @param password has�o administratora.
	 * @return BAD_PASSWORD b��dne dane logowania, AUTHORIZATION_FAILED zalogowany u�ytkownik
	 * nie jest administratorem (tylko administrator mo�e zwolni� ankietera), BAD_DATA_FORMAT,
	 * je�li nie ma ankietera o podanym id, OPERATION_OK, je�li zwolniono ankietera.
	 * SERVER_UNAVAILABLE, je�li nie mo�na po��czy� z serwerem.
	 */
	public int dismissInterviewer(String interviewerId, GregorianCalendar relieveDay,
			String usersId, char[] password){
		if(password == null || usersId == null || interviewerId == null || relieveDay == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return SERVER_UNAVAILABLE;
		
		if(!login(usersId, password)){
		disconnect();
		return BAD_PASSWORD;
		}
		else{
			sendInt(DISMISS_INTERVIEWER);
			int authorization = readInt();
			if(authorization == AUTHORIZATION_FAILED){
				disconnect();
				return AUTHORIZATION_FAILED; 
			}
			else{
				sendString(interviewerId);
				int status = readInt();
				if(status == BAD_DATA_FORMAT){
					disconnect();
					return BAD_DATA_FORMAT;
				}
				else{
					sendObject(relieveDay);
					disconnect();
					return OPERATION_OK;
				}
			}	
		}
	}
	
	/**
	 * Pobierz list� wype�nionych ankiet (mo�e to zrobi� tylko administrator).
	 * @param idOfSurveys id grupy ankiet do pobrania.
	 * @param usersId id administratora.
	 * @param password has�o administratora.
	 * @return list� wype�nionych ankiet dla danej grupy ankiet lub null, je�li: podano b��dne
	 * dane logowania, loguj�cy si� u�ytkownik nie jest administratorem, nie ma grupy ankiet o
	 * podanym id lub nie przes�ano jeszcze �adnego wyniku, wyst�pi� nieznany b��d.
	 * null, je�li nie mo�na po��czy� z serwerem.
	 */
	public List<Survey> getSurveysFilledByInterviewer(String interviewerId, 
			String usersId, char[] password){
		if(interviewerId == null || password == null || usersId == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return null;
		
		if(!login(usersId, password)){
			disconnect();
			return null;
		}
		sendInt(GET_SURVEYS_FILLED_BY_INTERVIEWER);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return null; 
		}
		else{
			sendString(interviewerId);
			int result = readInt();
			if(result == BAD_DATA_FORMAT){
				disconnect();
				return null;
			}
			else{
				int size = readInt();
				List<Survey> list = new ArrayList<Survey>(size);
				for(int i = 0; i < size; i++){
					Survey survey = (Survey) readObject();
					list.add(survey);
				}
				disconnect();
				return list;
			}
		}
	}
	
	/**
	 * Wysy�a na serwer uprawnienia ankietera odno�nie danej grupy ankiet.
	 * @param interviewerId id ankietera.
	 * @param privileges przywileje.
	 * @param idOfSurveys id grupy ankiet.
	 * @param usersId id admnistratora wysy�aj�cego uprawnienia.
	 * @param password has�o.
	 * @return  BAD_PASSWORD, je�li dane do logowania s� niepoprawne, 
	 * AUTHORIZATION_FAILED, je�li nadaj�cy uprawnienia nie jest administratorem,
	 * BAD_DATA_FORMAT, je�li nie ma zadanej grupy ankiet albo nie ma ankietera o zadanym id,
	 * jesli wszystko przebieg�o pomy�lnie - OPERATION_OK.
	 * SERVER_UNAVAILABLE, je�li nie mo�na po��czy� z serwerem.
	 */
	public int sendInterviewerPrivileges(String interviewerId, 
			InterviewerSurveyPrivileges privileges, String idOfSurveys,
			String usersId, char[] password){
		if(interviewerId == null || password == null || usersId == null || privileges == null
				|| idOfSurveys == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		
		if(!connect()) 
			return SERVER_UNAVAILABLE;
		
		if(!login(usersId, password)){
			disconnect();
			return BAD_PASSWORD;
		}
		sendInt(GET_SURVEYS_FILLED_BY_INTERVIEWER);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return AUTHORIZATION_FAILED; 
		}
		else{
			sendString(interviewerId);
			int result = readInt();
			if(result == BAD_DATA_FORMAT){
				disconnect();
				return BAD_DATA_FORMAT;
			}
			else{
				sendObject(privileges);
				sendString(idOfSurveys);
				return readInt();
			}
		}
	}
	
	
	/**
	 * Odczytuje uprawnienia ankietera grup ankiet.
	 * @param interviewerId id ankietera.
	 * @param usersId id u�ytkownika pytaj�cego o uprawnienia. (pyta� mo�e
	 * u�ytkownik o siebie samego albo administrator)
	 * @param password has�o.
	 * @return  null, je�li dane do logowania s� niepoprawne, 
	 * albo pytaj�cy nie ma odpowiednich uprawnie� (pyta� mo�e
	 * u�ytkownik o siebie samego albo administrator),
	 * albo nie ma ankietera o zadanym id,
	 * jesli wszystko przebieg�o pomy�lnie - zwraca map� z danymi.
	 * null, je�li nie mo�na po��czy� z serwerem.
	 */
	public Map<String, InterviewerSurveyPrivileges> getAllInterviewerPrivileges(String interviewerId, 
			String usersId, char[] password){
		if(interviewerId == null || password == null || usersId == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return null;
		
		if(!login(usersId, password)){
			disconnect();
			return null;
		}
		sendInt(GET_SURVEYS_FILLED_BY_INTERVIEWER);
		sendString(interviewerId);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return null; 
		}
		else{
			int result = readInt();
			if(result == BAD_DATA_FORMAT){
				disconnect();
				return null;
			}
			else{
				@SuppressWarnings("unchecked")
				Map<String, InterviewerSurveyPrivileges> map =
						(Map<String, InterviewerSurveyPrivileges>)readObject();
				return map;
				}
			}
	}
	
	/**
	 * Ustala uprawnienia ankietera do tworzenia ankiet.
	 * @param interviewerId id ankietera, kt�remu nale�y nada� uprawnienia.
	 * @param canCreate true, je�li ankieter mo�e tworzy� nowe ankiety.
	 * @param usersId id administratora nadaj�cego uprawnienia. 
	 * @param password has�o administratora.
	 * @return  BAD_PASSWORD, je�li dane do logowania s� niepoprawne, AUTHORIZATION_FAILED
	 * je�li pytaj�cy nie ma odpowiednich uprawnie� (nie jest administratorem),
	 * BAD_DATA_FORMAT, je�li nie ma ankietera o zadanym id,
	 * OPERATION_OK, je�li wszystko przebieg�o pomy�lnie.
	 * SERVER_UNAVAILABLE, je�li nie mo�na po��czy� z serwerem.
	 */
	public int setInterviewerCreatingPrivileges(Interviewer interviewer, boolean canCreate,
			String usersId, char[] password){
		if(interviewer == null || password == null || usersId == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return SERVER_UNAVAILABLE;
		
		if(!login(usersId, password)){
			disconnect();
			return BAD_PASSWORD;
		}
		sendInt(SET_INTERVIEWER_CREATING_PRIVILIGES);
		sendString(interviewer.getId());
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return AUTHORIZATION_FAILED; 
		}
		else{
			int result = readInt();
			if(result == BAD_DATA_FORMAT){
				disconnect();
				return BAD_DATA_FORMAT;
			}
			else{
					sendInt((canCreate)? 1 : 0);
					return OPERATION_OK;
				}
			}
	}
	
	/**
	 * Odczytuje uprawnienia ankietera do tworzenia ankiet.
	 * @param interviewerId id ankietera.
	 * @param usersId id u�ytkownika pytaj�cego o uprawnienia. (pyta� mo�e
	 * u�ytkownik o siebie samego albo administrator)
	 * @param password has�o.
	 * @return  BAD_PASSWORD, je�li dane do logowania s� niepoprawne, AUTHORIZATION_FAILED
	 * je�li pytaj�cy nie ma odpowiednich uprawnie� (pyta� mo�e
	 * u�ytkownik o siebie samego albo administrator),
	 * BAD_DATA_FORMAT, je�li nie ma ankietera o zadanym id,
	 * 1 je�li ankieter mo�e tworzy� ankiety, w przeciwnym przypadku 0.
	 * SERVER_UNAVAILABLE, je�li nie mo�na po��czy� z serwerem.
	 */
	public int getInterviewerCreatingPrivileges(String interviewerId, 
			String usersId, char[] password){
		if(interviewerId == null || password == null || usersId == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return SERVER_UNAVAILABLE;
		
		if(!login(usersId, password)){
			disconnect();
			return BAD_PASSWORD;
		}
		sendInt(GET_INTERVIEWER_CREATING_PRIVILIGES);
		sendString(interviewerId);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return AUTHORIZATION_FAILED; 
		}
		else{
			int result = readInt();
			if(result == BAD_DATA_FORMAT){
				disconnect();
				return BAD_DATA_FORMAT;
			}
			else{
					return readInt();
				}
			}
	}
	
	/**
	 * Pobiera list� z indeksami ankiet, kt�re mo�e wype�nia� dany ankieter.
	 * @param interviewerId id ankietera.
	 * @param usersId id pytaj�cego.
	 * @param password has�o pytaj�cego.
	 * @return null, je�li dane do logowania s� niepoprawne,
	 * je�li pytaj�cy nie ma odpowiednich uprawnie� (pyta� mo�e
	 * u�ytkownik o siebie samego albo administrator),
	 * , je�li nie ma ankietera o zadanym id,
	 * w przeciwnym przypadku list� z indeksami.
	 * null, je�li nie mo�na po��czy� z serwerem.
	 */
	public List<String> getActiveIdTemplateForInterviewer(String interviewerId, String usersId, char[] password){
		if(interviewerId == null || password == null || usersId == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		
		if(!connect())
			return null; 
		
		if(!login(usersId, password)){
			disconnect();
			return null;
		}
		sendInt(GET_ACTIVE_TEMPLATES_ID_FOR_INTERVIEWER);
		sendString(interviewerId);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return null; 
		}
		else{
			int status = readInt();
			if(status == BAD_DATA_FORMAT){
				disconnect();
				return null;
			}
			else{
				int size = readInt();
				List<String> result = new ArrayList<String>(size);
				for(int i = 0; i < size; i++){
					result.add(readString());
				}
				return result;
			}
		}
	}
	
	/**
	 * Pobiera list� z indeksami ankiet, kt�re mo�e edytowa� dany ankieter.
	 * @param interviewerId id ankietera.
	 * @param usersId id pytaj�cego.
	 * @param password has�o pytaj�cego.
	 * @return null, je�li dane do logowania s� niepoprawne,
	 * je�li pytaj�cy nie ma odpowiednich uprawnie� (pyta� mo�e
	 * u�ytkownik o siebie samego albo administrator),
	 * , je�li nie ma ankietera o zadanym id,
	 * w przeciwnym przypadku list� z indeksami.
	 * SERVER_UNAVAILABLE, je�li nie mo�na po��czy� z serwerem.
	 */
	public List<String> getEditableIdTemplateForInterviewer(String interviewerId, String usersId, char[] password){
		if(interviewerId == null || password == null || usersId == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect()) 
			return null;
		
		if(!login(usersId, password)){
			disconnect();
			return null;
		}
		sendInt(GET_EDITABLE_TEMPLATES_ID_FOR_INTERVIEWER);
		sendString(interviewerId);
		int authorization = readInt();
		if(authorization == AUTHORIZATION_FAILED){
			disconnect();
			return null; 
		}
		else{
			int status = readInt();
			if(status == BAD_DATA_FORMAT){
				disconnect();
				return null;
			}
			else{
				int size = readInt();
				List<String> result = new ArrayList<String>(size);
				for(int i = 0; i < size; i++){
					result.add(readString());
				}
				return result;
			}
		}
	}
	
	/**
	 * Pobierz szablon ankiety.
	 * @param idOfSurveys id szablonu.
	 * @param usersId id pytaj�cego.
	 * @param password has�o pytaj�cego.
	 * @return null, je�li dane do logowania s� niepoprawne,
	 * je�li pytaj�cy nie ma odpowiednich uprawnie� (nieaktywne ankiety mo�e pobra�
	 * tylko admnistrator, inne ka�dy),
	 * , je�li nie ma szablonu o zadanym id,
	 * w przeciwnym przypadku ankiet�.
	 * null, je�li nie mo�na po��czy� z serwerem.
	 */
	public Survey getSurveyTemplate(String idOfSurveys, String usersId, char[] password){
		if(idOfSurveys == null || password == null || usersId == null)
			throw new NullPointerException("Przekazane argumenty nie mog� by� nullami.");
		if(!connect())
			return null;
		
		if(!login(usersId, password)){
			disconnect();
			return null;
		}
		sendInt(GET_SURVEY_TEMPLATE);
		sendString(idOfSurveys);
		int status = readInt();
		if(status == BAD_DATA_FORMAT){
			disconnect();
			return null;
		}
		else{
			int authorization = readInt();
			if(authorization == AUTHORIZATION_FAILED){
				disconnect();
				return null;
			}
			else{
				return receiveSurveyTemplate();
			}
		}
	}
	
	public boolean tryConnection(){
		boolean result = connect();
		if(result) disconnect();
		return result;
	}
	
	private boolean login(String usersId, char[] password){
		System.out.println("Wysy�am id");
		sendString(usersId);
		System.out.println("Wysy�am has�o");
		sendString(new String(password));
		
		int answer = readInt();
		
			for(int i = 0; i < password.length; i++){
				password[i] = 'a';
			}
		return (answer != BAD_PASSWORD);
	}
	
	private boolean connect(){
		try {
			//socketChannel = SocketChannel.open(new InetSocketAddress(HOST, PORT));
			socketChannel = new Socket();
			socketChannel.setSoTimeout(20000); //20 sekund na wys�anie i odebranie danych
			socketChannel.connect(new InetSocketAddress(HOST, PORT), 2000); //20 sekund na po��czenie
			//out = new PrintWriter(Channels.newOutputStream(socketChannel), true);
			//in = new Scanner(socketChannel);
			out = new PrintWriter(socketChannel.getOutputStream(), true);
			in = new Scanner(socketChannel.getInputStream());
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	private void disconnect(){
			try {
				out.close();
				in.close();
				socketChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	private void sendInt(int i){
		System.out.println("Wysy�am: " + i);	
		out.println(i);
	}
	
	private void sendObject(Object obj){
		try {
			
		//	ObjectOutputStream outOb = new ObjectOutputStream(Channels.newOutputStream(socketChannel));
			ObjectOutputStream outOb = new ObjectOutputStream(socketChannel.getOutputStream());
			outOb.writeObject(obj);
			outOb.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendString(String s){
		System.out.println("Wysy�am: " + s);	
		out.println(s);
	}
	
	private int readInt(){
		try{
			int i = Integer.parseInt(in.nextLine());
			System.out.println("Odczyta�em: " + i);
			return i;
		}
		catch(NumberFormatException e){
			return BAD_DATA_FORMAT;
		}
	}
	
	private String readString(){
		String received =  in.nextLine();
		System.out.println("Odczyta�em: " + received);
		return received;
	}
	
	private Object readObject(){
		try {
			//ObjectInputStream inObj = new ObjectInputStream(Channels.newInputStream(socketChannel));
			ObjectInputStream inObj = new ObjectInputStream(socketChannel.getInputStream());
			Object obj = inObj.readObject();
			System.out.println("Odczyta�em obiekt: " + obj);
			return obj;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void sendSurveyTemplate(Survey survey){
		sendString(survey.getTitle());
		sendString(survey.getDescription());
		sendString(survey.getSummary());
		sendString(survey.getIdOfSurveys());
		Gson gson = new Gson();
		sendString(gson.toJson(survey.getInterviewer()));
		int i = survey.questionListSize();
		sendInt(i);
		for(int j = 0; j < i; j++){
			Question question = survey.getQuestion(j);
			int type = question.getQuestionType();
			sendInt(type);
			if(type == Question.TEXT_QUESTION){
				TextQuestion txt = (TextQuestion) question;
				sendString(txt.getErrorMessage());
				sendString(txt.getHint());
				sendString(txt.getPictureURL());
				sendString(txt.getQuestion());
				IConstraint constraint = txt.getConstraint();
				if(constraint != null){
					if(constraint instanceof TextConstraint){
						sendString("text");
						TextConstraint textConst = (TextConstraint) constraint;
						sendString((textConst.getMaxLength() == null)? "null" : "" + textConst.getMaxLength());
						sendString((textConst.getMinLength() == null)? "null" : "" + textConst.getMinLength());
						sendString((textConst.getRegex() == null)? "null" : textConst.getRegex().pattern());
					}
					else{
						sendString("number");
						NumberConstraint numbConst = (NumberConstraint) constraint;
						sendString((numbConst.getMaxValue() == null)? "null" : "" + numbConst.getMaxValue());
						sendString((numbConst.getMinValue() == null)? "null" : "" + numbConst.getMinValue());
						sendString((numbConst.getNotEquals() == null)? "null" : "" + numbConst.getNotEquals());
						sendString((numbConst.isMustBeInteger())? "true" : "false");
						sendString((numbConst.isNotBetweenMaxAndMinValue())? "true" : "false");
						System.out.println("Ograniczenie liczbowe min: " + ((((NumberConstraint) constraint).getMinValue() == null)? "null" : ((NumberConstraint) constraint).getMinValue()));
					}
				}
				else sendString("nothing"); //brak ogranicze�
			}
			else{
				sendString(question.toJson());
			}
		}
	}
	
	private Survey receiveSurveyTemplate(){
		Survey survey = new Survey(null);
		String read1;
		survey.setTitle(((read1 = readString()) == null)? null : read1);
		survey.setDescription(((read1 = readString()) == null)? null : read1);
		survey.setSummary(((read1 = readString()) == null)? null : read1);
		survey.setIdOfSurveys(readString());
		Gson gson = new Gson();
		survey.setInterviewer(gson.fromJson(readString(), Interviewer.class));
	
		int i = readInt();
		for(int j = 0; j < i; j++){
			int type = readInt();
			Question question;
			if(type == Question.DATE_QUESTION || type == Question.TIME_QUESTION){
				question = gson.fromJson(readString(), DateTimeQuestion.class);
			}
			else if(type == Question.DROP_DOWN_QUESTION || type == Question.ONE_CHOICE_QUESTION){
				question = gson.fromJson(readString(), OneChoiceQuestion.class);
			}
			else if(type == Question.GRID_QUESTION){
				question = gson.fromJson(readString(), GridQuestion.class);
			}
			else if(type == Question.MULTIPLE_CHOICE_QUESTION){
				question = gson.fromJson(readString(), MultipleChoiceQuestion.class);
			}
			else if(type == Question.SCALE_QUESTION){
				question = gson.fromJson(readString(), ScaleQuestion.class);
			}
			else{
				TextQuestion txt = new TextQuestion();
				txt.setErrorMessage(readString());
				txt.setHint(readString());
				txt.setPictureURL(readString());
				txt.setQuestion(readString());
				IConstraint constraint;
				String s = readString();
				if(s.equals("text")){
					String read;
					Integer maxLength = ((read = readString()).equals("null"))? null : Integer.parseInt(read);
					Integer minLength = ((read = readString()).equals("null"))? null : Integer.parseInt(read);
					String regexS = ((read = readString()).equals("null"))? null : read;
					Pattern regex = null;
					if(regexS != null)
						regex = Pattern.compile(regexS);
					constraint = new TextConstraint(minLength, maxLength, regex);
				}
				else if(s.equals("number")){
					String read;
					Double maxValue = ((read = readString()).equals("null"))? null : Double.parseDouble(read);
					Double minValue = ((read = readString()).equals("null"))? null : Double.parseDouble(read);
					Double notEquals = ((read = readString()).equals("null"))? null : Double.parseDouble(read);
					boolean mustBeInteger = Boolean.valueOf(readString());
					boolean notBetweenMaxAndMinValue = Boolean.valueOf(readString());
					constraint = new NumberConstraint(minValue, maxValue, mustBeInteger, notEquals, notBetweenMaxAndMinValue);
				}
				else constraint = null;
				txt.setConstraint(constraint);
				question = txt;
			}
			survey.addQuestion(question);
		}
		return survey;
	}
	
	private void sendFilledSurvey(Survey survey){
		sendSurveyTemplate(survey);
		
		sendInt(survey.getNumberOfSurvey());
	
		Gson gson = new Gson();
		sendString(gson.toJson(survey.getStartTime()));
		sendString(gson.toJson(survey.getFinishTime()));
		
		for(int i = 0; i < survey.questionListSize(); i++){
			List<String> answers = survey.getQuestion(i).getUserAnswersAsStringList();
			sendInt(answers.size());
			
			for(int j = 0; j < answers.size(); j++){
				sendString(answers.get(j));
			}
		}
	}
	
	private Survey receiveFilledSurvey(){
		Survey survey = receiveSurveyTemplate();
		int surveyNumber = readInt();
		survey.setNumberOfSurvey(surveyNumber);
		Gson gson = new Gson();
		String fromString = readString();
		System.out.println("Odczyta�em dat�: " + fromString);
		GregorianCalendar from = gson.fromJson(fromString, GregorianCalendar.class);
		GregorianCalendar to = gson.fromJson(readString(), GregorianCalendar.class);
		survey.setStartTime(from);
		survey.setFinishTime(to);
		
		for(int i = 0; i < survey.questionListSize(); i++){
			int amount = readInt();
			System.out.println("W pytaniu: " + i + " mam " + amount + " odpowiedzi");
			List<String> answers = new ArrayList<String>(amount);
			for(int j = 0; j < amount; j++){
				answers.add(readString());
			}
			survey.getQuestion(i).setUserAnswers(answers);
		}
		return survey;
	}
	
	public static void main(String[] args) {
		ServerConnectionFacade facade = new ServerConnectionFacade("150.254.79.95");
		Interviewer interviewer = new Interviewer("", "", "12345678999", new GregorianCalendar());
		interviewer.setInterviewerPrivileges(true);
		Survey survey = new Survey(interviewer);
		survey.setIdOfSurveys("ja");
		Survey survey2 = new Survey(interviewer);
		survey2.setIdOfSurveys("ja");
		
	//	List<String> list = (List<String>) facade.getActiveIdTemplateForInterviewer(interviewer.getId(), interviewer.getId(), new char[] {'a', 'b', 'c'});
    //	System.out.println(Arrays.toString(list.toArray()));
		facade.sendSurveyTemplate(survey, interviewer.getId(), new char[] {'a', 'b', 'c'});
    //	facade.changeSurveyStatus(survey.getIdOfSurveys(), SurveyHandler.ACTIVE, "admin", new char[] {'a', 'd', 'm', 'i', 'n'});
    //	facade.updateSurveyTemplate(survey, interviewer.getId(), new char[] {'a', 'b', 'c'});
    	//facade.changeSurveyStatus(survey.getIdOfSurveys(), SurveyHandler.IN_PROGRESS, "admin", new char[] {'a', 'd', 'm', 'i', 'n'});
    	//facade.updateSurveyTemplate(survey, interviewer.getId(), new char[] {'a', 'b', 'c'});
    	//facade.sendSurveyTemplate(survey, interviewer.getId(), new char[] {'a', 'b', 'c'});
    //	Survey su = facade.getSurveyTemplate("12345678999000001", interviewer.getId(), new char[] {'a', 'b', 'c'});
    	Gson gson = new Gson();
    	GridQuestion q = new GridQuestion();
    	q.setQuestion("Nana");
    	String s = gson.toJson(q);
    	System.out.println(s);
    	Question e = gson.fromJson(s, GridQuestion.class);
    	System.out.println(e.getQuestion());
		//System.out.println(su.getIdOfSurveys());
	}
}
