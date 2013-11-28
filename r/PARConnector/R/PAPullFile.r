PAPullFile <- function(space, path, fileName, outputFile, 
                       client = .scheduler.client) {
  tryCatch({
    pulled <- J(client, "pullFile", .getSpaceName(space), str_c(path,"/", fileName), outputFile)
  }, Exception = function(e) {
    print(str_c("Error in PAPullFile(",space,",",path,",",fileName,",",outputFile,") :"))
    e$jobj$printStackTrace()
    stop()
  })
  return (pulled)
}