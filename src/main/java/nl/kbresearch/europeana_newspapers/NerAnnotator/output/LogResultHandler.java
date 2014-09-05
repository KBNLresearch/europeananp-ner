package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import org.w3c.dom.Document;


/**
 * Output handler that writes the result of the NER process to stdout.
 *
 * @author Rene
 *
 */


public class LogResultHandler implements ResultHandler {
    Document altoDocument;

    public LogResultHandler() {

    }

    @Override
    public void addToken(String wordid, String originalContent, String word, String label, String continuationId) {
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

    }

    @Override
    public void startTextBlock() {

    }

    @Override
    public void stopTextBlock() {

    }

    @Override
    public void stopDocument() {

    }

    @Override
    public void newLine(boolean hyphenated) {

    }

    @Override
    public void globalShutdown() {

    }

    @Override
    public void setAltoDocument(Document doc) {
        altoDocument = doc;
    }

}
