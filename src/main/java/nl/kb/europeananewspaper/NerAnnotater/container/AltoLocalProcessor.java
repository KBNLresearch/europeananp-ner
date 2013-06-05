package nl.kb.europeananewspaper.NerAnnotater.container;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import nl.kb.europeananewspaper.NerAnnotater.alto.AltoProcessor;
import nl.kb.europeananewspaper.NerAnnotater.output.ResultHandlerFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 * Processor for MPEG21-DIDL files. This parser is tested with the KB DDD
 * Collection.
 * 
 * @author rene
 * 
 */
public class AltoLocalProcessor implements ContainerProcessor {

	/**
	 * the default instance of the Alto processor
	 */
	public static AltoLocalProcessor INSTANCE = new AltoLocalProcessor();

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

		System.out.println("Processing Alto-File " + urlStr);

		String[] split = urlStr.split("/");
		String altoFilename = split[split.length - 1];

		AltoProcessor.handlePotentialAltoFile(url, "text/xml"
  			,lang, ResultHandlerFactory
			.createResultHandlers(context, altoFilename));

		return (true);

	}

}
