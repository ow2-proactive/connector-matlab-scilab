# source('H:/Temp/workspace/R-dev/connectors/matlab_scilab_connector/r/tests/test.R');

{
library("PARConnector");

print("*** Starting the Scheduler ***");

sched_dirname = "/ProActiveScheduling-3.4.0_bin_full";

if ( file.exists(sched_dirname) ){
  print("Reusing existing Scheduler");
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

print("*** TEST ***");
#source('../rest_client/pa_connect.R');

#PAConnect(url='http://localhost:8080/rest/rest', login='demo', pwd='demo')

}