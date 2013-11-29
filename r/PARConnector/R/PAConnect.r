PAConnect <- function(url, login, pwd, 
                      cred=NULL, .print.error = TRUE) {
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
    client <<- new(J("org.ow2.proactive.scheduler.rest.SchedulerClient"))
    client$init(url, login, pwd)
  } , .handler = function(e,.print.error,.default.handler) {
    print(str_c("Error in PAConnect(",url,") :"))
    .default.handler(e,.print.error)
  })
     
  .scheduler.client <<- client
               
  
  return (.scheduler.client)
}