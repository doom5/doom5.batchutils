package doom5.batchutils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import doom5.batchutils.config.Configuration.DbConfiguration;
import doom5.batchutils.config.Configuration.Properties;


public abstract class GenericBatchImpl implements GenericBatch {
	

	protected String[] args;
	protected Map<String, DbConfiguration> dbConfigurations;
	protected Properties properties;
	protected Logger logger;
	protected Map<DbConfiguration, Connection> openConnections = new HashMap<DbConfiguration, Connection>();
	
	protected GenericBatchImpl(String[] args, Map<String, DbConfiguration> dbConfigurations, Properties properties)
	{
		super();
		logger = LogManager.getRootLogger();
		this.args = args;
		this.dbConfigurations = dbConfigurations;
		this.properties = properties;
		logger.debug("args: "+Arrays.toString(this.args));
		logger.debug("dbConfigurations: "+this.dbConfigurations);
		logger.debug("properties: "+this.properties);
		//logger.error("This is generic GenericBatchImpl. You should implement your class");
	}

	public boolean start() throws Exception {
		logger.debug("Running start() method");
		return true;
	}
	public boolean process() throws Exception {
		logger.debug("Running process() method");
		return true;
	}
	public boolean end() throws Exception {
		logger.debug("Running end() method");
		return closeAllDbConnections ();
	}
	public boolean error() throws Exception {
		logger.debug("Running error() method");
		return closeAllDbConnections ();
	}

	protected Connection openDbConnection (DbConfiguration dbc) throws SQLException
	{
		Connection conn = null;
		if (openConnections.containsKey(dbc)) {
			conn = openConnections.get(dbc);
			if (conn == null || conn.isClosed())
			{
				conn = new doom5.batchutils.db.JDBCDatabase(dbc).getConnection();
			}
		} else {
			conn = new doom5.batchutils.db.JDBCDatabase(dbc).getConnection();
			this.openConnections.put(dbc, conn);
		}
		return conn;
	}
	
	protected boolean closeDbConnections (Connection conn) throws SQLException
	{
		boolean ret = false;
		if(null != conn && !conn.isClosed()) {
			conn.close();
			logger.info("DB connection closed!");
			ret = true;
		}
		return ret;
	}
	
	protected boolean closeAllDbConnections ()
	{
		boolean ret = true;
		for (Map.Entry<DbConfiguration, Connection> entry : openConnections.entrySet())  
		{
			DbConfiguration n = entry.getKey();
			Connection c = entry.getValue();
			logger.info("Closing DB connection: "+n.getConnectionType());
			try {
				closeDbConnections(c);
				openConnections.remove(n);
			} catch (SQLException e) {
				e.printStackTrace();
				logger.error("Unable to close DB connection: "+n.getConnectionType());
				ret = false;
			}
		}
				
		return ret;
	}


}
