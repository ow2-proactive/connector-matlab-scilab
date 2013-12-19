setClass( 
  Class="PAJobResult", 
  representation = representation(
    job = "PAJob",
    job.id = "character",
    task.names = "character",
    client = "jobjRef"
  )  
)

PAJobResult <- function(job,jid,tnames, client) {
  new (Class="PAJobResult" , job = job, job.id = jid, task.names = tnames, client=client)
}

setMethod(
  f="[",
  signature="PAJobResult",
  definition = function(x,i,j,drop) {
      if (is.numeric(i)) {
        selected.names <- x@task.names[i]        
      } else if (is.character(i)) {
        selected.names <- i    
      }
      return (new (Class="PAJobResult" , job = x@job, job.id = x@job.id, task.names = selected.names, client=x@client))
  }
)

setMethod(
  f="[[",
  signature="PAJobResult",
  definition = function(x,i,j,drop) {
    if (is.numeric(i)) {
      selected.names <- x@task.names[i]        
    } else if (is.character(i)) {
      selected.names <- i    
    }
    return (new (Class="PAJobResult" , job = x@job, job.id = x@job.id, task.names = selected.names, client=x@client))
  }
)

setClassUnion("PAJobResultOrMissing", c("PAJobResult", "missing"))

setMethod("PAWaitFor","PAJobResultOrMissing", function(paresult = .last.result, timeout = .Machine$integer.max, client = PAClient(), callback = identity) {
            
            if (client == NULL || is.jnull(client) ) {
              stop("You are not currently connected to the scheduler, use PAConnect")
            }             
            
            tnames <- paresult@task.names
            task.list <- .jnew(J("java.util.ArrayList"))
            for (i in 1:length(tnames)) {
              task.list$add(tnames[i])
            }           
            tryCatch ({
              listentry <- client$waitForAllTasks(paresult@job.id,task.list,.jlong(timeout))
            } , Exception = function(e) {
              e$jobj$printStackTrace()
              stop()
            })           
            answer <- list()
            for (i in 1:length(tnames)) {              
              entry <- listentry$get(as.integer(i-1))             
              tresult <- entry$getValue()  
              # print logs                 
              jlogs <- tresult$getOutput()         
              logs <- jlogs$getAllLogs(TRUE)
              if (!is.null(logs)) {
                # cat(str_c(tnames[i], " : ","\n"))
                cat(logs)
                cat("\n")
              }
              
              if(tresult$hadException()) {     
                answer[[tnames[i]]] <- simpleError(tresult$value())
              } else {
                # transferring output files
                tasks <- paresult@job@tasks
                
                outfiles <- tasks[[i]]@outputfiles
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
                  answer[[tnames[i]]] <- callback(tmpoutput)
                } else {                              
                  answer[[tnames[i]]] <- callback(jobj)      
                } 
              }
            }
            return(answer)
            
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
