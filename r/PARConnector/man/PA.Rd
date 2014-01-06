\name{PA}
\alias{PA}
\title{Creates parallel PATasks which can be submitted to PASolve}
\usage{
PA(funcOrFuncName, ..., varies = NULL, input.files = list(),
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
  executions, see Details for more information}

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

  \item{isolate.io.files}{should input/output files be
  isolated in the remote executions, default FALSE.  If set
  to TRUE, when input and output files are copied to
  USER/GLOBAL space or to the NODE execution, they will be
  isolated in a folder specific to the current job.  It
  thus guaranties that they will be separated from other
  jobs execution. On the other hand it will not be possible
  to reuse the remote files directly in other jobs.}

  \item{client}{connection handle to the scheduler, if not
  provided the handle created by the last call to PAConnect
  will be used}

  \item{.debug}{debug mode}
}
\value{
a list of PATask objects which can be submitted to the
ProActive Scheduler via a \code{\link{PASolve}} call or
given as parameter to other \code{\link{PA}},
\code{\link{PAS}} or \code{\link{PAM}} functions
}
\description{
\code{PA} creates a list of PATasks using a syntax similar
to mapply. Where mapply applies multi-parameters to a
function, PA will create multi-parameter remote executions
of a given function.
}
\details{
The function can be provided via its name or via a closure
object. For builtin functions, it is necessary to provide
the name instead of the closure. For user defined function,
the function will be analysed and all its depdendencies
will be automatically transferred to the remote executions.
Dependencies can include other functions or variables
defined in the function closure. If the function has a
dependency on a package, it's mandatory to manually install
and load the package in the remote R executions.
PARConnector does not handle automatic package
installation. It's of course possible though to do the
installation and loading of a package from within the
function provided to \code{PA}

The cardinality (the number of PATask to be created) will
be determined by analysing the parameters. If the
parameters contains lists or vectors, the biggest length
will be the cardinality. Only parameters of the following
types logical, integer, character, double, complex, raw,
and list will be considered.

It is possible to force unvarying parameters (which will
not be taken into account when computing the cardinality),
those parameters will be transmitted as they are to the
remote evaluations, and will not be scattered. Similarly to
mapply, varying lists or vector which are smaller than the
cardinality will be extended via looping to match the
cardinality. See \code{\link{mapply}} for more information.

When used alone, PA allows to create parallel independant
tasks. When used in combination with the two other job
conscrution primitives (\code{\link{PAM}} and
\code{\link{PAS}}), it allows to create split/merge
workflows.

\subsection{\strong{Input/Output Files patterns}}{

Files path in input.files and output.files list can contain
special patterns which are detailed below :\cr

\subsection{\strong{a) Location Patterns}}{

This pattern must be used in the beginning of the path and
determines the itinerary of the file from the local
computer to the remote compute engine.

The semantic of these patterns varies wether the file is an
input file or an output file. It can take the following
values : \cr \itemize{ \item{"$LOCAL:"}{\cr \cr
\emph{Input} : the LOCAL pattern means that the file path
references a file existing on the local machine and will be
transferred to the remote node with an intermediate copy in
the USER space. (LOCAL to USER , USER to NODE)\cr
\emph{Output} : the LOCAL pattern means that the file will
be produced by a remote execution and transferred back to
the Local machine, with an intermediate copy in the USER
space (NODE to USER , USER to LOCAL)\cr }

\item{"$USER:"}{\cr \cr \emph{Input} : the USER pattern
means that the file path references a file existing on the
USER space and will be transferred to the remote node (USER
to NODE)\cr \emph{Output} : the USER pattern means that the
file will be produced by a remote execution and transferred
back to the USER space (NODE to USER)\cr }

\item{"$GLOBAL:"}{\cr \cr \emph{Input} : the USER pattern
means that the file path references a file existing on the
GLOBAL space and will be transferred to the remote node
(GLOBAL to NODE)\cr \emph{Output} : the USER pattern means
that the file will be produced by a remote execution and
transferred back to the GLOBAL space (NODE to GLOBAL)\cr }

\item{"$LOCAL:$USER:" and "$LOCAL:$GLOBAL:"}{\cr \cr
\emph{Input} : Only valid for input files. It is the
explicit version of the $LOCAL: pattern for input files
(which is equivalent to $LOCAL:$USER:), this notation
allows to choose the GLOBAL space instead of the USER space
as intermediate\cr }

\item{"$USER:$LOCAL:" and "$GLOBAL:$LOCAL:"} {\cr \cr
\emph{Output} : Only valid for output files. It is the
explicit version of the $LOCAL: pattern for output
files(which is equivalent to $USER:$LOCAL:), this notation
allows to choose the GLOBAL space instead of the USER space
as intermediate\cr } } }

\subsection{\strong{b) Parameter Patterns}}{ This pattern
can be used anywhere in the file path and will be replaced
by parameters of the funcOrFuncName function taken from the
... list.  The pattern is of the form %expr%, where expr
can take the following values : \itemize{ \item{An integer
i}{\cr In that case \%expr\% refers to the parameter at
index i. For each individual PATask created by the PA call,
the \%expr\% pattern will be replaced by the value of the
parameter i for this execution.  If this value is a scalar
value V, the pattern will generate a single input/output
file containing the toString coercion of V.  If this value
is a vector or list, the pattern will generate multiple
input/output files with replacements taken from the
vector/list.  } \item{A character string S}{\cr In that
case \%expr\% refers to the parameter named S. The semantic
is similar to when using an integer parameter reference.  }
\item{An integer vector V}{\cr In that case \%expr\% refers
to multiple parameters at index taken from V. It will use
the parameter values and generate as many input/output
files as elements of V.  } }

The parameter replacement will be done by using the
toString coercion on the parameter value, but if the
parameter referenced is a PATask (i.e. a result of a PA
call), the pattern will be replaced by the same
replacements that were done inside this PATask.  This is
particularly useful when build split-merge workflows, where
an initial replacement needs to be transferred to dependant
tasks.  } }
}
\examples{
\dontrun{
 PA("cos", 1:4)      # will produce 4 PATasks : cos(1) , cos(2), cos(3) and cos(4) (parametric sweep with one parameter)

 PA("sum", 1:4, 1:2)            # will produce 4 PATasks : sum(1,1) , sum(2,2), sum(3,1) and sum(4,2)    (parametric sweep with two parameters)

 PA("c", 1:4, 1:2, varies= list(1) )               # will produce 4 PATasks : c(1,1:2) , sum(2,1:2), sum(3,1:2) and sum(4,1:2)  ( parametric sweep with one varying parameter and one fixed parameter)

 PA( function(in,i) file.show(paste0(in,i)),"in", 1:4, input.files="in\%2\%")     # will produce 4 PATasks which transfer the following files in1, in2, in3, in4 and display their content

 PA( function(in,out,i) file.copy(paste0(in,i), paste0(out,i)),"in","out" 1:4, input.files="in\%3\%", output.files="out\%3\%") # will produce 4 PATasks which transfer the following files in1, in2, in3, in4 and transfer back out1, out2, out3, out4

 To submit tasks simply pass the produced tasks to a PASolve call :

 PASolve(PA("cos", 1:4))

 See examples in  PAS and PAM help sections for split/merge examples

 }
}
\seealso{
\code{\link{PAS}} \code{\link{PAM}} \code{\link{PASolve}}
\code{\link{mapply}} \code{\link{PAJobResult}}
\code{\link{PAConnect}}
}

