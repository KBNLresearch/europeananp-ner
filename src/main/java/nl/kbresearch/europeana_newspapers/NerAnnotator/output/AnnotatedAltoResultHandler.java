package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import nl.kbresearch.europeana_newspapers.NerAnnotator.TextElementsExtractor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;

import java.io.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Rene
 * @author Willem Jan Faber
 *
 */

public class AnnotatedAltoResultHandler implements ResultHandler {
    private ContainerContext context;
    private String name;
    private PrintWriter outputFile;
    private Document altoDocument;

    /**
     * @param context
     * @param name
     */
    public AnnotatedAltoResultHandler(final ContainerContext context, final String name) {
        this.context = context;
        this.name = name;
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void startTextBlock() {
    }

    @Override
    public void newLine(boolean hyphenated) {
    }

    @Override
    public void addToken(String wordid, String originalContent, String word, String label, String continuationid) {
        if ((label != null) && (altoDocument != null)) {
            // Find the alto node with the corresponding wordid.
            // Needed to add the alternative label to the alto document
            Element domElement = TextElementsExtractor.findAltoElementByStringID(altoDocument, wordid);
            domElement.setAttribute("ALTERNATIVE", label);
        }
    }

    @Override
    public void stopTextBlock() {
    }

    @Override
    public void stopDocument() {
        try {
            // Output file for alto format.
            outputFile = new PrintWriter(new File(context.getOutputDirectory(), name + ".alto.xml"), "UTF-8");
            DOMSource domSource = new DOMSource(altoDocument);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            // Vanilla transformer from DOM to string.
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);

            // Store results to file.
            outputFile.print(writer.toString());
            outputFile.flush();
            outputFile.close();
        } catch(TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void globalShutdown() {
    }

    @Override
    public void setAltoDocument(Document doc) {
        altoDocument = doc;
    }
}
