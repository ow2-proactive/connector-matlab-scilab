PADeleteFile <- function(space, pathname,
                         client = .scheduler.client) {
  tryCatch ({
    deleted = J(client, "deleteFile", space, pathname)
  }, Exception = function(e) {
    print("Error in PADeleteFile(...):")
    e$jobj$printStackTrace()
    stop()
  })
  return (deleted)
}