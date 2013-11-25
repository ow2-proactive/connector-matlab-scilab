PADeleteFile <- function(space, pathname,client = .scheduler.client) {
  return(client$deleteFile(space, pathname))
}