
#' Connects to a ProActive Scheduler
#' 
#' \code{PAConnect} connects to a running ProActive Scheduler using its url and login information. 
#' The url and login information can be provided inside the PAConnect call or asked interactively.
#' 
#'  @param url url of ProActive Scheduler 
#'  @param login login of the user
#'  @param pwd password of the user, if not provided a popup window will ask to type the password
#'  @param cred (default to NULL) the path to an encrypt credential file which stores the login information (see ProActive Scheduler manual for more details)
#'  @param .print.stack (default to TRUE) in case there is a connection problem, should the full Java stack trace be printed or simply the error message
#'  @return a scheduler connection handle, which can be used in other PARConnector functions
#'  @examples
#'  \dontrun{
#'  PAConnect("http://localhost:8080/rest/rest","demo","demo") # connects to a local ProActive Scheduler running on port 8080 with demo credentials
#'  
#'  }
#'  @seealso \code{\link{PASolve}}
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
    SchedulerClient <- J("org.ow2.proactive.scheduler.rest.SchedulerClient")
    client <- SchedulerClient$createInstance()
    client$init(url, login, pwd)
  } , .handler = function(e,.print.stack) {
    print(str_c("Error in PAConnect(",url,") :"))
    PAHandler(e,.print.stack)
  }, .print.stack = .print.stack)
     
  PAClient(client)
               
  cat("Connected to Scheduler at ",url,"\n")
  return (client)
}