### Common

setGeneric(
  name="getName",
  def=function(object,value) {standardGeneric("getName")}  
)

setGeneric(
  name="setName",
  def=function(object,value) {standardGeneric("setName")}  
) 

setGeneric(
  name="getJavaObject",
  def=function(object,value) {standardGeneric("getJavaObject" )}  
)

setGeneric(
  name="getDefinition",
  def=function(object,value) {standardGeneric("getDefinition")}  
)
setGeneric(
  name="setDefinition",
  def=function(object,value) {standardGeneric("setDefinition")}  
)

### PAJob

setGeneric(
  name="getProjectName",
  def=function(object,value) {standardGeneric("getProjectName")}  
)
setGeneric(
  name="setProjectName",
  def=function(object,value) {standardGeneric("setProjectName")}  
) 

setGeneric(
  name="addTask<-",
  def=function(object,value) {standardGeneric("addTask<-" )}  
)


### PATask

setGeneric(
  name="getScript",
  def=function(object) {standardGeneric("getScript")}  
)

setGeneric(
  name="setScript",
  def=function(object,value) {standardGeneric("setScript")}  
) 

setGeneric(
  name="getDescription",
  def=function(object,value) {standardGeneric("getDescription" )}  
) 

setGeneric(
  name="setDescription",
  def=function(object,value) {standardGeneric("setDescription" )}  
) 

setGeneric(
  name="getInputFiles",
  def=function(object,value) {standardGeneric("getInputFiles" )}  
)

setGeneric(
  name="getOutputFiles",
  def=function(object,value) {standardGeneric("getOutputFiles" )}  
)

setGeneric(
  name="getSelectionScripts",
  def=function(object,value) {standardGeneric("getSelectionScripts" )}  
)

setGeneric(
  name="getPreScript",
  def=function(object,value) {standardGeneric("getPreScript" )}  
)

setGeneric(
  name="setPreScript",
  def=function(object,value) {standardGeneric("setPreScript" )}  
)

setGeneric(
  name="getPostScript",
  def=function(object,value) {standardGeneric("getPostScript" )}  
)

setGeneric(
  name="setPostScript",
  def=function(object,value) {standardGeneric("setPostScript" )}  
)

setGeneric(
  name="getCleanScript",
  def=function(object,value) {standardGeneric("getCleanScript" )}  
)

setGeneric(
  name="setCleanScript",
  def=function(object,value) {standardGeneric("setCleanScript" )}  
)

setGeneric(
  name="addDependency<-",
  def=function(object,value) {standardGeneric("addDependency<-" )}  
) 

setGeneric(
  name="addInputFiles<-",
  def=function(object,value) {standardGeneric("addInputFiles<-" )}  
)

setGeneric(
  name="addOutputFiles<-",
  def=function(object,value) {standardGeneric("addOutputFiles<-" )}  
)

setGeneric(
  name="addSelectionScript<-",
  def=function(object,value) {standardGeneric("addSelectionScript<-" )}  
)

### PAJobResult
setGeneric(
  name="PAWaitFor",
  def=function(paresult, ...) {standardGeneric("PAWaitFor" )}  
)



.cat_list <- function(ll) {
  cat(.toString_list(ll))  
}

.toString_list <- function(ll) {
  output <- t("{ ")
  for (k in 1:length(ll)) {
    output <- str_c(output,toString(ll[[k]])," ")
    if (k < length(ll) ) {        
      output <- str_c(output,",")     
    }
  }
  output <- str_c(output,"}")
  return(output)
}

.enum <- function ( name, ...)
{
  choices <- list( ... )
  names   <- attr(choices,"names")
  
  pos <- pmatch( name, names )
  
  max <- length(names)
  
  if ( any( is.na(pos) ) )
    stop("symbolic value must be chosen from ", list(names) )
  else if ( (max+1) %in% pos )
    pos <- seq_along(names)
  
  id  <- unlist(choices[pos])
  
  if ( length(id) > 1 )
    stop("multiple choices not allowed")
  
  return( id )
}