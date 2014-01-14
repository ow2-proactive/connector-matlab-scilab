library("PARConnector");
cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

if(!file.exists("foo.txt")) {
  file.create("foo.txt", showWarnings = TRUE)
}

if(file.exists("foo2.txt")) {
  file.remove("foo2.txt")
}

.Last <- function() {
  file.remove("foo.txt")
  file.remove("foo2.txt")
}

PAPushFile("USER", "/", "foo.txt", "foo.txt") 
cat("file pushed !!", "\n");

PAPullFile("USER", "/foo.txt", "foo2.txt") 
cat("file pulled !!", "\n");

if(!file.exists("foo2.txt")) {
  stop("Error, can't find file foo2.txt")
}