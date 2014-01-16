source("functionalTests/utils.r")

library("PARConnector");
cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

PADebug(TRUE)

n <- 4

copyfile <- function(index) {
  file.copy(paste0("in_",index),paste0("out_",index), overwrite=TRUE)
  return(TRUE)
}

.Last <- function() {
  removeFiles("in_", n)
  removeFiles("out_", n)
}

# testing with standard indexes replacements
createFiles("in_", n)
removeFiles("out_", n)

res <- PASolve(copyfile, 1:n, input.files="in_%1%", output.files="out_%1%")
val <- PAWaitFor(res, TEN_MINUTES)
print(val)

for (i in 1:n) {
  filename <- paste0("out_",i)
  if(!file.exists( filename )) {
     cat("Can't find file ", filename)
     q(status=1)
  }
}

# testing with named parameter replacements
createFiles("in_", n)
removeFiles("out_", n)

res <- PASolve(PA(copyfile,index=1:n, input.files="in_%index%",output.files="out_%index%"))
val <- PAWaitFor(res, TEN_MINUTES)
print(val)

for (i in 1:n) {
  filename <- paste0("out_",i)
  if(!file.exists( filename )) {  
    cat("Can't find file ", filename)
    q(status=1)
  }
}