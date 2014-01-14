# Some function usefull  for tests

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