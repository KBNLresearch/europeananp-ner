package nl.kb.europeananewspaper.NerAnnotater.alto;

import edu.stanford.nlp.ling.CoreAnnotation;

public class ContinuationAltoStringID implements CoreAnnotation<String>{

	public Class<String> getType() {
		return String.class;
	}

}
