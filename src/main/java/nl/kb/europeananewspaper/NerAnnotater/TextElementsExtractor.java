package nl.kb.europeananewspaper.NerAnnotater;

import java.util.LinkedList;
import java.util.List;

import nl.kb.europeananewspaper.NerAnnotater.alto.AltoStringID;
import nl.kb.europeananewspaper.NerAnnotater.alto.HyphenatedLineBreak;
import nl.kb.europeananewspaper.NerAnnotater.alto.OriginalContent;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

public class TextElementsExtractor {

	public static List<List<CoreMap>> getCoreMapElements(Document altoDocument) {
		List<List<CoreMap>> result = new LinkedList<List<CoreMap>>();

		Elements blocks = altoDocument.getElementsByTag("TextBlock");
		for (Element e : blocks) {
			List<CoreMap> newBlock = new LinkedList<CoreMap>();

			Elements tokens = e.getElementsByTag("TextLine");
			for (Element token : tokens) {

				Elements textLineTokens = token.children();
				boolean hyphenatedEnd = false;
				for (Element textLineToken : textLineTokens) {
					
					if (textLineToken.tagName().equalsIgnoreCase("string")) {
						newBlock.add(getWordToLabel(textLineToken));
					} else if (textLineToken.tagName().equalsIgnoreCase("hyp")) {
						hyphenatedEnd = true;
					}
				}
				newBlock.add(getLineBreak(hyphenatedEnd));
			}
			
			result.add(newBlock);
		}
		return result;
	}

	private static CoreMap getLineBreak(boolean hyphenatedEnd) {
		CoreMap lineBreak=new CoreLabel();
		lineBreak.set(HyphenatedLineBreak.class,hyphenatedEnd);
		return lineBreak;
	}

	private static CoreLabel getWordToLabel(Element token) {
		CoreLabel label = new CoreLabel();
		label.set(OriginalContent.class, token.attr("CONTENT"));
		label.setWord(cleanWord(token.attr("CONTENT")));
		label.set(AltoStringID.class,
				calcuateAltoStringID(token));
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
		params.add(new Integer(word.siblingIndex()).toString());
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
