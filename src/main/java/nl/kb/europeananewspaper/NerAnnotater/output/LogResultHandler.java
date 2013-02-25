package nl.kb.europeananewspaper.NerAnnotater.output;

public class LogResultHandler implements ResultHandler {

	public LogResultHandler(String name) {
		System.out.println("Initializing log output for ALTO file " + name);
	}

	public void addToken(String wordid, String originalContent, String word,
			String label) {
		if (label != null) {
			System.out.println("Wordid: " + wordid + " OriginalContent"
					+ originalContent + " Word: " + word + " Label: " + label);
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
		
		
	}

}
