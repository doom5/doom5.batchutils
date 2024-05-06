package doom5.batchutils;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import doom5.batchutils.config.Configuration;
import doom5.batchutils.config.Configuration.DbConfiguration;
import doom5.batchutils.config.Configuration.Properties;
import doom5.batchutils.xml.UnmarshalXmlFile;


public class App {
	private static final String LOG4J_PROP_NAME = "log4j.configurationFile";
	private static final String LOG4J_FILE_DEFAULT = "resources/log4Jconfiguration.xml";
	private static final String CONF_PROP_NAME = "batchutils.configurationFile";
	private static final String CONF_FILE_DEFAULT = "resources/configuration.xml";
	
	//Exit codes:
	private final static Integer OK = 0;
	private final static Integer UNINITIALIZED = -99;
	private final static Integer KO_PARAMETERS_ERROR = -1;
	private final static Integer KO_CONF_ERROR = -2;
	private final static Integer KO_JOB_ERROR = -3;
	
	private static Configuration conf;
	private static String implementationsPackages;
	private static Logger logger;
	
	public static void main(String[] args) {
		
		Integer exitCode = UNINITIALIZED;
		
		//Load LOG4J configuration
		if (System.getProperty(LOG4J_PROP_NAME) == null)
		{
			System.setProperty(LOG4J_PROP_NAME,LOG4J_FILE_DEFAULT);	
			System.out.println("Using default configuration: " + CONF_PROP_NAME+"="+CONF_FILE_DEFAULT);
		} 
		System.out.println("Log4j configuration: "+ System.getProperty(LOG4J_PROP_NAME));
	   
		// Redirect outputs to LOG4J
	    logger = LogManager.getRootLogger();
		System.setOut(createLoggingProxy(System.out, "INFO",logger));
		System.setErr(createLoggingProxy(System.err, "ERROR",logger));
		
		//Load application configuration
		try {
			if (System.getProperty(CONF_PROP_NAME) == null)
			{
				System.setProperty(CONF_PROP_NAME, CONF_FILE_DEFAULT);	
				logger.trace("Using default configuration: " + CONF_PROP_NAME+"="+CONF_FILE_DEFAULT);
			} 
			conf = (Configuration) (new UnmarshalXmlFile(new File(System.getProperty(CONF_PROP_NAME)), "doom5.batchutils.config")).getObject();
			implementationsPackages = conf.getImplementationsPackage();
			logger.info("Using configuration file: "+ System.getProperty(CONF_PROP_NAME));
		} catch (Exception e)
		{
			exitCode = KO_CONF_ERROR;
			e.printStackTrace();
		}
	    
	    logger.info("Starting BATCH");
	    if(args.length > 0) {
	    	
	    	if (execute(args)) {
	    		exitCode = OK;
	    	} else {
	    		exitCode = KO_JOB_ERROR;
	    	}

	    }else {
	    	exitCode = KO_PARAMETERS_ERROR;
		    logger.error("NO PARAMETERS INSERTED INDICATING THE BATCH TO BE EXECUTED");
	    }
	    logger.info("End BATCH with exitCode: "+exitCode);
	    
	    System.exit(exitCode);
	  
	}
	
	private static Boolean execute(String[] args) {
		String jobClass = args[0];
		String otherParameters [] = Arrays.copyOfRange(args, 1, args.length);
		Boolean result = false;
		Map<String,Properties> bP = getBatchProperties (conf);
		Map<String,DbConfiguration> dbC = getBatchConnections (conf);
		GenericBatchImpl batch = null;
		try {
			
			
			String className = implementationsPackages+"."+jobClass;
			logger.info("Running class: "+className);
			@SuppressWarnings("unchecked")
			Class<GenericBatchImpl> classe = (Class<GenericBatchImpl>) Class.forName(className);
			Constructor<GenericBatchImpl> ctor = classe.getDeclaredConstructor(String[].class, Map.class, Properties.class);
			batch = ctor.newInstance(otherParameters, dbC, bP.get(jobClass));
			
			if (batch.start())
			{
				logger.debug("START phase successfully completed");
				if (batch.process()) 
				{
					logger.debug("PROCESS phase successfully completed");
					if (batch.end())
					{
						logger.debug("END phase successfully completed");
						logger.info("All phases successfully completed");
						result = true;
					} else {
						logger.error("Error in the END phase");
						batch.error();
					}
				} else {
					logger.error("Error in the PROCESS phase");
					batch.error();
				}
			} else {
				logger.error("Error in the START phase");
				batch.error();
			}

			
		} catch (Exception e) {
			e.printStackTrace();
			if (batch != null)
			{
				try {
					batch.error();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			result = false;
		}
		return result;
	}
	
	private static Map<String,Properties> getBatchProperties (Configuration c)
	{
		Map<String,Properties> batchProperty = new HashMap<String,Properties>();
		c.getProperties().forEach(p->{
			batchProperty.put(p.getBatchName(), p);
		});
		return batchProperty;
	}
	
	private static Map<String,DbConfiguration> getBatchConnections (Configuration c)
	{
		Map<String,DbConfiguration> batchProperty = new HashMap<String,DbConfiguration>();
		c.getDbConfiguration().forEach(d->{
			batchProperty.put(d.getConnectionType(), d);
		});
		return batchProperty;
	}
	
	 private static PrintStream createLoggingProxy(final PrintStream realPrintStream, String level, Logger logger) {
	        return new PrintStream(realPrintStream) {
	            public void print(final String string) {
	                //realPrintStream.print(string);
	            switch (level)
	            {
	            	case "INFO": 
	            		logger.info(string);
	            	break;
	            	case "ERROR": 
	            		logger.error(string);
	            	break;
	            	default: 
	            		logger.fatal(string);
	            	break;
	            }
	        }
	    };
	}
}
