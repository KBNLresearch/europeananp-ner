package nl.kb.europeananewspaper.NerAnnotater;

import java.util.LinkedList;
import java.util.List;

import nl.kb.europeananewspaper.NerAnnotater.alto.AltoStringID;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.stanford.nlp.ling.CoreLabel;

public class TextElementsExtractor {
	
	public static List<List<CoreLabel>> getCoreLabelElements(Document altoDocument) {
		List<List<CoreLabel>> result=new LinkedList<List<CoreLabel>>();
		
		Elements lines = altoDocument.getElementsByTag("TextBlock");
		for (Element e:lines) {			
			List<CoreLabel> newLine=new LinkedList<CoreLabel>();
			Elements words = e.getElementsByTag("String");
			for (Element word:words) {
				CoreLabel label = new CoreLabel();
				label.setWord(cleanWord(word.attr("CONTENT")));
				label.set(AltoStringID.class,calcuateAltoStringID(word));
				newLine.add(label);
			}
			result.add(newLine);
		}
		return result;
	}

	private static String cleanWord(String attr) {
		String cleaned=attr.replace(".", "");
		cleaned=cleaned.replace(",", "");
		return cleaned;
	}

	private static String calcuateAltoStringID(Element word) {
		// TODO Auto-generated method stub
		Element parent = word.parent();
		
		String parentHpos=nullsafe(parent.attr("HPOS"));
		String parentVpos=nullsafe(parent.attr("VPOS"));
		String parentWidth=nullsafe(parent.attr("WIDTH"));
		String parentHeight=nullsafe(parent.attr("HEIGHT"));
		String optionalStringHpos=nullsafe(word.attr("HPOS"));
		String optionalStringVpos=nullsafe(word.attr("VPOS"));
		String optionalStringWidth=nullsafe(word.attr("WIDTH"));
		String optionalStringHeight=nullsafe(word.attr("HEIGHT"));
		LinkedList<String> params=new LinkedList<String>();
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
		return attr==null ? "":attr; 
	}
}
