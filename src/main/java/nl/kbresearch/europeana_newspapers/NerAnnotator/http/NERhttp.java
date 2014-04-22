package nl.kbresearch.europeana_newspapers.NerAnnotator.http;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.*;

import javax.servlet.http.*;
import java.util.Locale;

import edu.stanford.nlp.ie.crf.CRFClassifier;

import java.util.Properties;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;

import nl.kbresearch.europeana_newspapers.NerAnnotator.NERClassifiers;




public class NERhttp extends HttpServlet {

    public final static String CONFIG_PATH = "WEB-INF/classes/config.ini";


    // Support the following requests: 
    // 
    //
    // /
    //   * Display help text and some sample links.
    //
    //
    // /?listClassifiers
    //   * Display the list of loaded classifiers and their checksums.
    //
    //
    // /?lang=nl&alto=http://resources2.kb.nl/000010000/alto/000010470/DDD_000010470_001_alto.xml
    //
    //   * If there is a classifier available to classify, return an 
    //     xml/text response with the result.
    //


    public void init() throws ServletException {

    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        HashMap config = parse_config();

        if (request.getParameter("lang") != null) {
            Object value = config.get(request.getParameter("lang"));
            if ( value != null ) {
                out.println(request.getParameter("lang"));
            } else {
                out.println("Error : Language " + (String) request.getParameter("lang") +  " not found in config, See <a href='?listClassifiers'>listClassifiers</a>");
            }
        } else {
            usage(out);
        }

    }

    public HashMap parse_config() {
        final String CONFIG_FILE = getServletContext().getRealPath("/") + CONFIG_PATH;
        HashMap classifiers = new HashMap();
        //classifiers.put("nl", "ok");

        try {
            InputStream input = new FileInputStream(CONFIG_FILE);
            Properties props = new Properties(); 
            props.load(input);
        } catch (IOException ex) {
            return classifiers;
        } 

        return classifiers;
    }

    public void usage(PrintWriter out) {
        out.println("<h1><a href='https://github.com/KBNLresearch/europeananp-ner'>europeananp-ner</a></h1><br>");
        out.println("Usage: <br><a href='?lang=nl&alto=/path/to/altofile'>example1</a>");
        out.println("<a href='?lang=nl&html=/path/to/altofile'>example2</a><br>");
    }

    public void destroy() {
    }
}
