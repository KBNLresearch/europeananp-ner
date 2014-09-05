package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import org.w3c.dom.Document;

/**
 * Output as CSV format, that maintains a list of labelled tokens.
 *
 * @author Rene
 *
 */


public class CsvResultHandler implements ResultHandler {

    ContainerContext context;
    String name;
    File outputFile;
    CsvListWriter csvListWriter;

    final String[] header = new String[] { "wordId", "originalText", "text", "label", "continuationId" };
    final CellProcessor[] processors = new CellProcessor[] { new NotNull(), new NotNull(), new NotNull(), new NotNull(), new NotNull() };

    /**
     * @param context
     * @param name
     */
    public CsvResultHandler(final ContainerContext context, final String name) {
        this.context = context;
        this.name = name;
    }

    public void addToken(String wordid, String originalContent, String word, String label, String continuationId) {
        if (label != null) {
            if (csvListWriter == null) {
                outputFile = new File(context.getOutputDirectory(), name + ".csv");
                try {
                    csvListWriter = new CsvListWriter(new FileWriter(outputFile), CsvPreference.STANDARD_PREFERENCE);
                    csvListWriter.writeHeader(header);
                } catch (IOException e) {
                    throw new IllegalStateException( "Could not open CSV writer for file " + outputFile.getAbsolutePath(), e);
                }
            }
            try {
                String continuationIdStr = "";
                if (continuationId != null) {
                    continuationIdStr = continuationId;
                }
                csvListWriter.write(wordid, originalContent, word, label, continuationIdStr);
            } catch (IOException e) {
                throw new IllegalStateException("Could not write to CSV writer for file " + outputFile.getAbsolutePath(), e);
            }
        }
    }

    public void close() {
        try {
            if (csvListWriter != null) {
                csvListWriter.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not close CSV writer for file " + outputFile.getAbsolutePath(), e);
        }
    }

    public void startDocument() {
    }

    public void startTextBlock() {
    }

    public void stopTextBlock() {
    }

    public void stopDocument() {
    }

    public void newLine(boolean hyphenated) {
    }

    public void globalShutdown() {
    }

    @Override
    public void setAltoDocument(Document doc) {
    }

}
