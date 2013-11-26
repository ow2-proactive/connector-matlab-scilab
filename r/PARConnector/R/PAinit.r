.onLoad <- function(libname, pkgname) {

  .jpackage(pkgname, lib.loc = libname)

  #.jinit()
 
  .jaddClassPath(dir(file.path(libname,pkgname,"java"), full.names=TRUE))
  
  # if this fails, there is a problem with classloading
  J("org.ow2.proactive.scripting.Script")
  
}



# PAinit <- function(sys) {
#   if (!exists("PA.initialized")) {
#     PA.initialized <<- FALSE
#   }
#   if (!PA.initialized) {
#     pkg.root <- system.file(package="PARConnector")
#     if (length(pkg.root) == 0) {
#       # package is not loaded, we are building it
#       pkg.root <- getwd()
#       print("Building PARConnector from :")
#       print(pkg.root)
#     } else {
#       print("Initializing PAConnector...")
#       print(pkg.root)
#     }
#     
#     .jinit()
#     
#     .jaddClassPath(dir(file.path(pkg.root,"java"), full.names=TRUE))
#     # if this fails, there is a problem with classloading
#     J("org.ow2.proactive.scripting.Script")
#     PA.initialized <<- TRUE
#   }
# }

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

