package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.AltoProcessor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.output.ResultHandlerFactory;

import java.io.File;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Processor for METS containers.
 *
 * @author Rene
 * @author Willem Jan Faber
 *
 */


public class MetsProcessor implements ContainerProcessor {
    // The default instance of the METS processor.
    public static MetsProcessor INSTANCE = new MetsProcessor();

    @Override
    public boolean processFile(ContainerContext context,
                               String urlStr,
                               Locale lang,
                               String md5sum) throws IOException {

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
                    Element token_element = (Element) tokens;
                    URL potentialAltoFilename;
                    try {
                        if (token_element.getAttribute("xlink:href").endsWith(".xml")) {
                            URI referencedFile = new URI(token_element.getAttribute("xlink:href"));

                            if ("file".equalsIgnoreCase(referencedFile.getScheme())) {
                                String path = referencedFile.getPath();
                                String relativeToUrl = url.toString();
                                potentialAltoFilename = new URI(relativeToUrl.substring(0,
                                                                                        relativeToUrl.lastIndexOf("/")) +
                                                                                        path).normalize().toURL();
                            } else {
                                potentialAltoFilename = referencedFile.normalize().toURL();
                            }

                            String[] split = potentialAltoFilename.toExternalForm().split("/");
                            String name;

                            if (split.length > 0 && !split[split.length - 1].isEmpty()) {
                                // Name from URL.
                                name = split[split.length - 1];
                            } else {
                                // Generic name, if not available.
                                name = "alto-" + (count++) + ".xml";
                            }

                            System.out.println(potentialAltoFilename);
                            AltoProcessor.handlePotentialAltoFile(potentialAltoFilename,
                                                                  "text/xml",
                                                                  lang,
                                                                  md5sum,
                                                                  ResultHandlerFactory.createResultHandlers(context,
                                                                                                            name,
                                                                                                            md5sum,
                                                                                                            null));
                        }
                    } catch (URISyntaxException error) {
                        System.err.println("Error parsing path to file in METS for file id " +
                                           token_element.getAttribute("ID"));
                        error.printStackTrace();
                        return false;
                   }

                }
            }
        } catch (javax.xml.parsers.ParserConfigurationException error) { 
            error.printStackTrace();
            return false;
        } catch (org.xml.sax.SAXException error) {
            error.printStackTrace();
            return false;
        }
        return count > 0;
    }
}
