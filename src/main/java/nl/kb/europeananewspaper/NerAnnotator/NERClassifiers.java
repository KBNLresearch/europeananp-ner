package nl.kb.europeananewspaper.NerAnnotator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import edu.stanford.nlp.ie.crf.CRFClassifier;

/**
 * Singleton holder for preloaded classifiers
 * 
 * @author rene
 * 
 * 
 */
public class NERClassifiers {

	@SuppressWarnings("rawtypes")
	static Map<Locale, CRFClassifier> classifierMap = new ConcurrentHashMap<Locale, CRFClassifier>();

	static Properties langModels;

	/**
	 * @param langModels
	 *            filenames of the classifier model for a language. E.g. de ->
	 *            /path/to/file/model.gz
	 */
	public static void setLanguageModels(final Properties langModels) {
		NERClassifiers.langModels = langModels;
	}

	/**
	 * @param lang
	 *            the language to get
	 * @return the classifier for the language
	 */
	public synchronized static CRFClassifier<?> getCRFClassifierForLanguage(
			final Locale lang) {
		if (lang == null) {
			throw new IllegalArgumentException(
					"No language defined for classifier!");
		}

		CRFClassifier<?> classifier = null;

		if (classifierMap.get(lang) == null) {
			// try to load model
			System.out.println("Loading language model for "
					+ lang.getDisplayLanguage());

			// load some defaults for now

			try {
				for (Object langKey : langModels.keySet()) {
					System.out.println(langKey + " -> "
							+ langModels.getProperty(langKey.toString()));
					if (lang.getLanguage().equals(
							new Locale(langKey.toString()).getLanguage())) {
						classifier = CRFClassifier
								.getClassifier(getDefaultInputModelStream(
										langModels.getProperty(langKey
												.toString()), lang));

					}

				}

			} catch (ClassCastException e) {
				throw new IllegalArgumentException(
						"Model does not seem to be the right class ", e);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(
						"Class not found while loading model ", e);
			} catch (IOException e) {
				throw new IllegalArgumentException(
						"I/O error while reading class ", e);
			}

			if (classifier == null) {
				throw new IllegalArgumentException(
						"No language model found for language "
								+ lang.getDisplayCountry());
			}
			System.out.println("Done");
			classifierMap.put(lang, classifier);

		}
		return classifierMap.get(lang);

	}

	private static InputStream getDefaultInputModelStream(String modelName,
			Locale lang) {
		InputStream modelStream = null;

		try {
			modelStream = new FileInputStream(modelName);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Model file not found: "
					+ modelName);
		}

		if (modelName.endsWith(".gz")) {
			try {
				return new GZIPInputStream(modelStream);
			} catch (IOException e) {
				return null;
			}
		} else
			return new BufferedInputStream(modelStream);
	}
}
