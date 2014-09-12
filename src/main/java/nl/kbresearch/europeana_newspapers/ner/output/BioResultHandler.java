package nl.kbresearch.europeana_newspapers.ner.output;

import nl.kbresearch.europeana_newspapers.ner.container.ContainerContext;

import java.io.*;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.w3c.dom.Document;


/**
 * Output as stanford-ner bio file format.
 *
 * @author Willem Jan Faber
 *
 */


public class BioResultHandler extends ResultHandler {
    ContainerContext context;
    String continuationId = null;
    String continuationLabel = null;
    String name;
    String sentence;
    Writer outputFile;
    int labelCount = 0;

    List<HashMap> labels = new ArrayList<HashMap>();

    // Fiddle with these values to reduce or increase
    // noise in the output BIO file.
    final static int minSentenceLength = 20;
    final static int minEntitiesCount = 1;

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
        this.sentence = "";
        this.labels = new ArrayList<HashMap>();
        this.labelCount = 0;
    }

    public void newLine(boolean hyphenated) {
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

        if (label != null) {
            this.labelCount += 1;
        }

        if (originalContent != null) {
            if (label != null) {
                HashMap<String, String> entry = new HashMap<String, String>();
                entry.put(originalContent, label);
                this.labels.add(entry);
            } else {
                HashMap<String, String> entry = new HashMap<String, String>();
                entry.put(originalContent, "");
                this.labels.add(entry);
            }
            this.sentence += " " + originalContent;
        }

        if ((this.sentence.length() > minSentenceLength) &&
                (this.sentence.endsWith(".")) &&
                (this.labelCount > minEntitiesCount)) {

            for (HashMap part: this.labels) {
                String outword = (String) part.keySet().toArray()[0];
                String outlabel = (String) part.get(outword);

                if (outlabel.equals("")) {
                    try {
                        outputFile.write(outword + " POS O\n");
                    } catch (IOException e) {
                        throw new IllegalStateException("Could not write to BIO file", e);
                    }
                } else {
                    try {
                        outputFile.write(outword + " POS " + outlabel + "\n");
                    } catch (IOException e) {
                        throw new IllegalStateException("Could not write to BIO file", e);
                    }
                }
            }
            this.sentence = "";
            this.labels = new ArrayList<HashMap>();
            this.labelCount = 0;
        }
    }

    public void stopTextBlock() {
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
