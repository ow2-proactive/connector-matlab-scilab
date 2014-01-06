\name{PAJobState}
\alias{PAJobState}
\title{Retrieves information about a ProActive job}
\usage{
PAJobState(job.id, client = PAClient())
}
\arguments{
  \item{job.id}{id of the proactive job}

  \item{client}{connection handle to the scheduler, if not
  provided the handle created by the last call to
  \code{\link{PAConnect}} will be used}
}
\description{
\code{PAJobState} prints a formatted table diplaying the
state of a ProActive job.
}
\seealso{
\code{\link{PAState}}
}

