package nl.kbresearch.europeana_newspapers.NerAnnotator.utils;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NERTStringUtils {

    static String[][] xmlEscapeChars = { {"&", "&amp;"}, {"<", "&lt;"}, {">", "&gt;"}, {"\"", "&quot;"}, {"'", "&apos;"}};
    private static String replacementForEuroSign = "__EUR__";

    public static String removeEuroSign(String text) {
        String oldText = text;
        Pattern pa = Pattern.compile("Û");
        Matcher m = pa.matcher(text);
        text = m.replaceAll(replacementForEuroSign);
        if (!oldText.equals(text)) {
            System.err.println("Succesfully removed eurosigns...");
        }
        return text;
    }

    public static String putBackEuroSign(String text) {
        String oldText = text;
        Pattern pa = Pattern.compile(replacementForEuroSign);
        Matcher m = pa.matcher(text);
        text = m.replaceAll("Û");
        if (!oldText.equals(text)) {
            System.err.println("Succesfully put back eurosigns...");
        }
        return text;
    }







    public static String[] splitListEntry(String s, boolean end) {
		/*	end: 	s has format LeidenLOC
		 * 	!end: 	s has format LOCLeiden
		 */

        String[] tgAndWd = new String[2]; //[0] = tag, [1] = word
        Pattern pat = Pattern.compile("^(PER|LOC|ORG)(.+)");
        if(end){pat = Pattern.compile("(.+)(PER|LOC|ORG)$");}
        Matcher m = pat.matcher(s);
        if (m.find()) {
            if(!end){
                tgAndWd[0] = m.group(1);
                tgAndWd[1] = m.group(2);
            }
            if(end){
                tgAndWd[0] = m.group(2);
                tgAndWd[1] = m.group(1);
            }
            //System.err.println(tg + " " + wd);
        }
        return tgAndWd;
    }


    public static String writeDataToString(JTable table){
		
		/*	Returns the entire content of the jtable as a string.
		 */

        StringBuffer sb = new StringBuffer();
        sb.append("");
        for (int j = 0; j < table.getRowCount(); j++){
            for (int k = 0; k < table.getColumnCount(); k++){
                if(k!=0){
                    sb.append("\t");
                }
                sb.append(table.getValueAt(j, k));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
	
	/*
	 * This method converts the key in the NETM from the matching factory, which
	 * has format 'LeidenLOC', 'Piet JansenPER' to 'LOC Leiden' and 'PER Piet
	 * Jansen'. It relies on the types consisting of 3 characters.
	 */

    public static String[] convertNETMformat(String k) {
        String[] key = new String[2];
        key[0] = k.substring(k.length() - 3, k.length());
        key[1] = k.substring(0, k.length() - 3);
        return key;
    }
	
	/*
	 * This method joins all B- and I-tags from the labeledtext String into
	 * single B-tags
	 */

    public static String joinBAndITags(String text) {

        System.err.println("Joining B- and I-tags");
		
		/*	Start with LOC, find first occurrence of B-LOC and first
		 * 	next occurrence of B-LOC, B-PER or B-ORG
		 */

        for(int i = 0; i < 3; i++){
            String thisNEType = "LOC";
            if(i==1){thisNEType = "ORG";}
            if(i==2){thisNEType = "PER";}
            System.err.println("loop "+ i+" thisNEType="+thisNEType);

            int startLoopFromIndex = 0;
            int[] endIndex = new int[3];

            while(startLoopFromIndex < text.length()){
                //System.err.println("text: "+text);
                //find the first occurrence of 'B-LOC'
                System.err.println("Looking for <B-"+thisNEType+"> from startFromLoopIndex="+startLoopFromIndex);
                int startPos = text.indexOf("<B-"+ thisNEType + ">", startLoopFromIndex);
                System.err.println("Found B-"+ thisNEType + " at startPos="+startPos);

                //if startPos == -1, this means that there are no more B- tags of this type in the
                //remaining text.
                if(startPos == -1){
                    startLoopFromIndex = text.length();
                }
                else{
                    //find the first next occurrence of 'B-LOC, B-ORG or B-PER'
                    endIndex[0] = text.indexOf("<B-LOC>", startPos+1);
                    endIndex[1] = text.indexOf("<B-ORG>", startPos+1);
                    endIndex[2] = text.indexOf("<B-PER>", startPos+1);
                    int minIndex = getMinIndex(endIndex);

                    int endPos = text.length();
                    if( (minIndex >= 0) && (endIndex[minIndex] >= 0) ){endPos = endIndex[minIndex];}

                    //endIndex[minIndex] could be -1, if no next B-tag is found.
				
					/*
					if(endPos == text.length()){
						//this is our last loop through the text for this tag
						System.err.println("Found no next occurrence of B-LOC/ORG/PER ");
						startLoopFromIndex = text.length();
					}
					*/
                    //else{
                    System.err.println("Found next occurrence of B-LOC/ORG/PER at "+endPos);
                    //within the limits of startPos - endPos, look for the last indexes of both </B-LOC>
                    //and </I-LOC> and see which one is the largest
                    int posBtag = text.substring(0, endPos).lastIndexOf("</B-" + thisNEType +">");
                    int posItag = text.substring(0, endPos).lastIndexOf("</I-" + thisNEType +">");
                    System.err.println("Found last end B-tag at "+ posBtag);
                    System.err.println("Found last end I-tag at "+ posItag);
                    if(posBtag > posItag){
                        //this NE only has B-tags, so we only need to change the end tag
                        //from <B-LOC> and </B-LOC> to <LOC> and </LOC>
                        text = text.replaceFirst("</B-" + thisNEType + ">", "</" + thisNEType + ">");
                        System.err.println("Replaced /B- with /");
                        //System.err.println("new text: "+text);
                        startLoopFromIndex = posBtag + 1;	//this is our new starting position.
                        //NB we shortened the text with two chars, so 'posBtag + 1' is
                        //actually not really necessary
                        System.err.println("New startloopfromindex = "+startLoopFromIndex);
                    }
                    else{
                        //this NE consists of both B- and I-tags, e.g. <B-LOC>St.</B-LOC><I-LOC>Kitts</I-LOC><I-LOC>Islands</I-LOC>

                        //change the last I-tag
                        text = removeCharAt(text, posItag+2);	//this turns </I-LOC> into </-LOC>
                        text = removeCharAt(text, posItag+2);	//this turns </-LOC> into </LOC>
                        //System.err.println("Replaced final I-Tag. New text: " + text);
                        startLoopFromIndex = posItag + 1;	//this is our new starting position.
                        //NB we shortened the text with two chars, so 'posItag + 1' is
                        //actually not really necessary
                        System.err.println("New startloopfromindex = "+startLoopFromIndex);

                        //now we iterate over this for all of the text and all three tags,
                        //and then we end by removing all <I-> and </I-> tags, and
                        //by converting all <B-> tags into <> tags.
                    }
                    //}
                }
            }
        }
        //Now we change all remaining B-LOC/ORG/PER tags into LOC/ORG/PER,
        //and we remove all I- and /I-tags.
        text = text.replaceAll("<B-LOC>", "<LOC>");
        text = text.replaceAll("<B-ORG>", "<ORG>");
        text = text.replaceAll("<B-PER>", "<PER>");

        text = text.replaceAll("<I-LOC>", "");
        text = text.replaceAll("<I-ORG>", "");
        text = text.replaceAll("<I-PER>", "");

        text = text.replaceAll("</I-LOC>", "");
        text = text.replaceAll("</I-ORG>", "");
        text = text.replaceAll("</I-PER>", "");
        System.err.println("Done with converting B- and I-tags. New text: "+text);
        return text;
    }

    public static String removeCharAt(String s, int pos) {
        return s.substring(0,pos)+s.substring(pos+1);
    }


    // Find index of minimum value of array using loop
    // Do not consider the value -1
    // NB Arrays.sort() would probably be faster but we need the index of the
    // array
    public static int getMinIndex(int[] numbers) {
        int minValue = numbers[0];
        int returnValue = 0;
        for (int i = 1; i < numbers.length; i++) {
            if(minValue == -1){
                minValue = numbers[i];
                returnValue = i;
            }
            else if ((numbers[i] >= 0) && (numbers[i] < minValue)) {
                minValue = numbers[i];
                returnValue = i;
            }
        }
        if(minValue == -1){
            returnValue = -1;
        }
        // NB the minValue can be -1, if there is no tag found in the text
        return returnValue;
    }

	
	/*	This method returns the absolute difference in length of two strings
	 */

    public static int stringLengthDiff(String s1, String s2){
        return Math.abs( s1.length() - s2.length() );
    }
	
	
	/* 	This method checks two strings and returns true if
	 * 	they differ only one character.
	 */

    public static boolean diffIsInsertion(String s1, String s2, boolean initialInsertionsAllowed){
        boolean diffIsInsertion = false;

        String w1 = s1;
        String w2 = s2;

        //check which string is the longest
        if( s2.length() > s1.length() ){
            w1 = s2;
            w2 = s1;
        }
		
		/* 	take the longest string w1, take out each of all
		 * 	the characters step by step, and check 
		 * 	if, with this char taken out, w1 == w2 
		 */

        String w1Temp;
		
		/*	If no initialInsertionsAllowed == false, 
		 * 	we start at the second character of the longest string,
		 * 	because we do not allow insertions at the beginning of the 
		 * 	string, e.g. Bakker <> Akker.
		 * 
		 * 	TODO: doesn't work both ways yet: Arends <> Barends
		 */
        int i = 0;
        if(!initialInsertionsAllowed){i = 0;}

        while( i < w1.length() && !diffIsInsertion){
            w1Temp = w1.substring(0,i) + w1.substring(i+1, w1.length());
            if(w1Temp.equals(w2)){
                System.err.println("\tMATCH: "+w1Temp+"<>"+w2);
                diffIsInsertion = true;
            }
            else{
                i++;
            }
        }
        return diffIsInsertion;
    }

    public static String normalizeString(String s){
        String[][] rewriterules = { {"void"} };
        s = normalizeString(s, rewriterules);
        return s;
    }


    public static String normalizeString(String s, String[][] rewriterules){

        //entries are normalized in two steps:
        //1) general normalization: removal of whitespace, diacritics, uppercase,
        //words between brackets, hyphens

        //System.err.println("WORD IN: >"+s+"<");
        //for now: St.>Sint
        if(!rewriterules[0][0].equals("void")){
            for(int i = 0; i < rewriterules.length; i++){
                s = s.replace(rewriterules[i][0], rewriterules[i][1]);
            }
        }

        //ignore words between brackets (), e.g. Ootmarssum (stad)
        //NB this implies that no trivial information should be put behind
        //brackets!
        Pattern pa = Pattern.compile("(.*)\\s*\\(.*\\)");
        Matcher m = pa.matcher(s);
        if(m.matches()){s = m.group(1);}

        //System.err.println("WORD OUT: >"+s+"<");


        s = s.toLowerCase();								/* lowercase */
        s = s.replaceAll("-", "");							/* dashes */
        s = s.replaceAll(" ", "");							/* whitespace */

        //Normalizer doesn't work for 1.6..
        //s = Normalizer.normalize(s, Normalizer.Form.NFD);	/* accents and stuff (, , ...)  */

        return s.replaceAll("[^\\p{ASCII}]", "");
    }

    public static String setProperString(String key){
	
		
		/* 	This method takes an entry from the NEfile as input,
		 * 	checks the entry for comma's, shuffles word order if
		 * 	necessary, and returns the proper key.
		 * 
		 * 	e.g. 	Bommel, Marc van > Marc van Bommel
		 * 			! Benthem van, Evert > Evert van Benthem
		 * 			Brecht in de, Hans > Hans in de Brecht
		 * 			!Hoen van de Wijer, Keessie > Keessie Hoen van de Wijer
		 * 
		 * 	step 1: look for comma
		 * 	step 2: if comma, take part before comma and search for last word with capital first character
		 * 	step 3: put all words after this word at the beginning of the string
		 * 	step 4: put whole part after the comma in front of the part before the comma
		 * 
		 */

        //NB only works with ONE comma so far...
        //and only works with 'proper' capitalization... (not with DALEN VAN, JAN)

        //System.err.println("WOORD IN: " + key);
        String keyOut = key;
        if(key.indexOf(", ")>=0){
            String[] entryParts = key.split(",");
            //look for last word that start with a capital letter.
            String[] firstPart = entryParts[0].split("\\s+");
            int thisWordIsCapitalized = 0;
            for(int i = 0 ; i < firstPart.length; i++){
                if(Character.isUpperCase(firstPart[i].charAt(0))){
                    thisWordIsCapitalized = i;
                    //System.err.println("This word is capitalized: "+i+" "+firstPart[i]);
                }
            }
            if(thisWordIsCapitalized < (firstPart.length-1)){
                String key2 = "";
                for(int i = (thisWordIsCapitalized+1); i < firstPart.length; i++){
                    if(i==(thisWordIsCapitalized+1)){key2 += firstPart[i];}
                    else{key2 += " " + firstPart[i];}
                    //System.err.println("\t>"+key2+"<");
                }
                //System.err.println("\tDONE with part 1: "+key2);
                for(int i = 0; i <= thisWordIsCapitalized; i++){
                    key2 += " " + firstPart[i];
                    //System.err.println("\t>"+key2+"<");
                }
                keyOut = entryParts[1] + " " + key2;
                //System.err.println("\tDONE WITH PART 2"+keyOut);
            }
            else{
                keyOut = entryParts[1] + " " + entryParts[0];
                //System.err.println("\t(2)"+keyOut);

            }
        }
        //delete any leading or trailing whitespace, just in case
        keyOut = keyOut.trim();
        //System.err.println("WOORD UIT: >" + keyOut+"<");
        return keyOut;
    }
	

	/* taken from Stanford / utils.Stringutils */

    public static int longestCommonSubstring(String s, String t) {
        int[][] d; // matrix
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        char s_i; // ith character of s
        char t_j; // jth character of t
        // int cost; // cost
        // Step 1
        n = s.length();
        m = t.length();
        if (n == 0) {
            return 0;
        }
        if (m == 0) {
            return 0;
        }
        d = new int[n + 1][m + 1];
        // Step 2
        for (i = 0; i <= n; i++) {
            d[i][0] = 0;
        }
        for (j = 0; j <= m; j++) {
            d[0][j] = 0;
        }
        // Step 3
        for (i = 1; i <= n; i++) {
            s_i = s.charAt(i - 1);
            // Step 4
            for (j = 1; j <= m; j++) {
                t_j = t.charAt(j - 1);
                // Step 5
                // js: if the chars match, you can get an extra point
                // otherwise you have to skip an insertion or deletion (no subs)
                if (s_i == t_j) {
                    d[i][j] = max(d[i - 1][j], d[i][j - 1], d[i - 1][j - 1] + 1);
                } else {
                    d[i][j] = Math.max(d[i - 1][j], d[i][j - 1]);
                }
            }
        }
        if (false) {
            // num chars needed to display longest num
            int numChars = (int) Math.ceil(Math.log(d[n][m]) / Math.log(10));
            for (i = 0; i < numChars + 3; i++) {
                //System.err.print(' ');
            }
            for (j = 0; j < m; j++) {
                //System.err.print(t.charAt(j) + " ");
            }
            //System.err.println();
            for (i = 0; i <= n; i++) {
                //System.err.print((i == 0 ? ' ' : s.charAt(i - 1)) + " ");
                for (j = 0; j <= m; j++) {
                    //System.err.print(d[i][j] + " ");
                }
                //System.err.println();
            }
        }
        // Step 7
        return d[n][m];
    }

    //taken from Stanford Math.sloppyMath
    /**
     * Returns the minimum of three int values.
     *
     * @return The minimum of three int values.
     */
    public static int max(int a, int b, int c) {
        int ma;
        ma = a;
        if (b > ma) {
            ma = b;
        }
        if (c > ma) {
            ma = c;
        }
        return ma;
    }

    public static boolean isCapitalized(String s){
        boolean yo = true;
        for(int i = 0; i < s.length(); i++){
            if(!Character.isUpperCase(s.charAt(i))){
                yo = false;
            }
        }
        return yo;
    }


    public static String decapitalizeString(String s) {
        String s2 = "";
        for(int i = 0; i < s.length(); i++){
            s2 += Character.toLowerCase(s.charAt(i));
        }
        return s2;
    }

    public static String capitalizeInitial(String s) {
        String sNew = "";
        if(s.length() > 0){
            sNew += Character.toUpperCase(s.charAt(0));
        }
        if(s.length() > 1){
            sNew += s.substring(1);
        }
        return sNew;
        //return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static boolean hasCapitalizedInitial(String s){
        boolean yo = true;
        if(!Character.isUpperCase(s.charAt(0))){
            yo = false;
        }
        return yo;
    }
	
		/*	Replace special characters &, <, >, ", ' for xml-output */

    public static String checkForXML(String s){

        for(int i = 0 ; i < xmlEscapeChars.length; i++){
            s = s.replace(xmlEscapeChars[i][0], xmlEscapeChars[i][1]);
        }
        return s;
    }
		
		/*
		 *	Remove <NE>-tags from running text: everything between
		 *	<> is removed. 
		 */

    public static String removeNETags(String s){
        s = s.replaceAll("<[^>]+>", "");
        return s;
    }
}
