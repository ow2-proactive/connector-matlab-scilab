
formatted_date <- function(datetime) {
  return (J("org.ow2.proactive.utils.Tools")$getFormattedDate(.jlong(datetime)))
}

formatted_duration <- function(start, end) {
  return (J("org.ow2.proactive.utils.Tools")$getFormattedDuration(.jlong(start), .jlong(end)))
}

js_get_job_data <- function(job.info, job.state) {
  return (c(
    paste("Job: ", job.info$getJobId()$value()),
    paste("name: ", job.info$getJobId()$getReadableName()),
    paste("owner: ", job.state$getOwner()),
    paste("status: ", job.info$getStatus()$name()),
    paste("#tasks: ", job.info$getTotalNumberOfTasks())))
}

js_print_job_data <- function(job.data) {
  matrix <- matrix(unlist(job.data), nrow = 1, byrow = T)
  write.table(matrix, "", quote = F, row.name = F, col.names = F)
}

get_task_data <- function(job.state) {
  task.states <- job.state$getTasks()
  return (sapply(task.states, function(task.state) {
    id <- task.state$getId()$value()
    name <- task.state$getName()
    if (task.state$getIterationIndex() > 0) {
      iindex <- ""
    } else {
      iindex <- task.state$getIterationIndex()
    }
    if (task.state$getReplicationIndex() > 0) {
      rindex <- ""
    } else {
      rindex <- task.state$getReplicationIndex()
    }
    status = task.state$getStatus()$name()
    if (is.null(task.state$getExecutionHostName())) {
      host = "UNKNOWN"
    } else {
      host = task.state$getExecutionHostName()
    }
    elapsed <- formatted_duration(0, task.state$getExecutionDuration())
    duration <- formatted_duration(task.state$getFinishedTime(), task.state$getStartTime())
    nodes <- task.state$getNumberOfNodesNeeded()
    
    max <- task.state$getMaxNumberOfExecution()
    remain <- task.state$getNumberOfExecutionLeft()
    executed <- max - remain
    if (executed < max) {
      executions <- paste((executed + 1), max, sep="/")
    } else {
      executions <- paste(executed, max, sep="/")
    }
    max.failures <- task.state$getMaxNumberOfExecution()
    max.failures.left <- task.state$getNumberOfExecutionOnFailureLeft()
    nodes.killed <- paste((max.failures - max.failures.left), max.failures, sep="/")    
    
    return (c(id = id, name = name, iindex = iindex, rindex = rindex, 
              status = status, host = host, elapsed = elapsed, 
              duration = duration, nodes = nodes, executions = executions,
              nodes.killed = nodes.killed))
  }))
}

print_task_data <- function(task.info)  {
  matrix <- matrix(unlist(task.info), nrow = length(task.info) /11, byrow = T)
  write.table(matrix, "", quote = F, row.name = F, col.names = 
                c("ID", "NAME", "ITER", "DUP", "STATUS", "HOSTNAME", 
                  "EXEC DURATION", "TOT DURATION", "#NODES USED", "#EXECUTIONS",
                  "#NODES KILLED"))
}

PAJobState <- function(job.id, 
                       client =  PAClient()) {
  
  if (client == NULL || is.jnull(client) ) {
    stop("You are not currently connected to the scheduler, use PAConnect")
  } 
  
  
  job.state <- j_try_catch ({
    return (J(client,"getJobState", job.id));
  })
  job.info <- job.state$getJobInfo()
  
  js_print_job_data(js_get_job_data(job.info, job.state))
  print_task_data(get_task_data(job.state))
}


