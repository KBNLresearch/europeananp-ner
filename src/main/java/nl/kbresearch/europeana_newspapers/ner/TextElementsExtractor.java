package nl.kbresearch.europeana_newspapers.ner;

import nl.kbresearch.europeana_newspapers.ner.alto.AltoStringID;
import nl.kbresearch.europeana_newspapers.ner.alto.ContinuationAltoStringID;
import nl.kbresearch.europeana_newspapers.ner.alto.HyphenatedLineBreak;
import nl.kbresearch.europeana_newspapers.ner.alto.OriginalContent;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Converter from ALTO elements to tokens for Stanford NER
 * 
 * @author Rene
 * @author Willem Jan Faber
 * 
 */


public class TextElementsExtractor {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
    * @param altoDocument
    * @return a list of text blocks, represented by their tokens.
    */
    public static List<List<CoreMap>> getCoreMapElements(Document altoDocument) {
        List<List<CoreMap>> result = new LinkedList<List<CoreMap>>();
        NodeList blocks = altoDocument.getElementsByTagName("TextBlock");
        for (int i = 0; i < blocks.getLength(); i++) {
            // Loop over TextBlock
            Node tokens = blocks.item(i);
            if (tokens.getNodeType() == Node.ELEMENT_NODE) {
                List<CoreMap> newBlock = new LinkedList<CoreMap>();
                Element eElement = (Element) tokens;
                NodeList textLineToken = tokens.getChildNodes();
                boolean hyphenatedEnd = false;
                Element hyphenWord = null;

                // TODO: Rewrite these nested if loops, split in functions.
                for (int j = 0; j < textLineToken.getLength(); j++) {
                    // Loop over TextLine
                    Node textLineNode = textLineToken.item(j);
                    if (textLineNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element textLineElement = (Element) textLineNode;
                        NodeList textLineNodeList = textLineElement.getChildNodes();
                        for (int k =0; k < textLineNodeList.getLength(); k++) {
                            // Loop over String/SP/HYP
                            if (textLineNodeList.item(k).getNodeType() == Node.ELEMENT_NODE) {
                                textLineElement = (Element) textLineNodeList.item(k);
                                if (textLineElement.getTagName().equalsIgnoreCase("string")) {
                                    if (hyphenatedEnd) {
                                        hyphenatedEnd = false;
                                        // Join the previous part of the word with the current word
                                        // Hyphe-nation -> Hyphenation
                                        newBlock.add(getWordToLabel(textLineElement,
                                                    hyphenWord.getAttribute("CONTENT") + 
                                                    textLineElement.getAttribute("CONTENT"), null)); 
                                    } else {
                                        if (isNextHyphen(textLineNodeList, k + 1)) {
                                            // If the next item is a hypen,
                                            // get next word-part, and join the strings
                                            Element nextElement = lookupNextWord(textLineToken, j);

                                            if (textLineElement != null && 
                                                    textLineElement.getAttribute("CONTENT") != null &&
                                                    nextElement != null &&
                                                    nextElement.getAttribute("CONTENT") != null) {

                                                newBlock.add(getWordToLabel(textLineElement,
                                                             textLineElement.getAttribute("CONTENT") + nextElement.getAttribute("CONTENT"),
                                                             nextElement));

                                            }
                                            hyphenatedEnd = false;
                                            hyphenWord = textLineElement;
                                        } else {
                                            newBlock.add(getWordToLabel(textLineElement, "", null)); 
                                            hyphenatedEnd = false;
                                            hyphenWord = textLineElement;
                                        }
                                    }
                                } else if (textLineElement.getTagName().equalsIgnoreCase("hyp")) {
                                    hyphenatedEnd = true;
                                }
                                newBlock.add(getLineBreak(hyphenatedEnd));
                            }
                        }
                    } 
                } // TextLine
                result.add(newBlock);
            } // TextBlock
        } // Alto
        return result;
    }


