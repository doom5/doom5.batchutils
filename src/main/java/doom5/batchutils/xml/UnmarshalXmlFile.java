package doom5.batchutils.xml;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.JAXBIntrospector;
import jakarta.xml.bind.Unmarshaller;


/* Xml configuration file*/
public class UnmarshalXmlFile {
	private Object obj;
	private String objPackage;
	private InputStream targetStream;
	
	public UnmarshalXmlFile(File xmlFile, String objPackage)
	{
		try {
			this.targetStream = new FileInputStream(xmlFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.objPackage = objPackage;
	}

	public UnmarshalXmlFile(InputStream xmlStream, String objPackage)
	{
	    this.targetStream = xmlStream;
		this.objPackage = objPackage;
	}
	
	private boolean readXml() 
	{
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(this.objPackage);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			this.obj = JAXBIntrospector.getValue(jaxbUnmarshaller.unmarshal(this.targetStream));
			return true;
		} catch (JAXBException e) {
			e.printStackTrace();
			return false;
		} 
	}
	
	public Object getObject() 
	{
		if (readXml())
		{
			return obj;
		}
		return null;
	}
}