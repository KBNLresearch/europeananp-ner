package nl.kb.europeananewspaper.NerAnnotater.container;

import java.io.IOException;
import java.util.Locale;

public interface ContainerProcessor {

	public void processFile(String urlStr, Locale lang) throws IOException;

}