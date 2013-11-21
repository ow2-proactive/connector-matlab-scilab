setClass( 
  Class="PATask", 
  representation = representation(
     javaObject = "jobjRef"
  ),
  prototype=prototype(
    javaObject = new(J("org.ow2.proactive.scheduler.common.task.ScriptTask"))    
  )
)

PATask <- function(name) {
  new (Class="PATask")
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
                   object@dependencies <- c(object@dependencies, value)
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

setMethod("show" ,"PATask" ,
          function(object) {
            cat(toString(object))                                 
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
            if (!is.null(object@javaObject$getPreScript())) {  
              output <- str_c(output,"  preScript : ",jo$getPreScript(),"\n")
            }
            if (!is.null(object@javaObject$getPostScript())) {  
              output <- str_c(output,"  postScript : ",jo$getPostScript(),"\n")
            }            
            if (!is.null(object@javaObject$getPostScript())) {  
              output <- str_c(output,"  cleanScript : ",jo$getCleanScript(),"\n")
            }
            if (object@javaObject$isRunAsMe()) {  
              output <- str_c(output,"  runAsMe : ",jo$isRunAsMe(),"\n")
            }
            if (!is.null(object@javaObject$getResultPreview())) {  
              output <- str_c(output,"  resultPreview : ",jo$getResultPreview(),"\n")
            }
            if (object@javaObject$getWallTime() > 0) {  
              output <- str_c(output,"  wallTime : ",object@wallTime,"\n")
            }
            if (is.null(object@javaObject$getDependencesList())) {      
              output <- str_c(output,"  dependencies : ")
#               for (i in 1:length(object@dependencies)) {
#                 output <- str_c(output,object@dependencies[[i]]@name)
#                 if (i < length(object@dependencies)) {
#                   output <- str_c(output,", ")
#                 }
#               }              
              output <- str_c(output,"\n")
            }
                        
            return(output)
          } 
)
