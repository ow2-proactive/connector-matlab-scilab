PAConnect <- function(url, login, pwd, 
                      cred=NULL, .print.stack = TRUE) {
  if (missing(url)) {
    url <- readline("Scheduler REST url:")
  } 
  
  if (is.null(cred)) {
    if (missing(login)) {
      login <- readline("Login:")
    }
    if (missing(pwd)) {
      if (exists(".rs.askForPassword")) {
        pwd <- .rs.askForPassword(str_c("Password for ",login))
      } else {
        pwd <- readline("Password:")
      }
    }
  }
    
  
  j_try_catch({
    client <- new(J("org.ow2.proactive.scheduler.rest.SchedulerClient"))
    client$init(url, login, pwd)
  } , .handler = function(e,.print.stack) {
    print(str_c("Error in PAConnect(",url,") :"))
    PAHandler(e,.print.stack)
  }, .print.stack = .print.stack)
     
  PAClient(client)
               
  
  return (client)
}