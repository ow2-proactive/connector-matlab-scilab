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

setMethod("toString","PAJobResult",
          function(x, ...) {
            object <- x                
            job.name = getName(object@job)
            state <- object@client$getJobState(object@job.id)
            task.states.list <- state$getTasks()
            output <- str_c(job.name," (id: ",object@job.id,") "," (status: ",state$getStatus()$toString(),")","\n")
            
            if (task.states.list$size() > 0) {
              print(task.states.list$size())
              for (i in 0:(task.states.list$size()-1)) {
                task.state <- task.states.list$get(as.integer(i))
                output <- str_c(output, task.state$getName(), " : ",  task.state$getStatus()$toString(), " (",task.state$getProgress(),"%)","\n")
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
