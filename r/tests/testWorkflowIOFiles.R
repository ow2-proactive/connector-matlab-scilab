library("PARConnector");
cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n") 
PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
cat("Sucessfully connected !!", "\n");

# split merge with I/O Files

split <- function(ind) {
  listoutfiles <- NULL
  if (!file.exists("in.txt")) {
    stop("in.txt", " does not exist")
  }
  for (i in ind) {
    outf <- paste0("split_",i,".txt")
    file.copy("in.txt",outf)
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
  file.create("out.txt")
  for (i in 1:length(dots)) {
    file.append("out.txt",dots[[i]])
  }
  return("out.txt")
} 

# testing with numeric indexes in file names

if(!file.exists("in.txt")) {
  file.create("in.txt", showWarnings = TRUE)
}


if(file.exists("out.txt")) {
  file.remove("out.txt")
}

r=PASolve(
  PAM(merge,
      PA(compute,
         PAS(split, 1:4, input.files="in.txt", output.files="split_%1%.txt"),
         input.files="split_%1%.txt",
         output.files="out_%1%.txt"),
      input.files="out_%1:4%.txt",
      output.files="out.txt")
) 

val <- PAWaitFor(r)
print(val)

if (val[["t6"]] != "out.txt") {
  msg <- paste0("val[t6]=",val[["t6"]], " should be out.txt\n")
  stop(msg) 
}

# testing with character indexes in file names

if(!file.exists("in.txt")) {
  file.create("in.txt", showWarnings = TRUE)
}


if(file.exists("out.txt")) {
  file.remove("out.txt")
}

r=PASolve(
  PAM(merge,
      PA(compute,
         PAS(split, c("a","b","c","d"), input.files="in.txt", output.files="split_%1%.txt"),
         input.files="split_%1%.txt",
         output.files="out_%1%.txt"),
      input.files="out_%1:4%.txt",
      output.files="out.txt")
) 

val <- PAWaitFor(r)
print(val)

if (val[["t6"]] != "out.txt") {
  msg <- paste0("val[t6]=",val[["t6"]], " should be out.txt\n")
  stop(msg) 
}

