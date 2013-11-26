## TODO implement all task methods

setClass( 
  Class="PATask", 
  representation = representation(
     javaObject = "jobjRef",
     dependencies = "list"
  ),
  prototype=prototype(
    javaObject = new(J("org.ow2.proactive.scheduler.common.task.ScriptTask")),
    dependencies = list()
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
                   object@tasks <- c(object@tasks,value)
                   jo = object@javaObject
                   jo$addDependence(getJavaObject(value))
                   return(object)
                 }
)



setReplaceMethod("addInputFiles" ,"PATask" ,
                 function(object,value) {
                   len <- length(object@inputFiles)
                   object@inputFiles[[len+1]] <- value
                   return(object)
                 }
)


setReplaceMethod("addOutputFiles" ,"PATask" ,
                 function(object,value) {
                   len <- length(object@outputFiles)
                   object@outputFiles[[len+1]] <- value
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
            
            if (!is.null(jo$getInputFilesList())) {      
              output <- str_c(output,"  inputFiles : ")
              output <- str_c(output,jo$getInputFiles()$toString())
              output <- str_c(output,"\n")
            }
            if (!is.null(jo$getOutputFilesList())) {      
              output <- str_c(output,"  outputFiles : ")
              output <- str_c(output,jo$getOutputFiles()$toString())  
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
            if (o$isRunAsMe()) {  
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
