source("functionalTests/utils.r")

library("PARConnector");

cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

PADebug(TRUE)

# simple split merge
r=PASolve(
  PAM("sum",
      PA(function(x) {x*x},
         PAS("identity", 1:4)))) 

val <- PAWaitFor(r)
print(val)

if (val[["t6"]] != 30) {
  msg <- paste0("Error when comparing val[t6]=",val[["t6"]], " with sum(1,2^2,3^2,4^2)=30\n")
  stop(msg) 
}