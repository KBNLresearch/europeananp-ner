package nl.kbresearch.europeana_newspapers.NerAnnotator.container;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import nl.kbresearch.europeana_newspapers.NerAnnotator.alto.AltoProcessor;
import nl.kbresearch.europeana_newspapers.NerAnnotator.output.ResultHandlerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;


/**
 * Processor for MPEG21-DIDL files. This parser is tested with the KB DDD
 * Collection.
 * 
 * @author Rene
 * @author Willem Jan Faber
 * 
 */
public class DIDLProcessor implements ContainerProcessor {

	/**
	 * the default instance of the MPEG21-DIDL processor
	 */
	public static DIDLProcessor INSTANCE = new DIDLProcessor();

	@Override
	public boolean processFile(ContainerContext context, String urlStr, Locale lang) throws IOException {
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
                            Element eElement = (Element) tokens;
                            if (eElement.getAttribute("mimeType").equals("text/xml")) {
                                URL url2 = new URL(eElement.getAttribute("ref"));
                                String altoFilename = eElement.getAttribute("dcx:filename");
                                if (altoFilename == null || altoFilename.isEmpty()) {
                                    altoFilename = "alto-" + (count++) + ".xml";
                                }
                                AltoProcessor.handlePotentialAltoFile(url2, eElement .getAttribute("mimetype"), lang, ResultHandlerFactory.createResultHandlers(context, altoFilename));
                            }
                        }
                    }
                } catch (javax.xml.parsers.ParserConfigurationException e) { 
                    e.printStackTrace(); 
                } catch (org.xml.sax.SAXException e) { 
                    e.printStackTrace(); 
                }
		return (count > 0);
	}
}
