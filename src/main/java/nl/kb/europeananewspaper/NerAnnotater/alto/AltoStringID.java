package nl.kb.europeananewspaper.NerAnnotater.alto;

import edu.stanford.nlp.ling.CoreAnnotation;

/**
 * Tagger class for the identifier of the Alto element.
 * 
 * @author rene
 * 
 */
public class AltoStringID implements CoreAnnotation<String> {

	public Class<String> getType() {
		return String.class;
	}

}
