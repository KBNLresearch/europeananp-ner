package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;

import java.io.*;

import org.w3c.dom.Document;

/**
 * Output for the stanford-ner bio file format.
 * 
 * @author Willem Jan Faber
 * 
 */
public class BioResultHandler implements ResultHandler {
    ContainerContext context;
    String continuationId = null;
    String continuationLabel = null;
    String name;
    String sentence;
    Writer outputFile;
    int labelCount = 0;

    final static int minSentenceLength = 20;
    final static int minEntitiesCount = 1;

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
        this.sentence = "";
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

            // This should be added to linked list, key->value style
            if (label != null) {
                if (originalContent.endsWith(".")) {
                    this.sentence += " <" + label + ">" + originalContent + "</" + label + ">."; 
                } else { 
                    this.sentence += " <" + label +">" + originalContent + "</" + label + ">"; 
                }
            } else {
                this.sentence += " " + originalContent;
            }
        }

        if ((this.sentence.length() > minSentenceLength) && (this.sentence.endsWith(".")) && (this.labelCount > minEntitiesCount)) {
            System.out.println(this.sentence);

            for (String part: this.sentence.split(" ")) {
                if (part.length() > 1) {
                    System.out.println(part);
                }

            }
            this.sentence = "";
            this.labelCount = 0;
        }
    
        /*

        try {
            if (label == null) {
                outputFile.write(originalContent + " POS O\n");
            } else {
                outputFile.write(originalContent
                                 + " POS "
                                 + label + "\n");
            }
            
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to BIO file", e);
        }
        */
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
