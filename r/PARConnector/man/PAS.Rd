\name{PAS}
\alias{PAS}
\title{Creates a single split PATask which can be used in combination with \code{\link{PA}} and \code{\link{PAM}} to create split/merge workflows}
\usage{
PAS(funcOrFuncName, ..., varies = NULL, input.files = list(),
  output.files = list(), in.dir = getwd(), out.dir = getwd(),
  hostname.selection = NULL, ip.selection = NULL,
  property.selection.name = NULL, property.selection.value = NULL,
  isolate.io.files = FALSE, client = PAClient(), .debug = PADebug())
}
\arguments{
  \item{funcOrFuncName}{function handle or function name}

  \item{...}{arguments of the funcOrFuncName function which
  will be vectorized over}

  \item{varies}{list of varying parameters which can be a
  parameter number or a parameter name, if NULL (default)
  then all parameters are varying}

  \item{input.files}{a list of input files which will be
  transferred from the local machine to the remote
  executions, see Details section in \code{\link{PA}} for
  more information}

  \item{output.files}{a list of output files which will be
  transferred from the remote executions to the local
  machine}

  \item{in.dir}{in case input files are used, the directory
  which will be used as base (default to current working
  directory)}

  \item{out.dir}{in.dir in case ouput files are used, the
  directory which will be used as base (default to current
  working directory)}

  \item{hostname.selection}{can be used to restrict the
  remote execution to a given machine, wildcards can be
  used}

  \item{ip.selection}{can be used to restrict the remote
  execution to a given machine given its IP address}

  \item{property.selection.name}{can be used to restrict
  the remote execution to a given JVM resource where the
  property is set to the according value}

  \item{property.selection.value}{is used in combination
  with property.selection.name}

  \item{client}{connection handle to the scheduler, if not
  provided the handle created by the last call to PAConnect
  will be used}

  \item{.debug}{debug mode}
}
\value{
a PATask object which can be submitted to the ProActive
Scheduler via a \code{\link{PASolve}} call or given as
parameter to other \code{\link{PA}}, \code{\link{PAS}} or
\code{\link{PAM}} functions
}
\description{
\code{PAS} uses the same parameter semantic as
\code{\link{PA}} , but instead of creating a set of
parallel tasks, it will produce a single task whose result
(expected to be a list of vector) will be scattered across
dependent tasks.
}
\details{
The cardinality will be, like for \code{\link{PA}},
determined by analysing the parameters and finding the
longest list/vector among them. But the cardinality will be
used in a different way, as it will be used when the result
of the \code{PAS} call is given to a \code{PA} call to
build a workflow. In that case, the cardinality of the
\code{PAS} result will be used to produce as many \code{PA}
tasks.
}
\examples{
\dontrun{
 PAS("identity", 1:4) # will produce a split task of cardinality 4 that will output the vector 1:4

 PA(function(x){x*x},  PAS("identity", 1:4)) # will produce 5 PATasks producing the following results :
 t1: 1:4
 t2: 1*1
 t3: 2*2
 t4: 3*3
 t5: 4*4

 Explanation : This is because the tasks created by the PAS call, given as parameter to the PA call will produce as many parallel tasks as the PAS task cardinality (here 4)
 as the PAS task produce the vector 1:4, the first PA task will receive the parameter 1, the second PA task will receive the parameter 2, etc...

 (PAS(function(out,ind){for (i in ind) {file.create(paste0(out,i))}}, "out", 1:4, output.files="out\%2\%") # will produce a split task of cardinality 4 that will create remotely the files out1, out2, out3 and out4 and transfer them back to the local machine

 }
}
\seealso{
\code{\link{PA}} \code{\link{PAM}} \code{\link{PASolve}}
\code{\link{mapply}} \code{\link{PAJobResult}}
\code{\link{PAConnect}}
}

