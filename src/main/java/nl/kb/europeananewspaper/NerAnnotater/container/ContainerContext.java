package nl.kb.europeananewspaper.NerAnnotater.container;

import java.io.File;

/**
 * The context information relevant for all documents that are referenced by a
 * container (METS,DIDL...).
 * 
 * @author rene
 * 
 */
public class ContainerContext {
	File outputDirectory;

	/**
	 * @param outputDirectory
	 *            the output directory for the result files
	 */
	public void setOutputDirectory(final File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	/**
	 * @return the output directory for the result files
	 */
	public File getOutputDirectory() {
		return outputDirectory;
	}

}
