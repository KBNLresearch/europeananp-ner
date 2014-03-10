package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import org.jsoup.nodes.Document;

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

	@Override
	public void addToken(String wordid, String originalContent, String word,
			String label, String continuationId) {
		if (label != null) {
			System.out.println("Wordid: " + wordid + " OriginalContent"
					+ originalContent + " Word: " + word + " Label: " + label
					+ " ContinuationId: " + continuationId);
		}
		
		
	}

	@Override
	public void close() {
		System.out.println("Output finished");

	}

    public void startDocument() {

		// TODO Auto-generated method stub

	}

	@Override
	public void startTextBlock() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopTextBlock() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopDocument() {
		// TODO Auto-generated method stub

	}

	@Override
	public void newLine(boolean hyphenated) {
		// TODO Auto-generated method stub

	}

	@Override
	public void globalShutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAltoDocument(Document doc) {
		altoDocument=doc;
		
	}

}
