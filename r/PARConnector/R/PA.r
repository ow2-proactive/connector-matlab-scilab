

.createReplacementFunction <- function(envir) {
  rlist <- get(".replacement.list",envir)
  return (function(i) { toString(rlist[[i]])})
}

# checks that the given object if a standard object with length
.withlengthtype <- function(val) {
   return(is.element(typeof(val), c("logical", "integer", "character", "double", "complex", "raw", "list")))
}

.addReplacementToMemoryList <- function(index,val,envir) {
  if (!is.null(envir)) {
    repl <- get(".replacement.list",envir) 
    if ( index > length(repl)) {
      for (z in length(repl)+1:index ) {
        repl[[z]] <- ""
      }
    }
    repl[[index]] <- str_c(repl[[index]], val)
    assign(".replacement.list",repl,envir)
  }
}

# for a given parameter value, create a list of replacements (usually of size 1, but can be bigger for split tasks)
.createReplacementList <- function(val) {
  # analyse the found value to check if it has length
  if(.withlengthtype(val)) {
    # if it has length, iterate over the values and build a list of replacements
    replval <- list()
    for (kk in 1:length(val)) {
      replval[[kk]] <- val[[kk]]
    }
  } else {
    # if it has no length, simply use it as it is
    replval <- list(val) 
  }
  return (replval)
}

# Replace parameter patterns in input/output files 
# this function replace all patterns in one input file for a given index ii in 1..maxlength 
# depending on the type of task (split, parametric sweep or merge, it can produce either one file or a list of files)
# the replacement is controlled by the index ii, in a list of 1..ii..n possible replacements, it will select replacement at position ii
.replaceFilePatterns <- function(filePath, dots, repldots,ii, envir) {
  
  # the tmpfilelist will contain the list of final paths after all replacement have been done
  tmpfilelist <- list(filePath)
  
  # all the names in the paramater list
  namesvector <- Filter(function(x) x != "",names(dots))  
  
  # extract patterns in filePath
  patterns <- str_extract_all(filePath, "%[^ %]+%")
  if (length(patterns) > 0) {
    # iterate over the patterns found, do all replacement for a found pattern, then move to next pattern
    
    for (jj in 1:length(patterns)) { 
      
      pattern <- patterns[[jj]]
      # (weirdly str_extract returns an array of size 1 containing a character vector of size 0 when no pattern is found )
      if (length(pattern) > 0) {
        
        # param : the pattern to replace, we try to find the parameter value which it represents
        param <- str_sub(pattern,2L,-2L)       
        
        # the pattern can be a name if it refers to a named parameter or an integer expression to simply match the parameters by their index
        
        # the replval variable will contain the list of replacements
        
        # if the pattern is a named param, evaluate its value 
        if(is.element(param,namesvector)) {
          index <- eval(parse(text=str_c("\"",param,"\"")))
          
          # we evaluate the parameter for the given index ii
          val <- repldots[[index]][[ii]]  
          
          # create a replacement list for a single parameter          
          replval <- .createReplacementList(val)
                    
        } else {
          # if the pattern is an integer expression, evaluate its value (we mind find more than one param, for e.g. 1:4 )
          index <- eval(parse(text=param))
          
          # if it's an integer expression of length 1, then do the same as for named params
          if (length(index) == 1) {
            val <- repldots[[index]][[ii]]
            
            # create a replacement list for a single parameter 
            replval <- .createReplacementList(val)
            
          } else {
            # if there are more than one parameter involved, iterate over the found parameters, 
            # and for each parameter, take the value of this parameter for the ii index
            replval <- list()
            for (kk in 1:length(index)) {
              current_param <- index[[kk]]
              replval[[kk]] <- repldots[[current_param]][[ii]]  
            }           
          }         
          
          # now that we have a list of replacements to make, check the current list of filepaths,
          # as the process is based on successive replacements, if the current list of filpaths is not big enough, 
          # extend it to match the replval length 
          if (length(tmpfilelist) < length(replval)) {
            tmpfilelist <- rep(tmpfilelist,length(replval))
          }
          
          # length of replval and tmpfilelist should now be equal, we iterate over the replval list to do all replacements
          for (kk in 1:length(replval)) {
            val <- replval[[kk]]
            if (class(val) == "PATask") {
              # if the replacement found if of class PATask, then it refers to a dependent Task, we extract from the task the replacement needed and use this info as replacement
              if (val@file.index == "") {
                stop("Error when replacing pattern ",pattern,", in ",filePath,", PATask parameter specified ",getName(val)," does not contain a file index")
              }
              # additionally, we store the replacement in the replacement memory list (which is used to memorize replacements done), this replacement knowledge will be stored in the PATask
              # either kk or ii should be varying at the same time
              .addReplacementToMemoryList(max(kk,ii), getFileIndex(val), envir)   
              
              # Replacement : (the getFileIndex(val) extracts from the task the replacement to do)
              
              tmpfilelist[[kk]] <- tryCatch( 
{gsub(pattern,  getFileIndex(val), tmpfilelist[[kk]],fixed=TRUE)}, 
warning = function(e) {print(str_c("Unexpected warning when replacing pattern ", pattern, " in filePath ", filePath, " (replacement class is ", class(getFileIndex(val)), ") : ")); stop(e)},
error = function(e) {print(str_c("Error when replacing pattern ", pattern, " in filePath ", filePath, " (replacement class is ", class(getFileIndex(val)), ") : ")); stop(e)} )
              
             
            } else {   
              # if the replacement found is something else than PATask, we do the replacement and memorize it
              # this replacement knowledge will be stored in the PATask
              
              .addReplacementToMemoryList(max(kk,ii), toString(val), envir)
              
              # Replacement : 
              tmpfilelist[[kk]] <- tryCatch( 
{gsub(pattern,  toString(val), tmpfilelist[[kk]],fixed=TRUE)}, 
warning = function(e) {print(str_c("Unexpected warning when replacing pattern ", pattern, " in filePath ", filePath, " (replacement class is ", class(val), ") : ")); stop(e)},
error = function(e) {print(str_c("Error when replacing pattern ", pattern, " in filePath ", filePath, " (replacement class is ", class(val), ") : ")); stop(e)} )
            }
          }
        }            
      }
    }
  }
  
  # at the end verify that there are no remaining pattern in the list
  for (kk in 1:length(tmpfilelist)) { 
    unmatched <- str_extract_all(tmpfilelist[[kk]], "%[[:alnum:]]+%")
    if ((!is.na(unmatched)) && (length(unmatched[[1]]) > 0)) {
      stop("There are unmatched pattern in filePath ",filePath, " : ",toString(unmatched), " please verify that the pattern correspond to an existing parameter")
    }
  }
  
  return(tmpfilelist)
}

