package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.AltoProcessor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.output.ResultHandlerFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.NodeList;
//import javax.xml.parsers.DocumentBuilder;


/**
 * Processor for METS containers
 * 
 * @author rene
 * 
 */
public class MetsProcessor implements ContainerProcessor {

	/**
	 * the default instance of the METS processor
	 */
	public static MetsProcessor INSTANCE = new MetsProcessor();

	@Override
	public boolean processFile(ContainerContext context, String urlStr,
			Locale lang) throws IOException {

		System.out.println("Processing METS file " + urlStr);
		URL url = null;
		File file = new File(urlStr);
		if (file.exists()) {
			url = file.toURI().toURL();
		} else {
			url = new URL(urlStr);
		}

		Document doc = null;

		doc = Jsoup.parse(url.openStream(), "UTF-8", "", Parser.xmlParser());
                // DocumentBuilder db = dbf.newDocumentBuilder();
                // doc = db.parse(file);
	
		Elements elementsByTag = doc.getElementsByTag("mets:flocat");

                //TODO: fix this loop.

		int count = 0;
		for (Element e : elementsByTag) {
			URL potentialAltoFilename;
			try {
				URI referencedFile = new URI(e.attr("xlink:href"));
				if ("file".equalsIgnoreCase(referencedFile.getScheme())) {

					String path = referencedFile.getPath();
					String relativeToUrl = url.toString();
					potentialAltoFilename = new URI(relativeToUrl.substring(0,
							relativeToUrl.lastIndexOf("/")) + path).normalize()
							.toURL();
				} else {
					potentialAltoFilename = referencedFile.normalize().toURL();
				}

				String[] split = potentialAltoFilename.toExternalForm().split(
						"/");
				String name;
				if (split.length > 0 && !split[split.length - 1].isEmpty()) {
					// name from url
					name = split[split.length - 1];
				} else {
					// Generic name, if not available
					name = "alto-" + (count++) + ".xml";
				}

				AltoProcessor.handlePotentialAltoFile(potentialAltoFilename, e
						.parent().attr("mimetype"), lang, ResultHandlerFactory
						.createResultHandlers(context, name));
			} catch (URISyntaxException e1) {
				System.err
						.println("Error parsing path to file in METS for file id "
								+ e.parent().attr("id"));
				e1.printStackTrace();
				return false;
			}

		}

		return (count > 0);
	}
}
