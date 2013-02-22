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
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class AltoProcessor {

	public static void handlePotentialAltoFile(URL potentialAltoFilename,
			String mimeType, Locale lang, ResultHandler handler) {
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

				@SuppressWarnings("unchecked")
				CRFClassifier<CoreMap> classifier = (CRFClassifier<CoreMap>) NERClassifiers
						.getCRFClassifierForLanguage(lang);

				List<List<CoreMap>> coreMapElements = TextElementsExtractor
						.getCoreMapElements(doc);

				// System.out.println("--------------------");
				// System.out.println("Results for "+potentialAltoFilename.toExternalForm());
				// int lineNmbr=1;
				int totalNumberOfWords = 0;
				int classified = 0;
				for (List<CoreMap> line : coreMapElements) {

					// System.out.print("Line #"+(lineNmbr++)+" ");
					// for (CoreLabel label:line) {
					// System.out.print(label.word()+" ");
					// }
					// System.out.println();

					List<CoreMap> classify = classifier.classify(line);

					for (CoreMap label : classify) {

						totalNumberOfWords += 1;
						if (!label.get(AnswerAnnotation.class).equals("O")) {
							classified += 1;
							handler.addEntry(label.get(AltoStringID.class),
									label.get(TextAnnotation.class),
									label.get(AnswerAnnotation.class));
						}
					}
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
				e.printStackTrace();
			} finally {
				handler.close();
			}
		}

	}
}
