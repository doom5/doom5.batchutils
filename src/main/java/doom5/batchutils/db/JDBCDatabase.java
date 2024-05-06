package doom5.batchutils.db;


import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import doom5.batchutils.config.Configuration.DbConfiguration;
import doom5.batchutils.config.Configuration.DbConfiguration.Prop;



public class JDBCDatabase {
	private Logger logger;
	
	private String connectionType;
	private String driverClass;
	private String connectionString;
	private List<Prop> prop;
	private List<String> onConnectStatement;
	private Connection connection;
	private boolean connected = false; 

	public JDBCDatabase (DbConfiguration dbConf) {
		logger = LogManager.getRootLogger();
		this.connectionType = dbConf.getConnectionType();
		this.driverClass = dbConf.getDriverClass();
		this.connectionString = dbConf.getConnectionString();
		this.prop = dbConf.getProp();
		this.onConnectStatement =dbConf.getOnConnectStatement();
		this.connection = null;
		this.connected = false;
	}

	private JDBCDatabase connect() throws SQLException{

		if (this.connection == null 
				|| this.connected==false 
				|| this.connection.isClosed() 
				|| !this.connection.isValid(1000))
		{
			logger.info("Connection to DB: "+this.connectionType);
			try {
				Class.forName(this.driverClass);
			} catch (ClassNotFoundException e) {
				logger.error("DB driver not found: "+this.driverClass);
				e.printStackTrace();
			}
	
			logger.debug("The driver has been registered "+this.driverClass);
	
			try {
				Properties connProperties = new Properties();
				prop.forEach(p->{
					connProperties.setProperty(p.getName(),p.getValue());
					logger.debug("Prop: "+p.getName()+" - "+p.getValue());
				});
				connection = DriverManager.getConnection(this.connectionString, connProperties);
			} catch (SQLException e) {
	
				logger.error("Connection failed.");
				e.printStackTrace();
			}
	
			if (connection != null) {
				this.connected = true;
				logger.info("Connection established successfully");
				
				// Execute "statement on connect" on connection
				
				PreparedStatement ps=null;
				if (this.onConnectStatement!=null)
				{
					try {
						Iterator<String> i =this.onConnectStatement.iterator();
						while (i.hasNext())
						{
							String oCS = i.next();
							logger.debug("Esegue: "+oCS);
							ps = connection.prepareStatement(oCS);
							ps.execute();
						}
					} catch (SQLException e) {
						logger.error("Error executing the defined onConnectStatement");
						e.printStackTrace();
					} finally {
						try {
							ps.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}				
			} else {
				logger.error("Connection failed due to an unknown error");
			}
		}
		else {
			logger.debug("Connection already open");
		}
		return this;
	}

	public boolean disconnect() {
		try {
			this.connection.close();
			this.connection = null;
			this.connected = false;
			logger.debug("Disconnection from DB: "+this.connectionType);
			return true;
		} catch (SQLException | NullPointerException e) {
			logger.error("Disconnection from DB failed");
			e.printStackTrace();
			return false;
		}
	}


	public Connection getConnection() throws SQLException {

		connect();
		return this.connection;
	}

}