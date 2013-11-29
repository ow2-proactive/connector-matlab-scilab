PADeleteFile <- function(space, pathname,
                         client = .scheduler.client) {
  deleted <- FALSE
  j_try_catch(
    deleted <- J(client, "deleteFile", .getSpaceName(space), pathname),     
    .handler = function(e,.print.error,.default.handler) {
      print(str_c("Error in PADeleteFile(",space,",",pathname,") :"))
      .default.handler(e,.print.error)
    })
  
  return (deleted)
}