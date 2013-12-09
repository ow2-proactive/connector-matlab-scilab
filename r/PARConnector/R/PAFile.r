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

PAFile <- function(filepath, pathdest = "", space = "USER", hash = "", working.dir = getwd()) {
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
          function(object, client = .scheduler.client) {
            if (object@hash == "") {
              stop("Hash directory is not set, use method setHash")
            }
            
            filename <- basename(object@filepath)
            if (object@pathdest == "") {
              pathdest <- str_c("/",object@hash,"/",str_replace_all(dirname(object@filepath),fixed("\\"), "/"))
              filepath <- file.path(object@working.dir, object@filepath)
            } else {
              pathdest <- str_c("/",object@hash,"/",str_replace_all(object@pathdest,fixed("\\"), "/"))
              if (.isRelative(object@filepath)) {
                filepath <- file.path(object@working.dir,object@filepath)
              } else {
                filepath <- object@filepath
              }
            }
            tryCatch (         
              return(PAPushFile(toupper(object@space),pathdest, filename, filepath )),                      
              Exception = function(e) { print(str_c("Warning, file ",filepath," not found"))} 
              )
          } 
)

setMethod("pullFile", "PAFile",
          function(object, client = .scheduler.client) {
            if (object@hash == "") {
              stop("Hash directory is not set, use method setHash")
            }
            
            filename <- basename(object@filepath)
            if (object@pathdest == "") {
              if (.isRelative(object@filepath)) {
                pathname <- str_c("/",object@hash,"/",str_replace_all(object@filepath,fixed("\\"), "/"))
                filepath <- file.path(object@working.dir, object@filepath)
              } else {
                stop(str_c(object@filepath, " is an absolute path and no dataspace destination path is provided"))
              }
            } else {
              pathname <- str_c("/",object@hash,"/",str_replace_all(object@pathdest,fixed("\\"), "/"),"/",basename(object@filepath))
              if (.isRelative(object@filepath)) {
                filepath <- file.path(object@working.dir,object@filepath)
              }
            }
            
            PAPullFile(toupper(object@space),pathname, filepath )                      
            
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

setMethod("getSelector", "PAFile",
          function(object) {
            if (object@hash == "") {
              stop("Hash directory is not set, use method setHash")
            }
            if (object@pathdest == "") {                             
              filepath <- str_c(object@hash,"/",str_replace_all(object@filepath,fixed("\\"), "/"))
              return(filepath)
            } else {                
              destpath <- str_c(object@hash,"/",str_replace_all(object@pathdest,fixed("\\"), "/"),"/",basename(object@filepath))                        
              return(destpath)
            }
          }
)



setMethod("show" ,"PAFile" ,
          function(object) {
            cat(toString(object))                                 
          } 
)

setMethod("toString","PAFile",
          function(x, input=TRUE) {
            if (.isRelative(x@filepath)) {
              filepath <- file.path(x@working.dir, x@filepath)
            } else {
              filepath <- x@filepath
            }
            if (x@pathdest == "") {              
              output <- str_c(filepath," ",ifelse(input,"->","<-")," $",x@space,"/", x@hash,"/",x@filepath)
            } else {
              output <- str_c(filepath," ",ifelse(input,"->","<-")," $",x@space, "/",x@pathdest,"/",basename(x@filepath))
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
