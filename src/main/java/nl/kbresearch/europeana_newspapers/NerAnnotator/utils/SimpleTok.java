package nl.kbresearch.europeana_newspapers.NerAnnotator.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleTok {

	/*
	 * input: a string with words output: array of words, tokenized on the basis
	 * of whitespace TODO: do a better deal with this tok.
	 */

    static Pattern nonwordchars = Pattern.compile(",|!|\\.|:|;|\\?|\"|'"); // add
    // whitespace
    // before
    // these
    // chars
    static Pattern endOfLine = Pattern.compile("\\r?\\n");

    public final static HashMap<String, String> nonwordExceptionsDutch = new HashMap<String, String>();
    static {
        nonwordExceptionsDutch.put("Dr .", "Dr.");
        nonwordExceptionsDutch.put("' t", "'t");
        nonwordExceptionsDutch.put("Mr .", "Mr.");
    }

    static Pattern regexNE = Pattern.compile("<NE_([^>]+)>(.*)</NE>\\s*$");



    public static ArrayList<String> simpletokWithNETags(String s,
                                                        boolean addTags) {
		
		/*
		 * This method tokenizes a string with NE-tags. It returns an arraylist with
		 * words and their bio-tags: "Jan POS B-PER"
		 */


        ArrayList<String> biolist = new ArrayList<String>();

        // we check the string in pieces, until we come across a
        // <>-tag
        int onset = 0;
        int offset = s.indexOf("<NE", onset);
        // System.err.println("found NE-tag: onset="+onset+" offset="+offset);
        boolean done = false;
        while (!done) {
            // System.err.println("beginning of while-loop. onset="+onset+" offset="+offset);
            if (offset == -1) {
                offset = s.length();
                done = true;
            }
            String sTemp = s.substring(onset, offset);
            // System.err.println("sTemp="
            // +sTemp+" onset="+onset+" offset="+offset);
            // no tags in sTemp:
            ArrayList<String> aa = simpletok(sTemp, false);
            for (String a : aa) {
                if (!a.equals("")) {
                    if (addTags) {
                        biolist.add(a + " POS O");
                    } else {
                        biolist.add(a);
                    }
                }
                if (a.equals(".") || a.equals("!") || a.equals("?")) {
                    biolist.add("");
                } // we add a emptyline after a sentence.
            }
            aa.clear();
            if (offset != s.length()) {
                // offset marks the beginning of a NE-tag
                onset = offset;
                offset = s.indexOf(">", onset);
                String tag = s.substring(onset, offset);
                // System.err.println("Tracking NE. tag="+tag+" onset="+onset+" offset="+offset);
                tag = tag.replace("<NE_", "");
                tag = tag.replace(">", "");
                onset = offset + 1;
                offset = s.indexOf("</NE>", onset);
                // the new chunk onset-offset holds the entire NE, which we
                // first tokenize
                String ne = s.substring(onset, offset);
                // System.err.println("Found NE: "+ne+" onset="+onset+" offset="+offset);
                aa = simpletok(ne, false);
                int neWordCounter = 0;
                for (String a : aa) {
                    if (neWordCounter == 0) {
                        if (addTags) {
                            biolist.add(a + " POS B-" + tag);
                        } else {
                            biolist.add(a);
                        }
                    } else {
                        if (addTags) {
                            biolist.add(a + " POS I-" + tag);
                        } else {
                            biolist.add(a);
                        }
                    }
                    neWordCounter++;
                }
                onset = offset + 5;
                offset = s.indexOf("<NE", onset);
                // System.err.println("End of loop. onset="+onset+" offset="+offset);
            }
        }
		/*
		 * for(String e : biolist){ System.err.println("WORD: >"+e+"<"); }
		 */
        // System.err.println("DONE WITH SIMPLETOK");
        return biolist;
    }

	/*
	 * This method tokenizes a string. It cannot handle <>-tags. It returns the
	 * found words in an arraylist.
	 */

    public static ArrayList<String> simpletok(String s, boolean includeEOL) {

        //System.err.print("Tokenizing ...");
        ArrayList<String> wordlist = new ArrayList<String>();

        s = addWhitespaces3(s);
        // s = replaceNewLines(s);
        s = replaceExceptions(s);

        // find next whitespace
        int offset = s.indexOf(" ");
        // in case we only have 1 word: return this
        if (offset == -1) {
            if (s.length() > 0) {
                wordlist.add(s);
            }
        }
        while (offset > -1) {
            String w = s.substring(0, offset);
            // offset = checkForNonWordCharacters(w, offset);
            // w = s.substring(0, offset);

            // w = w.replaceAll("\\r?\\n", ""); //remove newlines from the words

            // System.err.println("Potential word w: >>"+w+"<<");

            // if w contains an end of line marking, split it
            Matcher matcher = endOfLine.matcher(w);
            if (matcher.find()) {
                // /System.err.println("endofline");
                // split w in parts
                ArrayList<String> list = splitWord(w, includeEOL);

                for (String a : list) {
                    wordlist.add(a);
                    // System.err.println("split word in >"+a+"<");
                }

            } else {
                wordlist.add(w);
                // System.err.println("Added w: >>"+w+"<< to wordlist");
            }
            s = s.substring(offset + 1);
            offset = s.indexOf(" ");

            if (offset == -1) {
                // end of string; add this word too
                w = s.substring(0, s.length());
                // offset = checkForNonWordCharacters(w, offset);
                // w = s.substring(0, offset);

                // w = w.replaceAll("\\r?\\n", "");
                // if w contains an end of line marking, split it
                matcher = endOfLine.matcher(w);
                if (matcher.find()) {
                    // /System.err.println("endofline");
                    // split w in parts
                    ArrayList<String> list = splitWord(w, includeEOL);

                    for (String a : list) {
                        wordlist.add(a);
                        // System.err.println("split word in >"+a+"<");
                    }

                } else {
                    wordlist.add(w);
                    // System.err.println("Added w: >>"+w+"<< to wordlist");
                }
                // wordlist.add(w);
                // System.err.println("Added w: >>"+w+"<< to wordlist");
            }
        }
		/*
		 * for(String e : wordlist){ System.err.println("WORD: >"+e+"<"); }
		 */
        //System.err.println(" done.");
        return wordlist;
    }

	/*
	 * Method for words that contain an end-of-line character. We split these
	 * words, preserving the end-of-line.
	 * 
	 * Note: words that are cut off at the end of a line, e.g. 'hun- gry', are
	 * stored as 'hun-gry', preceded by a newline, which alters the output
	 * slightly.
	 */

    public static ArrayList<String> splitWord(String w, boolean includeEOL) {
        // System.err.println("checking word >"+w+"< for splitting");
        ArrayList<String> list = new ArrayList<String>();
        Matcher matcher = endOfLine.matcher(w);

        if (matcher.find()) {
            int onset = matcher.start();
            int offset = matcher.end();

            if ((w.length() > onset - 1)
                    && (onset > 0 && w.charAt(onset - 1) == '-')) {
                // System.err.println("if");
                // if( (w.length() > onset-1) && (w.charAt(onset-1) ) == '-'){
                // if(onset == 0){list.add(w.substring(offset));}
                // else{list.add(w.substring(0, onset) + w.substring(offset));}
                list.add(w.substring(0, onset) + w.substring(offset));
                if (includeEOL) {
                    list.add(w.substring(onset, offset));
                }
            } else {
                // System.err.println("else");
                // System.err.println("onset="+onset+" offset="+offset+" w.length="+w.length());
                list.add(w.substring(0, onset));
                if (includeEOL) {
                    list.add(w.substring(onset, offset));
                }
                String t = w.substring(offset);
                t = t.replaceAll("\\r?\\n", "");
                list.add(t);
            }
        }
		/*
		 * for(String a : list){ System.err.println("returning list: >"+a+"<");
		 * }
		 */
        return list;
    }

    public static String addWhitespaces3(String s){
		
		/*	A much faster and more elegant way, and seems
		 * 	to be working...
		 * 	Idea: add whitespace before any special character, if
		 * 	no whitespace is already there.
		 * 
		 */

        //System.err.print("Adding whitespaces3 ...");
        s = s.replaceAll("(\\S)([,|!|\\.|:|;|\\?|\"|'])", "$1 $2");
        //System.err.println(" done.");
        return s;
    }

    public static String addWhitespaces2(String s) {
		
		/*	Same as addWhitespaces(), but not using regex..
		 * 
		 */

        System.err.print("Adding whitespaces2 ...");

        String[] diacritic = {",", "!", ".", ":", ";", "?", "\"", "'"};

        for(int i = 0; i < diacritic.length; i++){
            StringBuilder sb = new StringBuilder();
            int onset = 0;
            int index = s.indexOf(diacritic[i], onset);
            while(index != -1){
                //System.err.println("diacritic=" +diacritic[i]+" onset="+onset+" index="+index);
                sb.append(s.substring(onset, index));

                // if previous character is not a whitespace, add a whitespace
                if (sb.length() >= 1) {
                    if (sb.toString().charAt(sb.length() - 1) != ' ') {
                        sb.append(" ");
                    }
                    sb.append(diacritic[i]);
                    onset = index+diacritic[i].length();
                }
                else{
                    sb.append(diacritic[i]);
                    onset = index+diacritic[i].length();
                }

                index = s.indexOf(diacritic[i], onset);
            }

            sb.append(s.substring(onset));
            s = sb.toString();
        }

        return s;
    }

	/*
		int index = s.indexOf(",");
		s.indexOf("!")
		int index3 = s.indexOf(".");
		int index4 = s.indexOf(":");
		int index5 = s.indexOf(";");
		int index6 = s.indexOf("?");
		int index7 = s.indexOf("\"");
		int index8 = s.indexOf("'");
*/


    public static String addWhitespaces(String s) {
        // look for a combination of a word-character and a non-word character

        // s=
        // "Russian, military convoy. Pas-sed through 'Russian' Mountains in Russian Ossetia,";
        // String regex="R\\w*";
        System.err.print("Adding whitespaces ...");

        StringBuilder sb = new StringBuilder();

        int onset = 0;

        // System.out.println("========================");
        Matcher matcher = nonwordchars.matcher(s);

        while (matcher.find()) {
            sb.append(s.substring(onset, matcher.start()));
            // if previous character is not a whitespace, add a whitespace
            if (sb.length() > 1) {
                if (sb.toString().charAt(sb.length() - 1) != ' ') {
                    sb.append(" ");
                }
                sb.append(matcher.group());
                onset = matcher.end();
                // text = matcher.replaceFirst(" " + matcher.group());
                // matcher.group() = the found non-word character
                // matcher.start() = the onset of the character
                // matcher.end() = the offset of the character
            }
        }
        sb.append(s.substring(onset));
		/*
		 * if (matcher.matches()) { System.out.println("m1.matches() start = " +
		 * matcher.start() + " end = " + matcher.end()); }
		 */
        // System.out.println("TEXT NIEUW: "+newText);
        // System.out.println("========================");
        System.err.println(" done.");
        return sb.toString();
    }

	/*
	 * Replace newlines with a whitespace, except when the last character before
	 * the newline is a dash
	 */

    public static String replaceNewLines(String s) {

        System.err.print("Replacing newlines ...");
        String newText = "";
        int onset = 0;

        Matcher matcher = endOfLine.matcher(s);
        while (matcher.find()) {
            // System.err.println("Found >>"+matcher.group()+"<< at pos " +
            // matcher.start() +" to " +matcher.end());
            if (matcher.start() > 0) {
                // System.err.println("character before newline: "
                // +s.charAt(matcher.start()-1));
                newText += s.substring(onset, matcher.start());
                if (newText.charAt(newText.length() - 1) != '-') {
                    newText += " ";
                }
                newText += matcher.group();
                onset = matcher.end();
            }
        }
        newText += s.substring(onset);
        System.err.println(" done.");
        return newText;
    }

    public static String replaceExceptions(String s) {
        Set<Map.Entry<String, String>> set = nonwordExceptionsDutch.entrySet();
        for (Map.Entry<String, String> ent : set) {
            s = s.replaceAll(ent.getKey(), ent.getValue());
        }

        return s;
    }


}
