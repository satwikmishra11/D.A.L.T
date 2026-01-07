package dedupe

import "control-plane-go/state"

type Idempotency struct {
	store state.Store
}

func New(store state.Store) *Idempotency {
	return &Idempotency{store}
}

func (i *Idempotency) Seen(id string) bool {
	_, ok := i.store.Get(id)
	return ok
}

func (i *Idempotency) Mark(id string) {
	i.store.Set(id, "done")
}
