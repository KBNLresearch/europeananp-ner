package nl.kbresearch.europeana_newspapers.NerAnnotator.alto;

import nl.kbresearch.europeana_newspapers.NerAnnotator.NERClassifiers;
import nl.kbresearch.europeana_newspapers.NerAnnotator.TextElementsExtractor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.output.ResultHandler;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.OriginalTextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.htmlparser.jericho.*;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Text file processing
 *
 * @author Willem Jan Faber
 *
 */


public class TxtProcessor {
    @SuppressWarnings("unchecked")
    /**
     * @param potentialTextFilename
     * @param mimeType
     * @param lang
     * @param md5sum
     * @param handler
     * @throws IOException
     */
    public static int handlePotentialTextFile(final URL potentialTextFilename,
                                              final String mimeType,
                                              final Locale lang,
                                              final String md5sum,
                                              final ResultHandler[] handler) throws IOException {

        long startTime = System.currentTimeMillis();
        System.out.println("Trying to process ALTO file " + potentialTextFilename);

        // Get the selected classifier for the language
        CRFClassifier classifier_text = NERClassifiers.getCRFClassifierForLanguage(lang);

        // Open filepointer
        BufferedReader in = new BufferedReader(new InputStreamReader(potentialTextFilename.openStream()));

        String text="";
        String inputLine;

        // Read the text file into memory
        while ((inputLine = in.readLine()) != null)
            text += inputLine;

        // Close filepointer
        in.close();

        // If the input is html, loose the tags
        if (mimeType.equals("text/html")) {
            Source html_source = new Source(text);
            TextExtractor textExtractor=new TextExtractor(html_source);
            text = textExtractor.setIncludeAttributes(false).toString();
        }

        // Run the selected classifier over the text
        List<List<CoreLabel>> out = classifier_text.classify(text);

        // Parse the classified output
        for (List<CoreLabel> block : out) {
            for (CoreLabel label: block) {
                if (label.get(AnswerAnnotation.class).equals("O")) {
                    //System.out.print(label.originalText()+ " ");
                    for (ResultHandler h : handler) {
                                h.addToken(
                                        label.get(AltoStringID.class),
                                        label.get(OriginalContent.class),
                                        label.get(TextAnnotation.class),
                                        label.get(AnswerAnnotation.class),
                                        label.get(ContinuationAltoStringID.class));
                            }
                } else {
                    //System.out.print("<" + label.get(AnswerAnnotation.class) + ">");
                    //System.out.print(label.originalText()+ " ");
                    //System.out.print("</" + label.get(AnswerAnnotation.class) + ">");

                    for (ResultHandler h : handler) {
                                h.addToken(
                                        label.get(AltoStringID.class),
                                        label.get(OriginalContent.class),
                                        label.get(TextAnnotation.class),
                                        label.get(AnswerAnnotation.class),
                                        label.get(ContinuationAltoStringID.class));
                            }

                }
            }
        }

        // Close all the textblocks
        for (ResultHandler h : handler) {
            h.stopTextBlock();
        }

        // Close all the documents
        for (ResultHandler h : handler) {
            h.stopDocument();
        }

        return 1;
        }
}