# Extract from the file pattern the data spaces definition
.splitFilePatternWithSpace <- function(filePath) {
  lspaces <- str_extract_all(filePath,   "\\$[a-zA-Z]+:")
  spaces <- lspaces[[1]]
  if(length(spaces) == 0 || spaces[1] == "") {
    return(NULL)
  } else {
    newpath <- str_replace_all(filePath, "\\$[a-zA-Z]+:","")
    return(list(spaces = sapply(spaces,function(x)str_sub(x,2L,-2L), USE.NAMES=FALSE), path = newpath))
  }
}


.createAndTransferPAFileFromPattern <- function(pattern, hash, dir, hasDependency, isOutput, isolate.io.files) {
  # check if the pattern contains a space pattern
  pair <- .splitFilePatternWithSpace(pattern)  
  if (is.null(pair)) {
    # if no explicit space pattern, try to infer it
    if(hasDependency && !isOutput) {
      # by default we say that the space is remote if task has dependencies and file is not an output file
      spaces <- "USER"   
    } else {
      spaces <- "LOCAL"
    }
    path <- pattern
    
  } else {
    spaces <- toupper(pair[["spaces"]])
    path <- pair[["path"]]
  }
        
  
  switch (paste(spaces,collapse = ""),
          LOCAL={
            # local file with implicit user space transfer, behaviour varies if it's an input or output file
            pafile <- PAFile(path, hash = ifelse(isolate.io.files,hash,""), working.dir = dir, space = "USER")            
            if (!isOutput) {
              pushFile(pafile, client = client)
            }
          },
          LOCALUSER={
            # explicit transfer local to user and user to node, valid only for input files
            pafile <- PAFile(path, hash = ifelse(isolate.io.files,hash,""), working.dir = dir, space = "USER") 
            if (isOutput) {
              stop("$LOCAL:$USER: pattern can be used only for input files, use $USER:$LOCAL: for output files")
            }            
            pushFile(pafile, client = client)                        
          },
          USERLOCAL={
            # explicit transfer node to user and user to local, valid only for output files
            pafile <- PAFile(path, hash = ifelse(isolate.io.files,hash,""), working.dir = dir, space = "USER") 
            if (!isOutput) {
              stop("$USER:$LOCAL: pattern can be used only for output files, use $LOCAL:$USER: for input files")
            }            
            pushFile(pafile, client = client)                        
          },
          LOCALGLOBAL={
            # explicit transfer local to global and global to node, valid only for input files
            pafile <- PAFile(path, hash = ifelse(isolate.io.files,hash,""), working.dir = dir, space = "GLOBAL") 
            if (isOutput) {
              stop("$LOCAL:$GLOBAL: pattern can be used only for input files, use $GLOBAL:$LOCAL: for output files")
            }            
            pushFile(pafile, client = client)                        
          },
          GLOBALLOCAL={
            # explicit transfer node to global and global to local, valid only for output files
            pafile <- PAFile(path, hash = ifelse(isolate.io.files,hash,""), working.dir = dir, space = "GLOBAL") 
            if (isOutput) {
              stop("$LOCAL:$GLOBAL: pattern can be used only for output files, use $LOCAL:$GLOBAL: for input files")
            }            
            pushFile(pafile, client = client)                        
          },
          USER={
            # transfer user to node or node to user, depending on input or output files
            pafile <- PAFile(pathdest = path, hash = ifelse(isolate.io.files,hash,""), space = "USER")                               
          },
          GLOBAL={
            # transfer global to node or node to global, depending on input or output files
            pafile <- PAFile(pathdest = path, hash = ifelse(isolate.io.files,hash,""), space = "GLOBAL")        
          }
          )  
  return (pafile)
}

