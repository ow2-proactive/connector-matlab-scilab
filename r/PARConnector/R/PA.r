.replaceFilePatterns <- function(filePath, dots, repldots,ii) {
  # this function replace all patterns in one input file for a given index i in 1..maxlength 
  tmpfilelist <- list()
  # for each input file, replace all matching parameters patterns             
  for (jj in 1:length(dots)) { 
    nm = names(dots[jj])
    if (is.null(nm) || nchar(nm) == 0) {
      pattern = str_c("%",jj,"%")
    } else {
      pattern = str_c("%",nm,"%")
    }
    replval <- repldots[[jj]][[ii]] 
    withlengthtype <- is.element(typeof(replval), c("logical", "integer", "double", "complex", "raw", "list"))
    
    # if length(repval) > 1 an array of indexes will be expanded to a list of files, used by a single task
    if (grepl(pattern, filePath, fixed = TRUE)) {
      for (kk in 1:length(replval)) {      
        tmpfilelist[[kk]] <- gsub(pattern,replval[[kk]], filePath,fixed=TRUE)      
      } 
    } else {
      tmpfilelist[[1]] <- filePath
    }       
    
  }       
  return(tmpfilelist)
}

.splitFilePatternWithSpace <- function(filePath) {
  space <- str_extract(filePath,   "\\$[a-zA-Z]+:")
  if(is.na(space)) {
    return(NULL)
  } else {
    newpath <- str_extract(filePath,fixed(space),"")
    return(list(space = str_sub(space,2L,-2L), path = newpath))
  }
}

.createAndTransferPAFileFromPattern <- function(pattern, hash, dir, hasDependency, isOutput) {
  # check if the pattern contains a space pattern
  pair <- .splitFilePatternWithSpace(pattern)
  if (is.null(pair)) {
    # if no explicit space pattern, try to infer it
    if(hasDependency && !isOutput) {
      # by default we say that the space is remote if task has dependencies
      space <- "USER"   
    } else {
      space <- "LOCAL"   
    }
    path <- pattern
    
  } else {
    space <- toupper(pair("space"))
    path <- pair("path")
  }
  
  if (space == "LOCAL") {
    # if it's a local file, we transfer it to the user space
    pafile <- PAFile(path, hash = hash, working.dir = dir)
    if (!isOutput) {
      pushFile(pafile, client = client)
    }
  } else {
    # if it's a remote file, no transfer, no hash used
    pafile <- PAFile(pathdest = path, space = space)
  }
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

# Merging
PAM <- function(funcOrFuncName, ..., varies=NULL, input.files=list(), output.files=list(), in.dir = getwd(), out.dir = getwd(), client = PAClient(), .debug = PADebug()) {  
  dots <- list(...)
  
  # if we are merging find all list of tasks in parameters, construct new function call
  newcall <- list(funcOrFuncName)
  for (i in 1:length(dots)) {   
    if ((typeof(dots[[i]]) == "list") && (length(dots[[i]]) > 0) && (class(dots[[i]]) == "PATask") ) {
      for (j in 1:length(dots[[i]])) {
        newcall <- c(newcall,dots[[i]][[k]])
      }
    } else {
      newcall <- c(newcall,dots[[i]])
    }
  }
  newcall <- c(newcall,varies=varies,input.files=input.files, output.files=output.files, in.dir = in.dir, out.dir = out.dir, client = client, .debug = .debug)
  return(do.call("PA",newcall))
}

# Splitting / Scattering
PAS <- function(funcOrFuncName, ..., varies=NULL, input.files=list(), output.files=list(), in.dir = getwd(), out.dir = getwd(), client = PAClient(), .debug = PADebug()) {    
  
  dots <- list(...)
  
  maxlength <- .findCardinality(dots, varies)
  
  task <- PA(funcOrFuncName, ..., varies=list(),input.files=input.files, output.files=output.files, in.dir = in.dir, out.dir = out.dir, client = client, .debug = .debug)
  if (length(task) > 1) {
    stop(paste0("Internal Error : Unexpected task list length, expected 1, received ",length(task)))
  }
  
  scatteredTasks <- list()
  for (i in 1:maxlength) {
    scatteredTasks[[i]] <- PACloneTaskWithIndex(task[[1]], i)
  }
  return(scatteredTasks)
}

# Standard Parametric sweep
PA <- function(funcOrFuncName, ..., varies=NULL, input.files=list(), output.files=list(), in.dir = getwd(), out.dir = getwd(), client = PAClient(), .debug = PADebug()) {
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
  repldots <- list()
  
  depVariableNames <- NULL
  newenvir <-  new.env()
  
    
  # compute maxlength (i.e. the length of the parameter which has the biggest length)
  maxlength <- .findCardinality(dots,varies)
      
  
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
  final.param.list <- list() 
  
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
  
  # pattern replacements in input files  
  final.input.files <- list()  
  if (length(input.files) > 0) {
    for (i in 1:maxlength) {
      tmp.input.files <- list();
      for (j in 1:length(input.files)) {
        tmp.input.files[[j]] <- .replaceFilePatterns(input.files[[j]], dots, repldots,i);               
      }  
      # merge all list received into one
      fif <- list()
      rapply(tmp.input.files, function(x) fif <<- c(fif,x))
      final.input.files[[i]] <- fif 
    }      
  }
  
  # pattern replacements in output files  
  final.output.files <- list()  
  if (length(output.files) > 0) {
    for (i in 1:maxlength) {
      tmp.output.files <- list();
      for (j in 1:length(output.files)) {
        tmp.output.files[[j]] <- .replaceFilePatterns(output.files[[j]], dots, repldots,i);               
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
    t <- PATask(tname) 
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
    
    
     
    
    total_script <- str_c("ifelse(file.exists(\"",hash,"\"),setwd(file.path(getwd(),\"",hash,"\")),NA)\n")
    if (.debug) {
      total_script <- str_c(total_script, "print(paste(\"[DEBUG] Working directory is :\",getwd()))\n")
      total_script <- str_c(total_script, "print(\"[DEBUG] Working directory content :\")\n")
      total_script <- str_c(total_script, "print(list.files(getwd()))\n")
    }
    total_script <- str_c(total_script, "ifelse(file.exists(\"",basename(env_file),"\"),load(\"",basename(env_file),"\"),stop(\"Could not find PASolve environment file : ",basename(env_file)," \"))\n")   
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
    }
    setScript(t,total_script)  
    
    if (length(input.files) > 0) {
      tmp.input.files <- final.input.files[[i]]
      for (j in 1:length(tmp.input.files)) {        
        pafile <- .createAndTransferPAFileFromPattern(tmp.input.files[[j]], hash, in.dir, length(getDependencies(t)) > 0, FALSE)
        
        addInputFiles(t) <- pafile      
      }
    }
    addInputFiles(t) <- pasolvefile   
    
    if (length(output.files) > 0) {
      tmp.output.files <- final.output.files[[i]]
      for (j in 1:length(tmp.output.files)) {
        pafile <- .createAndTransferPAFileFromPattern(tmp.output.files[[j]], hash, out.dir, length(getDependencies(t)) > 0, TRUE)
        
        addOutputFiles(t) <- pafile
      }
    }
    patasks <- c(patasks,t)
  }
  return(patasks)
}

