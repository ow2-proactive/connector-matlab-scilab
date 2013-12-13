PADeleteFile <- function(space, pathname,
                         client = PAClient()) {
  deleted <- FALSE
  j_try_catch(
    deleted <- J(client, "deleteFile", .getSpaceName(space), pathname),     
    .handler = function(e,.print.error) {
      print(str_c("Error in PADeleteFile(",space,",",pathname,") :"))
      PAHandler(e,.print.error)
    })
  
  return (deleted)
}