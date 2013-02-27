package nl.kb.europeananewspaper.NerAnnotater.output;

public interface ResultHandler {

	public void startDocument();
	public void startTextBlock();
	public void newLine(boolean hyphenated);
	public void addToken(String wordid, String originalContent, String word,String label,String continuationid);
	public void stopTextBlock();
	public void stopDocument();
	
	public void close();
}
