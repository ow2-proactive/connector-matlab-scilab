\name{PAPushFile}
\alias{PAPushFile}
\title{Transfer a file from the local machine to a ProActive Data space}
\usage{
PAPushFile(space, path, fileName, inputFile, client = PAClient(),
  .print.stack = TRUE)
}
\arguments{
  \item{space}{name of the data space to transfer the file
  to}

  \item{path}{path inside the remote data space where the
  file will be copied to}

  \item{fileName}{name of the file that will be created in
  the remote data space}

  \item{inputFile}{local path of the file}

  \item{client}{connection handle to the scheduler, if not
  provided the handle created by the last call to
  \code{\link{PAConnect}} will be used}
}
\description{
\code{PAPushFile} will copy a local file to a shared data
space available to a ProActive Scheduler. The Scheduler
controls two main spaces :\cr
}
\details{
\itemize{ \item{The USER Space}{ : a data space reserved
for a specific user.} \item{The GLOBAL Space}{ : a data
space accessible to all users.} }
}
\examples{
\dontrun{
 PAPushFile("USER","/","in.txt", "in.txt") # will transfer local file in.txt to the USER space
 }
}
\seealso{
\code{\link{PAPullFile}}
}

