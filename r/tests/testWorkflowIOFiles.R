source("tests/utils.r")

library("PARConnector");
cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

# split merge with I/O Files

split <- function(ind) {
  listoutfiles <- NULL
  if (!file.exists("in")) {
    stop("The file 'in' does not exist")
  }
  for (i in ind) {
    outf <- paste0("split_",i)
    file.copy("in",outf)
    listoutfiles <- c(listoutfiles,outf)
  }
  return(listoutfiles)
}

compute <- function(infile) {
  outfile <- gsub("split_","out_",infile)
  file.copy(infile,outfile, overwrite=TRUE)
  return(outfile)
}

merge <- function(...) {
  dots <- list(...)
  file.create("out")
  for (i in 1:length(dots)) {
    file.append("out",dots[[i]])
  }
  return("out")
}

.Last <- function() {  
  file.remove("in")
  file.remove("out")
  removeFiles("split_", 4)
  removeFiles("out_", 4)
}

# testing with numeric indexes in file names

if(!file.exists("in")) {
  file.create("in", showWarnings = TRUE)
}

if(file.exists("out")) {
  file.remove("out")
}

r=PASolve(
  PAM(merge,
      PA(compute,
         PAS(split, 1:4, input.files="in", output.files="split_%1%"),
         input.files="split_%1%",
         output.files="out_%1%"),
      input.files="out_%1:4%",
      output.files="out")
) 

val <- PAWaitFor(r)
print(val)

if (val[["t6"]] != "out") {
  stop("val[t6]=", val[["t6"]], " should be out\n")  
}

# testing with character indexes in file names

if(!file.exists("in")) {
  file.create("in", showWarnings = TRUE)
}

if(file.exists("out")) {
  file.remove("out")
}

r=PASolve(
  PAM(merge,
      PA(compute,
         PAS(split, c("a","b","c","d"), input.files="in", output.files="split_%1%"),
         input.files="split_%1%",
         output.files="out_%1%"),
      input.files="out_%1:4%",
      output.files="out")
) 

val <- PAWaitFor(r)
print(val)

if (val[["t6"]] != "out") {
  stop("val[t6]=", val[["t6"]], " should be out\n")
}