package leadership

import (
	"sync/atomic"
	"time"
)

var leader atomic.Bool

func StartElection() {
	go func() {
		for {
			leader.Store(true) // mock leader election
			time.Sleep(10 * time.Second)
		}
	}()
}

func IsLeader() bool {
	return leader.Load()
}
