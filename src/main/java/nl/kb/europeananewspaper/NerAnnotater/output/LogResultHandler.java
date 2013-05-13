package nl.kb.europeananewspaper.NerAnnotater.output;

import nl.kb.europeananewspaper.NerAnnotater.TextElementsExtractor;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Output handler that writes the result of the NER process to stdout
 * 
 * @author rene
 * 
 */
public class LogResultHandler implements ResultHandler {

	Document altoDocument;
	/**
	 * 
	 */
	public LogResultHandler() {
	}

	public void addToken(String wordid, String originalContent, String word,
			String label, String continuationId) {
		if (label != null) {
			System.out.println("Wordid: " + wordid + " OriginalContent"
					+ originalContent + " Word: " + word + " Label: " + label
					+ " ContinuationId: " + continuationId);
		}
		
		
	}

	public void close() {
		System.out.println("Output finished");

	}

	public void startDocument() {
		// TODO Auto-generated method stub

	}

	public void startTextBlock() {
		// TODO Auto-generated method stub

	}

	public void stopTextBlock() {
		// TODO Auto-generated method stub

	}

	public void stopDocument() {
		// TODO Auto-generated method stub

	}

	public void newLine(boolean hyphenated) {
		// TODO Auto-generated method stub

	}

	public void globalShutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAltoDocument(Document doc) {
		altoDocument=doc;
		
	}

}
