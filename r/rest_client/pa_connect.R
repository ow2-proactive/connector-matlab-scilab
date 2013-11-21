# Example:
# PAConnect(url='http://localhost:8080/rest', login='demo', pwd='demo')
# pushed <- PAPushFile("USERSPACE", "/my-files", "test.txt", "/home/user/text.txt");


# script directory
script.dir <- dirname(sys.frame(1)$ofile)
lib.path <- paste(script.dir, '/lib', sep="")

library(rJava)
.jinit()
.jaddClassPath(dir(lib.path, full.names=TRUE))
.jclassPath()


PAConnect <- function(url, login, pwd, cred=NULL) {
  if(exists('client') && !is.null(client)) {
    .jcall(client,"V","disconnect")
  }
  client <<- .jnew("org.ow2.proactive.scheduler.rest.SchedulerClient", check = TRUE)
  if (is.null(cred)) {
    .jcall(client,"S","init", url, login, pwd, check = TRUE)
  } else {
	print("using cred file")  
  }
}

PAPushFile <- function(space, path, fileName, inputFile) {
    return (.jcall(client, "Z", "pushFile", space, path, fileName, inputFile))
}

PAPullFile <- function(space, path, fileName, outputFile) {
	return (.jcall(client, "Z", "pullFile", space, path, fileName, outputFile))
}

PADeleteFile <- function(space, pathname) {
    return (.jcall(client, "Z", "deleteFile", space, pathname))
}

if (exists('client')) {
	print(client) 
} else {
	print('client does not exist')
}



