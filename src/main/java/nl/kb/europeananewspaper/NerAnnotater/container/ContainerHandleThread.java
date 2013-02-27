package nl.kb.europeananewspaper.NerAnnotater.container;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.Callable;

import nl.kb.europeananewspaper.NerAnnotater.App;

public class ContainerHandleThread implements Callable<Boolean> {

	private String filePath;
	private Locale lang;
	private ContainerProcessor processor;

	public ContainerHandleThread(String filePath, Locale lang,
			ContainerProcessor processor) {
		this.filePath = filePath;
		this.lang = lang;
		this.processor = processor;
	}

	public Boolean call() throws Exception {
		try {
			ContainerContext containerContext = new ContainerContext();			
			File outputDir = getOutputDirectory();
			containerContext.setOutputDirectory(outputDir);
			processor.processFile(containerContext,filePath, lang);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private File getOutputDirectory() {
		String cleanedPath=filePath.trim();
		while (cleanedPath.endsWith("/")) {
			cleanedPath=cleanedPath.substring(0, cleanedPath.length()-1);
		}
		String[] split = cleanedPath.split("/");
		String fileName = split[split.length-1];
		
		File outputDir = new File(App.getOutputDirectoryRoot(),fileName+"-annotations");
		if ((!outputDir.isDirectory())&&(!outputDir.mkdirs())) {
			
			throw new IllegalArgumentException("Could not create output directory "+outputDir.getAbsolutePath());
		}
		return outputDir;
	}

}
