source("functionalTests/utils.r")

library("PARConnector");

cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

PADebug(TRUE)

# test PAWaitFor with individual waits
n <- 4
res <- PASolve('sin',1:n)
val <- list()
for (i in 1:n) {
  v <- PAWaitFor(res[i])
  val <- c(val, v)
}

print(val)

if (!all(unlist(val) == sin(1:n))) {
  msg <- paste0("Error when comparing val=",val, " with sin(1:n)=",sin(1:n),"\n")
  stop(msg)   
}
# test PAWaitAny
n <- 4
res <- PASolve('sin',1:n)
val <- list()
for (i in 1:n) {
  v <- PAWaitAny(res)
  tn <- names(v)
  ind <- strtoi(substr(tn,2,nchar(tn)))
  val[ind] <- v
}

print(val)
if (!all(unlist(val) == sin(1:n))) {
  msg <- paste0("Error when comparing val=",val, " with sin(1:n)=",sin(1:n) ,"\n" )
  stop(msg)  
}

