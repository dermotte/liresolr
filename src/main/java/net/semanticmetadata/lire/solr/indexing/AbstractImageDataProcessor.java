package net.semanticmetadata.lire.solr.indexing;

public abstract class AbstractImageDataProcessor implements ImageDataProcessor{

	String imageData;

	@Override
	public String getAdditionalFields() {
	    return "";
	}

	@Override
	public String getFilePath() {
		return getImageData();
	}

	@Override
	public void setImageData(String imageData) {
		this.imageData = imageData;
	}

	@Override
	public String getImageData() {
		return imageData;
	}
	
	@Override
	public void appendSolrFields(StringBuilder sb) {
		sb.append("<field name=\"id\">");
		sb.append(getIdentifier());
		sb.append("</field>");
		
		sb.append("<field name=\"title\">");
		sb.append(getTitle());
		sb.append("</field>");
		
		sb.append(getAdditionalFields());
	}

}
