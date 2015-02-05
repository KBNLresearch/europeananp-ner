package nl.kbresearch.europeana_newspapers.NerAnnotator;

import nl.kbresearch.europeana_newspapers.NerAnnotator.container.*;
import nl.kbresearch.europeana_newspapers.NerAnnotator.output.ResultHandlerFactory;

import org.apache.commons.cli.*;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.File;

import org.apache.commons.io.FileUtils;

import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;

/**
 * Command line interface of application
 * 
 * @author rene
 * @author Willem Jan Faber
 */

public class App {
    static Map<String, Future<Boolean>> results = new LinkedHashMap<String, Future<Boolean>>();
    static File outputDirectoryRoot;
    static String[] outputFormats;

    /**
     * @return root for the output files
     */
    public static File getOutputDirectoryRoot() {
        return outputDirectoryRoot;
    }

    /**
     * @return list of output formats to be generated
     */
    public static String[] getOutputFormats() {
        return outputFormats;
    }


    /**
     * @return md5sum of file given path
     */
    private static String getMD5sum(String path) {
        String md5sum = "";
        File jarFile = new File(path);
        try {
            byte[] artifactBytes = FileUtils.readFileToByteArray(jarFile);
            md5sum = DigestUtils.md5Hex(artifactBytes);
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return md5sum;
    }

    private static void usage() {
    	System.err.println("usage: java -jar NerAnnotator.jar [OPTIONS] [INPUTFILES..]");
		System.err.println(" -c,--container <FORMAT>             Input type: mets (Default), didl,");
		System.err.println("                                     alto, text, html");
		System.err.println(" -d,--output-directory <DIRECTORY>   output DIRECTORY for result files.");
		System.err.println("                                     Default ./output");
		System.err.println(" -f,--export <FORMAT>                Output type: log (Default), csv,");
		System.err.println("                                     html, db, alto, alto2_1, bio.");
		System.err.println("                                     Multiple formats:' -f html -f csv'");
		System.err.println(" -l,--language <ISO-CODE>            use two-letter ISO-CODE for language");
		System.err.println("                                     selection: en, de, nl ....");
		System.err.println(" -m,--models <language=filename>     models for languages. Ex. -m");
		System.err.println("                                     de=/path/to/file/model_de.gz -m");
		System.err.println("                                     nl=/path/to/file/model_nl.gz");
		System.err.println(" -n,--nthreads <THREADS>             maximum number of threads to be used");
		System.err.println("                                     for processing. Default 8");
		System.err.println(" --                                  no more options after this");
        System.err.println("\nIf there are no input files specified, a list of file names is read from stdin.");
        System.exit(1);
    }
    
    /**
     * @param args
     * @throws ClassCastException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void main(final String[] args) throws ClassCastException, ClassNotFoundException, InterruptedException {
    	LinkedList<String> optContainer = new LinkedList<String>();
    	LinkedList<String> optOutputDir = new LinkedList<String>();
    	LinkedList<String> optExport = new LinkedList<String>();
    	LinkedList<String> optLanguage = new LinkedList<String>();
    	LinkedList<String> optModels = new LinkedList<String>();
    	LinkedList<String> optThreads = new LinkedList<String>();
    	LinkedList<String> optFiles = new LinkedList<String>();
    	    	
    	boolean moreOptions = true;
    	for (int n=0; n<args.length; n++) {
    		if (args[n].equals("--")) {
    			moreOptions = false;
    		}
    		else if (moreOptions && (args[n].equals("-c") || args[n].equals("--container"))) {
    			if ((n+1)<args.length) {
    				n++;
    				optContainer.addLast(args[n]);
    			}
    			else {
    				usage();
    			}
    		}
    		else if (moreOptions && (args[n].equals("-d") || args[n].equals("--output-directory"))) {
    			if ((n+1)<args.length) {
    				n++;
    				optOutputDir.addLast(args[n]);
    			}
    			else {
    				usage();
    			}
    		}
    		else if (moreOptions && (args[n].equals("-f") || args[n].equals("--export"))) {
    			if ((n+1)<args.length) {
    				n++;
    				optExport.addLast(args[n]);
    			}
    			else {
    				usage();
    			}
    		}    		
    		else if (moreOptions && (args[n].equals("-l") || args[n].equals("--language"))) {
    			if ((n+1)<args.length) {
    				n++;
    				optLanguage.addLast(args[n]);
    			}
    			else {
    				usage();
    			}
    		}    		
    		else if (moreOptions && (args[n].equals("-m") || args[n].equals("--models"))) {
    			if ((n+1)<args.length) {
    				n++;
    				optModels.addLast(args[n]);
    			}
    			else {
    				usage();
    			}
    		}    		
    		else if (moreOptions && (args[n].equals("-n") || args[n].equals("--nthreads"))) {
    			if ((n+1)<args.length) {
    				n++;
    				optThreads.addLast(args[n]);
    			}
    			else {
    				usage();
    			}
    		}
    		else if (moreOptions && (args[n].startsWith("-"))) {
    			System.err.println("unknown option "+args[n]);
    			usage();
    		}
    		else {
    			optFiles.addLast(args[n]);
    		}
    	}
    	

        try {
            ContainerProcessor processor = MetsProcessor.INSTANCE;

            outputFormats = new String[] { "http" };
            if (optExport.size()>0) {
                outputFormats = optExport.toArray(new String[0]);
            }

            Locale lang = Locale.ENGLISH;
            if (optLanguage.size()>0) {
            	lang = new Locale(optLanguage.getLast());
            }

            int maxThreads = 8;
            if (optThreads.size()>0) {
            	maxThreads = Integer.parseInt(optThreads.getLast());
            }

            if (optContainer.size()>0) {
                System.out.println("Container format: " + optContainer.getLast());
                if (optContainer.getLast().equals("didl")) {
                    processor = DIDLProcessor.INSTANCE;
                } else if (optContainer.getLast().equals("alto")) {
                    processor = AltoLocalProcessor.INSTANCE;
                } else if (optContainer.getLast().equals("mets")) {
                    processor = MetsProcessor.INSTANCE;
                } else if (optContainer.getLast().equals("text")) {
                    processor = TextProcessor.INSTANCE;
                } else if (optContainer.getLast().equals("html")) {
                    processor = HtmlProcessor.INSTANCE;
                } else {
                    throw new ParseException("Could not identify container format: " + optContainer.getLast());
                }
            }

            if (optModels.size()==0) {
                throw new ParseException("No language models defined!");
            }

            String classifierFileName = "";
            for (String opt: optModels) {
            	if (!opt.contains("=")) {
            		usage();
            	}
                classifierFileName = opt.substring(opt.indexOf('=')+1);
            }

            NERClassifiers.setLanguageModels(optModels);

            String outputDirectory = null;
            if (optOutputDir.size()>0) {
            	outputDirectory = optOutputDir.getLast();
            }
            else {
                outputDirectory = "." + File.separator + "output";
            }

            outputDirectoryRoot = new File(outputDirectory);

            // all others should be files
            List<String> fileList = optFiles;
            
            if (fileList.isEmpty()) {
                System.out.println("No file specified, read file list from stdin");
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")));                      
                    String input;                    
                    while ((input = br.readLine()) != null) {
                        fileList.add(input);
                    }
                } catch (IOException io){
                    io.printStackTrace();
                }
            }

            BlockingQueue<Runnable> containerHandlePool = new LinkedBlockingQueue<Runnable>();

            long startTime = System.currentTimeMillis();

            // Load classifier
            NERClassifiers.getCRFClassifierForLanguage(lang);

            // Fetch the version from maven settings
            String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            // Generate an md5sum from the current running jar
            String versionString = "Version: " + App.class.getPackage().getSpecificationVersion() + " md5sum: " + getMD5sum(path);
            // Generate an md5sum for the used classifier 
            versionString += "\nClassifier: " + classifierFileName + " md5sum: " + getMD5sum(classifierFileName);

            // Create the needed threads
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(Math.min(2, maxThreads), maxThreads, 1000, TimeUnit.MILLISECONDS, containerHandlePool);

            for (String arg : fileList) {
                System.out.println(arg);
                // Fire up the created threads
                results.put(arg, threadPoolExecutor.submit(new ContainerHandleThread(arg, lang, processor, versionString)));
            }
           
            // Shutdown and wait for threads to end
            threadPoolExecutor.shutdown();
            threadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            ResultHandlerFactory.shutdownResultHandlers();
           
            // Display stats to stdout
            System.out.println("Total processing time: " + (System.currentTimeMillis() - startTime)); 
            boolean errors = false;
            int successful = 0;
            int withErrors = 0;

            for (String key : results.keySet()) {
                try {
                    if (results.get(key).get()) {
                        successful += 1;
                    } else {
                        withErrors += 1;
                        errors = true;
                    }
                } catch (ExecutionException e) {
                    System.err.println("Error while parsing of container file: " + key + " . Cause: " + e.getCause().getMessage());
                    e.printStackTrace();
                    withErrors += 1;
                    errors = true;
                }
            }

            System.out.println(successful + " container documents successfully processed, " + withErrors + " with errors.");

            if (errors) {
                System.err.println("There were errors while processing.");
                System.exit(1);
            } else {
                System.out.println("Successful.");
                System.exit(0);
            }
        } 
        catch (org.apache.commons.cli.ParseException e) {
        	usage();
        }
    }

}
