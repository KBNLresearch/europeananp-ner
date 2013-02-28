package nl.kb.europeananewspaper.NerAnnotater.container;

import java.io.IOException;
import java.util.Locale;

/**
 * Processor for a container format
 * 
 * @author rene
 * 
 */
public interface ContainerProcessor {

	/**
	 * @param context
	 *            container-specific context
	 * @param urlStr
	 *            the file location
	 * @param lang
	 *            the language to use for named entity recognition
	 * @return true iff the container and the dependent documents where
	 *         successfully processed
	 * @throws IOException
	 */
	public boolean processFile(ContainerContext context, String urlStr,
			Locale lang) throws IOException;

}