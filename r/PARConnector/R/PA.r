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

PA <- function(funcOrFuncName, ..., varies=NULL, merge = FALSE, input.files=list(), output.files=list(), in.dir = getwd(), out.dir = getwd(), client = .scheduler.client, .debug = PADebug()) {
  if (is.character(funcOrFuncName)) {
    fun <- match.fun(funcOrFuncName)
    funname <- funcOrFuncName
  } else {
    if (typeof(funcOrFuncName) != "closure") {
      stop("unexpected type for parameter funcOrFuncName ",typeof(funcOrFuncName), ", consider using function name instead")
    }
    fun <- funcOrFuncName
    funname <- "fun"
  }
  dots <- list(...)
  repldots <- list()
  
  
  if (merge) {
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
    newcall <- c(newcall,varies=varies,merge=FALSE,input.files=input.files, output.files=output.files, in.dir = in.dir, out.dir = out.dir, client = client, .debug = .debug)
    return(do.call("PA",newcall))
  } 
  # compute maxlength (i.e. the length of the parameter which has the biggest length)
  maxlength <- 0
  for (i in 1:length(dots)) {   
    if (is.null(varies) || is.element(i, varies) || (!is.null(names(dots[i])) && is.element(names(dots[i]),varies))) {      
      maxlength = max(maxlength, length(dots[[i]]))
    }
  }
  
  if (maxlength == 0) {
    error("No varying argument")
  }
  
  # harmonize parameters (i.e. extends each varying parameter to match the size of the biggest, by relooping)
  for (i in 1:length(dots)) {   
    handletype <- is.element(typeof(dots[[i]]), c("logical", "integer", "double", "complex", "raw", "list"))
    
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
    if (!exists("fun",envir)) {
      assign("fun",fun,envir=envir)
    }
    pairlist <- .PASolve_computeDependencies("fun", envir=envir,.do.verbose=.debug)    
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
        deptskname <- getName(deptsk)
        final.param.list[[i]][[j]] <- bquote(results[[.(deptskname)]])
        # set this task as dependant
        addDependency(t) <- deptsk        
      } 
    }
    
    PASolveCall <- as.call(c(fun,final.param.list[[i]]))
    
    if (typeof(fun) == "closure") {
      assign("PASolveCall", PASolveCall, envir = pairlist[["newenvir"]])
      
      save(list = c(pairlist[["subpair"]],"PASolveCall"),file = env_file, envir = pairlist[["newenvir"]]);     
    } else {
      save(PASolveCall,file = env_file);
    }
    
    
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
    }
    total_script <- str_c(total_script, "result <- eval(PASolveCall)\n")
    setScript(t,total_script)  
    
    if (length(input.files) > 0) {
      tmp.input.files <- final.input.files[[i]]
      for (j in 1:length(tmp.input.files)) {
        pafile <- PAFile(tmp.input.files[[j]], hash = hash, working.dir = in.dir)
        pushFile(pafile, client = client)
        addInputFiles(t) <- pafile      
      }
    }
    addInputFiles(t) <- pasolvefile   
    
    if (length(output.files) > 0) {
      tmp.output.files <- final.output.files[[i]]
      for (j in 1:length(tmp.output.files)) {
        pafile <- PAFile(tmp.output.files[[j]], hash = hash, working.dir = out.dir)
        addOutputFiles(t) <- pafile
      }
    }
    patasks <- c(patasks,t)
  }
  return(patasks)
}

