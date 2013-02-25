package nl.kb.europeananewspaper.NerAnnotater.alto;

import edu.stanford.nlp.ling.CoreAnnotation;

public class HyphenatedLineBreak implements CoreAnnotation<Boolean> {

	public Class<Boolean> getType() {
		return Boolean.class;
	}



}
