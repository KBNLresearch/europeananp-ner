package nl.kb.europeananewspaper.NerAnnotater.output;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
	 */
	
	static Map<Class<? extends ResultHandler>, ResultHandler> registeredHandlers=new LinkedHashMap<Class<? extends ResultHandler>, ResultHandler>();
	
	
	/**
	 * @param context
	 * @param name 
	 * @return array of ResultHandlers according to the configuration
	 */
	public static ResultHandler[] createResultHandlers(
			final ContainerContext context, final String name) {

		String[] outputFormats = App.getOutputFormats();

		ArrayList<ResultHandler> result = new ArrayList<ResultHandler>();

		for (String outputFormat : outputFormats) {
			if (outputFormat.equals("log")) {
				LogResultHandler logResultHandler = new LogResultHandler();
				registeredHandlers.put(LogResultHandler.class,logResultHandler);
				result.add(logResultHandler);
			} else if (outputFormat.equals("csv")) {
				CsvResultHandler csvResultHandler = new CsvResultHandler(context, name);
				registeredHandlers.put(CsvResultHandler.class, csvResultHandler);
				result.add(csvResultHandler);
			} else if (outputFormat.equals("alto")) {
				AnnotatedAltoResultHandler annotatedAltoResultHandler = new AnnotatedAltoResultHandler(context, name);
				registeredHandlers.put(AnnotatedAltoResultHandler.class, annotatedAltoResultHandler);
				result.add(annotatedAltoResultHandler);
                        } else if (outputFormat.equals("alto2_1")) {
                                Alto2_1ResultHandler alto2_1ResultHandler = new Alto2_1ResultHandler(context, name);
                                registeredHandlers.put(Alto2_1ResultHandler.class, alto2_1ResultHandler);
                                result.add(alto2_1ResultHandler);
			} else if (outputFormat.equals("html")) {
				HtmlResultHandler htmlResultHandler = new HtmlResultHandler(context, name);
				registeredHandlers.put(HtmlResultHandler.class, htmlResultHandler);
				result.add(htmlResultHandler);
			} else if (outputFormat.equals("db")) {
				try {
					DbResultHandler dbResultHandler = new DbResultHandler(context, name);
					registeredHandlers.put(DbResultHandler.class, dbResultHandler);
					result.add(dbResultHandler);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			else {
				throw new IllegalArgumentException("Unknown output format: "
						+ outputFormat);
			}
		}
		return result
				.toArray(new ResultHandler[result.size()]);
	}
	
	/**
	 * 
	 */
	public static void shutdownResultHandlers() {
		for (@SuppressWarnings("rawtypes") Class c: registeredHandlers.keySet()) {
			registeredHandlers.get(c).globalShutdown();
		}
	}
}
