package dedupe

import (
	"context"
	"admission/state"
)

type Idempotency struct {
	store state.Store
}

func New(store state.Store) *Idempotency {
	return &Idempotency{store}
}

func (i *Idempotency) Seen(ctx context.Context, id string) bool {
	// Check if key exists in store
	_, ok := i.store.Get(ctx, id)
	return ok
}

func (i *Idempotency) Mark(ctx context.Context, id string) {
	// Mark key as seen with a TTL (managed by store implementation ideally, or simple Set)
	// For Redis, we might want to set expiration, but the Store interface currently is simple Set.
	// We assume the underlying store handles ephemeral nature or we add SetWithTTL later.
	i.store.Set(ctx, id, "seen")
}
