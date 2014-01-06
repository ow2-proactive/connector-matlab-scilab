\docType{methods}
\name{PAWaitAny}
\alias{PAWaitAny}
\title{Waits for the first available result among a list of results controlled by a PAJobResult object}
\usage{
PAWaitAny(paresult = PALastResult(), ...)
}
\arguments{
  \item{paresult}{a PAJobResult object}

  \item{timeout}{a long value specifying an optional
  timeout in milisecond}

  \item{callback}{a single parameter function which can be
  called when results are received. It can be useful to
  udapte graphical user interfaces for examples. Default to
  NULL.}
}
\value{
A result
}
\description{
PAWaitAny is used on a PAJobResult object to block the R
interpreter until the first result is available. The R
result object will be then returned as a factor, named by
the task name. If the PAWaitAny is called a second time,
then the second result will be waited and returned. After
all results are consumed, a call to PAWaitAny will return
NA.
}
\seealso{
\code{\link{PASolve}} and \code{\link{PAWaitFor}}
}

