
PAGetResult <- function(job.id, 
                        client = PAClient()) {
  if (client == NULL || is.jnull(client) ) {
    stop("You are not currently connected to the scheduler, use PAConnect")
  } 
  
  
  job.state <- j_try_catch({
    return (J(client, "getJobState", toString(job.id)))
  })
  task.states <- job.state$getTasks()
  
  rjob <- PAJob(job.state$getName(),"ProActive R Job") 
  for (i in 1:task.states$size()) {
    task.state <- task.states$get(as.integer(i-1))
    name <- task.state$getName()
    rtask <- PATask(name=name)
    addTask(rjob) <- rtask
  }
  
  task.names <- sapply(task.states, function(task.state) {
    return (task.state$getName())
  })
  
  rjob.result <- PAJobResult(rjob, toString(job.id), task.names, client)
  return (rjob.result)
}