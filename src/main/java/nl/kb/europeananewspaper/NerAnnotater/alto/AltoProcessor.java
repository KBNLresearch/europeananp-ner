package nl.kb.europeananewspaper.NerAnnotater.alto;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import nl.kb.europeananewspaper.NerAnnotater.NERClassifiers;
import nl.kb.europeananewspaper.NerAnnotater.TextElementsExtractor;
import nl.kb.europeananewspaper.NerAnnotater.output.ResultHandler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * ALTO file processing
 * 
 * @author rene
 * 
 */
public class AltoProcessor {

	/**
	 * @param potentialAltoFilename
	 * @param mimeType
	 * @param lang
	 * @param handler
	 * @throws IOException
	 */
	public static void handlePotentialAltoFile(final URL potentialAltoFilename,
			final String mimeType, final Locale lang, final ResultHandler[] handler)
			throws IOException {
		if ("text/xml".equalsIgnoreCase(mimeType)
				|| potentialAltoFilename.getFile().endsWith(".xml")) {
			try {
				System.out.println("Trying to process ALTO file "
						+ potentialAltoFilename);
				long startTime = System.currentTimeMillis();
				Document doc = Jsoup.parse(potentialAltoFilename.openStream(),
						"UTF-8", "", Parser.xmlParser());
				Elements elementsByTag = doc.getElementsByTag("alto");
				if (elementsByTag.isEmpty()) {
					System.err.println("Does not seem to be a ALTO file: "
							+ potentialAltoFilename.toExternalForm());
					return;
				}

				// we have an Alto file

				for (ResultHandler h : handler) {
					h.startDocument();
				}
				@SuppressWarnings("unchecked")
				CRFClassifier<CoreMap> classifier = (CRFClassifier<CoreMap>) NERClassifiers
						.getCRFClassifierForLanguage(lang);

				List<List<CoreMap>> coreMapElements = TextElementsExtractor
						.getCoreMapElements(doc);
				int totalNumberOfWords = 0;
				int classified = 0;
				for (List<CoreMap> block : coreMapElements) {

					for (ResultHandler h : handler) {
						h.startTextBlock();
					}
					List<CoreMap> classify = classifier.classify(block);
					for (CoreMap label : classify) {

						if (label.get(HyphenatedLineBreak.class) != null) {
							for (ResultHandler h : handler) {
								h.newLine(label.get(HyphenatedLineBreak.class));
							}

						} else {

							totalNumberOfWords += 1;
							if (!label.get(AnswerAnnotation.class).equals("O")) {
								classified += 1;
								for (ResultHandler h : handler) {
									h.addToken(
											label.get(AltoStringID.class),
											label.get(OriginalContent.class),
											label.get(TextAnnotation.class),
											label.get(AnswerAnnotation.class),
											label.get(ContinuationAltoStringID.class));
								}

							} else {
								for (ResultHandler h : handler) {
									h.addToken(
											label.get(AltoStringID.class),
											label.get(OriginalContent.class),
											label.get(TextAnnotation.class),
											null,
											label.get(ContinuationAltoStringID.class));
								}

							}
						}
					}
					for (ResultHandler h : handler) {
						h.stopTextBlock();
					}

				}

				for (ResultHandler h : handler) {
					h.stopDocument();
				}
				System.out.println();
				System.out.println("Statistics: "
						+ classified
						+ " out of "
						+ totalNumberOfWords
						+ " ("
						+ (new Double(classified) / new Double(
								totalNumberOfWords)) + ") classified");
				System.out.println("Total millisecs: "
						+ (System.currentTimeMillis() - startTime));

			} catch (IOException e) {
				System.err.println("Could not read ALTO file "
						+ potentialAltoFilename.toExternalForm());
				throw e;
			} finally {
				for (ResultHandler h : handler) {
					h.close();
				}
			}
		}

	}
}
