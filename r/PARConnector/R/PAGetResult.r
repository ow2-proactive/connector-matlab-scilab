
PAGetResult <- function(job.id, 
                        client = .scheduler.client) {
  job.state <- j_try_catch({
    return (J(client, "getJobState", job.id))
  })
  task.states <- job.state$getTasks()
  task.names <- sapply(task.states, function(task.state) {
    return (task.state$getName())
  })
  rjob <- PAJob()
  setName(rjob, job.state$getName())
  rjob.result <- PAJobResult(rjob, job.id, task.names, client)
  return (rjob.result)
}