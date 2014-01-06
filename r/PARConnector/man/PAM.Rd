\name{PAM}
\alias{PAM}
\title{Creates a single merge PATask which can be used in combination with \code{\link{PA}} and \code{\link{PAS}} to create split/merge workflows}
\usage{
PAM(funcOrFuncName, ..., varies = list(), input.files = list(),
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
\code{PAM} uses the same parameter semantic as
\code{\link{PA}} , but instead of creating a set of
parallel tasks, it will produce a single task which will
aggregate the results of a list of tasks produced by
\code{\link{PA}}.
}
\details{
\code{PAM} has always a cardinality of 1, it is used solely
as a multi-parameter aggregation.
}
\examples{
\dontrun{
 see examples in PAS and PA help sections before reading these examples

 PAM("sum",
   PA(function(x) {x*x},
      PAS("identity", 1:4)))  # will produce 6 PATasks producing the following results :

 t1: 1:4
 t2: 1*1
 t3: 2*2
 t4: 3*3
 t5: 4*4
 t6: sum(1*1,2*2,3*3,4*4)

 Explanation for t6 : the lower part of the statement produces 4 parallel tasks which are given as parameter to the \\code{PAM} call with the sum function.
 The results of those tasks are merged via the sum function, similar to sum( res[t2],res[t3],res[t4],res[t4]) , of course this is possible only with function which accept variable number of parameters

 }
}
\seealso{
\code{\link{PA}} \code{\link{PAS}} \code{\link{PASolve}}
\code{\link{mapply}} \code{\link{PAConnect}}
}

