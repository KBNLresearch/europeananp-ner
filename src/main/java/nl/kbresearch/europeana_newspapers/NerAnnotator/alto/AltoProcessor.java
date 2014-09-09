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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * ALTO file processing
 *
 * @author Rene
 * @author Willem Jan Faber
 *
 */


public class AltoProcessor {
    public static int handlePotentialAltoFile(final URL potentialAltoFilename,
                                              final String mimeType,
                                              final Locale lang,
                                              final String md5sum,
                                              final ResultHandler[] handler) throws IOException,
                                                                                    ParserConfigurationException,
                                                                                    SAXException {
        try {
            System.out.println("Trying to process ALTO file " + potentialAltoFilename);
            long startTime = System.currentTimeMillis();
            InputSource input_file = null;

            try {
                input_file = new InputSource(potentialAltoFilename.openStream());
            } catch (Exception error) {
                System.out.println(error);
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(input_file);

            for (ResultHandler h : handler) {
                h.startDocument();
                h.setAltoDocument(doc);
            }

            @SuppressWarnings("unchecked")
            CRFClassifier<CoreMap> classifier_alto = (CRFClassifier<CoreMap>) NERClassifiers.getCRFClassifierForLanguage(lang);
            CRFClassifier classifier_text = NERClassifiers.getCRFClassifierForLanguage(lang);
            List<List<CoreMap>> coreMapElements = TextElementsExtractor.getCoreMapElements(doc);

            int totalNumberOfWords = 0;
            int classified = 0;

            Map<String,String> answer = new HashMap<String, String>();

            for (List<CoreMap> block : coreMapElements) {
                int offset = 0;
                int offsetCount = 0;
                int sentenceCount = 0;

                for (ResultHandler h : handler) {
                    h.startTextBlock();
                }

                // Loop over the alto to extract text elements. Make one long string (sentence).
                List<CoreMap> classify_alto = classifier_alto.classify(block);
                String text = "";

                // label :
                // [ValueAnnotation=Verhulst TextAnnotation=Verhulst OriginalTextAnnotation=Verhulst CharacterOffsetBeginAnnotation=260 
                // CharacterOffsetEndAnnotation=268 BeforeAnnotation=  PositionAnnotation=60 ShapeAnnotation=Xxxxx GoldAnswerAnnotation=null AnswerAnnotation=B-LOC]
                // [ValueAnnotation=; TextAnnotation=; OriginalTextAnnotation=; CharacterOffsetBeginAnnotation=268 CharacterOffsetEndAnnotation=269 BeforeAnnotation= 
                // PositionAnnotation=61 ShapeAnnotation=; GoldAnswerAnnotation=null AnswerAnnotation=O]
                // [ValueAnnotation=Rhap TextAnnotation=Rhap OriginalTextAnnotation=Rhap CharacterOffsetBeginAnnotation=270 CharacterOffsetEndAnnotation=274 BeforeAnnotation=  
                // PositionAnnotation=62 ShapeAnnotation=Xxxx GoldAnswerAnnotation=null AnswerAnnotation=O]

                for (CoreMap label : classify_alto) {
                    if (label.get(HyphenatedLineBreak.class) == null) {
                        String word = label.get(OriginalContent.class);
                        text = text + word + " ";
                    }
                }

                ArrayList<Map<String , String>> stanford_tokens  = new ArrayList<Map<String,String>>();
                // Classify the output text, using the stanford tokenizer.
                @SuppressWarnings("unchecked")
                List<List<CoreLabel>> out = classifier_text.classify(text);
                Map<String, String> map = new HashMap<String, String>();

                // Loop over the stanford tokenized words to map them to the alto later on.
                for (List<CoreLabel> sentence : out) {
                    for (CoreLabel label: sentence) {
                        if (label.get(HyphenatedLineBreak.class) == null) {
                            StringTokenizer st = new StringTokenizer(
                                                        TextElementsExtractor.cleanWord(
                                                            label.get(OriginalTextAnnotation.class)));
                            // Sometimes the stanford tokenizer does not cut on whitespace (with numbers).
                            while (st.hasMoreTokens()) {
                                answer = new HashMap<String, String>();
                                answer.put(st.nextToken(),
                                           TextElementsExtractor.cleanWord(
                                                            label.get(AnswerAnnotation.class)));
                                stanford_tokens.add(offsetCount, answer);
                                offsetCount += 1;
                            }
                        }
                    }
                }

                // label :
                // [OriginalContent=Verhulst; TextAnnotation=Verhulst; AltoStringID=69:3233:45:880:29:677:3237:37:143 AnswerAnnotation=O]
                // [OriginalContent=Rhap TextAnnotation=Rhapsodie AltoStringID=69:3233:45:880:33:850:3242:35:79 ContinuationAltoStringID=69:3274:43:878:1:69:3275:30:70 AnswerAnnotation=O]
                for (CoreMap label : classify_alto) {
                    if (label.get(HyphenatedLineBreak.class) != null) {
                        for (ResultHandler h : handler) {
                            h.newLine(label.get(HyphenatedLineBreak.class));
                        }
                    } else {
                        boolean match = false;
                        String seek_str = "";
                        String stanford = "";
                        String stanfordClassification = "O";
                        String stanford_org = "";

                        // Matching the stanford tokenized output to the alto format.
                        if (label.get(TextAnnotation.class) != null) {
                            if (sentenceCount + offset < stanford_tokens.size()) {
                                Set<String> stanfordKeyset = stanford_tokens.get(sentenceCount +
                                                                                 offset).keySet();
                                seek_str = TextElementsExtractor.cleanWord(label.get(TextAnnotation.class));
                                stanford_org = stanfordKeyset.toArray(new String[stanfordKeyset.size()])[0];
                                stanford = TextElementsExtractor.cleanWord(stanford_org);
                                stanfordClassification = stanford_tokens.get(sentenceCount +
                                                                             offset).get(stanford);

                                if (seek_str.equals("")) {
                                    stanford = "";
                                    offset -= 1;
                                }

                                while ((!match) && (seek_str.length() > 0)) {
                                    if (stanford.equals(seek_str)) {
                                        match = true;
                                        label.set(AnswerAnnotation.class, stanfordClassification);
                                    } else {
                                        offset += 1;
                                        if (sentenceCount + offset < stanford_tokens.size()) {
                                            // Here the stanford tokenizer and the ALTO's don't match,
                                            // keep on looking, by concatonating results.
                                            stanfordKeyset = stanford_tokens.get(sentenceCount +
                                                                                 offset).keySet();
                                            stanford_org = stanfordKeyset.toArray(new String[stanfordKeyset.size()])[0];
                                            if (stanford_org.equals(seek_str)) {
                                                match = true;
                                            } else {
                                                stanford = stanford + stanford_org;
                                            }
                                        } else {
                                            match = true;
                                        }
                                    }
                                }
                            }
                        } else {
                            offset -= 1;
                        }
                        if (!label.get(AnswerAnnotation.class).equals("O")) {
                            classified += 1;
                            for (ResultHandler h : handler) {
                                h.addToken(
                                        label.get(AltoStringID.class),
                                        label.get(OriginalContent.class),
                                        label.get(TextAnnotation.class),
                                        label.get(AnswerAnnotation.class),
                                        label.get(ContinuationAltoStringID.class));
                            }
                        } else {
                            for (ResultHandler h : handler) {
                                h.addToken(
                                        label.get(AltoStringID.class),
                                        label.get(OriginalContent.class),
                                        label.get(TextAnnotation.class),
                                        null,
                                        label.get(ContinuationAltoStringID.class));
                            }
                        }
                        totalNumberOfWords += 1;
                        sentenceCount += 1;
                    }
                }
                for (ResultHandler h : handler) {
                    h.stopTextBlock();
                }
            }

            for (ResultHandler h : handler) {
                h.stopDocument();
            }

            System.out.println("\nStatistics: " +
                               classified +
                               " out of " +
                               totalNumberOfWords +
                               "/ " +
                               ((double) classified / (double) totalNumberOfWords) +
                               ") classified");

            System.out.println("Total millisecs: " +
                               (System.currentTimeMillis() -
                               startTime));

        } catch (IOException error) {
            System.err.println("Could not read ALTO file " +
                               potentialAltoFilename.toExternalForm());
            throw error;
        } catch (SAXException error) {
            throw error;
        } catch (ParserConfigurationException error) {
            throw error;
        }

        for (ResultHandler h : handler) {
            h.close();
        }

        return (0);
    }
}
