\docType{methods}
\name{PAWaitFor}
\alias{PAWaitFor}
\title{Waits for all results controlled by  a PAJobResult object}
\usage{
PAWaitFor(paresult = PALastResult(), ...)
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
A list of results
}
\description{
PAWaitFor is used on a PAJobResult object to block the R
interpreter until all results are available. The R result
objects will be then returned inside a list. It is possible
to wait for a subset instead of the whole list by using
subscript indexing : PAWaitFor(res[1:3]) will wait for only
the results at index 1,2,3.
}
\seealso{
\code{\link{PASolve}} and \code{\link{PAWaitAny}}
}

