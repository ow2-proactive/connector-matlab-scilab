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

setMethod("PAWaitFor","PAJobResult", 
          function(paresult, timeout = .Machine$integer.max, client = .scheduler.client, callback = identity) {
            tnames <- paresult@task.names
            task.list <- .jnew(J("java.util.ArrayList"))
            for (i in 1:length(tnames)) {
              task.list$add(tnames[i])
            }
            print("l1")
            tryCatch ({
            listentry <- client$waitForAllTasks(paresult@job.id,task.list,.jlong(timeout))
            } , Exception = function(e) {
              e$jobj$printStackTrace()
              stop()
            })
            print("l2")
            answer <- list()
            for (i in 1:length(tnames)) {
              print(strc_c("l3_",i))
              tresult <- listentry$get(i-1)$getValue()
              print(strc_c("l4_",i))
              jobj <- tresult$value()
              
              answer[[i]] <- 
              print(strc_c("l5_",i))
            }
            return(answer)
            
          }
)


setMethod("toString","PAJobResult",
          function(x, ...) {
            object <- x                
            job.name = getName(object@job)
            state <- object@client$getJobState(object@job.id)
            task.states.list <- state$getTasks()
            output <- str_c(job.name," (id: ",object@job.id,") "," (status: ",state$getStatus()$toString(),")","\n")            
            if (task.states.list$size() > 0) {              
              for (i in 0:(task.states.list$size()-1)) {
                task.state <- task.states.list$get(as.integer(i))
                if (is.element(task.state$getName(), object@task.names)) {             
                  output <- str_c(output, task.state$getName(), " : ",  task.state$getStatus()$toString(), " (",task.state$getProgress(),"%)","\n")
                }
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
