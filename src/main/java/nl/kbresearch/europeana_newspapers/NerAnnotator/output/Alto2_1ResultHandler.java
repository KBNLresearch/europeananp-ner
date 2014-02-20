package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import nl.kbresearch.europeana_newspapers.NerAnnotator.TextElementsExtractor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;

import java.io.*;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

/**
 * @author Willem Jan Faber
 *
 */

public class Alto2_1ResultHandler implements ResultHandler {

    private ContainerContext context;
    private String name;
    private PrintWriter outputFile;
    private Document altoDocument;
    private List<HashMap> Entity_list = new ArrayList();

    String continuationId = null;
    String continuationLabel = null;

    String prevWord = "";
    String prevType = "";

    boolean prevIsNamed = false;

    int tagCounter = 0;

    /**
     * @param context
     * @param name
     */
    public Alto2_1ResultHandler(final ContainerContext context, final String name) {
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
    public void addToken(String wordid, String originalContent, String word,
                         String label, String continuationid) {

        HashMap mMap = new HashMap();

        // try to find out if this is a continuation of the previous word
        if (continuationId != null) {
            this.continuationId = continuationId;
            this.continuationLabel = label;
        }

        if (wordid.equals(this.continuationId)) {
            label = continuationLabel;
        }

        if (label != null) {
            if ((label.equals("B-LOC")) || (label.equals("I-LOC"))) {
                label = "location";
            }
            if ((label.equals("B-PER")) || (label.equals("I-PER"))) {
                label = "person";
            }
            if ((label.equals("B-ORG") || (label.equals("I-ORG")))) {
                label = "organization";
            }

            Element domElement = TextElementsExtractor.findAltoElementByStringID(altoDocument, wordid);

            if (this.prevIsNamed) {
                if (!this.prevWord.equals("")) {
                    if (this.prevType.equals(label)) {
                        // Concatenation string to generate one label.
                        word = this.prevWord + word;
                        this.Entity_list.remove(this.Entity_list.size()-1);
                    }
                }
                this.prevWord = word + " ";
                this.prevType = label;
                domElement.setAttribute("TAGREFS", "Tag" + String.valueOf(this.tagCounter-1));
                mMap.put("id", String.valueOf(this.tagCounter-1));
                mMap.put("label", label);
                mMap.put("word", word);
                this.Entity_list.add(mMap);
            } else {
                mMap.put("id", String.valueOf(this.tagCounter));
                domElement.setAttribute("TAGREFS", "Tag" + String.valueOf(this.tagCounter-1));
                mMap.put("label", label);
                mMap.put("word", word);
                this.Entity_list.add(mMap);
                this.tagCounter += 1;
                this.prevWord = word + " ";
                this.prevType = label;
            }
            this.prevIsNamed = true;
        } else {
            this.prevIsNamed = false;

        }
    }

    @Override
    public void stopTextBlock() {
    }

    @Override
    public void stopDocument() {
        Element child = altoDocument.createElement("Tags");
        for (HashMap s: this.Entity_list) {
            Element childOfTheChild = altoDocument.createElement("NamedEntityTag");
            childOfTheChild.setAttribute("type", (String) s.get("label"));
            childOfTheChild.setAttribute("label", (String) s.get("word"));
            childOfTheChild.setAttribute("id", (String) s.get("id"));
            child.appendChild(childOfTheChild);
        }

        NodeList alto = altoDocument.getElementsByTagName("alto");
        altoDocument.getDocumentElement().appendChild(child);
        Element alto_root = (Element) alto.item(0);
        alto_root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        alto_root.setAttribute("xmlns" , "http://www.loc.gov/standards/alto/ns-v2#");
        alto_root.setAttribute("xsi:schemaLocation", "http://www.loc.gov/standards/alto/ns-v2# https://raw.github.com/altoxml/schema/master/v2/alto-2-1-draft.xsd");

        try {
            outputFile = new PrintWriter(new File(context.getOutputDirectory(), name + ".alto2_1.xml"), "UTF-8");
            DOMSource domSource = new DOMSource(altoDocument);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(domSource, result);
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
        altoDocument=doc;

    }
}
