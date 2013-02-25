package nl.kb.europeananewspaper.NerAnnotater.container;

import java.util.Locale;
import java.util.concurrent.Callable;

public class ContainerHandleThread implements Callable<Boolean> {

	private String fileName;
	private Locale lang;
	private ContainerProcessor processor;
	
	public ContainerHandleThread(String fileName, Locale lang, ContainerProcessor processor) {
		this.fileName=fileName;
		this.lang=lang;
		this.processor=processor;
	}
	

	public Boolean call() throws Exception {
		try {
		processor.processFile(fileName, lang);
		return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
