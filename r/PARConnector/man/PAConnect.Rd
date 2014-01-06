\name{PAConnect}
\alias{PAConnect}
\title{Connects to a ProActive Scheduler}
\usage{
PAConnect(url, login, pwd, cred = NULL, .print.stack = TRUE)
}
\arguments{
  \item{url}{url of ProActive Scheduler}

  \item{login}{login of the user}

  \item{pwd}{password of the user, if not provided a popup
  window will ask to type the password}

  \item{cred}{(default to NULL) the path to an encrypt
  credential file which stores the login information (see
  ProActive Scheduler manual for more details)}

  \item{.print.stack}{(default to TRUE) in case there is a
  connection problem, should the full Java stack trace be
  printed or simply the error message}
}
\value{
a scheduler connection handle, which can be used in other
PARConnector functions
}
\description{
\code{PAConnect} connects to a running ProActive Scheduler
using its url and login information. The url and login
information can be provided inside the PAConnect call or
asked interactively.
}
\examples{
\dontrun{
 PAConnect("http://localhost:8080/rest/rest","demo","demo") # connects to a local ProActive Scheduler running on port 8080 with demo credentials

 }
}
\seealso{
\code{\link{PASolve}}
}

