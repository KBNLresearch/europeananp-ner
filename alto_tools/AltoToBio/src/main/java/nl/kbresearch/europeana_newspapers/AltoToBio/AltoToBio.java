package nl.kbresearch.europeana_newspapers.AltoToBio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class AltoToBio 
{
	static private int MIN_LINE_LENGTH = 20;
	static private int MIN_NR_OF_ANNOTATIONS = 2;
	static EntityReference entityReference = new EntityReference();
	
	private int lineLength;
	private int annotationsCount;
	
	class AltoString {
		String alternative;
		String content;
	}
	
	void processFile(String fname) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		LinkedList<AltoString> stringList = new LinkedList<AltoString>();
		
		File f = new File(fname);
		if (!f.exists()) {
			System.err.println("File "+fname+" could not be opened or does not exist");
		}
		else {
			System.err.println("Processing "+f.getCanonicalPath());
		}
		
		String altoStr;
		{
			FileInputStream is = new FileInputStream(f);
			byte[] ba = new byte[(int)f.length()];
			for (int i=0; i<ba.length; i++) {
			   is.read(ba, i, 1); // could be optimized
			}
			is.close();
			altoStr = new String(ba, "UTF-8");
		}
		
		/*
		 * Replace illegal html entity references from xml so it can be properly parsed
		 */
		altoStr = entityReference.normalize(altoStr);
		
		/*
		 * Strip BOM from start-of-file
		 */
		int offset = 0;
		while (offset<altoStr.length() && altoStr.charAt(offset)!='<') {
			offset++;
		}
        if (offset!=0) {
        	altoStr = altoStr.substring(offset);
        }
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
       	InputSource xx = new InputSource(new StringReader(altoStr));
        Document doc = db.parse((InputSource)xx);
        doc.getDocumentElement().normalize();
        
        if ("alto".equals(doc.getDocumentElement().getNodeName())) {
            NodeList strings = doc.getElementsByTagName("String"); // This is how it should be according to the alto schema
            if (strings.getLength()==0) {
            	strings = doc.getElementsByTagName("string"); // non-standard, for German and KB (output from INL tool???)
            }
            
            lineLength = 0;
            annotationsCount = 0;
            for (int n=0; n<strings.getLength(); n++) {
            	AltoString as = new AltoString();
            	Element e = (Element)strings.item(n);
            	String alternative = e.getAttribute("ALTERNATIVE");
            	String content = e.getAttribute("CONTENT"); // This is how it should be according to the alto schema
            	if (content==null || content.length()==0) {
            		content = e.getAttribute("content"); // non-standard, for German and KB altos (output from INL tool???)
            	}
            	if (content==null) {
            		as.content = "";
            	}
            	else {
            		content = content.trim();
            		as.content = content;
            		lineLength += content.length()+1;
            	}
            	if (alternative==null || alternative.length()==0) {
            		as.alternative = "O";
            	}
            	else if ("B-NOT KNOWN".equals(alternative.toUpperCase())) {
            		as.alternative = "O";
            	}
            	else if ("B-MISC".equals(alternative.toUpperCase())) {
            		as.alternative = "O";
            	}
            	else {
            		as.alternative = alternative;
            		annotationsCount++;
            	}
            	stringList.addLast(as);
            	if (content.endsWith(".") || content.endsWith("!") || content.endsWith("?")) {
            		if (lineLength >= (MIN_LINE_LENGTH + 1) &&  annotationsCount >= MIN_NR_OF_ANNOTATIONS) {
	            		for (AltoString tmp : stringList) {
	            			System.out.write((tmp.content+" POS "+tmp.alternative+"\n").getBytes(Charset.forName("UTF-8")));
	            		}
            		}
            		annotationsCount = 0;
            		lineLength = 0;
            		stringList.clear();
            	}
            }        	
        }
	}
	
	private static void usage() {
		System.err.println("Usage: ");
		System.err.println("  AltoToBio [-l <min_line_length>] [-a <min_nr_of_annotations>] [--] [list_of_alto_files]");
		System.exit(1);
	}
	
    public static void main( String[] args ) throws Exception
    {
    	AltoToBio x = new AltoToBio();
    	boolean checkNext = true;
    	if (args.length==0) {
    		usage();
    	}
    	for (int n=0; n<args.length; n++) {
    		String arg = args[n];
    		if (checkNext) {
    			if ("--".equals(arg)) {
    				checkNext = false;
    			}
    			else if ("-l".equals(arg)) {
    				n++;
    				if (n>=args.length) {
    					usage();
    				}
    				MIN_LINE_LENGTH = Integer.parseInt(args[n]);
    			}
    			else if ("-a".equals(arg)) {
    				n++;
    				if (n>=args.length) {
    					usage();
    				}
    				MIN_NR_OF_ANNOTATIONS = Integer.parseInt(args[n]);
    			}
    			else {
        			x.processFile(arg);    				
    			}
    		}
    		else {
    			x.processFile(arg);
    		}
    	}
    }
}
