setClass( 
  Class="PAJob", 
  representation = representation(
       javaObject = "jobjRef",
       tasks = "list"
       
    ),
  prototype=prototype(
    javaObject = .jnew(J("org.ow2.proactive.scheduler.common.job.TaskFlowJob")),
    tasks = list()
  )
)

PAJob <- function() {
  new (Class="PAJob", javaObject = .jnew(J("org.ow2.proactive.scheduler.common.job.TaskFlowJob")))
}

 

setMethod("getJavaObject", "PAJob",
          function(object) {
            return(object@javaObject)                          
          } 
)

 

setReplaceMethod("addTask" ,"PAJob" ,
          function(object,value) {
            object@tasks <- c(object@tasks,value)
            jo = object@javaObject
            jo$addTask(getJavaObject(value))
            return(object)
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
            if (length(jo$getTasks()) > 0) {      
              output <- str_c(output,"  tasks : ")
              for (i in 1:length(object@tasks)) {
                output <- str_c(output,getName(object@tasks[[i]]))
                if (i < length(object@tasks)) {
                  output <- str_c(output,", ")
                }
              }              
              output <- str_c(output,"\n")
            }
            output <- str_c(output,"\n")
            return(output)
          } 
)

setMethod("show" ,"PAJob",
          function(object) {
            cat(toString(object))                        
          } 
)

.cat_list <- function(ll) {
  cat(.toString_list(ll))  
}

.toString_list <- function(ll) {
  output <- t("{ ")
  for (k in 1:length(ll)) {
    output <- str_c(output,toString(ll[[k]])," ")
    if (k < length(ll) ) {        
      output <- str_c(output,",")     
    }
  }
  output <- str_c(output,"}")
  return(output)
}