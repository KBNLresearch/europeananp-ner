package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.AltoProcessor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.output.ResultHandlerFactory;

/*
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
*/

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/**
 * Processor for METS containers
 * 
 * @author Rene
 * @author Willem Jan Faber
 * 
 */
public class MetsProcessor implements ContainerProcessor {
    /**
    * the default instance of the METS processor
    */
    public static MetsProcessor INSTANCE = new MetsProcessor();

    @Override
    public boolean processFile(ContainerContext context, String urlStr, Locale lang, String md5sum) throws IOException {
        System.out.println("Processing METS file " + urlStr);
        URL url = null;
        File file = new File(urlStr);

        if (file.exists()) {
            url = file.toURI().toURL();
        } else {
            url = new URL(urlStr);
        }
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        int count = 0;

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(file);
            NodeList nodesByTag = doc.getElementsByTagName("mets:FLocat");
            for (int i = 0; i<nodesByTag.getLength(); i++) {
                Node tokens = nodesByTag.item(i);
                if (tokens.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) tokens;
                    URL potentialAltoFilename;
                    try {
                        if (e.getAttribute("xlink:href").endsWith(".xml")) {
                            URI referencedFile = new URI(e.getAttribute("xlink:href"));
                            if ("file".equalsIgnoreCase(referencedFile.getScheme())) {
                                String path = referencedFile.getPath();
                                String relativeToUrl = url.toString();
                                potentialAltoFilename = new URI(relativeToUrl.substring(0, relativeToUrl.lastIndexOf("/")) + path).normalize() .toURL();
                            } else {
                                potentialAltoFilename = referencedFile.normalize().toURL();
                            }
                            String[] split = potentialAltoFilename.toExternalForm().split("/");
                            String name;
                            if (split.length > 0 && !split[split.length - 1].isEmpty()) {
                                // name from url
                                name = split[split.length - 1];
                            } else {
                                // Generic name, if not available
                                name = "alto-" + (count++) + ".xml";
                            }
                            System.out.println(potentialAltoFilename);
                            AltoProcessor.handlePotentialAltoFile(potentialAltoFilename, "text/xml",
                                                                  lang, md5sum, ResultHandlerFactory.createResultHandlers(context, name, md5sum));
                        }
                   } catch (URISyntaxException ee) {
                        System.err.println("Error parsing path to file in METS for file id " + e.getAttribute("ID"));
                        ee.printStackTrace();
                        return false;
                   }

                }
            }
        } catch (javax.xml.parsers.ParserConfigurationException e) { 
            e.printStackTrace();
            return false;
        } catch (org.xml.sax.SAXException e) {
            e.printStackTrace();
            return false;
        }
        return (count > 0);
    }
}
