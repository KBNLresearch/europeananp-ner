package nl.kb.europeananewspaper.NerAnnotater.output;

public interface ResultHandler {

	public void addEntry(String wordid, String word,String label);
	public void close();
}
