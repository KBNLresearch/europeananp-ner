package nl.kbresearch.europeana_newspapers.ner.output;

import nl.kbresearch.europeana_newspapers.ner.container.ContainerContext;

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
 */


public class CsvResultHandler extends ResultHandler {
    ContainerContext context;
    CsvListWriter csvListWriter;
    File outputFile;
    String name;

    final String[] header = new String[] { "wordId",
                                           "originalText",
                                           "text",
                                           "label",
                                           "continuationId"
    };

    final CellProcessor[] processors = new CellProcessor[] { new NotNull(),
                                                             new NotNull(),
                                                             new NotNull(),
                                                             new NotNull(),
                                                             new NotNull()
    };

    public CsvResultHandler(final ContainerContext context, final String name) {
        this.context = context;
        this.name = name;
    }

    public void addToken(String wordid,
                         String originalContent,
                         String word,
                         String label,
                         String continuationId) {

        if (label != null) {
            if (csvListWriter == null) {
                // Create a new file(handler) .csv 
                outputFile = new File(context.getOutputDirectory(), name + ".csv");

                try {
                    csvListWriter = new CsvListWriter(
                                        new FileWriter(outputFile),
                                        CsvPreference.STANDARD_PREFERENCE);

                    csvListWriter.writeHeader(header);

                } catch (IOException error) {
                    String msg = "Could not open CSV writer for file " +
                                 outputFile.getAbsolutePath();
                    throw new IllegalStateException(msg, error);
                }
            }
            try {
                String continuationIdStr = "";

                if (continuationId != null) {
                    continuationIdStr = continuationId;
                }

                csvListWriter.write(wordid,
                                    originalContent,
                                    word,
                                    label,
                                    continuationIdStr);

            } catch (IOException error) {
                String msg = "Could not open CSV writer for file " +
                             outputFile.getAbsolutePath();
                throw new IllegalStateException(msg, error);
            }
        }
    }

    public void close() {
        try {
            if (csvListWriter != null) {
                csvListWriter.close();
            }
        } catch (IOException error) {
            String msg = "Could not open CSV writer for file " +
                         outputFile.getAbsolutePath();
            throw new IllegalStateException(msg, error);
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
