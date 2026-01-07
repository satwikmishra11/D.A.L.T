package observability

import "sync/atomic"

var (
	requestsTotal atomic.Int64
	requestsDenied atomic.Int64
)

func IncTotal() {
	requestsTotal.Add(1)
}

func IncDenied() {
	requestsDenied.Add(1)
}
