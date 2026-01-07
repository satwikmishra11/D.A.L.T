package state

import "sync"

type MemoryStore struct {
	data sync.Map
}

func NewMemoryStore() *MemoryStore {
	return &MemoryStore{}
}

func (m *MemoryStore) Get(k string) (string, bool) {
	v, ok := m.data.Load(k)
	if !ok {
		return "", false
	}
	return v.(string), true
}

func (m *MemoryStore) Set(k, v string) {
	m.data.Store(k, v)
}
