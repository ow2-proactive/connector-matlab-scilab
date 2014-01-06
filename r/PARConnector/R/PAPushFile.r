
#' Transfer a file from the local machine to a ProActive Data space
#' 
#' \code{PAPushFile} will copy a local file to a shared data space available to a ProActive Scheduler. The Scheduler controls two main spaces :\cr
#' 
#' \itemize{
#'      \item{The USER Space}{ : a data space reserved for a specific user.}
#'      \item{The GLOBAL Space}{ : a data space accessible to all users.}
#'  } 
#' 
#'  @param space name of the data space to transfer the file to 
#'  @param path path inside the remote data space where the file will be copied to
#'  @param fileName name of the file that will be created in the remote data space
#'  @param inputFile local path of the file
#'  @param client connection handle to the scheduler, if not provided the handle created by the last call to \code{\link{PAConnect}} will be used
#'  @seealso  \code{\link{PAPullFile}}
#'  @examples
#'  \dontrun{
#'  PAPushFile("USER","/","in.txt", "in.txt") # will transfer local file in.txt to the USER space
#'  }
PAPushFile <- function(space, path, fileName, inputFile, 
                       client = PAClient(), .print.stack = TRUE) {
  
  if (client == NULL || is.jnull(client) ) {
    stop("You are not currently connected to the scheduler, use PAConnect")
  } 
  pushed <- FALSE
  
  j_try_catch(
    pushed <- J(client, "pushFile", .getSpaceName(space), path, fileName, inputFile),     
    .handler = function(e,.print.stack) {
      if (.print.stack) {
        print(str_c("Error in PAPushFile(",space,",",path,",",fileName,",",inputFile,") : ", e$jobj$getMessage()))
      }
    PAHandler(e,.print.stack)
  },.print.stack = .print.stack)
  return (pushed)
}
