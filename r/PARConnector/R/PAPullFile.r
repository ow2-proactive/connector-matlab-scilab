PAPullFile <- function(space, pathname, outputFile, 
                       client = .scheduler.client, .nb.retries = 1) {
  while(.nb.retries > 0) {
  tryCatch({
    pulled <- J(client, "pullFile", .getSpaceName(space), pathname, outputFile)
    .nb.retries <- 0
  }, Exception = function(e) {
    if (.nb.retries == 0) {
      print(str_c("Error in PAPullFile(",space,",",pathname,",",outputFile,") :"))
      e$jobj$printStackTrace()
      stop()
    } else {
      .nb.retries <- .nb.retries -1
    }
  })
  }
  return (pulled)
}