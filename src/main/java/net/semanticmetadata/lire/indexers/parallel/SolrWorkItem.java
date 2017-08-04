package net.semanticmetadata.lire.indexers.parallel;

import java.util.List;

import net.semanticmetadata.lire.imageanalysis.features.LocalFeature;
import net.semanticmetadata.lire.solr.indexing.ImageDataProcessor;

public class SolrWorkItem extends WorkItem {

	ImageDataProcessor dataProcessor;
	
	public ImageDataProcessor getImageDataProcessor() {
		return dataProcessor;
	}

	public SolrWorkItem(ImageDataProcessor dataProcessor, byte[] buffer) {
		super(null, buffer);
		this.dataProcessor = dataProcessor;
		setFileName(dataProcessor.getFilePath());
	}

	public SolrWorkItem(ImageDataProcessor dataProcessor, List<? extends LocalFeature> listOfFeatures) {
		super(null, listOfFeatures);
		this.dataProcessor = dataProcessor;
		setFileName(dataProcessor.getFilePath());
	}

}
