package nl.kb.europeananewspaper.NerAnnotater;

import java.util.LinkedList;
import java.util.List;

import nl.kb.europeananewspaper.NerAnnotater.alto.AltoStringID;
import nl.kb.europeananewspaper.NerAnnotater.alto.ContinuationAltoStringID;
import nl.kb.europeananewspaper.NerAnnotater.alto.HyphenatedLineBreak;
import nl.kb.europeananewspaper.NerAnnotater.alto.OriginalContent;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

/**
 * Converter from ALTO elements to tokens for Stanford NER
 * 
 * @author rene
 * 
 */
public class TextElementsExtractor {

	/**
	 * @param altoDocument
	 * @return a list of text blocks, represented by their tokens.
	 */
	public static List<List<CoreMap>> getCoreMapElements(Document altoDocument) {
		List<List<CoreMap>> result = new LinkedList<List<CoreMap>>();

		Elements blocks = altoDocument.getElementsByTag("TextBlock");
		for (Element e : blocks) {
			List<CoreMap> newBlock = new LinkedList<CoreMap>();

			Elements tokens = e.getElementsByTag("TextLine");
			Boolean firstSegmentAfterHyphenation = false;
			for (Element token : tokens) {

				Elements textLineTokens = token.children();
				boolean hyphenatedEnd = false;
				for (Element textLineToken : textLineTokens) {
					if (textLineToken.tagName().equalsIgnoreCase("string")) {
						newBlock.add(getWordToLabel(textLineToken,
								firstSegmentAfterHyphenation));
						firstSegmentAfterHyphenation = false;
					} else if (textLineToken.tagName().equalsIgnoreCase("hyp")) {
						hyphenatedEnd = true;
						firstSegmentAfterHyphenation = true;
					}
				}
				newBlock.add(getLineBreak(hyphenatedEnd));
			}

			result.add(newBlock);
		}
		return result;
	}

	private static CoreMap getLineBreak(boolean hyphenatedEnd) {
		CoreMap lineBreak = new CoreLabel();
		lineBreak.set(HyphenatedLineBreak.class, hyphenatedEnd);
		return lineBreak;
	}

	private static CoreLabel getWordToLabel(Element token,
			Boolean wordSegmentAfterHyphenation) {

		boolean continuesNextLine = false;
		String cleanedContent;
		Element nextNextSibling = null;

		if (wordSegmentAfterHyphenation) {
			cleanedContent = null;
		} else {
			// if the word is at the end of line and hyphenated, pull the
			// content from the second for NER into the first token
			String nextWordSuffix = "";
			Element nextSibling = token.nextElementSibling();

			if (nextSibling != null
					&& nextSibling.tagName().equalsIgnoreCase("hyp")) {
				// get first String element of next line, if it exists
				Element nextLine = nextSibling.parent().nextElementSibling();
				if (nextLine != null) {
					nextNextSibling = nextLine.child(0);
					if (nextNextSibling != null) {
						nextWordSuffix = nextNextSibling.attr("CONTENT");
						if (nextWordSuffix == null)
							nextWordSuffix = "";
						else {
							continuesNextLine = true;
						}
					}
				}
			}

			cleanedContent = cleanWord(token.attr("CONTENT") + nextWordSuffix);
		}

		CoreLabel label = new CoreLabel();
		label.set(OriginalContent.class, token.attr("CONTENT"));
		label.setWord(cleanedContent);
		label.set(AltoStringID.class, calcuateAltoStringID(token));
		if (continuesNextLine) {
			label.set(ContinuationAltoStringID.class,
					calcuateAltoStringID(nextNextSibling));
		}
		return label;
	}

	private static String cleanWord(String attr) {
		String cleaned = attr.replace(".", "");
		cleaned = cleaned.replace(",", "");
		return cleaned;
	}

	private static String calcuateAltoStringID(Element word) {
		// TODO Auto-generated method stub
		Element parent = word.parent();

		String parentHpos = nullsafe(parent.attr("HPOS"));
		String parentVpos = nullsafe(parent.attr("VPOS"));
		String parentWidth = nullsafe(parent.attr("WIDTH"));
		String parentHeight = nullsafe(parent.attr("HEIGHT"));
		String optionalStringHpos = nullsafe(word.attr("HPOS"));
		String optionalStringVpos = nullsafe(word.attr("VPOS"));
		String optionalStringWidth = nullsafe(word.attr("WIDTH"));
		String optionalStringHeight = nullsafe(word.attr("HEIGHT"));
		LinkedList<String> params = new LinkedList<String>();
		params.add(parentHpos);
		params.add(parentVpos);
		params.add(parentHeight);
		params.add(parentWidth);
		params.add(Integer.toString(word.siblingIndex()));
		params.add(optionalStringHpos);
		params.add(optionalStringVpos);
		params.add(optionalStringHeight);
		params.add(optionalStringWidth);

		return StringUtil.join(params, ":");
	}

	private static String nullsafe(String attr) {
		return attr == null ? "" : attr;
	}
}
