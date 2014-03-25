package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;

import java.io.*;

import org.w3c.dom.Document;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Output for the stanford-ner bio file format.
 * 
 * @author Willem Jan Faber
 * 
 */
public class BioResultHandler implements ResultHandler {
    ContainerContext context;
    String name;

    Writer outputFile;
    String spacePrefix = "";
    String continuationId = null;
    String continuationLabel = null;

    /**
     * @param context
     * @param name
     */
    public BioResultHandler(final ContainerContext context, final String name) {
        this.context = context;
        this.name = name;
    }

    public void startDocument() {
        try {
            outputFile = new BufferedWriter(new FileWriter(new File(context.getOutputDirectory(), name + ".bio")));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to BIO file", e);
        }
    }

    public void startTextBlock() {
        try {
            outputFile.write("\n");
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to BIO file", e);
        }
        spacePrefix = "";
    }

    public void newLine(boolean hyphenated) {
        try {
            if (hyphenated) {
                outputFile.write("&#8208;\n");
            } else {
                outputFile.write("\n");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to BIO file", e);
        }
        spacePrefix = "";
    }

    public void addToken(String wordid, String originalContent, String word, String label, String continuationId) {
        // Find out if this is a continuation of the previous word
        if (continuationId != null) {
                this.continuationId = continuationId;
                this.continuationLabel = label;
        }

        // Replace the label if it is an continuation
        if (wordid.equals(this.continuationId)) {
            label = continuationLabel;
        }

        try {
            if (label == null) {
                outputFile.write(StringEscapeUtils.escapeHtml4(spacePrefix + originalContent));
            } else {
                outputFile.write(spacePrefix
                                 + StringEscapeUtils.escapeHtml4(originalContent)
                                 + " POS "
                                 + StringEscapeUtils.escapeHtml4(label) );
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to BIO file", e);
        }
        spacePrefix = " ";
    }

    public void stopTextBlock() {
        try {
            outputFile.write("\n");
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to BIO file", e);
        }
    }

    public void stopDocument() {
    }

    public void close() {
        try {
            outputFile.close();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to BIO file", e);
        }
    }

    public void globalShutdown() {
    }

    @Override
    public void setAltoDocument(Document doc) {
    }
}