.findCardinality <- function(dots, varies) {
  maxlength <- 1
  for (i in 1:length(dots)) {   
    if (is.null(varies) || is.element(i, varies) || (!is.null(names(dots[i])) && is.element(names(dots[i]),varies))) {      
      maxlength = max(maxlength, length(dots[[i]]))
    }
  }
  return(maxlength)
}

#' Creates a single merge PATask which can be used in combination with \code{\link{PA}} and \code{\link{PAS}} to create split/merge workflows
#' 
#' \code{PAM} uses the same parameter semantic as \code{\link{PA}} , but instead of creating a set of parallel tasks, it will produce a single task which will aggregate the results of a list of tasks produced by \code{\link{PA}}. 
#' 
#' \code{PAM} has always a cardinality of 1, it is used solely as a multi-parameter aggregation.
#'               
#' 
#'  @param funcOrFuncName function handle or function name
#'  @param ... arguments of the funcOrFuncName function which will be vectorized over 
#'  @param varies list of varying parameters which can be a parameter number or a parameter name, if NULL (default) then all parameters are varying
#'  @param input.files a list of input files which will be transferred from the local machine to the remote executions, see Details section in  \code{\link{PA}} for more information
#'  @param output.files a list of output files which will be transferred from the remote executions to the local machine
#'  @param in.dir in case input files are used, the directory which will be used as base (default to current working directory)
#'  @param out.dir in.dir in case ouput files are used, the directory which will be used as base (default to current working directory)
#'  @param hostname.selection can be used to restrict the remote execution to a given machine, wildcards can be used
#'  @param ip.selection can be used to restrict the remote execution to a given machine given its IP address
#'  @param property.selection.name can be used to restrict the remote execution to a given JVM resource where the property is set to the according value
#'  @param property.selection.value is used in combination with property.selection.name
#'  @param client connection handle to the scheduler, if not provided the handle created by the last call to PAConnect will be used
#'  @param .debug debug mode
#'  @return a PATask object which can be submitted to the ProActive Scheduler via a \code{\link{PASolve}} call or given as parameter to other \code{\link{PA}}, \code{\link{PAS}} or \code{\link{PAM}} functions
#'  
#'  @examples 
#'  \dontrun{
#'  see examples in PAS and PA help sections before reading these examples
#'        
#'  PAM("sum",
#'    PA(function(x) {x*x},
#'       PAS("identity", 1:4)))  # will produce 6 PATasks producing the following results :
#'         
#'  t1: 1:4 
#'  t2: 1*1
#'  t3: 2*2
#'  t4: 3*3
#'  t5: 4*4
#'  t6: sum(1*1,2*2,3*3,4*4)       
#'  
#'  Explanation for t6 : the lower part of the statement produces 4 parallel tasks which are given as parameter to the \code{PAM} call with the sum function.
#'  The results of those tasks are merged via the sum function, similar to sum( res[t2],res[t3],res[t4],res[t4]) , of course this is possible only with function which accept variable number of parameters
#'  
#'  }
#'  @seealso  \code{\link{PA}} \code{\link{PAS}}  \code{\link{PASolve}} \code{\link{mapply}} \code{\link{PAConnect}} 
PAM <- function(funcOrFuncName, ..., varies=list(), input.files=list(), output.files=list(), in.dir = getwd(), out.dir = getwd(), hostname.selection = NULL, ip.selection = NULL, property.selection.name = NULL, property.selection.value = NULL, isolate.io.files = FALSE, client = PAClient(), .debug = PADebug()) {  
  dots <- list(...)
  
  # if we are merging find all list of tasks in parameters, construct new function call
  newcall <- list(funcOrFuncName)
  newcallindex <- 2
  for (i in 1:length(dots)) { 
    nm <- names(dots[i])    
    if ((typeof(dots[[i]]) == "list") && (length(dots[[i]]) > 0) && (class(dots[[i]][[1]]) == "PATask") ) {      
      if (length(nm) > 0 && nchar(nm) > 0) {
          tmplist <- list()
          for (j in 1:length(dots[[i]])) {        
            tmplist[[j]] <- dots[[i]][[j]]
          }
          newcall[[nm]] <- tmplist
        } else {
          for (j in 1:length(dots[[i]])) {     
            newcall[[newcallindex]] <- dots[[i]][[j]]
            newcallindex <- newcallindex+1
          }
        }             
    } else {  
      if (length(nm) > 0 && nchar(nm) > 0) {
        newcall[[nm]] <- dots[[i]]
      } else {
        newcall[[newcallindex]] <- dots[[i]]
      }
      newcallindex <- newcallindex+1
    }    
  }
  
  newcall[["varies"]] <- varies
  newcall[["input.files"]] <- input.files
  newcall[["output.files"]] <- output.files
  newcall[["in.dir"]] <- in.dir
  newcall[["out.dir"]] <- out.dir
  newcall[["hostname.selection"]] <- hostname.selection
  newcall[["ip.selection"]] <- ip.selection
  newcall[["property.selection.name"]] <- property.selection.name
  newcall[["property.selection.value"]] <- property.selection.value
  newcall[["isolate.io.files"]] <- isolate.io.files
  newcall[["client"]] <- client
  newcall[[".debug"]] <- .debug
  return(do.call("PA",newcall))
}

