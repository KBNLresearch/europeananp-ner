package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import java.io.File;
import java.io.IOException;
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
 * Processor for MPEG21-DIDL files. This parser is tested with the KB DDD
 * Collection.
 * 
 * @author rene
 * 
 */
public class DIDLProcessor implements ContainerProcessor {

	/**
	 * the default instance of the MPEG21-DIDL processor
	 */
	public static DIDLProcessor INSTANCE = new DIDLProcessor();

	@Override
	public boolean processFile(ContainerContext context, String urlStr,
			Locale lang) throws IOException {
		URL url = null;
		File file = new File(urlStr);
		if (file.exists()) {
			url = file.toURI().toURL();
		} else {

			url = new URL(urlStr);
			System.out.println("File not found, trying to get from URL: "
					+ url.toExternalForm());
		}

		Document doc = null;

                
		System.out.println("Processing DIDL-File " + urlStr);
                // DocumentBuilder db = dbf.newDocumentBuilder();
                // doc = db.parse(file);
		doc = Jsoup.parse(url.openStream(), "UTF-8", "", Parser.xmlParser());

		Elements elementsByTag = doc.getElementsByTag("didl:resource");


                // TODO: fix-thisloop!
		int count = 0;
		for (Element e : elementsByTag) {
			if (e.attr("mimetype").equals("text/xml")) {
				if (e.attr("ref").endsWith(":alto")) {
					URL url2 = new URL(e.attr("ref"));
					String altoFilename = e.attr("dcx:filename");
					if (altoFilename == null || altoFilename.isEmpty()) {
						altoFilename = "alto-" + (count++) + ".xml";
					}

					AltoProcessor.handlePotentialAltoFile(url2, e
							.attr("mimetype"), lang, ResultHandlerFactory
							.createResultHandlers(context, altoFilename));
				}
			}
		}

		return (count > 0);

	}

}
