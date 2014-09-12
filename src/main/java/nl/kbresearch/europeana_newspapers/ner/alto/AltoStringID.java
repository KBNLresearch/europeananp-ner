package nl.kbresearch.europeana_newspapers.ner.alto;

import edu.stanford.nlp.ling.CoreAnnotation;


/**
 * Tagger class for the identifier of the Alto element.
 *
 * @author rene
 *
 */


public class AltoStringID implements CoreAnnotation<String> {

    @Override
    public Class<String> getType() {
        return String.class;
    }

}
