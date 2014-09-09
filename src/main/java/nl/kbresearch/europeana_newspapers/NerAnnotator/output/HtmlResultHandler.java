package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;

import java.io.*;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Document;


/**
 * Output for a simple HTML format, that tries to keep the logical structure
 * intact and highlights labelled tokens.
 *
 * @author Rene
 *
 */


public class HtmlResultHandler implements ResultHandler {
    ContainerContext context;
    String continuationId = null;
    String continuationLabel = null;
    String name;
    String spacePrefix = "";
    Writer outputFile;

    public HtmlResultHandler(final ContainerContext context,
                             final String name) {
        this.context = context;
        this.name = name;
    }

    public void startDocument() {
        try {
            outputFile = new BufferedWriter(
                            new FileWriter(
                                new File(context.getOutputDirectory(), name + ".html")));

            outputFile.write("<!doctype html>\n<html lang=en>\n" +
                             + "<head>\n<meta charset=utf-8><title>" +
                             + StringEscapeUtils.escapeHtml4(name) + "</title>\n"
                             + "</head>\n<body>\n</html>\n");
        } catch (IOException error) {
            String msg = "Could not wirte to HTML file";
            throw new IllegalStateException(msg, error);
        }
    }

    // Add a div to the body
    public void startTextBlock() {
        try {
            outputFile.write("<div>\n");
        } catch (IOException error) {
            String msg = "Could not write to HTML file";
            throw new IllegalStateException(msg, error);
        }
        spacePrefix = "";
    }

    // Add a newline to the HTML,
    // just a BR will do, use &#8208; 
    // for hypenation.
    public void newLine(boolean hyphenated) {
        try {
            if (hyphenated) {
                outputFile.write("&#8208;<br/>");
            } else {
                outputFile.write("<br/>");
            }
        } catch (IOException error) {
            String msg = "Could not write to HTML file";
            throw new IllegalStateException(msg, error);
        }
        spacePrefix = "";
    }

    public void addToken(String wordid,
                         String originalContent,
                         String word,
                         String label,
                         String continuationId) {

        // Find out if this is a continuation of the previous word(part).
        if (continuationId != null) {
                this.continuationId = continuationId;
                this.continuationLabel = label;
        }

        // Replace the label if it is a continuation.
        if (wordid.equals(this.continuationId)) {
            label = continuationLabel;
        }

        try {
            if (label == null) {
                outputFile.write(
                        StringEscapeUtils.escapeHtml4(
                            spacePrefix + originalContent));
            } else {
                outputFile.write(spacePrefix +
                                 "<span style=\"background-color:#ddddff;\" title=\"" +
                                 StringEscapeUtils.escapeHtml4(label) + "\">" +
                                 StringEscapeUtils.escapeHtml4(originalContent) +
                                 "</span>");
            }
        } catch (IOException error) {
            String msg = "Could not write to HTML file";
            throw new IllegalStateException(msg, error);
        }
        spacePrefix = " ";
    }

    // Close the DIV.
    public void stopTextBlock() {
        try {
            outputFile.write("</div>\n");
        } catch (IOException error) {
            String msg = "Could not write to HTML file";
            throw new IllegalStateException(msg, error);
        }
    }

    // Close the HTML.
    public void stopDocument() {
        try {
            outputFile.write("</body>\n</html>\n");
        } catch (IOException error) {
            String msg = "Could not write to HTML file";
            throw new IllegalStateException(msg, error);
        }
    }

    // Write HTML to disk.
    public void close() {
        try {
            outputFile.close();
        } catch (IOException error) {
            String msg = "Could not write to HTML file";
            throw new IllegalStateException(msg, error);
        }
    }

    public void globalShutdown() {
    }

    @Override
    public void setAltoDocument(Document doc) {
    }
}
