package nl.kb.europeananewspaper.NerAnnotater.output;

public class ResultHandlerFactory {
	public static ResultHandler createResultHandler(String name) {
		return new LogResultHandler(name);
	}
}
