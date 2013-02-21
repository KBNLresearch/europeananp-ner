package nl.kb.europeananewspaper.NerAnnotater.container;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import nl.kb.europeananewspaper.NerAnnotater.alto.AltoProcessor;
import nl.kb.europeananewspaper.NerAnnotater.output.LogResultHandler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class DIDLProcessor implements ContainerProcessor {

	public static DIDLProcessor INSTANCE = new DIDLProcessor();

	public void processFile(String urlStr, Locale lang) throws IOException {
		URL url = null;
		File file = new File(urlStr);
		if (file.exists()) {
			url = file.toURI().toURL();
		} else {
			url = new URL(urlStr);
		}

		Document doc = null;

		System.out.println("Processing DIDL-File "+urlStr);
		doc = Jsoup.parse(url.openStream(), "UTF-8", "", Parser.xmlParser());
		
		Elements elementsByTag = doc.getElementsByTag("didl:resource");
		
		for (Element e:elementsByTag) {
			if (e.attr("mimetype").equals("text/xml")) {
				if (e.attr("ref").endsWith(":alto")) {
					URL url2 = new URL(e.attr("ref"));
					AltoProcessor.handlePotentialAltoFile(url2, e.attr("mimetype"), lang, new LogResultHandler());
				}
			}
		}
	
		

	}

}
