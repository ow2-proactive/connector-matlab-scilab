\name{PAState}
\alias{PAState}
\title{Display the list of jobs in the scheduler wether pending, running or finished}
\usage{
PAState(client = PAClient())
}
\arguments{
  \item{client}{connection handle to the scheduler, if not
  provided the handle created by the last call to PAConnect
  will be used}
}
\description{
\code{PAState} display the current state of the scheduler
with a list of jobs
}
\seealso{
\code{\link{PAConnect}}
}

