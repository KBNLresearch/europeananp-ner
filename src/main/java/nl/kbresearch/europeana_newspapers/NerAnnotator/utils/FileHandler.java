package nl.kbresearch.europeana_newspapers.NerAnnotator.utils;

import javax.swing.*;
import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class FileHandler {

    private JFileChooser fileChooser = new JFileChooser("/mnt/Projecten/Impact/NER/NER_tools/");
    private JFrame frame;
	
	
	/*	A general method for opening a file chooser dialogue. Used 
	 * 	for opening (open == true) and saving (open == false) file.
	 */


    public File getFile(boolean open) {
        System.out.println("FileHandler.getFile. Open? " + open);
        File file = null;
        int returnVal;
        if (open) {
            returnVal = fileChooser.showOpenDialog(frame);
        } else {
            returnVal = fileChooser.showSaveDialog(frame);
        }
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
            if (open && !checkFile(file)) {
                file = null;
            }
        }
        return file;
    }


    public boolean checkFile(File file) {
        if (file.isFile()) {
            fileChooser.setCurrentDirectory(file.getParentFile());
            return true;
        } else {
            String message = "File Not Found: " + file.getAbsolutePath();
            displayError("File Not Found Error", message);
            return false;
        }
    }

    public void displayError(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title,
                JOptionPane.ERROR_MESSAGE);
    }
	
	
	/*	General method for writing a String to a file.
	 */

    public void printToFile(File file, String message) {
        System.out.println("Exporting data to file ...");
        PrintWriter pw = null;
        try {
            Writer fw = new FileWriter(file);
            pw = new PrintWriter(fw);
            pw.print(message);
        } catch (Exception e) {
            System.err.println("Exception: in printToFile " + file.getAbsolutePath());
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.flush();
                pw.close();
            }
        }
        System.out.println("Done exporting data to file.");
    }


    public static String readFile(String filepath){
        return readFile(new File(filepath));
    }
	
	/* 	Puts content of file in string. Called from the extractor class
	 */

    public static String readFile(File file){

        String lineSep = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();
        try {
            FileReader doc = new FileReader(file.getAbsolutePath());
            BufferedReader buff = new BufferedReader(doc);
            boolean eof = false;
            while (!eof) {
                String line = buff.readLine();
                if (line == null) {
                    eof = true;
                } else {
                    sb.append(line);
                    sb.append(lineSep);
                }
            }
            buff.close();
        } catch (IOException e) {
            System.err.println("Woops. Error reading file. " + e.toString());
        }
        return sb.toString();
    }

    public static void writeNEsToFile(String file, TreeMap<String, Integer>NEListHM) {

        FileOutputStream out;
        PrintStream p;

        try {
            out = new FileOutputStream(file);
            p = new PrintStream(out);
            Iterator it = NEListHM.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                p.println(pairs.getKey() + " " + pairs.getValue());
            }
            p.close();
            System.out.println("Written NE-list to file " + file);
        } catch (Exception e) {
            System.err.println("Error writing named entity list to file");
        }
    }
}
