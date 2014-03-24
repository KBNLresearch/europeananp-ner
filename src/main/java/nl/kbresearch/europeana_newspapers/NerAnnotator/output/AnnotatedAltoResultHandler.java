package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import nl.kbresearch.europeana_newspapers.NerAnnotator.TextElementsExtractor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;

import java.io.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.Comment;
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
    private String versionString;

    /**
     * @param context
     * @param name
     * @param versionString
     */
    public AnnotatedAltoResultHandler(final ContainerContext context, final String name, final String versionString) {
        this.context = context;
        this.name = name;
        this.versionString = versionString;
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

            Element element = altoDocument.getDocumentElement();
            // Add the version information to the output xml.
            Comment comment = altoDocument.createComment(versionString);
            element.getParentNode().insertBefore(comment, element);


            DOMSource domSource = new DOMSource(altoDocument);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            // Vanilla transformer from DOM to string.
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();

            // Reformat output, because of additional nodes added.
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            // Set the right output encoding
            transformer.setOutputProperty("encoding", "ISO-8859-1");
            // Transform the input document to output
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
