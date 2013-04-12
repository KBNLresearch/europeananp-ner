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
	 * @param context
	 * @param name
	 * @return a list of result handlers
	 * @throws SQLException 
	 */
	
	static Map<Class<? extends ResultHandler>, ResultHandler> registeredHandlers=new LinkedHashMap<Class<? extends ResultHandler>, ResultHandler>();
	
	
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
	
	public static void shutdownResultHandlers() {
		for (Class c: registeredHandlers.keySet()) {
			registeredHandlers.get(c).globalShutdown();
		}
	}
}
