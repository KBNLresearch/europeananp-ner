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

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.URL;
import java.io.*;
import java.util.*;

/**
 * Text file processing
 * 
 * @author Willem Jan Faber
 * 
 */
public class TxtProcessor {
    /**
     * @param potentialTextFilename
     * @param mimeType
     * @param lang
     * @param md5sum
     * @param handler
     * @throws IOException
     */
     public static int handlePotentialTextFile(final URL potentialTextFilename, final String mimeType, final Locale lang, final String md5sum, final ResultHandler[] handler) throws IOException {

        System.out.println("Trying to process ALTO file " + potentialTextFilename);
        long startTime = System.currentTimeMillis();
        CRFClassifier classifier_text = NERClassifiers.getCRFClassifierForLanguage(lang);


        // Read the text file into mem
        BufferedReader in = new BufferedReader(new InputStreamReader(potentialTextFilename.openStream()));
        String text="";
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            text += inputLine;
        in.close();

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


        for (ResultHandler h : handler) {
                h.stopTextBlock();
        }

        for (ResultHandler h : handler) {
                h.stopDocument();
        }

        return 1;
        }
}
