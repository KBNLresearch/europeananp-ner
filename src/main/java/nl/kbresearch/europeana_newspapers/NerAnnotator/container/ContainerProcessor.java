package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import java.io.IOException;
import java.util.Locale;

/**
 * Processor for a container format
 * 
 * @author rene
 * @author Willem Jan Faber
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
     * @param md5sum
     *            the md5sum of the jar file producing the output
     * @return true iff the container and the dependent documents where
     *         successfully processed
     * @throws IOException
     */
    public boolean processFile(ContainerContext context, String urlStr, Locale lang, String md5sum) throws IOException;
}
