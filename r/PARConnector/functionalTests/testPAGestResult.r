source("functionalTests/utils.r")

library("PARConnector");

cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

r = PASolve('sin', 1)
PAGetResult(r)