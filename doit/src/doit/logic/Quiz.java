package doit.logic;

import doit.xml.Parser;

import java.util.Map;
import java.util.Date;
import java.util.HashMap;

//
//Die Quiz-Klasse erstellt ein neues Quiz
//und bietet Methoden um den Quiz-Verlauf
//zu steuern und zu verwalten
//
//Konstruktoren:
//
//Quiz()
//Quiz(boolean weightedRandom)
//Quiz(int questionsLenth, boolean weightedRandom)
//Quiz(String[] categories, int questionsLength, boolean weightedRandom)
//
//�ffentliche Methoden:
//
//getTotalQuestionsCount():int
//getDoneQuestionsCount():int
//getCorrectQuestionsCount():int
//getNextQuestion():Question
//evaluateAnswers(Question question, boolean[] answers):boolean[]
//getResult():Result
//

public class Quiz {

	//Tick-Counter
	public Timer timer;
	
	//Verf�gbare Fragen pro Kategorie (assoziatives Array)
	private Map<String, Integer> categoryMap;
	private Question[] questions;
	private String[] categories;
	private String userName;
	private int questionsLength;
	private int currentQuestion;
	private int questionsDone;
	private int questionsCorrect;
	
	//Standard-Konstruktor (25 Fragen aus allen Kategorien)
	public Quiz() {
		this.categories = Parser.getCategoryList();
		this.questionsLength = 20;
		InitializeValues(true);
	}
	
	//...mit �bergabe der Zufallsfunktion
	public Quiz(boolean weighted) {
		this.categories = Parser.getCategoryList();
		this.questionsLength = 20;
		InitializeValues(weighted);
	}
	
	//...mit �bergabe von L�nge und Zufallsfunktion
	public Quiz(int questionsLength, boolean weighted) {
		this.categories = Parser.getCategoryList();
		this.questionsLength = questionsLength;
		InitializeValues(weighted);
	}
	
	//...mit �bergabe von Kategorien, L�nge und Zufallsfunktion
	public Quiz(String[] categories, int questionsLength, boolean weighted) {
		this.questionsLength = questionsLength;
		this.categories = categories;
		InitializeValues(weighted);
	}

	//Gemeinsame variablen initialisieren
	private void InitializeValues(boolean weighted) {
		categoryMap = createCategoryMap(categories);
		questions = createQuestionsArray(categoryMap, questionsLength, weighted);
		if(questionsLength < 1)
			questionsLength = 1;
		int questionsSum = calcQuestionsSum(categoryMap);
		if(questionsLength > questionsSum)
			questionsLength = questionsSum;
		timer = new Timer();
		currentQuestion = 0;
		questionsDone = 0;
		questionsCorrect = 0;
	}

	//
	//GETTER(SETTER):
	//
	
	//Eingabe d. Benutzernamens
	public void setUsername(String userName){this.userName = userName;}	
	//Abfrage d. Kategorieliste
	public String[] getCategories() {return (String[])categoryMap.keySet().toArray(new String[0]);}
	//Abfrage d. Benutzernamens
	public String getUsername() {return userName;}
	//Direkte Abfrage der Quizl�nge
	public int getTotalQuestionsCount() {return questionsLength;}
	//Anzahl beanworteter Fragen
	public int getDoneQuestionsCount() {return questionsDone;}
	//Anzahl richtig beantworteter Fragen
    public int getCorrectQuestionsCount() {return questionsCorrect;}
	
    //
    //HAUPTMETHODEN:
    //
    
	//n�chste Frage aus Array �bergeben
	public Question getNextQuestion() {
		if(currentQuestion < questionsLength) {
			currentQuestion++;
			if(!timer.isRunning())
				timer.start();
			questions[currentQuestion - 1].setStartTime(new Date().getTime());
			return questions[currentQuestion - 1];
		}
		return null;
	}
	
	//Evaluiert die �bergebene Frage/Antwort  
	public boolean[] evaluateAnswers(Question question, boolean[] answers) {
		//R�ckgabe-Array (welche Vorschl�ge wurden korrekt "gechecked"?)
		boolean[] evaluation = new boolean[answers.length];
		//Evaluierung (alle Vorschl�ge korrekt "gechecked"?)
		boolean isCorrect = true;
		//f�r alle Vorschl�ge...
		for(int i=0; i<answers.length; i++) {
			//Antwort mit Vorschlag vergleichen (Vorschlag "gechecked" == "Vorschlag beginnt mit #"
			evaluation[i] = answers[i] == question.getProposals()[i].substring(0,1).equals("#");
			//EIN Vorschlag falsch "gechecked" => Anwort nicht korrekt!
			if(!evaluation[i])
				isCorrect = false;
		}
		//Status der Frage setzen
		if(!question.isCorrect() && isCorrect) {
			question.setCorrect(true);
			questionsCorrect++;
		}
		if(!question.isDone()) {
			question.setDone(true);
			questionsDone++;
		}
		//Ende der Bearbeitungszeit
		question.setStopTime(new Date().getTime());
		//Antworten f�r sp�tere Auswertung speichern
		question.setAnswers(answers);
		//R�ckgabe der Evaluation
		return evaluation;
	}
	
