package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import nl.kbresearch.europeana_newspapers.NerAnnotator.EuropeanaNER;

import java.io.File;

import java.util.concurrent.Callable;
import java.util.Locale;


/**
 * Handles the processing lifecycle of a single container document.
 *
 * @author Rene
 *
 */


public class ContainerHandleThread implements Callable<Boolean> {
    private ContainerProcessor processor;
    private Locale lang;
    private String filePath;
    private String md5sum;

    public ContainerHandleThread(final String filePath,
                                 final Locale lang,
                                 final ContainerProcessor processor,
                                 final String md5sum) {

        this.filePath = filePath;
        this.lang = lang;
        this.md5sum = md5sum;
        this.processor = processor;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            ContainerContext containerContext = new ContainerContext();
            containerContext.setOutputDirectory(getOutputDirectory());
            processor.processFile(containerContext,
                                  filePath,
                                  lang,
                                  md5sum);
            return true;
        } catch (Exception error) {
            error.printStackTrace();
            return false;
        }
    }

    private File getOutputDirectory() {
        String cleanedPath = filePath.trim();

        while (cleanedPath.endsWith("/")) {
            cleanedPath = cleanedPath.substring(0,
                                                cleanedPath.length() - 1);
        }

        String[] split = cleanedPath.split("/");
        String fileName = split[split.length - 1];

        File outputDir = new File(EuropeanaNER.getOutputDirectoryRoot(),
                                  fileName + "-annotations");

        if ((!outputDir.isDirectory()) && (!outputDir.mkdirs())) {
            String msg = "Could not create output directory " +
                         outputDir.getAbsolutePath();
            throw new IllegalArgumentException(msg);
        }

        return outputDir;
    }
}
