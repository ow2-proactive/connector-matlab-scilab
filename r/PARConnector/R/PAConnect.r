PAConnect <- function(url, login, pwd, 
                      cred=NULL) {
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
  
  tryCatch ({
    .scheduler.client <<- new(J("org.ow2.proactive.scheduler.rest.SchedulerClient"))
    .scheduler.client$init(url, login, pwd)
  } , Exception = function(e) {
    print("Error in PAConnect(...):")
    e$jobj$printStackTrace()
    stop()
  })
  
  return (.scheduler.client)
}