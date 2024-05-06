doom5.batchutils

This is a simple library written in Java 17 useful for developing batch programs.
The library is responsible for managing:
- Configurations read from xml files
- Logs via log4j
- Database connections via JDBC (SQlite, Oracle, SqlServer, ... Integrating the correct driver)
- Utilities (e.g. operation on remote file system with access via SMB)

The compiled jar can be included as an external library in your project, or the sources can be included directly in your project.
In both cases, a java class will need to be developed which:
- extends doom5.batchutils.GenericBatchImpl and implements doom5.batchutils.GenericBatch
- implement start(), process(), end(), error()
You will also need to configure correctly in the configuration xml file:
- configuration.implementationsPackage: Package in which the implementation classes are included
- configuration.properties.batchName: Name of the class to which the properties are associated

A simple example of implementation of a java class is the doom5.example.impl.ExampleImpl class

For execution, the class that contains the main doom5.bachutils.App method must always be launched and the name of the class that implements the batch must be passed as a parameter.

Launch example:
java -jar doom5.batchutils-0.0.1-SNAPSHOT.jar ExampleImpl