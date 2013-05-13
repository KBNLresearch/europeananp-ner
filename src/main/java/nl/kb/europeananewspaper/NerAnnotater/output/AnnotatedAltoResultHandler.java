package nl.kb.europeananewspaper.NerAnnotater.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import nl.kb.europeananewspaper.NerAnnotater.TextElementsExtractor;
import nl.kb.europeananewspaper.NerAnnotater.container.ContainerContext;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * @author rene
 *
 */
public class AnnotatedAltoResultHandler implements ResultHandler {

	private ContainerContext context;
	private String name;
	private BufferedWriter outputFile;
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
			outputFile = new BufferedWriter(new FileWriter(new File(
					context.getOutputDirectory(), name + ".alto.xml")));
			outputFile.write(altoDocument.toString());
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
