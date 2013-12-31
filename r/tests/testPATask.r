library("PARConnector");
cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");


t1 = PATask('t1',f(x)<-x+1)
PASolve(t1, 1)