	//Quiz-Auswertung...
	public Result getQuizResult() {
		Result result = new Result();
		//Laufzeit in Sekunden
		result.setQuizDuration((int)(timer.getDuration() / 1000));
		//Resultat (in Prozent)
		if(questionsDone > 0)
			result.setResultPercent((double)questionsCorrect / (double)questionsDone);
		//Resultat (als IHK-Benotung)
		result.setResultIHK(ResultFunctions.getResultIHK(result.getResultPercent()));
		return result;
	}
	
	//
	//STATISCHE HILFSMETHODEN:
	//
	
	//Anzahl der verf�gbaren Fragen pro Kategorie ermitteln
	//und in HashMap hinterlegen (f�r Zufallsfunktion: Obergrenze/Gewichtung)
	private static Map<String, Integer> createCategoryMap(String[] categoriesArray) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for(int i=0; i<categoriesArray.length; i++)
			//Name der Kategorie <=> Anzahl der Fragen
			map.put(categoriesArray[i], Parser.getQuestionsCount(categoriesArray[i]));
		return map;
	}

	//Question-Array erstellen und "bef�llen"...
	private static Question[] createQuestionsArray(Map<String, Integer> map, int length, boolean weighted) {
		Question[] questionsArray = new Question[length];
		for(int i=0; i<length; i++) {
			//Variablen deklarieren
			String category, questionID;
			//Kategorien-Array aus HashMap
			String[] categoriesArray = map.keySet().toArray(new String[0]);
			do {
				//wenn weighted und min. zwei Kategorien...
				if(categoriesArray.length > 1 && weighted)
					category = weightedRandomCategory(map);
				//...sonst
				else category = randomCategory(categoriesArray);
				questionID = randomID(category, map);
				//Schleife wiederholen solange Frage bereits vorhanden!
			} while(checkQuestionUID(questionsArray, i, category, questionID));
			//Question-Object erstellen
			questionsArray[i] = Parser.getQuestion(category, questionID);
		}
		//und �bergeben
		return questionsArray;
	}
	
	//Summe der verf�gbaren Fragen aus Map ermitteln
	private static int calcQuestionsSum(Map<String, Integer> map) {
		int sum = 0;
		//f�r jede "Zeile" in "Werte" (Anzahl)
		for(Object i : map.values().toArray())
			sum += (Integer)i;
		return sum;
	}
	
	//Zuf�llige Kategorie, gewichtet aus HashMap generieren
	private static String weightedRandomCategory(Map<String, Integer> map) {
		//Zufallswert �ber die Summer aller Fragen
		double random = Math.random() * calcQuestionsSum(map);
		//f�r jede "Zeile" in "Schl�ssel" (Kategorien)
		for(Object category : map.keySet().toArray()) {
			//Anzahl der Fragen dekrementieren
			random -= (int)map.get((String)category);
			//wenn 0 erreicht, Kategorie zur�ckgeben
			if(random <= 0)
				return (String)category;
		}
		return null;
	}

	//Zuf�llige Kategorie aus Kategorie-Liste
	private static String randomCategory(String[] categoriesArray) {
		return categoriesArray[(int)(Math.random() * categoriesArray.length)];
	}
	
	//Zuf�llige Frage aus Kategorie
	//EDIT:Optional Kategorien nacheinander �bergeben!?
	private static String randomID(String category, Map<String, Integer> map) {
		return String.valueOf((int)(Math.random() * (int)map.get(category)));
	}
	
	//�berpr�fen ob die Frage bereits im Question-Array vorhanden ist...
	private static boolean checkQuestionUID(Question[] questionsArray, int pos, String category, String questionID) {
		for(int i=0; i<pos; i++)
			//Kombination aus Kategorie und ID bereits vorhanden?
			//if(questionsArray[i] != null)//WORKAROUND!!!
				if(questionsArray[i].getCategory().equals(category)
						&& questionsArray[i].getId().equals(questionID))
					return true;
		return false;
	}
}