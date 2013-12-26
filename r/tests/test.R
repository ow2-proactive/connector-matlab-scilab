# source('H:/Temp/workspace/R-dev/connectors/matlab_scilab_connector/r/tests/test.R');

{
  library("PARConnector");
  
  cat("*** Locating Scheduler home ***","\n");  
  schedHomePath = Sys.getenv("SCHEDULER_HOME");
  
  if ( file.exists(schedHomePath) ){
    cat("Using Scheduler:", schedHomePath,"\n");
  }  else {    
    sched_dirname = "ProActiveScheduling-3.4.0_bin_full";
    if ( file.exists(sched_dirname) ){      
      cat("Reusing existing Scheduler");
    } else {
      print("Downloading from www.activeoon.com");
      #sched_file <- paste(getwd(), '/sched.zip', sep="")
      #download.file("http://www.activeeon.com/public_content/releases/ProActive/3.4.0/ProActiveScheduling-3.4.0_bin_full.zip",
      #              sched_file,
      #              "auto",
      #              quiet = FALSE, mode = "w",
      #              cacheOK = TRUE,
      #              extra = getOption("download.file.extra"))
      print("Unzipping ...");
      #unzip(sched.zip);
    }  
  }
  
  cat("*** Starting the Scheduler ***","\n");
  #process = system(
  #  paste(
  #    "cmd.exe", "/c", "cd", "/d",
  #    paste(schedHomePath,"\\bin", sep=""),
  #    "&&", "jrunscript", "start-server.js", sep=" "), wait = FALSE);
  
  cat(process, "\n");
  
  cat("*** Trying to connect to http://localhost:8080/rest/rest ***","\n")  
  nbTries <- 0;

  repeat {
    nbTries <- nbTries + 1;
    x <- tryCatch({
      if(nbTries == 10) {
        cat("*** After ", nbTries, " tries unable to connect, existing ..." , "\n");
        break();
      }
      PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo');
      cat("Sucessfully connected !!", "\n");
      break();
    }, error = function(e) e)  
  }
}