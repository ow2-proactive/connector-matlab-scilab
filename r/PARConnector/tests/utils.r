# Some function usefull  for tests

# error handler to quit the R session on error
options(
  error = bquote(
{ifelse(is.element("QUITONERROR",commandArgs(TRUE)), 
        q(save = "no", status=1), stop("Error during test"))
}))

# Create nb files named like prefix1, prefix2, ... prefixn
createFiles <- function(prefix, nb) {
  for (i in 1:nb) {
    name <- paste0(prefix,i)
    if(!file.exists(name)) {
      file.create(name, showWarnings = TRUE)
    }    
  }
}

# Removes nb files named like prefix1, prefix2, ... prefixn
removeFiles <- function(prefix, nb) {
  for (i in 1:nb) {
    name <- paste0(prefix,i)    
    if(file.exists(name)) {
      file.remove(name)
    }    
  }
}