#' Creates a single split PATask which can be used in combination with \code{\link{PA}} and \code{\link{PAM}} to create split/merge workflows
#' 
#' \code{PAS} uses the same parameter semantic as \code{\link{PA}} , but instead of creating a set of parallel tasks, it will produce a single task whose result (expected to be a list of vector) will be scattered across dependent tasks. 
#' 
#' The cardinality will be, like for \code{\link{PA}}, determined by analysing the parameters and finding the longest list/vector among them. But the cardinality will be used in a different way, as it will be used when the result of the \code{PAS} call is given to a \code{PA} call to build a workflow. In that case, the cardinality of the \code{PAS} result will be used to produce as many \code{PA} tasks.
#'               
#' 
#'  @param funcOrFuncName function handle or function name
#'  @param ... arguments of the funcOrFuncName function which will be vectorized over 
#'  @param varies list of varying parameters which can be a parameter number or a parameter name, if NULL (default) then all parameters are varying
#'  @param input.files a list of input files which will be transferred from the local machine to the remote executions, see Details section in  \code{\link{PA}} for more information
#'  @param output.files a list of output files which will be transferred from the remote executions to the local machine
#'  @param in.dir in case input files are used, the directory which will be used as base (default to current working directory)
#'  @param out.dir in.dir in case ouput files are used, the directory which will be used as base (default to current working directory)
#'  @param hostname.selection can be used to restrict the remote execution to a given machine, wildcards can be used
#'  @param ip.selection can be used to restrict the remote execution to a given machine given its IP address
#'  @param property.selection.name can be used to restrict the remote execution to a given JVM resource where the property is set to the according value
#'  @param property.selection.value is used in combination with property.selection.name
#'  @param client connection handle to the scheduler, if not provided the handle created by the last call to PAConnect will be used
#'  @param .debug debug mode
#'  @return a PATask object which can be submitted to the ProActive Scheduler via a \code{\link{PASolve}} call or given as parameter to other \code{\link{PA}}, \code{\link{PAS}} or \code{\link{PAM}} functions
#'  @examples 
#'  \dontrun{
#'  PAS("identity", 1:4) # will produce a split task of cardinality 4 that will output the vector 1:4
#'  
#'  PA(function(x){x*x},  PAS("identity", 1:4)) # will produce 5 PATasks producing the following results : 
#'  t1: 1:4 
#'  t2: 1*1
#'  t3: 2*2
#'  t4: 3*3
#'  t5: 4*4
#'  
#'  Explanation : This is because the tasks created by the PAS call, given as parameter to the PA call will produce as many parallel tasks as the PAS task cardinality (here 4)
#'  as the PAS task produce the vector 1:4, the first PA task will receive the parameter 1, the second PA task will receive the parameter 2, etc...
#'  
#'  (PAS(function(out,ind){for (i in ind) {file.create(paste0(out,i))}}, "out", 1:4, output.files="out%2%") # will produce a split task of cardinality 4 that will create remotely the files out1, out2, out3 and out4 and transfer them back to the local machine
#'
#'  }       
#'  @seealso  \code{\link{PA}} \code{\link{PAM}}  \code{\link{PASolve}} \code{\link{mapply}} \code{\link{PAJobResult}} \code{\link{PAConnect}} 
PAS <- function(funcOrFuncName, ..., varies=NULL, input.files=list(), output.files=list(), in.dir = getwd(), out.dir = getwd(), hostname.selection = NULL, ip.selection = NULL, property.selection.name = NULL, property.selection.value = NULL, isolate.io.files = FALSE, client = PAClient(), .debug = PADebug()) {    
  
  dots <- list(...)
   
  # we generate a task with a forced cardinality of 1
  task <- PA(funcOrFuncName, ..., varies=list(),input.files=input.files, output.files=output.files, in.dir = in.dir, out.dir = out.dir, hostname.selection = hostname.selection, ip.selection = ip.selection, property.selection.name = property.selection.name, property.selection.value = property.selection.value, isolate.io.files = isolate.io.files, client = client, .debug = .debug)
  if (length(task) > 1) {
    stop(paste0("Internal Error : Unexpected task list length, expected 1, received ",length(task)))
  }
  
  # now we find the real cardinality of the task (with the provided varies parameter)
  maxlength <- .findCardinality(dots, varies)
  
  # we generate as many tasks corresponding to this cardinality
  scatteredTasks <- list()
  for (i in 1:maxlength) {
    scatteredTasks[[i]] <- PACloneTaskWithIndex(task[[1]], i, i, task[[1]]@file.index.function)
  }
  return(scatteredTasks)
}

