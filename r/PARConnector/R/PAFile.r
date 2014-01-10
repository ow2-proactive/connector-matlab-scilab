## TODO implement all task methods

setClass( 
  Class="PAFile", 
  representation = representation(    
    filepath = "character", # absolute or relative local file path
    pathdest = "character", # data space path (can be empty if filepath is relative, in which case it will match the local relative path)
    space = "character", # remote data space name
    hash = "character", # hash directory used to separate the jobs, must not be empty
    working.dir = "character" # local working directory, used to resolve local relative paths
  ), 
  prototype=prototype(
    filepath = "",
    pathdest = "",
    space = "",
    hash = "",
    working.dir = getwd()
  )
)


PAFile <- function(filepath = "", pathdest = "", space = "USER", hash = "", working.dir = getwd()) {
  if (pathdest == "") {
    # testing that filepath is not absolute 
    if (!.isRelative(filepath)) {
      stop(str_c(filepath, " is an absolute path and no dataspace destination path is provided"))
    } 
  }
  ff <- new (Class="PAFile", filepath = filepath, pathdest = pathdest, space = space, hash = hash, working.dir = working.dir)
  return (ff)
}

setReplaceMethod("setHash", "PAFile",
                 function(object, value) {
                   if (class(value) != "character") {
                     stop("unexpected argument class, expected character, received ",class(value))
                   }
                   object@hash <- value
                   return(object)
                 }
)
                 

setMethod("pushFile", "PAFile",
          function(object, client = PAClient()) {  
            if (object@filepath == "") {
              stop("Cannot transfer file if no local path is provided")
            }
            
            filename <- basename(object@filepath)
            if (object@pathdest == "") {
              if (object@hash == "") {
                pathdest <- str_c("/",str_replace_all(dirname(object@filepath),fixed("\\"), "/"))
              } else {
                pathdest <- str_c("/",object@hash,"/",str_replace_all(dirname(object@filepath),fixed("\\"), "/"))
              }
              filepath <- file.path(object@working.dir, object@filepath)
            } else {
              if (object@hash == "") {
                pathdest <- str_c("/",str_replace_all(object@pathdest,fixed("\\"), "/"))
              } else {
                pathdest <- str_c("/",object@hash,"/",str_replace_all(object@pathdest,fixed("\\"), "/"))
              }
              if (.isRelative(object@filepath)) {
                filepath <- file.path(object@working.dir,object@filepath)
              } else {
                filepath <- object@filepath
              }
            }
            tryCatch (         
              return(PAPushFile(toupper(object@space),pathdest, filename, filepath, .print.stack = FALSE )),                      
              Exception = function(e) { print(str_c("error occurred when trying to push file ",filepath," -> ",toupper(object@space),":",pathdest,"/",filename))} 
              )
          } 
)

setMethod("pullFile", "PAFile",
          function(object, client = PAClient()) {    
            
            if (object@filepath == "") {
              stop("Cannot transfer file if no local path is provided")
            }
            
            filename <- basename(object@filepath)
            if (object@pathdest == "") {
              if (.isRelative(object@filepath)) {
                if (object@hash == "") {
                  pathname <- str_c("/",str_replace_all(object@filepath,fixed("\\"), "/"))
                } else {
                  pathname <- str_c("/",object@hash,"/",str_replace_all(object@filepath,fixed("\\"), "/"))
                }
                filepath <- file.path(object@working.dir, object@filepath)
              } else {
                stop(str_c(object@filepath, " is an absolute path and no dataspace destination path is provided"))
              }
            } else {
              if (object@hash == "") {
                pathname <- str_c("/",str_replace_all(object@pathdest,fixed("\\"), "/"),"/",basename(object@filepath))
              } else {
                pathname <- str_c("/",object@hash,"/",str_replace_all(object@pathdest,fixed("\\"), "/"),"/",basename(object@filepath))
              }
              if (.isRelative(object@filepath)) {
                filepath <- file.path(object@working.dir,object@filepath)
              }
            }
            
            tryCatch (         
              return(PAPullFile(toupper(object@space),pathname, filepath, .print.stack = FALSE  )),                      
              Exception = function(e) { print(str_c("Warning, error occurred when trying to pull file ",toupper(object@space),":",pathname," -> ",filepath))} 
            )                     
            
          } 
)

