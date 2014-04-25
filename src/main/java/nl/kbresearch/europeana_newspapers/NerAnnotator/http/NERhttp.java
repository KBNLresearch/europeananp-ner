package nl.kbresearch.europeana_newspapers.NerAnnotator.http;

import nl.kbresearch.europeana_newspapers.NerAnnotator.NERClassifiers;
import edu.stanford.nlp.ie.crf.CRFClassifier;

import java.io.BufferedInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class NERhttp {
    // List the available classifiers
    static public void listClassifiers(HashMap config, PrintWriter out) {
        if (config.get("mode").equals("xml")) {
            out.println("<classifiers>");
            for (Object value: config.keySet()) {
                if ((config.get((String) value) != null) && (((String) value).endsWith("__checksum"))) {
                    out.println("<classifier>");
                    out.println("<lang>");
                    out.println(((String) value).split("__")[0]);
                    out.println("</lang>");
                    out.println("<checksum>");
                    out.println((String) config.get((String) value));
                    out.println("</checksum>");
                    out.println("</classifier>");
                }
            }
            out.println("</classifiers>");
        } else {
            out.println("<h2>Available languages :</h2><br/>");
            for (Object value: config.keySet()) {
                if ((config.get((String) value) != null) && (((String) value).endsWith("__checksum"))) {
                    out.println(((String) value).split("__")[0] + " : ");
                    out.println((String) config.get((String) value) + "</br>");
                }
            }
        }
    }

    // Set the classifier language
    static public HashMap setLang(HashMap config, String lang, PrintWriter out) {
        return config;
    }


    // Initialize by loading/interpeting the config file
    static public HashMap init(String configPath) {
        HashMap config = new HashMap();
        config.put("status", "init");
        config.put("lang", "unknown");

        Ini configIni = new Ini();

        Properties optionProperties = new Properties();
        // optionProperties.setProperty("nl", "path_to_classifiers");

        try {
            configIni = new Ini(new FileReader(configPath));
        } catch (IOException ex) {
            System.out.println("Error while reading config file, located here: " + configPath);
        } 

        for (String sectionName: configIni.keySet()) {
            Section section = configIni.get(sectionName);

            for (String optionKey: section.keySet()) {
                    config.put(sectionName + "__" + optionKey , section.get(optionKey));
                    System.out.println(optionKey);
                    if (optionKey.equals("classifier")) {
                        optionProperties.setProperty(sectionName, section.get(optionKey));
                    }
            }
        }

        // TODO: Load classifiers from disk, and add them to the config object.
        //
        // NERClassifiers.setLanguageModels(optionProperties);
        // CRFClassifier classifier_text = NERClassifiers.getCRFClassifierForLanguage(new Locale("nl"));
        // classifiers.put("nl__classifier", classifier_text);
        // CRFClassifier classifier_text = NERClassifiers.getCRFClassifierForLanguage(lang);
        // classifier = CRFClassifier.getClassifier(getDefaultInputModelStream(langModels.getProperty(langKey.toString()), lang));
        // getDefaultInputModelStream -> modelStream = new FileInputStream(modelName); // return new GZIPInputStream(modelStream); 
        // return classifiers;

        // NERClassifiers.setLanguageModels(optionProperties);
        // CRFClassifier classifier_text = NERClassifiers.getCRFClassifierForLanguage(new Locale("nl"));
        // classifiers.put("nl__classifier", classifier_text);
        return config;
    }

    // Parse the supplied alto file
    static public void parse_alto(HashMap config, String altoPath, PrintWriter out) {
        URL url = null;

        try { 
            url = new URL(altoPath);
        } catch(MalformedURLException ex) {
            out.println("Error : " + ex.toString());
            return;
        }

        URLConnection urlConnection = null;
        InputStream in = null;

        try {
            urlConnection = url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream());
        } catch (IOException ex) {
            out.println("Error: "  + ex.toString());
        }
        

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;

        try {
            db = dbf.newDocumentBuilder(); 
        } catch (ParserConfigurationException ex) {
            System.out.println("Error while parsing xml: " + ex.toString());
        }

        Document doc = null;

        try {
            doc = db.parse(in);
        } catch (SAXException ex) {
            System.out.println("Error while parsing xml: " + ex.toString());
        } catch (IOException ex) {
            System.out.println("Error while parsing xml: " + ex.toString());
        }
        // TODO: Invoke classifier  via  AltoProcessor.java (Class needs to be 
        // adepted to cope with pre-loaded classifiers, and existing doc as input)
        // And also see if there needs to be a different kind of outputWriter.
    }


    // Parse the supplied html file
    static public void parse_html(HashMap config, String htmlPath, PrintWriter out) {
        out.println("Parsing html file " + htmlPath);
    }

    // Parse the supplied mets file
    static public void parse_mets(HashMap config, String metsPath, PrintWriter out) {
        out.println("Parsing mets file " + metsPath);
    }
}
