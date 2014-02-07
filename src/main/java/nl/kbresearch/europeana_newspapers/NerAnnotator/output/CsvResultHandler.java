package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;

import org.jsoup.nodes.Document;
//import org.w3c.dom.Document;

import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Output handler for a simple CSV format, that maintains a list of labelled
 * tokens.
 * 
 * @author rene
 * 
 */
public class CsvResultHandler implements ResultHandler {

	ContainerContext context;
	String name;
	File outputFile;
	CsvListWriter csvListWriter;

	final String[] header = new String[] { "wordId", "originalText", "text",
			"label", "continuationId" };
	final CellProcessor[] processors = new CellProcessor[] { new NotNull(),
			new NotNull(), new NotNull(), new NotNull(), new NotNull() };

	/**
	 * @param context
	 * @param name
	 */
	public CsvResultHandler(final ContainerContext context, final String name) {
		this.context = context;
		this.name = name;

	}

	public void addToken(String wordid, String originalContent, String word,
			String label, String continuationId) {

		if (label != null) {
			if (csvListWriter == null) {
				outputFile = new File(context.getOutputDirectory(), name
						+ ".csv");
				try {
					csvListWriter = new CsvListWriter(
							new FileWriter(outputFile),
							CsvPreference.STANDARD_PREFERENCE);
					csvListWriter.writeHeader(header);

				} catch (IOException e) {
					throw new IllegalStateException(
							"Could not open CSV writer for file "
									+ outputFile.getAbsolutePath(), e);
				}
			}

			try {
				String continuationIdStr = "";
				if (continuationId != null) {
					continuationIdStr = continuationId;
				}
				csvListWriter.write(wordid, originalContent, word, label,
						continuationIdStr);
			} catch (IOException e) {
				throw new IllegalStateException(
						"Could not write to CSV writer for file "
								+ outputFile.getAbsolutePath(), e);
			}
		}
	}

	public void close() {
		try {
			if (csvListWriter != null)
				csvListWriter.close();
		} catch (IOException e) {
			throw new IllegalStateException(
					"Could not close CSV writer for file "
							+ outputFile.getAbsolutePath(), e);
		}
	}

	public void startDocument() {
		// TODO Auto-generated method stub

	}

	public void startTextBlock() {
		// TODO Auto-generated method stub

	}

	public void stopTextBlock() {
		// TODO Auto-generated method stub

	}

	public void stopDocument() {
		// TODO Auto-generated method stub

	}

	public void newLine(boolean hyphenated) {
		// TODO Auto-generated method stub

	}

	public void globalShutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAltoDocument(Document doc) {
		// TODO Auto-generated method stub
		
	}

}
