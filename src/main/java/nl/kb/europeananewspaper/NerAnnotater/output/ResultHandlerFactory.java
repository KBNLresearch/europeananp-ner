package nl.kb.europeananewspaper.NerAnnotater.output;

import java.util.ArrayList;

import nl.kb.europeananewspaper.NerAnnotater.App;
import nl.kb.europeananewspaper.NerAnnotater.container.ContainerContext;

/**
 * Generates a list of result handlers for the configured output formats on a
 * single ALTO document
 * 
 * @author rene
 * 
 */
public class ResultHandlerFactory {
	/**
	 * @param context
	 * @param name
	 * @return a list of result handlers
	 */
	public static ResultHandler[] createResultHandlers(
			final ContainerContext context, final String name) {

		String[] outputFormats = App.getOutputFormats();

		ArrayList<ResultHandler> result = new ArrayList<ResultHandler>();

		for (String outputFormat : outputFormats) {
			if (outputFormat.equals("log")) {
				result.add(new LogResultHandler());
			} else if (outputFormat.equals("csv")) {
				result.add(new CsvResultHandler(context, name));
			} else if (outputFormat.equals("html")) {
				result.add(new HtmlResultHandler(context, name));
			}

			else {
				throw new IllegalArgumentException("Unknown output format: "
						+ outputFormat);
			}
		}
		return (ResultHandler[]) result
				.toArray(new ResultHandler[result.size()]);
	}
}
