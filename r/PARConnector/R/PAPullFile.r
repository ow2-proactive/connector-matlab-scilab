PAPullFile <- function(space, pathname, outputFile, 
                       client = PAClient(), .nb.tries = 2) {
  
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
    .handler = function(e, .print.stack, .default.handler) {
      .nb.tries <<- .nb.tries - 1
      if (.nb.tries <= 0) {
        if (.print.stack) {
          print(str_c("Error in PAPullFile(",space,",",pathname,",",outputFile,") : ",e$jobj$getMessage()))
        }
        .default.handler(e, .print.stack)       
      }   
    }
    ,.print.stack = FALSE)
  }
  return (pulled)
}