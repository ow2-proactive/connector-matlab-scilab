\name{PASolve}
\alias{PASolve}
\title{Create and submit a ProActive R Job}
\usage{
PASolve(..., client = PAClient(), .debug = PADebug(),
  jobName = str_c("PARJob", .peekNewSolveId()),
  jobDescription = "ProActive R Job", priority = "normal",
  cancelOnError = TRUE)
}
\arguments{
  \item{...}{list of PATasks produced by \code{\link{PA}}
  \code{\link{PAS}} or \code{\link{PAM}} calls}

  \item{client}{connection handle to the scheduler, if not
  provided the handle created by the last call to
  \code{\link{PAConnect}} will be used}

  \item{jobName}{name of the ProActive job to be created}

  \item{jobDescription}{description of this job}

  \item{priority}{priority of this job}

  \item{cancelOnError}{sets the cancelling mode mechanism
  whenever an error occur in one tasks, does it cancel the
  whole job ? Default to TRUE}
}
\value{
a \code{\link{PAJobResult}} object which acts as a
placeholder for receiving actual results
}
\description{
\code{PASolve} take in parameter a list of PATasks produced
by \code{\link{PA}} \code{\link{PAS}} or \code{\link{PAM}}
calls and submits a new job to ProActive Scheduler.
}
\details{
a \code{\link{PAJobResult}} object will be returned. The
object will bear the current state of the job, which can be
displayed simply by showing or printing the object. Special
functions \code{\link{PAWaitFor}} and
\code{\link{PAWaitAny}} can be used to wait for the
results.
}
\examples{
\dontrun{

 > res = PASolve("cos",1:4)   # shortcut for PASolve(PA("cos",1:4)), submits a parallel job of 4 tasks
 Job submitted (id : 405)
 with tasks : t1, t2, t3, t4
 > res                            # display the current state
 PARJob1 (id: 405)  (status: Running)
 t1 : Pending
 t2 : Running at 192.168.1.187 (local-LocalNodes-0) (0\%)
 t3 : Running at 192.168.1.187 (local-LocalNodes-2) (0\%)
 t4 : Pending
 > PAWaitFor(res)                 # wait for the results and return them in a list
 $t1
 [1] 0.5403023

 $t2
 [1] -0.4161468

 $t3
 [1] -0.9899925

 $t4
 [1] -0.6536436



 > res = PASolve(PAM("sum",
                 PA(function(x) {x*x},
                   PAS("identity", 1:4))))         # submits a split/merge job of six tasks

 > res
 PARJob2 (id: 406)  (status: Running)
 t1 : Running at 192.168.1.187 (local-LocalNodes-0) (0\%)
 t2 : Pending
 t3 : Pending
 t4 : Pending
 t5 : Pending
 t6 : Pending

 > PAWaitFor(res)        # wait for the results and return them in a list
 $t1
 [1] 1 2 3 4

 $t2
 [1] 1

 $t3
 [1] 4

 $t4
 [1] 9

 $t5
 [1] 16

 $t6
 [1] 30
 }
}
\seealso{
\code{\link{PA}} \code{\link{PAS}} \code{\link{PAM}}
\code{\link{PAJobResult}} \code{\link{PAConnect}}
}

