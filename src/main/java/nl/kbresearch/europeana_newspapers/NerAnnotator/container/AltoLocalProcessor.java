package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.AltoProcessor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.output.ResultHandlerFactory;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import java.util.Locale;
import java.util.logging.Logger;


/**
 * Processor for MPEG21-DIDL files.
 *
 * @author Rene
 *
 */


public class AltoLocalProcessor implements ContainerProcessor {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    public static AltoLocalProcessor INSTANCE = new AltoLocalProcessor();

    @Override
    public boolean processFile(ContainerContext context,
                               String urlStr,
                               Locale lang,
                               String md5sum) throws IOException {

        File file = new File(urlStr);
        URL url = null;

        if (file.exists()) {
            url = file.toURI().toURL();
        } else {
            url = new URL(urlStr);
            this.logger.warn("File not found, trying to get from URL: " +
                             url.toExternalForm());
        }

        logger.info("Processing Alto-File " +
                    urlStr);

        String[] split = urlStr.split("/");
        String altoFilename = split[split.length - 1];

        AltoProcessor.handlePotentialAltoFile(url,
                                              "text/xml",
                                              lang,
                                              md5sum,
                                              ResultHandlerFactory.createResultHandlers(context,
                                                                                        altoFilename,
                                                                                        md5sum));

        return (true);
    }
}
