package nl.kb.europeananewspaper.NerAnnotater.container;

import java.io.File;

public class ContainerContext {
	File outputDirectory;
	
	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory=outputDirectory;
	}
	
	public File getOutputDirectory() {
		return outputDirectory;
	}
	
}
