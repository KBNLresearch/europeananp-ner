package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

import java.util.HashMap;


import nl.kbresearch.europeana_newspapers.NerAnnotator.TextElementsExtractor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * @author rene
 *
 */

public class Alto2_1ResultHandler implements ResultHandler {

    private ContainerContext context;
    private String name;
    private PrintWriter outputFile;
    private Document altoDocument;
    //private List<String> Entity_list = new ArrayList<String>();
    private List<HashMap> Entity_list = new ArrayList();
    // List<Map> list = new ArrayList();

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
        // TODO Auto-generated method stub

    }

    @Override
    public void newLine(boolean hyphenated) {
        // TODO Auto-generated method stub

    }



    ///  TODO Add alto2_1 handler here..
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

                domElement.attr("TAGREFS", "Tag" + String.valueOf(this.tagCounter-1));
                mMap.put("id", String.valueOf(this.tagCounter-1));
                mMap.put("label", label);
                mMap.put("word", word);
                this.Entity_list.add(mMap);
            } else {
                mMap.put("id", String.valueOf(this.tagCounter));
                domElement.attr("TAGREFS", "Tag" + String.valueOf(this.tagCounter));
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
        // TODO Auto-generated method stub
    }

    @Override
    public void stopDocument() {
        try {

            Element alto = altoDocument.select("alto").first();
            alto.attr("xmlns" , "http://www.loc.gov/standards/alto/ns-v2#");
            alto.attr("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            alto.attr("xsi:schemaLocation", "http://www.loc.gov/standards/alto/ns-v2# https://raw.github.com/altoxml/schema/master/v2/alto-2-1-draft.xsd");
            // create the alto tags section.
            altoDocument.select("Styles").after("<Tags>");
            Element e = altoDocument.select("Tags").first();

            // add the tags in the order that where detected.
            for (HashMap s: this.Entity_list) {
                Element tag = e.appendElement("NamedEntityTag");
                tag.attr("id", "Tag" + (String)s.get("id"));
                tag.attr("type", (String)s.get("label"));
                tag.attr("label", (String)s.get("word"));
            }

            outputFile = new PrintWriter(new File(context.getOutputDirectory(), name + ".alto2_1.xml"), "UTF-8");
            outputFile.print(altoDocument.toString().replaceAll("></namedentitytag>", "/>"));
            outputFile.flush();
            outputFile.close();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to Alto XML file", e);
        }
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public void globalShutdown() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setAltoDocument(Document doc) {
        altoDocument=doc;

    }
}
