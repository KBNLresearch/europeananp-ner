package nl.kb.europeananewspaper.NerAnnotater;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.kb.europeananewspaper.NerAnnotater.container.ContainerHandleThread;
import nl.kb.europeananewspaper.NerAnnotater.container.ContainerProcessor;
import nl.kb.europeananewspaper.NerAnnotater.container.DIDLProcessor;
import nl.kb.europeananewspaper.NerAnnotater.container.MetsProcessor;
import nl.kb.europeananewspaper.NerAnnotater.output.ResultHandlerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * Command line interface of application
 * 
 * @author rene
 */
public class App {

	static Map<String, Future<Boolean>> results = new LinkedHashMap<String, Future<Boolean>>();
	static File outputDirectoryRoot;
	static String[] outputFormats;

	/**
	 * @return the root for the output files
	 */
	public static File getOutputDirectoryRoot() {
		return outputDirectoryRoot;
	}

	/**
	 * @return the list of output formats to be generated
	 */
	public static String[] getOutputFormats() {
		return outputFormats;
	}

	/**
	 * @param args
	 * @throws ClassCastException
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("static-access")
	public static void main(final String[] args) throws ClassCastException,
			ClassNotFoundException, InterruptedException {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption(OptionBuilder
				.withLongOpt("export")
				.withDescription(
						"use FORMAT for export: log (Default), csv, html, db, alto.\n Multiple formats:\" -f html -f csv\"")
				.hasArgs().withArgName("FORMAT").withType(String.class)
				.create("f"));

		options.addOption(OptionBuilder
				.withLongOpt("language")
				.withDescription(
						"use two-letter ISO-CODE for language selection: en, de, nl ....")
				.hasArg().withArgName("ISO-CODE").withType(String.class)
				.create("l"));

		options.addOption(OptionBuilder
				.withLongOpt("container")
				.withDescription(
						"which FORMAT are the container input files: mets (Default), didl")
				.hasArg().withArgName("FORMAT").withType(String.class)
				.create("c"));

		options.addOption(OptionBuilder
				.withLongOpt("nthreads")
				.withDescription(
						"maximum number of threads to be used for processing. Default 8 ")
				.hasArg().withArgName("THREADS").withType(Integer.class)
				.create("n"));

		options.addOption(OptionBuilder
				.withLongOpt("models")
				.withDescription(
						"models for languages. Ex. -m de=/path/to/file/model_de.gz -m nl=/path/to/file/model_nl.gz")
				.hasArgs().withArgName("language=filename")
				.withValueSeparator().create("m"));

		options.addOption(OptionBuilder
				.withLongOpt("output-directory")
				.withDescription(
						"output DIRECTORY for result files. Default ./output")
				.hasArg().withArgName("DIRECTORY").withType(String.class)
				.create("d"));

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			outputFormats = new String[] { "log" };

			String[] formats = line.getOptionValues("f");
			if (formats != null && formats.length > 0) {
				outputFormats = formats;

			}

			Locale lang = Locale.ENGLISH;

			if (line.getOptionValue("l") != null) {
				lang = new Locale(line.getOptionValue("l"));

			}

			int maxThreads = 8;
			if (line.getOptionValue("n") != null) {
				maxThreads = new Integer(line.getOptionValue("n"));

			}

			ContainerProcessor processor = MetsProcessor.INSTANCE;
			if (line.getOptionValue("c") != null) {
				System.out.println("Container format: "
						+ line.getOptionValue("c"));
				if (line.getOptionValue("c").equals("didl"))
					processor = DIDLProcessor.INSTANCE;
				else if (line.getOptionValue("c").equals("mets")) {
					processor = MetsProcessor.INSTANCE;
				} else {
					throw new ParseException(
							"Could not identify container format: "
									+ line.getOptionValue("c"));
				}

			}
			Properties optionProperties = line.getOptionProperties("m");

			if (optionProperties == null || optionProperties.isEmpty()) {
				throw new ParseException("No language models defined!");
			}
			NERClassifiers.setLanguageModels(optionProperties);

			String outputDirectory = line.getOptionValue("d");
			if (outputDirectory == null || outputDirectory.isEmpty()) {
				outputDirectory = "." + File.separator + "output";
			}

			outputDirectoryRoot = new File(outputDirectory);

			// all others should be files
			
			List fileList = line.getArgList();
			
			if (fileList.isEmpty()) {
				System.out.println("No file specified, read file list from stdin");
				try{
					BufferedReader br = 
			                      new BufferedReader(new InputStreamReader(System.in));			 
					String input;			 
					while((input=br.readLine())!=null){
						fileList.add(input);
					}
			 
				} catch (IOException io){
					io.printStackTrace();
				}
			}
			BlockingQueue<Runnable> containerHandlePool = new LinkedBlockingQueue<Runnable>();

			long startTime = System.currentTimeMillis();
			// initialize preload of language classifier
			NERClassifiers.getCRFClassifierForLanguage(lang);
			ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
					Math.min(2, maxThreads), maxThreads, 1000,
					TimeUnit.MILLISECONDS, containerHandlePool);
			for (Object arg : fileList) {
				System.out.println(arg);
				results.put(arg.toString(), threadPoolExecutor
						.submit(new ContainerHandleThread(arg.toString(), lang,
								processor)));
			}
			
			
			threadPoolExecutor.shutdown();
			threadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			
			ResultHandlerFactory.shutdownResultHandlers();
			
			System.out.println("Total processing time: "
					+ (System.currentTimeMillis() - startTime));

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
					System.err
							.println("Something went wrong during the parsing of container file "
									+ key
									+ " . Cause: "
									+ e.getCause().getMessage());
					e.printStackTrace();
					withErrors += 1;
					errors = true;
				}
			}

			System.out.println("" + successful
					+ " container documents successfully processed, "
					+ withErrors + " with errors.");
			if (errors) {
				System.err.println("There were ERRORS while processing");
				System.exit(1);
			} else {
				System.out.println("SUCCESSFUL");
				System.exit(0);
			}

		} catch (org.apache.commons.cli.ParseException e) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp(
					"java -jar NerAnnotater.jar [OPTIONS] [INPUTFILES..]",
					options);
			System.out.println("\nIf there are no input files specified, a list of filenames is read from stdin.");
		}
	}

}
