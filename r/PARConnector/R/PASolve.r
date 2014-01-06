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

#' Create and submit a ProActive R Job
#' 
#' \code{PASolve} take in parameter a list of PATasks produced by \code{\link{PA}} \code{\link{PAS}} or \code{\link{PAM}} calls and submits a new job to ProActive Scheduler. 
#' 
#' a \code{\link{PAJobResult}} object will be returned. The object will bear the current state of the job, which can be displayed simply by showing or printing the object.
#' Special functions \code{\link{PAWaitFor}} and \code{\link{PAWaitAny}} can be used to wait for the results.
#' 
#'  @param ... list of PATasks produced by \code{\link{PA}} \code{\link{PAS}} or \code{\link{PAM}} calls
#'  @param client connection handle to the scheduler, if not provided the handle created by the last call to \code{\link{PAConnect}} will be used
#'  @param jobName name of the ProActive job to be created
#'  @param jobDescription description of this job
#'  @param priority priority of this job
#'  @param cancelOnError sets the cancelling mode mechanism whenever an error occur in one tasks, does it cancel the whole job ? Default to TRUE
#'  @return a \code{\link{PAJobResult}} object which acts as a placeholder for receiving actual results
#'  @examples
#'  \dontrun{
#'  
#'  > res = PASolve("cos",1:4)   # shortcut for PASolve(PA("cos",1:4)), submits a parallel job of 4 tasks
#'  Job submitted (id : 405)
#'  with tasks : t1, t2, t3, t4
#'  > res                            # display the current state
#'  PARJob1 (id: 405)  (status: Running)
#'  t1 : Pending
#'  t2 : Running at 192.168.1.187 (local-LocalNodes-0) (0%)
#'  t3 : Running at 192.168.1.187 (local-LocalNodes-2) (0%)
#'  t4 : Pending
#'  > PAWaitFor(res)                 # wait for the results and return them in a list
#'  $t1
#'  [1] 0.5403023
#'
#'  $t2
#'  [1] -0.4161468
#'
#'  $t3
#'  [1] -0.9899925
#'
#'  $t4
#'  [1] -0.6536436
#'  
#'  
#'  
#'  > res = PASolve(PAM("sum",
#'                  PA(function(x) {x*x},
#'                    PAS("identity", 1:4))))         # submits a split/merge job of six tasks
#'                    
#'  > res
#'  PARJob2 (id: 406)  (status: Running)
#'  t1 : Running at 192.168.1.187 (local-LocalNodes-0) (0%)
#'  t2 : Pending
#'  t3 : Pending
#'  t4 : Pending
#'  t5 : Pending
#'  t6 : Pending
#'  
#'  > PAWaitFor(res)        # wait for the results and return them in a list
#'  $t1
#'  [1] 1 2 3 4
#'  
#'  $t2
#'  [1] 1
#'  
#'  $t3
#'  [1] 4
#'  
#'  $t4
#'  [1] 9
#'  
#'  $t5
#'  [1] 16
#'  
#'  $t6
#'  [1] 30
#'  }
#'  @seealso  \code{\link{PA}} \code{\link{PAS}} \code{\link{PAM}} \code{\link{PAJobResult}} \code{\link{PAConnect}}
PASolve <- function(..., client = PAClient(), .debug = PADebug(), jobName = str_c("PARJob",.peekNewSolveId()) , jobDescription = "ProActive R Job", priority = "normal", cancelOnError = TRUE) {  
  
  dots <- list(...)
  
  if (length(dots) == 0) {
    stop("Not enough arguments")
  }
  
  if (client == NULL || is.jnull(client) ) {
    stop("You are not currently connected to the scheduler, use PAConnect")
  } 
  
  
  cl <- class(dots[[1]])
  if ((cl == "function") || (cl == "character")) {
    # simplified syntax (a simple parametric sweep) => rebuild a new call
    answer <- do.call("PASolve",list(do.call("PA",dots,envir=parent.frame())), envir=parent.frame())
    return (answer)
  }    
  
  jobresult <- tryCatch (
{  
  .peekNewSolveId()
  job <- PAJob(jobName, jobDescription)
  setPriority(job, priority)
  setCancelJobOnError(job, cancelOnError)
  task.names <- NULL
  all.tasks <- list()
  
  for (i in 1:length(dots)) {
    tasklist <- dots[[i]]
    
    for (j in 1:length(tasklist)) {
      .compute.task.dependencies(tasklist[[j]],environment())    
    }
  }
  # sort tasks by their names
  unordered <- unlist(lapply(task.names, function(x)strtoi(str_sub(x,2))))
  new.indexes <- sort(unordered,index.return=TRUE)
  task.names <- task.names[new.indexes[["ix"]]]
  all.tasks <- all.tasks[new.indexes[["ix"]]]
  
  for (i in 1:length(all.tasks)) {
    addTask(job) <- all.tasks[[i]]
  }
  
  if (.debug) {
    print("Submitting job : ")
    cat(toString(job))
  }
  jobid <- j_try_catch(client$submit(getJavaObject(job)))
  cat(str_c("Job submitted (id : ",jobid$value(),")","\n"," with tasks : ",toString(task.names),"\n"))
  
  jobresult <- PAJobResult(job, jobid$value(),  task.names, client)  
  PALastResult(jobresult)
  return(jobresult)
}, finally = {
  .commitNewSolveId()
})
  
};