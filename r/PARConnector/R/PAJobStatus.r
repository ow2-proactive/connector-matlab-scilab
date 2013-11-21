setClass( 
  Class="PAJobStatus", 
  representation = representation(
    status = "character",
    alive = "function",
    description = "function"),
  prototype=prototype(
    status = "PENDING",
    alive = function(status) {
      .enum(status,PENDING=TRUE, RUNNING=TRUE, STALLED=TRUE, FINISHED=FALSE, PAUSED=TRUE, CANCELED=FALSE, FAILED=FALSE, KILLED=FALSE)
    },
    description = function(status) {
      .enum(status,PENDING="Pending", RUNNING="Running", STALLED="Stalled", FINISHED="Finished", PAUSED="Paused", CANCELED="Cancelled", FAILED="Failed", KILLED="Killed")
    }    
  )  
)

PAJobStatus <- function(status) {
  new (Class="PAJobStatus" , status = status)
}

setMethod("toString","PAJobStatus",
          function(x) {
            object <- x
            output <- object@description(object@status)
          }
)

setMethod("show" ,"PAJobStatus" ,
          function(object) {
            cat(toString(object))                                 
          } 
)

setGeneric(
  name="isJobAlive",
  def=function(object) {standardGeneric("isJobAlive" )}  
)

setMethod("isJobAlive" ,"PAJobStatus" ,
          function(object) {
            return(object@alive(object@status))                                
          } 
)