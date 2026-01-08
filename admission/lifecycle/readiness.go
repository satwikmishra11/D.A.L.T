package lifecycle

import (
	"context"
	"time"

	"admission/state"
)

func WaitForRedis(store state.Store) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	for {
		if _, ok := store.Get(ctx, "ping"); ok || ctx.Err() != nil {
			return
		}
		time.Sleep(500 * time.Millisecond)
	}
}
