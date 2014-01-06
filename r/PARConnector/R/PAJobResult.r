setClass( 
  Class="PAJobResult", 
  representation = representation(
    job = "PAJob",
    job.id = "character",
    task.names = "character",
    client = "jobjRef",
    results = "jobjRef"
  ) ,
  prototype=prototype(
    job = NULL,
    job.id = "-1",
    task.names = "",
    client = NULL,
    results = NULL
  )
)



PAJobResult <- function(job,jid,tnames, client) {
  
  results =  new(J("java.util.HashMap"))
  
  new (Class="PAJobResult" , job = job, job.id = jid, task.names = tnames, client=client, results = results)
}

setMethod(
  f="[",
  signature="PAJobResult",
  definition = function(x,i,j,drop) {
      if (is.numeric(i)) {
        selected.names <- x@task.names[i]          
      } else if (is.character(i)) {
        if (! is.element(i,x@task.names)) {
          stop("Unknown task : ",i)
        }
        selected.names <- i    
      }          
      
      return (new (Class="PAJobResult" , job = x@job, job.id = x@job.id, task.names = selected.names, client = x@client, results = x@results))
  }
)

setMethod(
  f="[[",
  signature="PAJobResult",
  definition = function(x,i,j,drop) {
    return(x[i])
  }
)

.getLogsFromJavaResult <- function(paresult, tresult) {              
  jlogs <- tresult$getOutput()         
  logs <- jlogs$getAllLogs(TRUE)
  
  return(logs)
}

.getRResultFromJavaResult <- function(paresult, tresult, i, callback) {
  if(tresult$hadException()) {  
    return(simpleError(tresult$value()))               
  } else {
    # transferring output files
    tasks <- paresult@job@tasks
    
    outfiles <- tasks[[paresult@task.names[i]]]@outputfiles
    if (length(outfiles) > 0) {
      for (j in 1:length(outfiles)) {
        pafile <- outfiles[[j]]
        if (isFileTransfer(pafile)) {
          pullFile(pafile, client = paresult@client)
        }
      }
    }
    
    
    jobj <- tresult$value()              
    if (class(jobj) == "jobjRef") {               
      rexp <- J("org.rosuda.jrs.RexpConvert")$jobj2rexp(jobj)                
      eng <- .jengine()                
      eng.assign("tmpoutput",rexp)   
      if (!is.null(callback)) {
        return(callback(tmpoutput))
      }
      else {
        return(tmpoutput)
      }
    } else {    
      if (!is.null(callback)) {
        return(callback(jobj))
      }
      else {
        return(jobj)
      }
    } 
  }
  
}

setClassUnion("PAJobResultOrMissing", c("PAJobResult", "missing"))

.getAvailableResults <- function(paresult, callback) {
  results <- list()
  tnames <- paresult@task.names
  for (i in 1:length(tnames)) {  
    tresult <- paresult@results$get(tnames[i])
    
    
    if (!is.null(tresult)) {
      log <- .getLogsFromJavaResult(paresult, tresult)
      if (!is.null(log) && !(log == "")) {
        cat(str_c(tnames[i], " : "))
        cat("\n")
        cat(log)
        cat("\n")
      }
      result <- .getRResultFromJavaResult(paresult, tresult, i, callback)
      results[[tnames[i]]] <- result
    }
  }
  return(results)
}


setMethod("PAWaitFor","PAJobResultOrMissing", function(paresult = PALastResult(), timeout = .Machine$integer.max, client = PAClient(), callback = NULL) {
            
            if (client == NULL || is.jnull(client) ) {
              stop("You are not currently connected to the scheduler, use PAConnect")
            }             
            
            tnames <- paresult@task.names            
            
            selected.names <- NULL
            task.list <- .jnew(J("java.util.ArrayList"))
            
            
            for (i in 1:length(tnames)) {
              if (is.null(paresult@results$get(tnames[i]))) {
                selected.names <- c(selected.names,tnames[i])
                task.list$add(tnames[i])
              }
            }       
            if (task.list$size() > 0) {
              tryCatch ({
                listentry <- client$waitForAllTasks(paresult@job.id,task.list,.jlong(timeout))
              } , Exception = function(e) {
                e$jobj$printStackTrace()
                stop()
              })           
              
              for (i in 0:(task.list$size()-1)) {              
                entry <- listentry$get(as.integer(i))             
                tresult <- entry$getValue()  
                
                paresult@results$put(selected.names[i+1], tresult)                                                
              }
            }
            
            return (.getAvailableResults(paresult, callback))            
            
          }
)

setMethod("PAWaitAny","PAJobResultOrMissing", function(paresult = PALastResult(), timeout = .Machine$integer.max, client = PAClient(), callback = NULL) {
  
  if (client == NULL || is.jnull(client) ) {
    stop("You are not currently connected to the scheduler, use PAConnect")
  }             
  
  tnames <- paresult@task.names            
  
  selected.names <- NULL
  task.list <- .jnew(J("java.util.ArrayList"))
  
  
  for (i in 1:length(tnames)) {
    if (is.null(paresult@results$get(tnames[i]))) {
      selected.names <- c(selected.names,tnames[i])
      task.list$add(tnames[i])
    }
  }       
  if (task.list$size() > 0) {
    tryCatch ({
      entry <- client$waitForAnyTask(paresult@job.id,task.list,.jlong(timeout))
    } , Exception = function(e) {
      e$jobj$printStackTrace()
      stop()
    })      
    
    tname <- entry$getKey()
    tresult <- entry$getValue() 
    
    paresult@results$put(tname, tresult)   
    res <- .getAvailableResults(paresult, callback)
    return(res[tname])
  }
  return(NA)
  
  
}
)


setMethod("toString","PAJobResult",
          function(x, ...) {
            object <- x 
            output.list <- list()
            
            job.name = getName(object@job)
            state <- object@client$getJobState(object@job.id)
            task.states.list <- state$getTasks()
            output <- str_c(job.name," (id: ",object@job.id,") "," (status: ",state$getStatus()$toString(),")","\n")            
            if (task.states.list$size() > 0) {              
              for (i in 0:(task.states.list$size()-1)) {
                task.state <- task.states.list$get(as.integer(i))
                if (is.element(task.state$getName(), object@task.names)) {
                  taskindex <- strtoi(str_sub(task.state$getName(),2))
                  status <- task.state$getStatus()$toString()
                  if (status == "Running") {
                    output.list[[taskindex]] <- str_c(task.state$getName(), " : ",  task.state$getStatus()$toString(), " at ",task.state$getTaskInfo()$getExecutionHostName()," (",task.state$getProgress(),"%)")
                  } else if (status == "Finished") {
                    date <- new(J("java.util.Date"),.jlong(task.state$getTaskInfo()$getFinishedTime()))
                    output.list[[taskindex]] <- str_c(task.state$getName(), " : ",  task.state$getStatus()$toString(), " at ", date$toString())
                  } else {
                    output.list[[taskindex]] <- str_c(task.state$getName(), " : ",  task.state$getStatus()$toString())
                  }
                  
                }
              }
              for (i in 1:length(output.list)) {
                output <- str_c(output,output.list[[i]],"\n")
              }
            }
            return(output)
          }
)

setMethod("show" ,"PAJobResult" ,
          function(object) {
            cat(toString(object))                                 
          } 
)