setMethod("getMode", "PAFile",
          function(object, input) {
            if (input) {
              return(.computeInputModeFromSpaceName(object@space))
            } else {
              return(.computeOutputModeFromSpaceName(object@space))
            }
                          
          } 
)

setMethod("isFileTransfer", "PAFile",
          function(object) {
            return(object@filepath != "")            
          } 
)

setMethod("getSelector", "PAFile",
          function(object) {           
            if (object@pathdest == "") { 
              # local file with transfer
              if (object@hash == "") {
                filepath <- str_c(str_replace_all(object@filepath,fixed("\\"), "/"))
              } else {
                filepath <- str_c(object@hash,"/",str_replace_all(object@filepath,fixed("\\"), "/"))
              }              
            } else {              
              if (object@filepath == "") {
                # remote file, no transfer
                if (object@hash == "") {
                  filepath <- str_c(str_replace_all(object@pathdest,fixed("\\"), "/"))
                } else {
                  filepath <- str_c(object@hash,"/",str_replace_all(object@pathdest,fixed("\\"), "/"))
                }
              } else {
                # local file with transfer
                if (object@hash == "") {
                  filepath <- str_c(str_replace_all(object@pathdest,fixed("\\"), "/"),"/",basename(object@filepath))                        
                } else {
                  filepath <- str_c(object@hash,"/",str_replace_all(object@pathdest,fixed("\\"), "/"),"/",basename(object@filepath))    
                }
              }              
            }
            return(filepath)
          }
)



setMethod("show" ,"PAFile" ,
          function(object) {
            cat(toString(object))                                 
          } 
)

setMethod("toString","PAFile",
          function(x, input=TRUE) {
            if (x@filepath == "") {
              output <- str_c("$",x@space, "/",x@pathdest)
            } else {
              if (.isRelative(x@filepath)) {
                filepath <- file.path(x@working.dir, x@filepath)
              } else {
                filepath <- x@filepath
              }
              if (x@pathdest == "") {        
                if (x@hash == "") {
                  output <- str_c(filepath," ",ifelse(input,"->","<-")," $",x@space,"/",x@filepath)
                } else {
                  output <- str_c(filepath," ",ifelse(input,"->","<-")," $",x@space,"/", x@hash,"/",x@filepath)
                }
              } else {
                output <- str_c(filepath," ",ifelse(input,"->","<-")," $",x@space, "/",x@pathdest,"/",basename(x@filepath))
              }
            }
            
            
            return (output)            
          }
)

.isRelative <- function(filepath) {
  jfile <- .jnew(J("java.io.File"),filepath)
  return(!jfile$isAbsolute())
}

.computeInputModeFromSpaceName<- function(space) {
  InputAccessMode <- J("org.ow2.proactive.scheduler.common.task.dataspaces.InputAccessMode")
  if (toupper(space) == "INPUT") {
    mode <- InputAccessMode$getAccessMode("transferFromInputSpace")  
  } else if (toupper(space) == "OUTPUT") {
    mode <- InputAccessMode$getAccessMode("transferFromOutputSpace")  
  } else if (toupper(space) == "GLOBAL") {
    mode <- InputAccessMode$getAccessMode("transferFromGlobalSpace")
  } else if (toupper(space) == "USER") {
    mode <- InputAccessMode$getAccessMode("transferFromUserSpace")
  }
  return (mode)
}

.computeOutputModeFromSpaceName<- function(space) {
  outputAccessMode <- J("org.ow2.proactive.scheduler.common.task.dataspaces.OutputAccessMode")
  if (toupper(space) == "OUTPUT") {
    mode <- outputAccessMode$getAccessMode("transferToOutputSpace")  
  } else if (toupper(space) == "GLOBAL") {
    mode <- outputAccessMode$getAccessMode("transferToGlobalSpace")
  } else if (toupper(space) == "USER") {
    mode <- outputAccessMode$getAccessMode("transferToUserSpace")
  }
  return (mode)
}
