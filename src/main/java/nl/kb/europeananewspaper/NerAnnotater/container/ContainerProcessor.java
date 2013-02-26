package nl.kb.europeananewspaper.NerAnnotater.container;

import java.io.IOException;
import java.util.Locale;

public interface ContainerProcessor {

	public boolean processFile(ContainerContext context, String urlStr, Locale lang) throws IOException;

}