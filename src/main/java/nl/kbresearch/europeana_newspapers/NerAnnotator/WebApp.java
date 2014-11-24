package nl.kbresearch.europeana_newspapers.NerAnnotator;

import nl.kbresearch.europeana_newspapers.NerAnnotator.http.NERhttp;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;

import javax.servlet.*;
import javax.servlet.http.*;


/**
 * HTTP interface for Europeana-NER
 * 
 * @author Willem Jan Faber
 */
public class WebApp extends HttpServlet {
    // Path to configuration file, specifies the path of the classifiers
    public final static String CONFIG_PATH = "WEB-INF/classes/config.ini";

    // Global object for storing references to the classifiers in mem
    public HashMap config = new HashMap();

    // Initialize and load classifiers from disk
    public void init() throws ServletException {
        this.config = NERhttp.init(getServletContext().getRealPath("/") + CONFIG_PATH);
    }

    // Handle the flow of the webapp
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Setup output handler
        PrintWriter out = null;

        // Set the desired output mode (default = XML)
        if ((request.getParameter("mode") != null) && (request.getParameter("mode").equals("html"))) {
            response.setContentType("text/html");
            out = response.getWriter();
            this.config.put("mode", "html");
        } else {
            response.setContentType("text/xml; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            out = response.getWriter();
            this.config.put("mode", "xml");
        }

        // Handle 'listClassifiers' argument
        if (request.getParameter("listClassifiers") != null) {
            NERhttp.listClassifiers(config, out);
            return;
        }


        String lang = null;
        // Handle 'lang' argument
        if (request.getParameter("lang") != null) {
            lang = request.getParameter("lang");
            config = NERhttp.setLang(config, request.getParameter("lang"), out);
            System.out.println(lang);
        }

        // Handle 'alto' argument
        if (request.getParameter("alto") != null) {
            NERhttp.parse_alto(config, request.getParameter("alto"), out, lang);
        }
    }
}
