
PAGetResult <- function(job.id, 
                        client = .scheduler.client) {
  job.state <- j_try_catch({
    return (J(client, "getJobState", job.id))
  })
  task.states <- job.state$getTasks()
  
  rjob <- PAJob()
  setName(rjob, job.state$getName())
  sapply(task.states, function(task.state){
    name <- task.state$getName()
    rtask <- PATask(name=name)
    addTask(rjob) <- rtask
  })
  
  task.names <- sapply(task.states, function(task.state) {
    return (task.state$getName())
  })
  
  rjob.result <- PAJobResult(rjob, job.id, task.names, client)
  return (rjob.result)
}