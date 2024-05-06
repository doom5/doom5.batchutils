package doom5.example.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import doom5.batchutils.GenericBatch;
import doom5.batchutils.GenericBatchImpl;
import doom5.batchutils.config.Configuration.DbConfiguration;
import doom5.batchutils.config.Configuration.Properties;
import doom5.batchutils.utils.SmbUtils;

public class ExampleImpl extends GenericBatchImpl implements GenericBatch{

	private Connection dbConn = null;
	private SmbUtils smbU = null;
	private Map<String,String> propertiesMap = new HashMap<String,String>();
	private static final String searchPeriodsQuery = "select IDPARAMETER, KEY, VALUE from PARAMETERS";
	
	public ExampleImpl(String[] args, Map<String, DbConfiguration> dbConfigurations, Properties properties) {
		super(args, dbConfigurations, properties);
		mapProperties ();
		// TODO Auto-generated constructor stub
	}
	
	public boolean start() throws Exception {
		boolean ret = super.start();
		logger.info("Running Example start() method: OPEN DB CONNECTION");
		//OPEN DB CONNECTION
		try {
			dbConn = openDbConnection (dbConfigurations.get("EXAMPLECONNECTION"));
			logger.debug("EXAMPLECONNECTION DB connection opened");
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error opening connection to EXAMPLECONNECTION");
			ret = false;
		} 
		smbU = new SmbUtils(propertiesMap.get("destServer"), propertiesMap.get("destShare"), propertiesMap.get("destDomain"), propertiesMap.get("destUser"), propertiesMap.get("destPass"));
		return ret;
	}
	public boolean process() throws Exception {
		boolean ret = super.process();
		logger.info("Running Example start() method: EXTRACT DATA FROM DB");
		ret = extractDataFromDB();
		
		/*logger.info("Running Example start() method: SEARCH FILE ON REMOTE SMB FS");
		if (smbU.connect()) {
			for (String f : smbU.listRemoteFile(propertiesMap.get("remoteSearchPath"),"*")) {
				logger.info("Remote file: " + f );
			}
		} */

		return ret;
	}
	public boolean end() throws Exception {
		boolean ret = super.end();
		/*logger.info("Running end() method: CLOSE SmbUtils CONNECTION");
		smbU.close();*/
		return ret;
	}
	public boolean error() throws Exception {
		boolean ret = super.error();
		/*logger.info("Running error() method: CLOSE SmbUtils CONNECTION");
		smbU.close();*/
		return ret;
	}
	
	//Extract data from DB
	private boolean extractDataFromDB() 
	{
		boolean ret = true;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = dbConn.prepareStatement(searchPeriodsQuery);
			rs =  ps.executeQuery();
			while (rs.next())
			{ 
				logger.info(rs.getInt("IDPARAMETER") + ": " + rs.getString("KEY")+ " - " +rs.getString("VALUE"));
			}
			rs.close();
		} catch (SQLException e) {
			ret = false;
		} finally {
			try {
				if (rs!=null && !rs.isClosed()) { rs.close(); }
				if (ps!=null && !ps.isClosed()) { ps.close(); }
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
	
	private void mapProperties ()
	{
		properties.getProp().forEach((i)->{
			this.propertiesMap.put(i.getName(), i.getValue());
		});
	}

}