    // Find the next 'String' element, and return it.
    private static Element lookupNextWord(NodeList textLineToken, int offset) {
        for (int j = offset + 1; j < textLineToken.getLength(); j++) {
            // Loop over TextLine
            Node textLineNode = textLineToken.item(j);
            if (textLineNode.getNodeType() == Node.ELEMENT_NODE) {
                Element textLineElement = (Element) textLineNode;
                NodeList textLineNodeList = textLineElement.getChildNodes();
                for (int k =0; k < textLineNodeList.getLength(); k++) {
                    // Loop over String/SP/HYP
                    if (textLineNodeList.item(k).getNodeType() == Node.ELEMENT_NODE) {
                        textLineElement = (Element) textLineNodeList.item(k);
                        if (textLineElement.getTagName().equalsIgnoreCase("string")) {
                            return textLineElement;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isNextHyphen(NodeList textLineNodeList, int offset) {
        // Loop over the next item in the TextLine block,
        // and return true if the next item is a hyphen
        for (int k = offset; k < textLineNodeList.getLength(); k++) {
            if (textLineNodeList.item(k).getNodeType() == Node.ELEMENT_NODE) {
                Element textLineElement = (Element) textLineNodeList.item(k);
                if (textLineElement.getTagName().equalsIgnoreCase("string")) {
                    return false;
                } else if (textLineElement.getTagName().equalsIgnoreCase("hyp")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static CoreMap getLineBreak(boolean hyphenatedEnd) {
        CoreMap lineBreak = new CoreLabel();
        lineBreak.set(HyphenatedLineBreak.class, hyphenatedEnd);
        return lineBreak;
    }

    private static CoreLabel getWordToLabel(Element token, String completeWord, Element nextElement) {
        // Create a label instance and add content/ alto_id
        // If the word was hypenated, use the completeWord to create a label
        CoreLabel label = new CoreLabel();

        if (completeWord.equalsIgnoreCase("")) {
            String Content = token.getAttribute("CONTENT");
            String cleanedContent = cleanWord(Content);
            label.set(OriginalContent.class, Content);
            label.setWord(cleanedContent);
            label.set(AltoStringID.class, calcuateAltoStringID(token));
        } else {
            // continuesNextLine
            String Content = completeWord;
            String cleanedContent = cleanWord(Content);

            label.set(OriginalContent.class, Content);
            label.setWord(cleanedContent);
            label.set(AltoStringID.class, calcuateAltoStringID(token));

            if (nextElement != null) {
                label.set(ContinuationAltoStringID.class, calcuateAltoStringID(nextElement));
            }
        }
        return label;
    }

    // Remove 'noisy' char's from string, and return it.
    public static String cleanWord(String attr) {
        String[] removeChars = {".", ",", ")", "(", ";", "'", "\"", "}", "{"};
        String cleaned = attr;
        for (int i=0; i < removeChars.length; i++) {
            String remove = removeChars[i];
            cleaned = cleaned.replace(remove, "");
        }
        return cleaned;
    }    

    // Find the corresponding ALTO_ID_STR from a Element.
    private static String calcuateAltoStringID(Element word) {
       String parentHpos = nullsafe(word.getAttribute("HPOS"));
       String parentVpos = nullsafe(word.getAttribute("VPOS"));
       String parentWidth = nullsafe(word.getAttribute("WIDTH"));
       String parentHeight = nullsafe(word.getAttribute("HEIGHT"));

       String optionalStringHpos = nullsafe(word.getAttribute("HPOS"));

       String optionalStringVpos = nullsafe(word.getAttribute("VPOS"));
       String optionalStringWidth = nullsafe(word.getAttribute("WIDTH"));
       String optionalStringHeight = nullsafe(word.getAttribute("HEIGHT"));

       LinkedList<String> params = new LinkedList<String>();

       params.add(parentHpos);
       params.add(parentVpos);
       params.add(parentHeight);
       params.add(parentWidth);

       params.add(optionalStringHpos);
       params.add(optionalStringVpos);
       params.add(optionalStringHeight);
       params.add(optionalStringWidth);

       String alto_id = "";

       for (String s : params) {
           if (alto_id.equals("")) {
               alto_id = s;
            } else {
               alto_id +=  ":" + s;
            }
       }

       return alto_id;
    }


    // Lookup a corresponding AltoElement for a given ALTO_WORD_ID.
    public static Element findAltoElementByStringID(Document altoDocument, String id) {
        if (id == null || id.isEmpty()) {
            logger.warning("Trying to find element in ALTO document, with empty or null id");
            return null;
        }

        if (altoDocument == null) {
            logger.warning("Trying to find an element in an ALTO document, which is null");
            return null;
        }

        // Preform the seek, by using a xpath expression,
        // to find the corresponding element.
        String[] split = id.split(":");

        //String[@HPOS='x'][@VPOS='y'][@HEIGHT='z'][@WIDTH='r']
        String expression = "//String[@HPOS='" +
                            split[0] +
                            "'][@VPOS='" +
                            split[1] +
                            "'][@HEIGHT='" +
                            split[2] +
                            "'][@WIDTH='" +
                            split[3] +
                            "']";

        XPath xpath = XPathFactory.newInstance().newXPath();

        try {
            NodeList nodes = (NodeList) xpath.evaluate(expression,
                                                       altoDocument,
                                                       XPathConstants.NODESET);

            Element alto_element = (Element) nodes.item(0);

            if (alto_element !=null) {
                return alto_element;
            }

        } catch (XPathExpressionException error) { 
            error.printStackTrace(); 
        }
        return null;
   }

   // Never return null as value.
   private static String nullsafe(String attr) {
        return attr == null ? "" : attr;
   }
}
