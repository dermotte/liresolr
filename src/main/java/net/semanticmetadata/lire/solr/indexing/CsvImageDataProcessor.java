package net.semanticmetadata.lire.solr.indexing;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

public class CsvImageDataProcessor extends AbstractImageDataProcessor {
	
	public static int POS_FILE_PATH = 0;
	public static int POS_ID = 1;
	public static int POS_IMAGE_TITLE = 2;
	//additional fields: e.g. web url for image
	public static int POS_ADDITIONAL_FIELDS = 3;
	public static final String CSV_SEPARATOR = ";";
	public static String POS_ADDITIONAL_FIELD_IMAGE_URL = "imgurl";
	
	
	String[] imageDataValues;
	
	/**
	 * pre-processes the provided image data
	 * @param csvRow
	 * @return
	 */
	protected boolean preprocessData(){
		if(getImageData() == null)
			return false;
		
		//allow empty strings as values in the CSV file
		this.imageDataValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(
				getImageData(), CSV_SEPARATOR, 4);
		
		return true;
	}

	public void setImageData(String imageData) {
		super.setImageData(imageData);
		preprocessData();
	}

	public String[] getImageDataValues() {
		return imageDataValues;
	}

	void setImageDataValues(String[] imageDataValues) {
		this.imageDataValues = imageDataValues;
	}

	@Override
	public String getFilePath() {
		if(getImageDataValues() == null || getImageDataValues().length <= POS_FILE_PATH)
			return null;
		return getImageDataValues()[POS_FILE_PATH];
	}
	
	@Override
	public String getIdentifier() {
		if(getImageDataValues() == null || getImageDataValues().length <= POS_ID)
			return null;
		return getImageDataValues()[POS_ID];	
	}
	
	@Override
	public String getTitle() {
		if(getImageDataValues() == null || getImageDataValues().length <= POS_IMAGE_TITLE)
			return null;
		
		return StringEscapeUtils.escapeXml(getImageDataValues()[POS_IMAGE_TITLE]);	
	}

	@Override
	public String getAdditionalFields() {
		if(getImageDataValues() == null || getImageDataValues().length <= POS_ADDITIONAL_FIELDS)
			return null;
		
		return "<field name=\""+ POS_ADDITIONAL_FIELD_IMAGE_URL +"\">" 
			+ StringEscapeUtils.escapeXml(getImageDataValues()[POS_ADDITIONAL_FIELDS]) + "</field>";
	}

	
}
