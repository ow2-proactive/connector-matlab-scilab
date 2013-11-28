PAPullFile <- function(space, pathname, outputFile, 
                       client = .scheduler.client) {
  tryCatch({
    J(client, "pullFile", space, pathname, outputFile)
  }, Exception = function(e) {
    print("Error in PAPullFile(...):")
    e$jobj$printStackTrace()
    stop()
  })
}