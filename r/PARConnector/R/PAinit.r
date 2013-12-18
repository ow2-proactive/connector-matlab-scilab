.onLoad <- function(libname, pkgname) {

  .jpackage(pkgname, lib.loc = libname)
  
  # callback engine
  .jengine(start=TRUE)

  #.jinit()
 
  
  # if this fails, there is a problem with classloading
  J("org.ow2.proactive.scripting.Script")
  
  # activate some debug info
  options(error = utils:::dump.frames)
  
}


local({
  pkg.root <- getwd()
  print("Building PARConnector from :")
  print(pkg.root)  
  .jinit()
  .jaddClassPath(dir(file.path(pkg.root,"inst","java"), full.names=TRUE))
  print(.jclassPath())
  # if this fails, there is a problem with classloading
  J("org.ow2.proactive.scripting.Script")
 })

