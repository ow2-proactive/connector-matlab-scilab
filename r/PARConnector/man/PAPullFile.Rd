\name{PAPullFile}
\alias{PAPullFile}
\title{Transfer a file from a ProActive Data space to the local machine}
\usage{
PAPullFile(space, pathname, outputFile, client = PAClient(), .nb.tries = 2,
  .print.stack = TRUE)
}
\arguments{
  \item{space}{name of the data space to transfer the file
  to}

  \item{pathname}{location of the file inside the remote
  data space}

  \item{outputFile}{local path of the file where the file
  will be copied to. The file must be absolute}

  \item{client}{connection handle to the scheduler, if not
  provided the handle created by the last call to
  \code{\link{PAConnect}} will be used}
}
\description{
\code{PAPullFile} will transfer a file existing in a shared
data space to the local computer. The Scheduler controls
two main spaces :\cr
}
\details{
\itemize{ \item{The USER Space}{ : a data space reserved
for a specific user.} \item{The GLOBAL Space}{ : a data
space accessible to all users.} }
}
\examples{
\dontrun{
 PAPullFile("USER","/in.txt",file.path(getwd(),"in2.txt")) # will transfer file at USER/in.txt to a local file in2.txt
 }
}
\seealso{
\code{\link{PAPushFile}}
}

