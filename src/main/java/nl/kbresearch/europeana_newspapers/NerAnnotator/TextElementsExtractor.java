package nl.kbresearch.europeana_newspapers.NerAnnotator;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

import org.jsoup.helper.StringUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.AltoStringID;
import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.ContinuationAltoStringID;
import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.HyphenatedLineBreak;
import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.OriginalContent;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.xpath.*;

import org.xml.sax.*;
import org.w3c.dom.*;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

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


   // CHECK THIS, THERE IS A BUG IN HERE SOMEWHERE!
   //
   public static List<List<CoreMap>> getCoreMapElements(Document altoDocument) {
       List<List<CoreMap>> result = new LinkedList<List<CoreMap>>();
       NodeList blocks = altoDocument.getElementsByTagName("TextBlock");
       System.out.println("**BL**");
       System.out.println(blocks.getLength());
       for (int i = 0; i<blocks.getLength(); i++) {
           Node tokens = blocks.item(i);
           if (tokens.getNodeType() == Node.ELEMENT_NODE) {
               List<CoreMap> newBlock = new LinkedList<CoreMap>();
               Element eElement = (Element) tokens;
               NodeList textLineToken = tokens.getChildNodes();
               Boolean firstSegmentAfterHyphenation = false;
               boolean hyphenatedEnd = false;

               for (int j = 0; j<textLineToken.getLength(); j++) {
                   Node tl = textLineToken.item(j);
                   if (tl.getNodeType() == Node.ELEMENT_NODE) {
                       Element tll = (Element) tl;
                       NodeList text = tll.getChildNodes();
                       for (int k =0;k<text.getLength(); k++) {
                           if (text.item(k).getNodeType() == Node.ELEMENT_NODE) {
                               Element tx = (Element) text.item(k);
                               if (tx.getTagName().equalsIgnoreCase("string")) {
                                   newBlock.add(getWordToLabel(tx, firstSegmentAfterHyphenation));
                                   firstSegmentAfterHyphenation = false;
                               } else if (tx.getTagName().equalsIgnoreCase("hyp")) {
                                   hyphenatedEnd = true;
                                   firstSegmentAfterHyphenation = true;
                               }
                           }
                       }
                       newBlock.add(getLineBreak(hyphenatedEnd));
                   }
               }   
               result.add(newBlock);
           }
       }
       return result;
   }

   private static CoreMap getLineBreak(boolean hyphenatedEnd) {
       CoreMap lineBreak = new CoreLabel();
       lineBreak.set(HyphenatedLineBreak.class, hyphenatedEnd);
       return lineBreak;
   }

   private static CoreLabel getWordToLabel(Element token, Boolean wordSegmentAfterHyphenation) {
        boolean continuesNextLine = false;
        String cleanedContent;
        Node nextNextSibling = null;
        Node nextSiblingNode = null;
        Element nextSibling = null;

        if (wordSegmentAfterHyphenation) {
            cleanedContent = null;
        } else {
            // if the word is at the end of line and hyphenated, pull the
            // content from the second for NER into the first token
            String nextWordSuffix = "";
            nextSiblingNode = token.getNextSibling();

            if (nextSiblingNode.getNodeType() == Node.ELEMENT_NODE) {
                    nextSibling = (Element) nextSiblingNode;
            }

            if (nextSibling != null
                            && nextSibling.getTagName().equalsIgnoreCase("hyp")) {
                    // get first String element of next line, if it exists
                    Node nextLine = nextSibling.getParentNode().getNextSibling();
                    if (nextLine != null) {
                            nextNextSibling = nextLine.getFirstChild();
                            if (nextNextSibling != null) {
                                    Element e = (Element) nextNextSibling;
                                    nextWordSuffix = e.getAttribute("CONTENT");
                                    if (nextWordSuffix == null)
                                            nextWordSuffix = "";
                                    else {
                                            continuesNextLine = true;
                                    }
                            }
                    }
            }
            cleanedContent = cleanWord(token.getAttribute("CONTENT") + nextWordSuffix);
        }

        CoreLabel label = new CoreLabel();
        label.set(OriginalContent.class, token.getAttribute("CONTENT"));
        label.setWord(cleanedContent);
        label.set(AltoStringID.class, calcuateAltoStringID(token));

        if (continuesNextLine) {
            Element e = (Element) nextNextSibling;
            label.set(ContinuationAltoStringID.class, calcuateAltoStringID(e));
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
           logger.warning("Trying to find element in ALTO document , with empty or null id");
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
