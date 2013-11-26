PAPushFile <- function(space, path, fileName, inputFile, 
                       client = .scheduler.client) {
  tryCatch({
     pushed <- J(client, "pushFile", space, path, fileName, inputFile)
  }, Exception = function(e) {
    print("Error in PAPushFile(...):")
    e$jobj$printStackTrace()
    stop()
  })
  return (pushed)
}