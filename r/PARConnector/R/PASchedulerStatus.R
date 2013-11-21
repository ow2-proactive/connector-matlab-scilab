setClass( 
  Class="PASchedulerStatus", 
  representation = representation(
    status = "character",
    description = "function"),
  prototype=prototype(
    status = "UNLINKED",
    description = function(status) {
      .enum(status, STARTED="Started", STOPPED="Stopped", FROZEN="Frozen", PAUSED="Paused", SHUTTING_DOWN="Shutting down", UNLINKED="Unlinked from RM", KILLED="Killed", DB_DOWN="Killed (DB down)")
    }
    )  
)

PASchedulerStatus <- function(status) {
  new (Class="PASchedulerStatus" , status = status)
}

setMethod("toString","PASchedulerStatus",
          function(x) {
            object <- x
            output <- object@description(object@status)
          }
)

setMethod("show" ,"PASchedulerStatus" ,
          function(object) {
            cat(toString(object))                                 
          } 
)

setGeneric(
  name="isSubmittable",
  def=function(object) {standardGeneric("isSubmittable" )}  
)

setGeneric(
  name="isKilled",
  def=function(object) {standardGeneric("isKilled" )}  
)

setGeneric(
  name="isShuttingDown",
  def=function(object) {standardGeneric("isShuttingDown" )}  
)

setMethod("isKilled" ,"PASchedulerStatus" ,
          function(object) {
            object@status == "SHUTTING_DOWN" || object@status == "KILLED" || object@status == "DB_DOWN"                                
          } 
)

setMethod("isShuttingDown" ,"PASchedulerStatus" ,
          function(object) {
            isKilled(object) || object@status == "SHUTTING_DOWN"                               
          } 
)

setMethod("isSubmittable" ,"PASchedulerStatus" ,
          function(object) {
            isShuttingDown(object) || object@status != "STOPPED"                                
          } 
)


    