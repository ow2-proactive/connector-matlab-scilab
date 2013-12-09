.compute.task.dependencies <- function(task,envir) {
  tname <- getName(task)
  task.names <- get("task.names",envir)
  all.tasks <- get("all.tasks",envir)
  if (!is.element(tname,task.names)) {
    task.names <- c(task.names,tname)
    all.tasks <- c(all.tasks, task)
    assign("task.names",task.names,envir)
    assign("all.tasks",all.tasks,envir)
    deps <- getDependencies(task)
    if (length(deps) > 0) {
      for (ii in 1:length(deps)) {
        .compute.task.dependencies(deps[[ii]],envir)
      }
    }
  }
}

PASolve <- function(tasklist, client = .scheduler.client, .debug = PADebug()) {  
  jobresult <- tryCatch (
{
  .peekNewSolveId()
  job <- PAJob()
  task.names <- NULL
  all.tasks <- list()
  for (i in 1:length(tasklist)) {
    .compute.task.dependencies(tasklist[[i]],environment())    
  }
  
  for (i in 1:length(all.tasks)) {
    addTask(job) <- all.tasks[[i]]
  }
  
  if (.debug) {
    print("Submitting job : ")
    cat(toString(job))
  }
  jobid <- j_try_catch(client$submit(getJavaObject(job)))
  cat(str_c("Job submitted (id : ",jobid$value(),")","\n"))
  
  jobresult <- PAJobResult(job, jobid$value(),  task.names, client)
  return(jobresult)
}, finally = {
  .commitNewSolveId()
})
  return(jobresult)
};