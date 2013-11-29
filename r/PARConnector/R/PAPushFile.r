PAPushFile <- function(space, path, fileName, inputFile, 
                       client = .scheduler.client) {
  
  if (client == NULL || is.jnull(client) ) {
    stop("You are not currently connected to the scheduler, use PAConnect")
  } 
  pushed <- FALSE
  
  j_try_catch(
    pushed <- J(client, "pushFile", .getSpaceName(space), path, fileName, inputFile),     
    .handler = function(e,.print.error,.default.handler) {
    print(str_c("Error in PAPushFile(",space,",",path,",",fileName,",",inputFile,") :"))
    .default.handler(e,.print.error)
  })
  return (pushed)
}
