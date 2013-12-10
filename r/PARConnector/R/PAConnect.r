PAConnect <- function(url, login, pwd, 
                      cred=NULL, .print.stack = TRUE) {
  if (missing(url)) {
    url <- readline("REST URL:")
  } 
  
  if (is.null(cred)) {
    if (missing(login)) {
      login <- readline("Login:")
    }
    if (missing(pwd)) {
      pwd <- readline("Password:")
    }
  }
    
  
  j_try_catch({
    client <- new(J("org.ow2.proactive.scheduler.rest.SchedulerClient"))
    client$init(url, login, pwd)
  } , .handler = function(e,.print.stack,.default.handler) {
    print(str_c("Error in PAConnect(",url,") :"))
    .default.handler(e,.print.stack)
  }, .print.stack = .print.stack)
     
  PAClient(client)
               
  
  return (client)
}