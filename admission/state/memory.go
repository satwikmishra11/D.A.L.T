package state

import (
	"context"
	"sync"
	"sync/atomic"
)

type MemoryStore struct {
	data   sync.Map
	limits sync.Map // map[string]*int64
}

func NewMemoryStore() *MemoryStore {
	return &MemoryStore{}
}

func (m *MemoryStore) Get(ctx context.Context, k string) (string, bool) {
	v, ok := m.data.Load(k)
	if !ok {
		return "", false
	}
	return v.(string), true
}

func (m *MemoryStore) Set(ctx context.Context, k, v string) {
	m.data.Store(k, v)
}

// In-memory approximation of Acquire/Release
func (m *MemoryStore) Acquire(ctx context.Context, key string, limit int64) bool {
	val, _ := m.limits.LoadOrStore(key, new(int64))
	counter := val.(*int64)
	
	if atomic.AddInt64(counter, 1) > limit {
		atomic.AddInt64(counter, -1)
		return false
	}
	return true
}

func (m *MemoryStore) Release(ctx context.Context, key string) {
	val, ok := m.limits.Load(key)
	if ok {
		atomic.AddInt64(val.(*int64), -1)
	}
}