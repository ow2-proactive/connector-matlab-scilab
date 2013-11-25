PADeleteFile <- function(client, space, pathname) {
  return(client$deleteFile(space, pathname))
}