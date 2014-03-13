package nl.kbresearch.europeana_newspapers.NerAnnotator;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.AltoStringID;
import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.HyphenatedLineBreak;
import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.OriginalContent;
import org.jsoup.helper.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Converter from ALTO elements to tokens for Stanford NER
 * 
 * @author Rene
 * @author Willem Jan Faber
 * 
 */

public class TextElementsExtractor {
    private static final Logger logger = Logger.getLogger("TextElementsExtractor.class");

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
                Boolean firstSegmentAfterHyphenation = false;
                boolean hyphenatedEnd = false;
                Element hyphenWord = null;
                for (int j = 0; j<textLineToken.getLength(); j++) {
                    // Loop over TextLine
                    Node tl = textLineToken.item(j);
                    if (tl.getNodeType() == Node.ELEMENT_NODE) {
                        Element tll = (Element) tl;
                        NodeList text = tll.getChildNodes();
                        for (int k =0; k < text.getLength(); k++) {
                            // Loop over String/SP/HYP
                            if (text.item(k).getNodeType() == Node.ELEMENT_NODE) {
                                Element tx = (Element) text.item(k);
                                if (tx.getTagName().equalsIgnoreCase("string")) {
                                    if (hyphenatedEnd) {
                                        hyphenatedEnd = false;
                                        // Join the previous part of the word with the current
                                        // newBlock.add(getWordToLabel(hyphenWord,
                                        //             hyphenWord.getAttribute("CONTENT") + 
                                         //            tx.getAttribute("CONTENT"))); 
                                        newBlock.add(getWordToLabel(tx,
                                                    hyphenWord.getAttribute("CONTENT") + 
                                                    tx.getAttribute("CONTENT"))); 
                                    } else {
                                        // If the next block is an hypen, 
                                        // we might not want to add the block here..
                                        if (isNextHyphen(textLineToken, j)) {
                                            newBlock.add(getWordToLabel(tx, "")); 
                                            hyphenatedEnd = false;
                                            hyphenWord = tx;
                                        } else {
                                            newBlock.add(getWordToLabel(tx, "")); 
                                            hyphenatedEnd = false;
                                            hyphenWord = tx;
                                        }
                                    }
                                } else if (tx.getTagName().equalsIgnoreCase("hyp")) {
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

    private static boolean isNextHyphen(NodeList textLineToken, int a) {
        for (int j = a; j<textLineToken.getLength(); j++) {
            // Loop over TextLine
            Node tl = textLineToken.item(j);
            if (tl.getNodeType() == Node.ELEMENT_NODE) {
                Element tll = (Element) tl;
                NodeList text = tll.getChildNodes();
                for (int k =0; k < text.getLength(); k++) {
                    // Loop over String/SP/HYP
                    if (text.item(k).getNodeType() == Node.ELEMENT_NODE) {
                        Element tx = (Element) text.item(k);
                        if (tx.getTagName().equalsIgnoreCase("hyp")) {
                            return true;
                        }
                    }
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

    private static CoreLabel getWordToLabel(Element token, String completeWord) {
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
            String Content = completeWord;
            String cleanedContent = cleanWord(Content);
            label.set(OriginalContent.class, Content);
            label.setWord(cleanedContent);
            label.set(AltoStringID.class, calcuateAltoStringID(token));
        }
        return label;
    }

    private static String cleanWord(String attr) {
        String cleaned = attr.replace(".", "");
        cleaned = cleaned.replace(",", "");
        return cleaned;
   }

   private static String calcuateAltoStringID(Element word) {
       Element e = (Element) word;

       String parentHpos = nullsafe(e.getAttribute("HPOS"));
       String parentVpos = nullsafe(e.getAttribute("VPOS"));
       String parentWidth = nullsafe(e.getAttribute("WIDTH"));
       String parentHeight = nullsafe(e.getAttribute("HEIGHT"));

       String optionalStringHpos = nullsafe(e.getAttribute("HPOS"));
       String optionalStringVpos = nullsafe(e.getAttribute("VPOS"));
       String optionalStringWidth = nullsafe(e.getAttribute("WIDTH"));
       String optionalStringHeight = nullsafe(e.getAttribute("HEIGHT"));

       LinkedList<String> params = new LinkedList<String>();

       params.add(parentHpos);
       params.add(parentVpos);
       params.add(parentHeight);
       params.add(parentWidth);

       params.add(optionalStringHpos);
       params.add(optionalStringVpos);
       params.add(optionalStringHeight);
       params.add(optionalStringWidth);

       return StringUtil.join(params, ":");
   }

    public static Element findAltoElementByStringID(Document altoDocument, String id) {
        if (id == null || id.isEmpty()) {
            logger.warning("Trying to find element in ALTO document, with empty or null id");
            return null;
        }

        if (altoDocument == null) {
            logger.warning("Trying to find an element in an ALTO document, which is null");
            return null;
        }

        String[] split = id.split(":");
        String expression = "//String[@HPOS='" + split[0] + "'][@VPOS='" + split[1] + "'][@HEIGHT='" + split[2] + "'][@WIDTH='" + split[3] + "']"; 
        XPath xpath = XPathFactory.newInstance().newXPath();

        try {
            NodeList nodes = (NodeList) xpath.evaluate(expression, altoDocument, XPathConstants.NODESET);
            Element ep = (Element) nodes.item(0);
            if (ep !=null) {
                return(ep);
            }
        } catch (XPathExpressionException e) { 
            e.printStackTrace(); 
        }
        return null;
   }

   private static String nullsafe(String attr) {
        return attr == null ? "" : attr;
   }
}
