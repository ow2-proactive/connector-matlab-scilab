source("functionalTests/utils.r")

library("PARConnector");

cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

r = PASolve('sin', 1)

r2 <- PAGetResult(r@job.id)

val <- PAWaitFor(r2)

if (unlist(val) != sin(1)) {
  msg <- paste0("Error when comparing val=",toString(unlist(val)), " with sin(1)=",toString(sin(1)),"\n" )
  stop(msg) 
}