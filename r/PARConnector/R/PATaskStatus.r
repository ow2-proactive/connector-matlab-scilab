setClass( 
  Class="PATaskStatus", 
  representation = representation(
    status = "character",
    alive = "function",
    description = "function"),
  prototype=prototype(
    status = "SUBMITTED",
    alive = function(status) {
      .enum(status,SUBMITTED=TRUE, PENDING=TRUE, PAUSED=TRUE, RUNNING=TRUE, WAITING_ON_ERROR=TRUE, WAITING_ON_FAILURE=TRUE, FAILED=FALSE, NOT_STARTED=FALSE, NOT_RESTARTED=FALSE, ABORTED=FALSE, FAULTY=FALSE, FINISHED=FALSE, SKIPPED=FALSE)
    },
    description = function(status) {
      .enum(status,SUBMITTED="Submitted", PENDING="Pending", PAUSED="Paused", RUNNING="Running", WAITING_ON_ERROR="Faulty...", WAITING_ON_FAILURE="Failed...", FAILED="Resource down", NOT_STARTED="Could not start", NOT_RESTARTED="Could not restart", ABORTED="Aborted", FAULTY="Faulty", FINISHED="Finished", SKIPPED="Skipped")
    }    
  )  
)

PAJobStatus <- function(status) {
  new (Class="PATaskStatus" , status = status)
}

setMethod("toString","PATaskStatus",
          function(x) {
            object <- x
            output <- object@description(object@status)
          }
)

setMethod("show" ,"PATaskStatus" ,
          function(object) {
            cat(toString(object))                                 
          } 
)

setGeneric(
  name="isTaskAlive",
  def=function(object) {standardGeneric("isTaskAlive" )}  
)

setMethod("isTaskAlive" ,"PATaskStatus" ,
          function(object) {
            return(object@alive(object@status))                                
          } 
)