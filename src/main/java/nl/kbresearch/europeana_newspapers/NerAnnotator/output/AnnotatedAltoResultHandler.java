package nl.kbresearch.europeana_newspapers.NerAnnotator.output;
import nl.kbresearch.europeana_newspapers.NerAnnotator.TextElementsExtractor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

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
		// TODO Auto-generated method stub

	}

	@Override
	public void newLine(boolean hyphenated) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addToken(String wordid, String originalContent, String word,
			String label, String continuationid) {
		if (label!=null&&altoDocument!=null) {
                        //System.out.println(wordid+  " : "+ word + " : " + label);
			Element domElement = TextElementsExtractor.findAltoElementByStringID(altoDocument, wordid);
                        //System.out.println(domElement);
			//domElement.getAttribute("alternative", label);
                        //ADD SOME MAGIC HERE
                        domElement.setAttribute("alternative", label);
		}

	}

	@Override
	public void stopTextBlock() {
            // TODO Auto-generated method stub
	}

	@Override
	public void stopDocument() {
            try {
                outputFile = new PrintWriter(new File(context.getOutputDirectory(), name + ".alto.xml"), "UTF-8");
                DOMSource domSource = new DOMSource(altoDocument);
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
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
