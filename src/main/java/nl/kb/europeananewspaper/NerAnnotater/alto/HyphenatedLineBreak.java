package nl.kb.europeananewspaper.NerAnnotater.alto;

import edu.stanford.nlp.ling.CoreAnnotation;

/**
 * Tagger class for a line break. If the value is true, there is a hyphen at the
 * end of the line
 * 
 * @author rene
 * 
 */
public class HyphenatedLineBreak implements CoreAnnotation<Boolean> {

	public Class<Boolean> getType() {
		return Boolean.class;
	}

}
