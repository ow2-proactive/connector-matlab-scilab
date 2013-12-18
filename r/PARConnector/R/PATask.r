## TODO implement all task methods

setClass( 
  Class="PATask", 
  representation = representation(
     javaObject = "jobjRef",
     dependencies = "list",
     inputfiles = "list",
     outputfiles = "list",
     scatter.index = "numeric",
     file.index = "numeric", 
     file.index.function = "function"
  ),
  prototype=prototype(
    javaObject = new(J("org.ow2.proactive.scheduler.common.task.ScriptTask")),
    dependencies = list(),
    inputfiles = list(),
    outputfiles = list(),
    scatter.index = 0,
    file.index = 0,
    file.index.function = toString
  )
)

PATask <- function(name, scatter.index = 0, file.index = 0, file.index.function = toString) {  
  tsk <- new (Class="PATask", javaObject = new(J("org.ow2.proactive.scheduler.common.task.ScriptTask")), dependencies = list(), scatter.index = scatter.index, file.index = file.index, file.index.function = file.index.function)
  setName(tsk,name)
  return (tsk)
}

PACloneTaskWithIndex <- function(task, scatter.index, file.index, file.index.function = toString) {  
  tsk <- new (Class="PATask", javaObject = task@javaObject, dependencies = task@dependencies, inputfiles =  task@inputfiles, outputfiles = task@outputfiles, scatter.index = scatter.index, file.index = file.index, file.index.function = file.index.function)
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
            sscript = new(J("org.ow2.proactive.scripting.SimpleScript"),value,"parscript") 
            tscript = new(J("org.ow2.proactive.scripting.TaskScript"),sscript)            
            return(object@javaObject$setScript(tscript))                          
          } 
)

setMethod("getQuoteExp", "PATask",
          function(object) {
            if (object@scatter.index == 0) {
              return(bquote(results[[.(getName(object))]]))
            } else {
              return(bquote(results[[.(getName(object))]][[.(object@scatter.index)]]))                       
            }
          } 
)

setMethod("getFileIndex", "PATask",
          function(object) {
            if (object@file.index == 0) {
              return("")
            } else {
              return(object@file.index.function(object@file.index))
            }
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
setMethod("getDependencies" ,"PATask" ,
                 function(object) {
                   return(object@dependencies)
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

setMethod("addSelectionScript" ,"PATask" ,
                 function(object,value,engine,is.dynamic) {          
                   sscript = new(J("org.ow2.proactive.scripting.SelectionScript"),value,engine,is.dynamic)
                   jo = object@javaObject    
                   jo$addSelectionScript(sscript)              
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
              output <- str_c(output,"  R script : \n{\n")       
              tscript <- jo$getScript()
              script_text <- tscript$getScript()
              output <- str_c(output, script_text)
              output <- str_c(output,"}\n")
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
