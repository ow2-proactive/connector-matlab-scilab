source("functionalTests/utils.r")

library("PARConnector");

cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

PADebug(TRUE)

# test PAWaitFor with timeout

res <- PASolve(function(x)FALSE,1)
# this should trigger a timout
timeout <- FALSE
v <- tryCatch(PAWaitFor(res,10), error = function(e) {print(e);TRUE})
if (islist(v) || !v) {
  msg <- paste0("Error timeout not received in PAWaitFor\n")
  stop(msg)
}

# test PAWaitAny with timeout

res <- PASolve(function(x)FALSE,1)
# this should trigger a timout
timeout <- FALSE
v <- tryCatch(PAWaitAny(res,10), error = function(e) {print(e);TRUE})
if (!v) {
  msg <- paste0("Error timeout not received in PAWaitFor\n")
  stop(msg)
}


n <- 4

# test PAWaitFor with individual waits

res <- PASolve('sin',1:n)
val <- list()
for (i in 1:n) {
  v <- PAWaitFor(res[i], TEN_MINUTES)
  val <- c(val, v)
}

print(val)

if (!all(unlist(val) == sin(1:n))) {
  msg <- paste0("Error when comparing val=",toString(unlist(val)), " with sin(1:n)=", toString(sin(1:n)),"\n")
  stop(msg)   
}
# test PAWaitAny
n <- 4
res <- PASolve('sin',1:n)
val <- list()
for (i in 1:n) {
  v <- PAWaitAny(res, 60000)
  tn <- names(v)
  ind <- strtoi(substr(tn,2,nchar(tn)))
  val[ind] <- v
}

print(val)
if (!all(unlist(val) == sin(1:n))) {
  msg <- paste0("Error when comparing val=",toString(unlist(val)), " with sin(1:n)=", toString(sin(1:n)) ,"\n" )
  stop(msg)  
}

