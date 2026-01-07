package shed

import "sync/atomic"

var inflight atomic.Int64

const maxInflight = 500

func Allow() bool {
	return inflight.Load() < maxInflight
}

func Enter() {
	inflight.Add(1)
}

func Exit() {
	inflight.Add(-1)
}
