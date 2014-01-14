source("functionalTests/utils.r")

library("PARConnector");
cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

n <- 4

# single param

res <- PASolve('sin',1:n)
val <- PAWaitFor(res)
print(val)

if (!all(unlist(val) == sin(1:n))) {
  msg <- paste0("Error when comparing val=",val, " with sin(1:3)=",sin(1:n) ,"\n")
  stop(msg) 
}

# multiple params


res <- PASolve('min',1:n, n:1)
val <- PAWaitFor(res)
print(val)

if (!all(unlist(val) == mapply(min, 1:n, n:1))) {
  msg <- paste0("Error when comparing val=",val, " with mapply(min, 1:n, n:1)=",mapply(min, 1:n, n:1) ,"\n")
  stop(msg) 
}    

# named params

res <- PASolve('min',1:n, n:1, NA, na.rm = TRUE)
val <- PAWaitFor(res)
print(val)

if (!all(unlist(val) == mapply(min, 1:n, n:1, NA, na.rm = TRUE))) {
  msg <- paste0("Error when comparing val=",val, " with mapply(min, 1:n, n:1, NA, na.rm = TRUE)=",mapply(min, 1:n, n:1, NA, na.rm = TRUE) ,"\n")
  stop(msg) 
}


# unvarying parameters, identified by index

res <- PASolve('c',1:4, 1:4, varies = list(1))
val <- PAWaitFor(res)
print(val)

if (!all(unlist(val) == c(1,1,2,3,4,2,1,2,3,4,3,1,2,3,4,4,1,2,3,4))) {
  msg <- paste0("Error when comparing val=",val, " with c(1,1,2,3,4,2,1,2,3,4,3,1,2,3,4,4,1,2,3,4)\n")
  stop(msg) 
}

# unvarying parameters, identified by name

res <- PASolve('append',x=1:4, values=1:4, varies = list("values"))
val <- PAWaitFor(res)
print(val)

if (!all(unlist(val) == c(1,2,3,4,1,1,2,3,4,2,1,2,3,4,3,1,2,3,4,4))) {
  msg <- paste0("Error when comparing val=",val, " with c(1,2,3,4,1,1,2,3,4,2,1,2,3,4,3,1,2,3,4,4)\n")
  stop(msg) 
}
