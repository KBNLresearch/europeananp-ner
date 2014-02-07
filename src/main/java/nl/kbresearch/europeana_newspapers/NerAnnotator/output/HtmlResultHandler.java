package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.nodes.Document;
//import org.w3c.dom.Document;

import java.io.*;

/**
 * Output for a simple HTML format, that tries to keep the logical structure
 * intact and highlights labelled tokens.
 * 
 * @author rene
 * 
 */
public class HtmlResultHandler implements ResultHandler {

	ContainerContext context;
	String name;

	Writer outputFile;
	String spacePrefix = "";
	String continuationId = null;
	String continuationLabel = null;

	/**
	 * @param context
	 * @param name
	 */
	public HtmlResultHandler(final ContainerContext context, final String name) {
		this.context = context;
		this.name = name;

	}

	public void startDocument() {
		try {
			outputFile = new BufferedWriter(new FileWriter(new File(
					context.getOutputDirectory(), name + ".html")));

			outputFile.write("	<!doctype html>\n" + "<html lang=en>\n"
					+ "<head>\n" + "<meta charset=utf-8>\n" + "<title>"
					+ StringEscapeUtils.escapeHtml4(name) + "</title>\n"
					+ "</head>\n" + "<body>\n");
		} catch (IOException e) {
			throw new IllegalStateException("Could not write to HTML file", e);
		}

	}

	public void startTextBlock() {
		try {
			outputFile.write("<div>\n");
		} catch (IOException e) {
			throw new IllegalStateException("Could not write to HTML file", e);
		}
		spacePrefix = "";
	}

	public void newLine(boolean hyphenated) {
		try {
			if (hyphenated) {
				outputFile.write("&#8208;<br/>");
			} else {
				outputFile.write("<br/>");
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not write to HTML file", e);
		}
		spacePrefix = "";
	}

	public void addToken(String wordid, String originalContent, String word,
			String label, String continuationId) {

		// try to find out if this is a continuation of the previous word
		if (continuationId != null) {
			this.continuationId = continuationId;
			this.continuationLabel = label;
		}

		if (wordid.equals(this.continuationId)) {
			label = continuationLabel;
		}

		try {
			if (label == null) {
				outputFile.write(StringEscapeUtils.escapeHtml4(spacePrefix
						+ originalContent));
			} else {
				outputFile.write(spacePrefix
						+ "<span style=\"background-color:#ddddff;\" title=\""
						+ StringEscapeUtils.escapeHtml4(label) + "\">"
						+ StringEscapeUtils.escapeHtml4(originalContent)
						+ "</span>");
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not write to HTML file", e);
		}

		spacePrefix = " ";

	}

	public void stopTextBlock() {
		try {
			outputFile.write("</div>\n");
		} catch (IOException e) {
			throw new IllegalStateException("Could not write to HTML file", e);
		}

	}

	public void stopDocument() {
		try {
			outputFile.write("</body>\n" + "</html>\n");
		} catch (IOException e) {
			throw new IllegalStateException("Could not write to HTML file", e);
		}

	}

	public void close() {
		try {
			outputFile.close();
		} catch (IOException e) {
			throw new IllegalStateException("Could not write to HTML file", e);
		}

	}

	public void globalShutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAltoDocument(Document doc) {
		// TODO Auto-generated method stub
		
	}

}
