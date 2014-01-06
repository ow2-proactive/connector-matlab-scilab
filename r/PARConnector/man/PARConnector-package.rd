\name{PARConnector-package}
\alias{PARConnector-package}
\title{Parallel execution of R functions and split/merge workflows using ProActive Scheduler}
\author{The ProActive Team}



\description{

The \strong{PARConnector} package allows remote execution of R functions using the ProActive Scheduler.

The package features :
\itemize{
  \item {}{simple parametric sweep remote execution of R functions with a syntax similar to \code{\link{mapply}}}
  \item {}{automatic transfer of user-defined functions and their dependencies}
  \item {}{automatic transfer of input/output files}
  \item {}{primitive for waiting results}
  \item {}{general purpose primitives to check the current state of ProActive Scheduler}
  \item {}{ability to create complex split/merge workflows in a compact,user-friendly syntax}
}

}

\examples{
\dontrun{

A typical PARConnector session :

> library(PARConnector)

> PAConnect("http://localhost:8080/rest/rest","demo","demo")

Connected to Scheduler at  http://localhost:8080/rest/rest 
[1] "Java-Object{org.ow2.proactive.scheduler.rest.SchedulerClient@39f46204}"

> res = PASolve("cos",1:4)

Job submitted (id : 405)
 with tasks : t1, t2, t3, t4
 
> res

PARJob1 (id: 405)  (status: Running)
t1 : Pending
t2 : Running at 192.168.1.187 (local-LocalNodes-0) (0%)
t3 : Running at 192.168.1.187 (local-LocalNodes-2) (0%)
t4 : Pending

> PAWaitFor(res)

$t1
[1] 0.5403023

$t2
[1] -0.4161468

$t3
[1] -0.9899925

$t4
[1] -0.6536436

> res = PASolve(PAM("sum",
+                 PA(function(x) {x*x},
+                   PAS("identity", 1:4))))

Job submitted (id : 406)
 with tasks : t1, t2, t3, t4, t5, t6
 
> res

PARJob2 (id: 406)  (status: Running)
t1 : Running at 192.168.1.187 (local-LocalNodes-0) (0%)
t2 : Pending
t3 : Pending
t4 : Pending
t5 : Pending
t6 : Pending

> PAWaitFor(res) 

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
