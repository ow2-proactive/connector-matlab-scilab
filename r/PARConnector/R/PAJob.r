## TODO implement all job methods

setClass( 
  Class="PAJob", 
  representation = representation(
       javaObject = "jobjRef",
       tasks = "list",
       hash = "character"            
    ),
  prototype=prototype(
    javaObject = .jnew(J("org.ow2.proactive.scheduler.common.job.TaskFlowJob")),
    tasks = list(),
    hash = "NOTSET"
  )
)

PAJob <- function() {
  object = new (Class="PAJob", javaObject = .jnew(J("org.ow2.proactive.scheduler.common.job.TaskFlowJob")), tasks = list())
  setName(object, str_c("PARJob",.peekNewSolveId()))
  setDescription(object, "ProActive R Job")
  object@hash <- .getSpaceHash()
  return(object)
}

 

setMethod("getJavaObject", "PAJob",
          function(object) {
            return(object@javaObject)                          
          } 
)

 

setReplaceMethod("addTask" ,"PAJob" ,
          function(object,value) {
            tsk <- value
            object@tasks <- c(object@tasks,tsk)
            jo = object@javaObject
            jtsk <- getJavaObject(tsk)
            if (length(tsk@inputfiles) > 0) {
              for (i in 1:length(tsk@inputfiles)) {
                pafile <- tsk@inputfiles[[i]]
                setHash(pafile)<-object@hash
                jtsk$addInputFiles(getSelector(pafile), getMode(pafile,TRUE))
              }
            }
            if (length(tsk@outputfiles) > 0) {
              for (i in 1:length(tsk@outputfiles)) {
                pafile <- tsk@outputfiles[[i]]
                setHash(pafile)<-object@hash
                jtsk$addOutputFiles(getSelector(pafile), getMode(pafile,FALSE))
              }
            }
            jo$addTask(jtsk)
            return(object)
          }
)

setMethod("getName", "PAJob",
          function(object) {
            return(object@javaObject$getName())                          
          } 
)

setMethod("setName", "PAJob",
          function(object,value) {
            return(object@javaObject$setName(value))                          
          } 
)

setMethod("getProjectName", "PAJob",
          function(object) {
            return(object@javaObject$getProjectName())                          
          } 
)

setMethod("setProjectName", "PAJob",
          function(object,value) {
            return(object@javaObject$setProjectName(value))                          
          } 
)

setMethod("getDescription", "PAJob",
          function(object) {
            return(object@javaObject$getDescription())                          
          } 
)

setMethod("setDescription", "PAJob",
          function(object,value) {
            return(object@javaObject$setDescription(value))                          
          } 
)



setMethod("toString" ,c("PAJob"),
          function(x, ...) {
            object <- x
            jo = object@javaObject
            output <- "ProActive Job\n"
            if (!is.null(jo$getName())) {     
              output <- str_c(output,"  name : ",jo$getName(),"\n")
            }
            
            if (!is.null(jo$getProjectName())) {     
              output <- str_c(output,"  projectName : ",jo$getProjectName(),"\n")
            }
            if (!is.null(jo$getDescription())) {  
              output <- str_c(output,"  description : ",jo$getDescription(),"\n")
            }
            if (!is.null(jo$getPriority())) { 
              output<- str_c(output,"  jobPriority : ",jo$getPriority()$toString(),"\n")
            }
            if (!is.null(jo$getInputSpace())) {      
              output <- str_c(output,"  inputSpace : ",jo$getInputSpace(),"\n")
            }
            if (!is.null(jo$getOutputSpace())) {
              output <- str_c(output,"  outputSpace : ",jo$getOutputSpace(),"\n")
            }
            if (!is.null(jo$getGlobalSpace())) {
              output <- str_c(output,"  globalSpace : ",jo$getGlobalSpace(),"\n")
            }
            if (!is.null(jo$getUserSpace())) {
              output <- str_c(output,"  userSpace : ",jo$getUserSpace(),"\n")
            }
            if (length(object@tasks) > 0) {      
              output <- str_c(output,"  tasks : ")
              for (i in 1:length(object@tasks)) {
                output <- str_c(output,getName(object@tasks[[i]]))
                if (i < length(object@tasks)) {
                  output <- str_c(output,", ")
                }
              }              
              output <- str_c(output,"\n")
            }
            
            if (length(object@tasks) > 0) {      
              output <- str_c(output,"  tasks contents : \n{\n")
              for (i in 1:length(object@tasks)) {
                output <- str_c(output,toString(object@tasks[[i]]))
                
                output <- str_c(output,"\n")
                
              }              
              output <- str_c(output,"}\n")
            }
            return(output)
          } 
)

setMethod("show" ,"PAJob",
          function(object) {
            cat(toString(object))                        
          } 
)

setMethod("print" ,"PAJob" ,
          function(x) {
            print(toString(x))                                 
          } 
)