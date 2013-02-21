package nl.kb.europeananewspaper.NerAnnotater.container;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import nl.kb.europeananewspaper.NerAnnotater.alto.AltoProcessor;
import nl.kb.europeananewspaper.NerAnnotater.output.LogResultHandler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class MetsProcessor implements ContainerProcessor {

	public static MetsProcessor INSTANCE = new MetsProcessor();

	public void processFile(String urlStr, Locale lang) throws IOException {

		System.out.println("Processing METS file "+urlStr);
		URL url = null;
		File file = new File(urlStr);
		if (file.exists()) {
			url = file.toURI().toURL();
		} else {
			url = new URL(urlStr);
		}

		Document doc = null;

		doc = Jsoup.parse(url.openStream(), "UTF-8", "", Parser.xmlParser());
		
		Elements elementsByTag = doc.getElementsByTag("mets:flocat");
		
		for (Element e : elementsByTag) {
			URL potentialAltoFilename;
			try {
				URI referencedFile = new URI(e.attr("xlink:href"));
				System.out.println("Potential ALTO reference: "+referencedFile);
				if ("file".equalsIgnoreCase(referencedFile.getScheme())) {
					
					String path = referencedFile.getPath();
					String relativeToUrl = url.toString();
					potentialAltoFilename = new URI(relativeToUrl.substring(0,
							relativeToUrl.lastIndexOf("/")) + path).normalize()
							.toURL();
				} else {
					potentialAltoFilename = referencedFile.normalize().toURL();
				}
				AltoProcessor.handlePotentialAltoFile(potentialAltoFilename, e
						.parent().attr("mimetype"), lang,
						new LogResultHandler());
			} catch (URISyntaxException e1) {
				System.err
						.println("Error parsing path to file in METS for file id "
								+ e.parent().attr("id"));
				e1.printStackTrace();
			}

		}
	}
}
