PAinit <- function( pa_toolbox_home ) {
  if (is.null(pa_toolbox_home)) {
    error("Toolbox home is not set")
  }
  if (!any(is.element(installed.packages(),"codetools"))) {
    install.packages("codetools")
  }
  if (!any(is.element(installed.packages(),"stringr"))) {
    install.packages("stringr")
  }
  if (!any(is.element(installed.packages(),"rJava"))) {
    install.packages("rJava")
  }
  require("codetools")
  require("stringr")
  require("rJava")
  
  .jinit()
  
  .jaddClassPath(dir(str_c(pa_toolbox_home,"/java"), full.names=TRUE))
  .jclassPath()

  # if this fails, there is a problem with classloading
  J("org.ow2.proactive.scripting.Script")
}