## TODO implement all task methods

setClass( 
  Class="PATask", 
  representation = representation(
     javaObject = "jobjRef",
     dependencies = "list",
     inputfiles = "list",
     outputfiles = "list"
  ),
  prototype=prototype(
    javaObject = new(J("org.ow2.proactive.scheduler.common.task.ScriptTask")),
    dependencies = list(),
    inputfiles = list(),
    outputfiles = list()
  )
)

PATask <- function(name) {
  tsk <- new (Class="PATask", javaObject = new(J("org.ow2.proactive.scheduler.common.task.ScriptTask")), dependencies = list())
  setName(tsk,name)
  return (tsk)
}


setMethod("getJavaObject", "PATask",
          function(object) {
            return(object@javaObject)                          
          } 
)

setMethod("getName", "PATask",
          function(object) {
            return(object@javaObject$getName())                          
          } 
)

setMethod("setName", "PATask",
          function(object,value) {
            return(object@javaObject$setName(value))                          
          } 
)

setMethod("getScript", "PATask",
          function(object) {
            return(object@javaObject$getScript())                          
          } 
)



setMethod("setScript", "PATask",
          function(object,value) {
            s_clz = J("org.ow2.proactive.scripting.Script")
            sscript = new(J("org.ow2.proactive.scripting.SimpleScript"),value,"parscript")         
            tscript = new(J("org.ow2.proactive.scripting.TaskScript"),sscript)            
            return(object@javaObject$setScript(tscript))                          
          } 
)



setReplaceMethod("addDependency" ,"PATask" ,
                 function(object,value) {
                   object@dependencies <- c(object@dependencies,value)
                   jo = object@javaObject
                   jo$addDependence(getJavaObject(value))
                   return(object)
                 }
)



setReplaceMethod("addInputFiles" ,"PATask" ,
                 function(object, value) {
                   if (class(value) != "PAFile") {
                     stop("unexpected argument class, expected PAFile, received ",class(value))
                   }
                   len <- length(object@inputfiles)
                   object@inputfiles[[len+1]] <- value
                   return(object)
                 }
)


setReplaceMethod("addOutputFiles" ,"PATask" ,
                 function(object,value) {
                   if (class(value) != "PAFile") {
                     stop("unexpected argument class, expected PAFile, received ",class(value))
                   }
                   len <- length(object@outputfiles)
                   object@outputfiles[[len+1]] <- value
                   return(object)
                 }
)

setReplaceMethod("addSelectionScript" ,"PATask" ,
                 function(object,value) {
                   object@selectionScripts <- c(object@selectionScripts, value)
                   return(object)
                 }
)


setMethod("toString","PATask",
  function(x) {
            object <- x
            output <- "ProActive Task\n"  
            jo = object@javaObject
            if (!is.null(jo$getName())) {     
              output <- str_c(output,"  name : ",jo$getName(),"\n")
            }
            if (!is.null(jo$getDescription())) {  
              output <- str_c(output,"  description : ",jo$getDescription(),"\n")
            }
            if (!is.null(jo$getScript())) {     
              output <- str_c(output,"  R script : ",jo$getScript()$toString(),"\n")
            }
            
            if (length(object@inputfiles) > 0) {      
              output <- str_c(output,"  input files : ")
              for (i in 1:length(object@inputfiles)) {
                output <- str_c(output,toString(object@inputfiles[[i]]))
                if (i < length(object@inputfiles)) {
                  output <- str_c(output,", ")
                }
              }       
              output <- str_c(output,"\n")
            }
            
            if (length(object@outputfiles) > 0) {
              output <- str_c(output,"  output files : ")
              for (i in 1:length(object@outputfiles)) {
                output <- str_c(output,toString(object@outputfiles[[i]], input=FALSE))
                if (i < length(object@outputfiles)) {
                  output <- str_c(output,", ")
                }
              }       
              output <- str_c(output,"\n")
            }
                        
            if (!is.null(jo$getSelectionScripts())) {      
              output <- str_c(output,"  selectionScripts : ")
              output <- str_c(output,jo$getSelectionScripts()$toString())   
              output <- str_c(output,"\n")
            }
            if (!is.null(jo$getPreScript())) {  
              output <- str_c(output,"  preScript : ",jo$getPreScript(),"\n")
            }
            if (!is.null(jo$getPostScript())) {  
              output <- str_c(output,"  postScript : ",jo$getPostScript(),"\n")
            }            
            if (!is.null(jo$getCleaningScript())) {  
              output <- str_c(output,"  cleanScript : ",jo$getCleaningScript(),"\n")
            }
            if (jo$isRunAsMe()) {  
              output <- str_c(output,"  runAsMe : ",jo$isRunAsMe(),"\n")
            }
            if (!is.null(jo$getResultPreview())) {  
              output <- str_c(output,"  resultPreview : ",jo$getResultPreview(),"\n")
            }
            if (jo$getWallTime() > 0) {  
              output <- str_c(output,"  wallTime : ",jo$getWallTime(),"\n")
            }
            if (length(object@dependencies) > 0) {      
              output <- str_c(output,"  dependencies : ")
              for (i in 1:length(object@dependencies)) {
                output <- str_c(output,getName(object@dependencies[[i]]))
                if (i < length(object@dependencies)) {
                  output <- str_c(output,", ")
                }
              }       
              output <- str_c(output,"\n")
            }
                        
            return(output)
          } 
)

setMethod("show" ,"PATask" ,
          function(object) {
            cat(toString(object))                                 
          } 
)

setMethod("print" ,"PATask" ,
          function(x) {
            print(toString(x))                                 
          } 
)
