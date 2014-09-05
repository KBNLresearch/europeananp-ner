package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.TxtProcessor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.output.ResultHandlerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import org.apache.commons.io.FileUtils;


/**
 * Processor for Text files.
 *
 * @author Rene
 * @author Willem Jan Faber
 *
 */


public class TextProcessor implements ContainerProcessor {
    // The default instance of the Text processor.
    public static TextProcessor INSTANCE = new TextProcessor();

    @Override
    public boolean processFile(ContainerContext context,
                               String urlStr,
                               Locale lang,
                               String md5sum) throws IOException {
        System.out.println("Processing TEXT-File " + urlStr);
        int count = 0;

        URL textURL = new File(urlStr).toURI().toURL();
        TxtProcessor.handlePotentialTextFile(textURL,
                                             "text/xml",
                                             lang,
                                             md5sum,
                                             ResultHandlerFactory.createResultHandlers(context,
                                                                                       urlStr,
                                                                                       md5sum));

        return true;
    }
}
