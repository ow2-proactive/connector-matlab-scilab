
# this test tests function dependency transfer, both for main function and for parameter functions
source("tests/utils.r")

library("PARConnector");
cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

# use foo as main function, check that dependencies of foo are transferred
a <- 1
foo <- function(x)x*a
bar <- function(x)foo(x)


# use foo as main function
res <- PASolve(foo,1:3)
val <- PAWaitFor(res)
print(val)

if (!all(unlist(val) == 1:3)) {
  msg <- paste0("Error when comparing val=",val, " with foo(1:3)=",1:3,"\n")
  stop(msg) 
}

# use foo as parameter, check that dependencies of foo are transferred

boo <- function(f,x)f(x)

res <- PASolve(boo,foo,1:3)

val <- PAWaitFor(res)
print(val)

if (!all(unlist(val) == 1:3)) {
  msg <- paste0("Error when comparing val=",val, " with foo(1:3)=",1:3,"\n")
  stop(msg) 
}