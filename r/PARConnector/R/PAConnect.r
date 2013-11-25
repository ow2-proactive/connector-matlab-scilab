PAConnect <- function(url, login, pwd, cred=NULL) {  
  # if(exists('client') && !is.null(client)) {
  #  .jcall(client,"V","disconnect")
  #}
  client <- .jnew("org.ow2.proactive.scheduler.rest.SchedulerClient", check = TRUE)
  if (is.null(cred)) {
    .jcall(client,"S","init", url, login, pwd, check = TRUE)
  } else {
    print("using cred file")  
  }  
  return(client)
}