# source('H:/Temp/workspace/R-dev/connectors/matlab_scilab_connector/r/tests/test.R');

library("PARConnector");
  
cat("*** Locating Scheduler home ***","\n");  
   
cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 

PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");