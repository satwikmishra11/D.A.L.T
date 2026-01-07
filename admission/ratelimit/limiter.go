package ratelimit

import (
	"sync"
	"time"
)

type Limiter struct {
	mu      sync.Mutex
	tokens  int
	lastRef time.Time
}

func New(max int) *Limiter {
	return &Limiter{
		tokens:  max,
		lastRef: time.Now(),
	}
}

func (l *Limiter) Allow() bool {
	l.mu.Lock()
	defer l.mu.Unlock()

	if time.Since(l.lastRef) > time.Second {
		l.tokens = 10
		l.lastRef = time.Now()
	}

	if l.tokens <= 0 {
		return false
	}

	l.tokens--
	return true
}
