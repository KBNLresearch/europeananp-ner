package nl.kb.europeananewspaper.NerAnnotater.output;

public class LogResultHandler implements ResultHandler {

	public LogResultHandler(String name) {
		// TODO Auto-generated method stub
	}

	public void addToken(String wordid, String originalContent, String word,
			String label, String continuationId) {
		if (label != null) {
			System.out.println("Wordid: " + wordid + " OriginalContent"
					+ originalContent + " Word: " + word + " Label: " + label+" ContinuationId: "+continuationId);
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

}
