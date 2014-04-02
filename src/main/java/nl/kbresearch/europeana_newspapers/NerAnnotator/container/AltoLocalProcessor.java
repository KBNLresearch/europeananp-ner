package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.AltoProcessor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.output.ResultHandlerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

/**
 * Processor for MPEG21-DIDL files. This parser is tested with the KB DDD
 * Collection.
 * 
 * @author rene
 * 
 */
public class AltoLocalProcessor implements ContainerProcessor {
    /**
     * the default instance of the Alto processor
     */
    public static AltoLocalProcessor INSTANCE = new AltoLocalProcessor();

    @Override
    public boolean processFile(ContainerContext context, String urlStr, Locale lang, String md5sum) throws IOException {
        URL url = null;
        File file = new File(urlStr);

        if (file.exists()) {
            url = file.toURI().toURL();
        } else {
            url = new URL(urlStr);
            System.out.println("File not found, trying to get from URL: " + url.toExternalForm());
        }

        System.out.println("Processing Alto-File " + urlStr);

        // BEGIN spelvar **********************
        String rightSeparator = "/";
        if (urlStr.split("/").length<urlStr.split("\\\\").length)
            rightSeparator = "\\\\";

        String[] split = urlStr.split(rightSeparator);
        // END spelvar **********************

        String altoFilename = split[split.length - 1];

        AltoProcessor.handlePotentialAltoFile(url, "text/xml" ,lang, md5sum, 
                ResultHandlerFactory.createResultHandlers(context, altoFilename, md5sum));

        return (true);
    }
}
