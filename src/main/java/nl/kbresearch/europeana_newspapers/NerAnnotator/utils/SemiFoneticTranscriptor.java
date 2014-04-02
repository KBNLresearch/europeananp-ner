package nl.kbresearch.europeana_newspapers.NerAnnotator.utils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemiFoneticTranscriptor {

    TreeMap<String, String> ruleListTM = new TreeMap<String, String>();
    public String ruleArray[][];
    //we use default transcription rules that can be overwritten
    List<String> ruleList;
    List<String> defaultRules = Arrays.asList(new String[]{
            "\\s+jr\\.?|\\s+junior}\\s+sr\\.?|\\s+senior|\\s+zn|,\\s+\\w+z\\b=>",
            "\\s-\\s=>",
            "n\\b|'s\\b|`s\\b|’s\\b=>",
            "sch([^aeiouy])=>s$1",
            "sche?\\b=>s",
            "^'s|^`s|^’s=>",
            "tz=>z",
            "n$|s$|'s$=>",
            "st\\s*\\.|sint\\s*|santo\\s*|saint\\s*=>",
            "\\W=>",
            "(\\w)\\1=>$1",
            "eij|ij|ei|y|ey=>Y",
            "(u|a|o)e=>$1",
            "ch=>g",
            "ou|au=>u",
            "z=>s",
            "d\\b=>t",
            "ck=>k",
            "uw=>w",
            "ce=>se",
            "ci=>si",
            "ca=>ka",
            "co=>ko",
            "cu=>ku",
            "c=>k",
            "ph=>f",
            "th=>t",
            "j=>i"
    });

    public void initTranscriptor(){
        //System.err.println("Using default rules...");
        //called when no rule list is used: use default list.
        initTranscriptor(defaultRules);
    }

    public void initTranscriptor(List<String> li){
        ruleList = new LinkedList<String>(li);
        list2Array();
    }

    public void list2Array(){
        ruleArray = new String[ruleList.size()][2];
        String ruleShape = "=>";
        int counter = 0;
        for(String li : ruleList){
            if(!li.equals("")){
                int ind = li.indexOf(ruleShape);
                if(ind > -1){
                    String r1 = li.substring(0, ind);
                    String r2 = li.substring(ind+ruleShape.length(), li.length());
                    ruleListTM.put(r1, r2);
                    ruleArray[counter][0] = r1;
                    ruleArray[counter][1] = r2;
                    counter++;
                }
            }
        }
    }

    public String convertToSemiFoneticTranscription(String s){

        String regex = "";
        String replace = "";
        Pattern p;
        Matcher m;

        for(int i = 0; i < ruleArray.length; i++){
            regex = ruleArray[i][0];
            replace = ruleArray[i][1];
            if(regex != null && replace != null){
                p = Pattern.compile(regex);
                m = p.matcher(s);
                s = m.replaceAll(replace);
            }
        }
        return s;
    }


    public String convertToSemiFoneticTranscriptionOld(String s){
		/*	This method converts the (already normalized) string
		 * 	into a string with a semi-fonetic transcription,
		 * 	using language-specific rules for Dutch.
		 */
        //(1) double character > single character
        //System.err.println("woord: "+s);

        //String s2 = s;
        String REGEX = "(\\w)\\1";
        String REPLACE = "$1";
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(s);
        s = m.replaceAll(REPLACE);

        //System.err.println("dubbel > enkel - Nieuw woord: "+s);

        //(2) ij/ei/y/eij/ey > Y
        REGEX = "eij|ij|ei|y|ey";
        REPLACE = "Y";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("ij/ei/y/eij/ey > Y - Nieuw woord: "+s);

        //(3) ue/ae/oe > e
        REGEX = "(u|a|o)e";
        REPLACE = "$1";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("ue/ae/oe > e - Nieuw woord: "+s);

        //(4) ch > g
        REGEX = "ch";
        REPLACE = "g";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("ch > g - Nieuw woord: "+s);

        //(5) ou/au > U
        REGEX = "ou|au";
        REPLACE = "u";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("ou/au > U - Nieuw woord: "+s);

        //(6) z > s
        REGEX = "z";
        REPLACE = "s";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("z > s - Nieuw woord: "+s);

        //(7) -d > -t
        REGEX = "d$";
        REPLACE = "t";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("-d > -t - Nieuw woord: "+s);

        //(8) ck > k
        REGEX = "ck";
        REPLACE = "k";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("ck > k - Nieuw woord: "+s);

        //(9) uw > w
        REGEX = "uw";
        REPLACE = "w";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("uw > w - Nieuw woord: "+s);

        //(10) ce > se
        REGEX = "ce";
        REPLACE = "se";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("ce > se - Nieuw woord: "+s);

        //(11) ci > si
        REGEX = "ci";
        REPLACE = "si";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("ci > si - Nieuw woord: "+s);

        //(12) ca > ka
        REGEX = "ca";
        REPLACE = "ka";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("ca > ka - Nieuw woord: "+s);

        //(13) co > ko
        REGEX = "co";
        REPLACE = "ko";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("co > ko - Nieuw woord: "+s);

        //(14) cu > ku
        REGEX = "cu";
        REPLACE = "ku";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("cu > ku - Nieuw woord: "+s);

        //(15) c > k
        REGEX = "c";
        REPLACE = "k";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("c > k - Nieuw woord: "+s);

        //(16) ph > f
        REGEX = "ph";
        REPLACE = "f";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);
        //System.err.println("c > k - Nieuw woord: "+s);

        //(17) th > t
        REGEX = "th";
        REPLACE = "t";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);

        //(18) j > i
        REGEX = "j";
        REPLACE = "i";
        p = Pattern.compile(REGEX);
        m = p.matcher(s);
        s = m.replaceAll(REPLACE);

        //if(!s2.equals(s)){System.err.println("oud: "+s2+". Nieuw: "+s);}

        return s;
    }



}
