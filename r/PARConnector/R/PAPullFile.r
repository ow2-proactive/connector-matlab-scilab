PAPullFile <- function(space, path, fileName, outputFile, 
                       client = .scheduler.client) {
  tryCatch({
    pulled <- J(client, "pullFile", space, path, fileName, outputFile)
  }, Exception = function(e) {
    print("Error in PAPullFile(...):")
    e$jobj$printStackTrace()
    stop()
  })
  return (pulled)
}