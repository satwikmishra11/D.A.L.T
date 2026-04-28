package shed

import (
	"context"
	"sync/atomic"

	"admission/observability"
	"admission/state"
)

type Shedder struct {
	inflight    atomic.Int64
	maxInflight int64
	store       state.Store // Kept if we want distributed coordination later
}

func New(store state.Store, limit int64) *Shedder {
	return &Shedder{
		maxInflight: limit,
		store:       store,
	}
}

// Enter attempts to acquire a concurrency token. 
// Returns true if allowed, false if rejected (load shedding).
func (s *Shedder) Enter(ctx context.Context) bool {
	current := s.inflight.Add(1)
	observability.InflightRequests.Set(float64(current))
	
	if current > s.maxInflight {
		s.inflight.Add(-1)
		observability.InflightRequests.Set(float64(current - 1))
		observability.Warn("Load shedding triggered", 
			observability.Field("current_inflight", current),
			observability.Field("limit", s.maxInflight),
		)
		return false
	}
	return true
}

func (s *Shedder) Exit(ctx context.Context) {
	current := s.inflight.Add(-1)
	observability.InflightRequests.Set(float64(current))
}

// SetLimit allows dynamic resizing of the concurrency limit
func (s *Shedder) SetLimit(limit int64) {
	atomic.StoreInt64(&s.maxInflight, limit)
}