#' Creates parallel PATasks which can be submitted to PASolve
#' 
#' \code{PA} creates a list of PATasks using a syntax similar to mapply. 
#' Where mapply applies multi-parameters to a function, PA will create multi-parameter remote executions of a given function. 
#' 
#' The function can be provided via its name or via a closure object. For builtin functions, it is necessary to provide the name instead of the closure.
#' For user defined function, the function will be analysed and all its depdendencies will be automatically transferred to the remote executions. Dependencies can include other functions or variables defined in the function closure.
#' If the function has a dependency on a package, it's mandatory to manually install and load the package in the remote R executions. PARConnector does not handle automatic package installation.
#' It's of course possible though to do the installation and loading of a package from within the function provided to \code{PA}
#' 
#' The cardinality (the number of PATask to be created) will be determined by analysing the parameters. If the parameters contains lists or vectors, the biggest length will be the cardinality. 
#' Only parameters of the following types logical, integer, character, double, complex, raw, and list will be considered.
#' 
#' It is possible to force unvarying parameters (which will not be taken into account when computing the cardinality), those parameters will be transmitted as they are to the remote evaluations, and will not be scattered.  
#' Similarly to mapply, varying lists or vector which are smaller than the cardinality will be extended via looping to match the cardinality. See \code{\link{mapply}} for more information.
#' 
#' When used alone, PA allows to create parallel independant tasks. When used in combination with the two other job conscrution primitives (\code{\link{PAM}} and \code{\link{PAS}}), it allows to create split/merge workflows.
#' 
#' \subsection{\strong{Input/Output Files patterns}}{
#' 
#' Files path in input.files and output.files list can contain special patterns which are detailed below :\cr
#' 
#' \subsection{\strong{a) Location Patterns}}{
#'      
#'      This pattern must be used in the beginning of the path and determines the itinerary of the file from the local computer to the remote compute engine.
#'      
#'      The semantic of these patterns varies wether the file is an input file or an output file. It can take the following values : \cr
#'      \itemize{
#'      \item{"$LOCAL:"}{\cr
#'            \cr
#'            \emph{Input} : the LOCAL pattern means that the file path references a file existing on the local machine and will be transferred to the remote node with an intermediate copy in the USER space. (LOCAL to USER , USER to NODE)\cr
#'            \emph{Output} : the LOCAL pattern means that the file will be produced by a remote execution and transferred back to the Local machine, with an intermediate copy in the USER space (NODE to USER , USER to LOCAL)\cr
#'      }
#'           
#'      \item{"$USER:"}{\cr
#'           \cr
#'           \emph{Input} : the USER pattern means that the file path references a file existing on the USER space and will be transferred to the remote node (USER to NODE)\cr
#'           \emph{Output} : the USER pattern means that the file will be produced by a remote execution and transferred back to the USER space (NODE to USER)\cr
#'      }
#'           
#'      \item{"$GLOBAL:"}{\cr
#'           \cr
#'           \emph{Input} : the USER pattern means that the file path references a file existing on the GLOBAL space and will be transferred to the remote node (GLOBAL to NODE)\cr
#'           \emph{Output} : the USER pattern means that the file will be produced by a remote execution and transferred back to the GLOBAL space (NODE to GLOBAL)\cr
#'      }    
#'       
#'       \item{"$LOCAL:$USER:" and "$LOCAL:$GLOBAL:"}{\cr
#'           \cr
#'           \emph{Input} : Only valid for input files. It is the explicit version of the $LOCAL: pattern for input files (which is equivalent to $LOCAL:$USER:), this notation allows to choose the GLOBAL space instead of the USER space as intermediate\cr
#'      }
#'      
#'      
#'      \item{"$USER:$LOCAL:" and "$GLOBAL:$LOCAL:"} {\cr
#'           \cr
#'           \emph{Output} : Only valid for output files. It is the explicit version of the $LOCAL: pattern for output files(which is equivalent to $USER:$LOCAL:), this notation allows to choose the GLOBAL space instead of the USER space as intermediate\cr
#'      }
#'      }
#'  }
#'  
#'  \subsection{\strong{b) Parameter Patterns}}{
#'      This pattern can be used anywhere in the file path and will be replaced by parameters of the funcOrFuncName function taken from the ... list.
#'      The pattern is of the form %expr%, where expr can take the following values :
#'      \itemize{
#'      \item{An integer i}{\cr
#'        In that case \%expr\% refers to the parameter at index i. For each individual PATask created by the PA call, the \%expr\% pattern will be replaced by the value of the parameter i for this execution. 
#'        If this value is a scalar value V, the pattern will generate a single input/output file containing the toString coercion of V.
#'        If this value is a vector or list, the pattern will generate multiple input/output files with replacements taken from the vector/list.
#'      }
#'      \item{A character  string S}{\cr
#'        In that case \%expr\% refers to the parameter named S. The semantic is similar to when using an integer parameter reference.
#'      }
#'      \item{An integer vector V}{\cr
#'        In that case \%expr\% refers to multiple parameters at index taken from V. It will use the parameter values and generate as many input/output files as elements of V.
#'      }
#'      }
#'      
#'      The parameter replacement will be done by using the toString coercion on the parameter value, but if the parameter referenced is a PATask (i.e. a result of a PA call), the pattern will be replaced by the same replacements that were done inside this PATask.
#'      This is particularly useful when build split-merge workflows, where an initial replacement needs to be transferred to dependant tasks. 
#'  }
#'  }
#'               
#' 
#'  @param funcOrFuncName function handle or function name
#'  @param ... arguments of the funcOrFuncName function which will be vectorized over 
#'  @param varies list of varying parameters which can be a parameter number or a parameter name, if NULL (default) then all parameters are varying
#'  @param input.files a list of input files which will be transferred from the local machine to the remote executions, see Details for more information
#'  @param output.files a list of output files which will be transferred from the remote executions to the local machine
#'  @param in.dir in case input files are used, the directory which will be used as base (default to current working directory)
#'  @param out.dir in.dir in case ouput files are used, the directory which will be used as base (default to current working directory)
#'  @param hostname.selection can be used to restrict the remote execution to a given machine, wildcards can be used
#'  @param ip.selection can be used to restrict the remote execution to a given machine given its IP address
#'  @param property.selection.name can be used to restrict the remote execution to a given JVM resource where the property is set to the according value
#'  @param property.selection.value is used in combination with property.selection.name
#'  @param isolate.io.files should input/output files be isolated in the remote executions, default FALSE. 
#'      If set to TRUE, when input and output files are copied to USER/GLOBAL space or to the NODE execution, they will be isolated in a folder specific to the current job. 
#'      It thus guaranties that they will be separated from other jobs execution. On the other hand it will not be possible to reuse the remote files directly in other jobs.
#'  @param client connection handle to the scheduler, if not provided the handle created by the last call to PAConnect will be used
#'  @param .debug debug mode
#'  @return a list of PATask objects which can be submitted to the ProActive Scheduler via a \code{\link{PASolve}} call or given as parameter to other \code{\link{PA}}, \code{\link{PAS}} or \code{\link{PAM}} functions
#'  
#'  @examples
#'  \dontrun{
#'  PA("cos", 1:4)      # will produce 4 PATasks : cos(1) , cos(2), cos(3) and cos(4) (parametric sweep with one parameter) 
#'  
#'  PA("sum", 1:4, 1:2)            # will produce 4 PATasks : sum(1,1) , sum(2,2), sum(3,1) and sum(4,2)    (parametric sweep with two parameters) 
#'  
#'  PA("c", 1:4, 1:2, varies= list(1) )               # will produce 4 PATasks : c(1,1:2) , sum(2,1:2), sum(3,1:2) and sum(4,1:2)  ( parametric sweep with one varying parameter and one fixed parameter) 
#'  
#'  PA( function(in,i) file.show(paste0(in,i)),"in", 1:4, input.files="in%2%")     # will produce 4 PATasks which transfer the following files in1, in2, in3, in4 and display their content 
#'  
#'  PA( function(in,out,i) file.copy(paste0(in,i), paste0(out,i)),"in","out" 1:4, input.files="in%3%", output.files="out%3%") # will produce 4 PATasks which transfer the following files in1, in2, in3, in4 and transfer back out1, out2, out3, out4 
#'  
#'  To submit tasks simply pass the produced tasks to a PASolve call :
#'  
#'  PASolve(PA("cos", 1:4))
#'  
#'  See examples in  PAS and PAM help sections for split/merge examples
#'  
#'  }
#'  @seealso  \code{\link{PAS}} \code{\link{PAM}}  \code{\link{PASolve}} \code{\link{mapply}} \code{\link{PAJobResult}} \code{\link{PAConnect}} 
PA <- function(funcOrFuncName, ..., varies=NULL, input.files=list(), output.files=list(), in.dir = getwd(), out.dir = getwd(), hostname.selection = NULL, ip.selection = NULL, property.selection.name = NULL, property.selection.value = NULL, isolate.io.files = FALSE,  client = PAClient(), .debug = PADebug()) {
  if (is.character(funcOrFuncName)) {
    fun <- match.fun(funcOrFuncName)
    funname <- funcOrFuncName
  } else {
    if (typeof(funcOrFuncName) != "closure") {
      stop("unexpected type for parameter funcOrFuncName ",typeof(funcOrFuncName), ", consider using function name instead")
    }
    fun <- funcOrFuncName
    funname <- ".pasolve.fun"
  }
  
  if (client == NULL || is.jnull(client) ) {
    stop("You are not currently connected to the scheduler, use PAConnect")
  } 
  
  dots <- list(...)  
  
  depVariableNames <- NULL
  newenvir <-  new.env()
  
    
  # compute maxlength (i.e. the length of the parameter which has the biggest length)
  maxlength <- .findCardinality(dots,varies)
  
  repldots <- vector("list", maxlength)
      
  
  # harmonize parameters (i.e. extends each varying parameter to match the size of the biggest, by relooping)
  for (i in 1:length(dots)) {   
    handletype <- is.element(typeof(dots[[i]]), c("logical", "character", "integer", "double", "complex", "raw", "list"))
    
    if (handletype && ((is.null(varies) || is.element(i, varies) || (!is.null(names(dots[i])) && is.element(names(dots[i]),varies))))) {
      len <- length(dots[[i]])
      nb_rep <- maxlength %/% len
      rem <- maxlength %% len
      if (rem != 0) {
        # TODO display a warning if the biggest length is not a multiple of this parameter's length
      } 
      if (is.null(names(dots[i])) || nchar(names(dots[i])) == 0)  {
        repldots[[i]] <- as.list(rep(dots[[i]],nb_rep,len = maxlength))
        
      } else {        
        tmplist <- as.list(rep(dots[[i]],nb_rep,len = maxlength))
        
        names(tmplist) <- rep(names(dots[i]),maxlength)
        repldots[[i]] <- tmplist                      
      }
      
    } else {
      # for unvarying parameter, replicate it as it is (i.e. a list is replicated into N lists,
      # instead of a list of size N)
      if (handletype) {
        repldots[[i]] <- rep(dots[i],maxlength)       
      } else {
        tmplist <- list()
        for (j in 1:maxlength) {
          tmplist[[j]] <- dots[[i]]
        } 
        repldots[[i]] <- tmplist
        
        # if the parameter is a function, check for its dependencies
        if (typeof(dots[[i]]) == "closure") {
          pairlist <- .PASolve_computeDependencies(dots[[i]], namelist <- depVariableNames, newenvir <- newenvir, envir=environment(dots[[i]]),.do.verbose=.debug)    
          depVariableNames <- pairlist[["variableNames"]]
        }
      }
    }
  } 
  
  # now, for each original parameter, we have produced a list.
  # we want instead for each remote function call, the list of parameters associated
  # this is equivalent to a matrix transposition, but for lists :
  final.param.list <- vector("list", maxlength)
  
  for (i in 1:maxlength) {
    tmp.param.list <- list()
    tmp.input.files <- list()
    tmp.output.files <- list()
    for (j in 1:length(dots)) {     
      tmp.param.list[j] = repldots[[j]][i]      
      nm = names(repldots[[j]][i])
      if (!is.null(nm) && !is.na(nm)) {
        names(tmp.param.list)[j] <- nm
      } else {
        names(tmp.param.list)[j] <- ""
      }      
    } 
    
    final.param.list[[i]] = tmp.param.list    
  }    
  
  repl.envir <- new.env()
  assign(".replacement.list",list(),repl.envir)
  
  # pattern replacements in input files  
  final.input.files <- vector("list", maxlength)
  if (length(input.files) > 0) {
    for (i in 1:maxlength) {
      tmp.input.files <- list();
      for (j in 1:length(input.files)) {
        tmp.input.files[[j]] <- .replaceFilePatterns(input.files[[j]], dots, repldots,i,NULL);               
      }  
      # merge all list received into one
      fif <- list()
      rapply(tmp.input.files, function(x) fif <<- c(fif,x))
      final.input.files[[i]] <- fif 
    }      
  }
  
  # pattern replacements in output files  
  final.output.files <- vector("list", maxlength)
  if (length(output.files) > 0) {
    for (i in 1:maxlength) {
      tmp.output.files <- list();
      for (j in 1:length(output.files)) {
        tmp.output.files[[j]] <- .replaceFilePatterns(output.files[[j]], dots, repldots,i,repl.envir);               
      }   
      # merge all list received into one
      fof <- list()
      rapply(tmp.output.files, function(x) fof <<- c(fof,x))
      final.output.files[[i]] <- fof 
    }      
  }  
  
  
  # compute function dependencies
  if (typeof(fun) == "closure") {
    # save function dependencies and push it to the space, only for closure  
    envir <- environment(fun)   
    # save the function in the environment if it has been given as a lambda
    if (typeof(funcOrFuncName) == "closure") {
      assign(funname,fun,envir=envir)
    }
    
    pairlist <- .PASolve_computeDependencies(funname, namelist <- depVariableNames, newenvir <- newenvir, envir=envir,.do.verbose=.debug)    
    depVariableNames <- c(depVariableNames, pairlist[["variableNames"]])
  }
  
  # Create list of PATask
  hash <- .getSpaceHash()
  hash.tmp.dir <- file.path(tempdir(),hash)
  if (!file.exists(hash.tmp.dir)) {
    dir.create(hash.tmp.dir, recursive = TRUE)
  }
  
  patasks <- list()
  patasknames <- ""
  for (i in 1:maxlength) {
    tname <- .getNewTaskName()
    .replacement.list <- get(".replacement.list",repl.envir)
    if (length(.replacement.list) < i) {
      t <- PATask(tname, file.index = i) 
    } else {
      t <- PATask(tname, file.index = i, file.index.function = .createReplacementFunction(repl.envir)) 
    }
    patasknames <- c(patasknames,tname)
    env_file <- str_replace_all(file.path(hash.tmp.dir,str_c("PASolve_",tname,".rdata")),fixed("\\"), "/") 
    
    # look for dependendent tasks in parameter list    
    for (j in 1:length(final.param.list[[i]])) {
      if (class(final.param.list[[i]][[j]]) == "PATask") {
        # replace the parameter with the expression evaluated on the node only
        deptsk <- final.param.list[[i]][[j]]
        final.param.list[[i]][[j]] <- getQuoteExp(deptsk)
        # set this task as dependant
        addDependency(t) <- deptsk        
      }       
    }
    
    PASolveCall <- as.call(c(fun,final.param.list[[i]]))
    
    # save function call and all dependencies in a file
    assign("PASolveCall", PASolveCall, envir = newenvir)
    save(list = c(depVariableNames,"PASolveCall"),file = env_file, envir = newenvir); 
            
    
    pasolvefile <- PAFile(basename(env_file),hash = hash,working.dir = file.path(str_replace_all(tempdir(),fixed("\\"), "/"),hash))
    pushFile(pasolvefile, client = client)      
    
    
     
    if (isolate.io.files) {
      # if input/output files are isolated the workdir will be set to the hash directory
      total_script <- str_c("ifelse(file.exists(\"",hash,"\"),setwd(file.path(getwd(),\"",hash,"\")),NA)\n")
    } else {
      total_script <- ""
    }
    if (.debug) {
      total_script <- str_c(total_script, "print(paste(\"[DEBUG] Working directory is :\",getwd()))\n")
      total_script <- str_c(total_script, "print(\"[DEBUG] Working directory content :\")\n")
      total_script <- str_c(total_script, "print(list.files(getwd()))\n")
    }
    if (isolate.io.files) {
      # if input/output files are isolated, the env_file will be present in the current directory, otherwise it's present in the hash subdir
      total_script <- str_c(total_script, "ifelse(file.exists(\"",basename(env_file),"\"),load(\"",basename(env_file),"\"),stop(\"Could not find PASolve environment file : ",basename(env_file)," \"))\n")   
    } else {
      total_script <- str_c(total_script, "ifelse(file.exists(\"",hash,"/",basename(env_file),"\"),load(\"",hash,"/",basename(env_file),"\"),stop(\"Could not find PASolve environment file : ",hash,"/",basename(env_file)," \"))\n")   
    }
    if (.debug) {
      total_script <- str_c(total_script, "print(\"[DEBUG] Environment :\")\n")
      total_script <- str_c(total_script,"print(ls())\n")     
      total_script <- str_c(total_script, "print(\"[DEBUG] PASolveCall :\")\n")
      total_script <- str_c(total_script, "print(PASolveCall)\n")
    }
    total_script <- str_c(total_script, "result <- eval(PASolveCall)\n")
    if (.debug) {
      total_script <- str_c(total_script, "print(\"[DEBUG] Result :\")\n")
      total_script <- str_c(total_script, "print(result)\n")
      total_script <- str_c(total_script, "print(\"[DEBUG] Working directory content after script execution:\")\n")
      total_script <- str_c(total_script, "print(list.files(getwd()))\n")
    }
    total_script <- str_c(total_script, "set_progress(100)\n")
    setScript(t,total_script) 
    
    
    # add selection script if hostname.selection is provided
    if (!is.null(hostname.selection)) {
      sspath <- system.file("extdata", "checkHostName.js", package="PARConnector")
      sscontents <- readChar(sspath, file.info(sspath)$size)
      sscontents <- str_replace_all(sscontents, fixed("args[0]"), str_c("\"",toString(hostname.selection),"\""))
      addSelectionScript(t,sscontents,"JavaScript",FALSE)
    }
    if (!is.null(ip.selection)) {
      sspath <- system.file("extdata", "checkIP.js", package="PARConnector")
      sscontents <- readChar(sspath, file.info(sspath)$size)
      sscontents <- str_replace_all(sscontents, fixed("args[0]"), str_c("\"",toString(ip.selection),"\""))
      addSelectionScript(t,sscontents,"JavaScript",FALSE)
    }
    if (!is.null(property.selection.name)) {
      sspath <- system.file("extdata", "checkJavaProperty.js", package="PARConnector")
      sscontents <- readChar(sspath, file.info(sspath)$size)
      sscontents <- str_replace_all(sscontents, fixed("args[0]"), str_c("\"",toString(property.selection.name),"\""))
      sscontents <- str_replace_all(sscontents, fixed("args[1]"), str_c("\"",toString(property.selection.value),"\""))
      addSelectionScript(t,sscontents,"JavaScript",FALSE)
    }
    
    if (length(input.files) > 0) {
      tmp.input.files <- final.input.files[[i]]
      for (j in 1:length(tmp.input.files)) {        
        pafile <- .createAndTransferPAFileFromPattern(tmp.input.files[[j]], hash, in.dir, length(getDependencies(t)) > 0, FALSE, isolate.io.files)
        
        addInputFiles(t) <- pafile      
      }
    }
    addInputFiles(t) <- pasolvefile   
    
    if (length(output.files) > 0) {
      tmp.output.files <- final.output.files[[i]]
      for (j in 1:length(tmp.output.files)) {
        pafile <- .createAndTransferPAFileFromPattern(tmp.output.files[[j]], hash, out.dir, length(getDependencies(t)) > 0, TRUE, isolate.io.files)
        
        addOutputFiles(t) <- pafile
      }
    }
    patasks <- c(patasks,t)
  }
  return(patasks)
}

