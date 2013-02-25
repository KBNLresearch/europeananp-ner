package nl.kb.europeananewspaper.NerAnnotater.output;

import nl.kb.europeananewspaper.NerAnnotater.App;
import nl.kb.europeananewspaper.NerAnnotater.container.ContainerContext;

public class ResultHandlerFactory {
	public static ResultHandler createResultHandler(ContainerContext context,
			String name) {
		if (App.getOutputFormat().equals("log")) {
			return new LogResultHandler(name);
		} else if (App.getOutputFormat().equals("csv")) {
			return new CsvResultHandler(context,name);
		}
		
		else {
			throw new IllegalArgumentException("Unknown output format: "
					+ App.getOutputFormat());
		}
	}
}
