package nl.kb.europeananewspaper.NerAnnotater;

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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * Hello world!
 * 
 */
public class App {

	static Map<String, Future<Boolean>> results = new LinkedHashMap<String, Future<Boolean>>();

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws ClassCastException,
			ClassNotFoundException, InterruptedException {
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("export")
				.withDescription("use FORMAT for export").hasArg()
				.withArgName("FORMAT").withType(String.class).create());

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
						"models for languages. Ex. de=/path/to/file/models")
				.hasArgs().withArgName("language=filename")
				.withValueSeparator().create("m"));

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

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

			// all others should be files
			List<?> fileList = line.getArgList();

			BlockingQueue<Runnable> containerHandlePool = new LinkedBlockingQueue<Runnable>();

			long startTime = System.currentTimeMillis();
			ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
					Math.min(2, maxThreads), maxThreads, 1000,
					TimeUnit.MILLISECONDS, containerHandlePool);
			for (Object arg : fileList) {
				results.put(arg.toString(), threadPoolExecutor
						.submit(new ContainerHandleThread(arg.toString(), lang,
								processor)));
			}

			threadPoolExecutor.shutdown();
			threadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			System.out.println("Total processing time: "
					+ (System.currentTimeMillis() - startTime));

			boolean errors = false;
			int successful = 0;
			int withErrors = 0;

			for (String key : results.keySet()) {
				try {
					if (results.get(key).get() == true) {
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

			System.out.println(""+successful+" container documents successfully processed, "+withErrors+" with errors.");
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
		}
	}

}
