PAPullFile <- function(space, pathname, outputFile, 
                       client = .scheduler.client, .nb.tries = 2) {
  
  if (client == NULL || is.jnull(client) ) {
    stop("You are not currently connected to the scheduler, use PAConnect")
  } 
  
  pulled <- FALSE
  
  while(.nb.tries > 0) {    
    j_try_catch(
    {
      pulled <- J(client, "pullFile", .getSpaceName(space),pathname, outputFile)
      .nb.tries <- 0      
    },
    .handler = function(e, .print.error, .default.handler) {
      .nb.tries <<- .nb.tries - 1
      if (.nb.tries <= 0) {
        print(str_c("Error in PAPullFile(",space,",",pathname,",",outputFile,") :"))
        .default.handler(e, .print.error)       
      }   
    }
    )
  }
  return (pulled)
}