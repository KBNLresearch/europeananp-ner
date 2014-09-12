package nl.kbresearch.europeana_newspapers.ner.container;

import nl.kbresearch.europeana_newspapers.ner.alto.TxtProcessor;
import nl.kbresearch.europeana_newspapers.ner.output.ResultHandlerFactory;

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


public class HtmlProcessor implements ContainerProcessor {
    // The default instance of the HTML processor.
    public static HtmlProcessor INSTANCE = new HtmlProcessor();

    @Override
    public boolean processFile(ContainerContext context,
                               String urlStr,
                               Locale lang,
                               String md5sum) throws IOException {

        System.out.println("Processing TEXT-File " + urlStr);
        int count = 0;
        URL textURL = new File(urlStr).toURI().toURL();

        TxtProcessor.handlePotentialTextFile(textURL,
                                             "text/html",
                                             lang,
                                             md5sum,
                                             ResultHandlerFactory.createResultHandlers(context,
                                                                                       urlStr,
                                                                                       md5sum,
                                                                                       null));

        return true;
    }
}