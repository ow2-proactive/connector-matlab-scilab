library("PARConnector");
cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

n <- 4

copyfile <- function(index) {
  file.copy(paste0("in_",index),paste0("out_",index), overwrite=TRUE)
  return(TRUE)
} 

# testing with standard indexes replacements

for (i in 1:n) {
  if(!file.exists(paste0("in_",i))) {
    file.create(paste0("in_",i), showWarnings = TRUE)
  }
  if(file.exists(paste0("out_",i))) {
    file.remove(paste0("out_",i))
  }
}

res <- PASolve(copyfile,1:n, input.files="in_%1%",output.files="out_%1%")
val <- PAWaitFor(res)
print(val)

for (i in 1:n) {
  if(!file.exists(paste0("out_",i))) {
    stop("Can't find file ",paste0("out_",i))
  } 
}

# testing with named parameter replacements

for (i in 1:n) {
  if(!file.exists(paste0("in_",i))) {
    file.create(paste0("in_",i), showWarnings = TRUE)
  }
  if(file.exists(paste0("out_",i))) {
    file.remove(paste0("out_",i))
  }
}

res <- PASolve(PA(copyfile,index=1:n, input.files="in_%index%",output.files="out_%index%"))
val <- PAWaitFor(res)
print(val)

for (i in 1:n) {
  if(!file.exists(paste0("out_",i))) {
    stop("Can't find file ",paste0("out_",i))
  } 
}