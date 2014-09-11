package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.AltoProcessor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.output.ResultHandlerFactory;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Processor for MPEG21-DIDL files.
 *
 * @author Rene
 * @author Willem Jan Faber
 *
 */


public class DIDLProcessor implements ContainerProcessor {
    // The default instance of the MPEG21-DIDL processor.
    public static DIDLProcessor INSTANCE = new DIDLProcessor();

    @Override
    public boolean processFile(ContainerContext context,
                               String urlStr,
                               Locale lang,
                               String md5sum) throws IOException {
        URL url = null;
        File file = new File(urlStr);

        if (file.exists()) {
           url = file.toURI().toURL();
        } else {
            url = new URL(urlStr);
            System.out.println("File not found, trying to get from URL: " + url.toExternalForm());
        }

        Document doc = null;
        System.out.println("Processing DIDL-File " + urlStr);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        int count = 0;

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(file);
            NodeList elementsByTag = doc.getElementsByTagName("didl:Resource");

            for (int i = 0; i<elementsByTag.getLength(); i++) {
                Node tokens = elementsByTag.item(i);
                if (tokens.getNodeType() == Node.ELEMENT_NODE) {
                    Element token_element = (Element) tokens;
                    if (token_element.getAttribute("mimeType").equals("text/xml") && 
                            (token_element.getAttribute("ref").endsWith(":alto"))) {

                        URL url2 = new URL(token_element.getAttribute("ref"));
                        String altoFilename = token_element.getAttribute("dcx:filename");
                        if (altoFilename == null || altoFilename.isEmpty()) {
                            altoFilename = "alto-" + (count++) + ".xml";
                        }

                        AltoProcessor.handlePotentialAltoFile(url2,
                                                              "text/xml",
                                                              lang,
                                                              md5sum,
                                                              ResultHandlerFactory.createResultHandlers(context,
                                                                                                        altoFilename,
                                                                                                        md5sum,
                                                                                                        null));
                    }
                }
            }
            return (count > 0);
        } catch (javax.xml.parsers.ParserConfigurationException error) {
            error.printStackTrace();
            return false;
        } catch (org.xml.sax.SAXException error) {
            error.printStackTrace();
            return false;
        }
    }
}
