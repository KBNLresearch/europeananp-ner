package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.TxtProcessor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.output.ResultHandlerFactory;

import org.apache.commons.io.FileUtils;


/**
 * Processor for Text files. 
 * 
 * @author Rene
 * @author Willem Jan Faber
 * 
 */
public class HtmlProcessor implements ContainerProcessor {

    /**
     * the default instance of the Text processor
     */
    public static HtmlProcessor INSTANCE = new HtmlProcessor();

    @Override
    public boolean processFile(ContainerContext context, String urlStr, Locale lang, String md5sum) throws IOException {
        System.out.println("Processing TEXT-File " + urlStr);
        int count = 0;

        URL textURL = new File(urlStr).toURI().toURL();


        //handlePotentialTextFile(final URL potentialTextFilename, final String mimeType, final Locale lang, final String md5sum, final ResultHandler[] handler)
        TxtProcessor.handlePotentialTextFile(textURL, "text/html", lang, md5sum, 
                                             ResultHandlerFactory.createResultHandlers(context, urlStr, md5sum));

        return true;
    }
}
