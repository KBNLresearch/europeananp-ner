package nl.kb.europeananewspaper.NerAnnotator.output;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import nl.kb.europeananewspaper.NerAnnotator.TextElementsExtractor;
import nl.kb.europeananewspaper.NerAnnotator.container.ContainerContext;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * @author rene
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
			Element domElement = TextElementsExtractor.findAltoElementByStringID(altoDocument, wordid);
			domElement.attr("alternative",label);
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
			outputFile.print(altoDocument.toString());
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
