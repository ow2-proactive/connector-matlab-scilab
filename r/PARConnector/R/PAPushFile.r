PAPushFile <- function(space, path, fileName, inputFile, 
                       client = .scheduler.client) {
  
  tryCatch({
     pushed <- J(client, "pushFile", .getSpaceName(space), path, fileName, inputFile)
  }, Exception = function(e) {
    print(str_c("Error in PAPushFile(",space,",",path,",",fileName,",",inputFile,") :"))
    e$jobj$printStackTrace()
    stop()
  })
  return (pushed)
}
