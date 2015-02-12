package nl.kbresearch.europeana_newspapers.AltoToBio;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityReference {
	Pattern getEntityReferences = Pattern.compile("&[^#].*?;");
	
	@SuppressWarnings("serial")
	HashMap<String, String> named = new HashMap<String, String>(){
		{
			put("&nbsp;","&#160;");
			put("&iexcl;","&#161;");
			put("&cent;","&#162;");
			put("&pound;","&#163;");
			put("&curren;","&#164;");
			put("&yen;","&#165;");
			put("&brvbar;","&#166;");
			put("&sect;","&#167;");
			put("&uml;","&#168;");
			put("&copy;","&#169;");
			put("&ordf;","&#170;");
			put("&laquo;","&#171;");
			put("&not;","&#172;");
			put("&shy;","&#173;");
			put("&reg;","&#174;");
			put("&macr;","&#175;");
			put("&deg;","&#176;");
			put("&plusmn;","&#177;");
			put("&sup2;","&#178;");
			put("&sup3;","&#179;");
			put("&acute;","&#180;");
			put("&micro;","&#181;");
			put("&para;","&#182;");
			put("&middot;","&#183;");
			put("&cedil;","&#184;");
			put("&sup1;","&#185;");
			put("&ordm;","&#186;");
			put("&raquo;","&#187;");
			put("&frac14;","&#188;");
			put("&frac12;","&#189;");
			put("&frac34;","&#190;");
			put("&iquest;","&#191;");
			put("&Agrave;","&#192;");
			put("&Aacute;","&#193;");
			put("&Acirc;","&#194;");
			put("&Atilde;","&#195;");
			put("&Auml;","&#196;");
			put("&Aring;","&#197;");
			put("&AElig;","&#198;");
			put("&Ccedil;","&#199;");
			put("&Egrave;","&#200;");
			put("&Eacute;","&#201;");
			put("&Ecirc;","&#202;");
			put("&Euml;","&#203;");
			put("&Igrave;","&#204;");
			put("&Iacute;","&#205;");
			put("&Icirc;","&#206;");
			put("&Iuml;","&#207;");
			put("&ETH;","&#208;");
			put("&Ntilde;","&#209;");
			put("&Ograve;","&#210;");
			put("&Oacute;","&#211;");
			put("&Ocirc;","&#212;");
			put("&Otilde;","&#213;");
			put("&Ouml;","&#214;");
			put("&times;","&#215;");
			put("&Oslash;","&#216;");
			put("&Ugrave;","&#217;");
			put("&Uacute;","&#218;");
			put("&Ucirc;","&#219;");
			put("&Uuml;","&#220;");
			put("&Yacute;","&#221;");
			put("&THORN;","&#222;");
			put("&szlig;","&#223;");
			put("&agrave;","&#224;");
			put("&aacute;","&#225;");
			put("&acirc;","&#226;");
			put("&atilde;","&#227;");
			put("&auml;","&#228;");
			put("&aring;","&#229;");
			put("&aelig;","&#230;");
			put("&ccedil;","&#231;");
			put("&egrave;","&#232;");
			put("&eacute;","&#233;");
			put("&ecirc;","&#234;");
			put("&euml;","&#235;");
			put("&igrave;","&#236;");
			put("&iacute;","&#237;");
			put("&icirc;","&#238;");
			put("&iuml;","&#239;");
			put("&eth;","&#240;");
			put("&ntilde;","&#241;");
			put("&ograve;","&#242;");
			put("&oacute;","&#243;");
			put("&ocirc;","&#244;");
			put("&otilde;","&#245;");
			put("&ouml;","&#246;");
			put("&divide;","&#247;");
			put("&oslash;","&#248;");
			put("&ugrave;","&#249;");
			put("&uacute;","&#250;");
			put("&ucirc;","&#251;");
			put("&uuml;","&#252;");
			put("&yacute;","&#253;");
			put("&thorn;","&#254;");
			put("&yuml;","&#255;");
		}
	};
	
	EntityReference() {}
	
	private String replace(String str, String entity) {
		System.err.print("found illegal html entity reference '"+entity+"' in xml file, ");
		if (named.containsKey(entity)) {
			System.err.println("replacing with '"+named.get(entity)+"'");
			return str.replace(entity, named.get(entity));			
		}
		else {
			System.err.println("unable to find proper replacement");
			return str;
		}
	}
	
	public String normalize(String str) {
		Matcher m = getEntityReferences.matcher(str);
		
		TreeSet<String> entities = new TreeSet<String>();
		
		while (m.find()) {
			// store all found named entities in a TreeSet to make
			// to create an unique list
			String entity = str.substring(m.start(), m.end());
			if (!"&lt;".equals(entity) && !"&gt;".equals(entity) && !"&amp;".equals(entity) && !"&quot;".equals(entity) && !"&apos;".equals(entity)) {
				entities.add(str.substring(m.start(), m.end()));
			}
		}
		
		if (entities.size()>0) {
			for (String entity : entities) {
				str = replace(str, entity);
			}
		}
		return str;
	}
}
