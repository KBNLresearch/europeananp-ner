package nl.kb.europeananewspaper.NerAnnotater.output;


public class LogResultHandler implements ResultHandler {

	public void addEntry(String wordid, String word,String label) {
		System.out.println("Wordid: "+wordid+" Word: "+word+" Label: "+label);

	}

	public void close() {
		System.out.println("Output finished");

	}

}
