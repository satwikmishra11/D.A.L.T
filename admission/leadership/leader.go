package leadership

import (
	"context"
	"sync/atomic"
	"time"

	"github.com/google/uuid"
	"github.com/redis/go-redis/v9"
)

var (
	isLeader atomic.Bool
	nodeID   = uuid.NewString()
)

const (
	leaderKey = "admission:leader:lease"
	leaseTTL  = 5 * time.Second
)

func StartElection(rdb *redis.Client) {
	ctx := context.Background()
	go func() {
		ticker := time.NewTicker(2 * time.Second)
		defer ticker.Stop()

		for range ticker.C {
			if isLeader.Load() {
				// We are the leader, renew the lease
				renewLease(ctx, rdb)
			} else {
				// Try to acquire the lease
				tryAcquire(ctx, rdb)
			}
		}
	}()
}

func tryAcquire(ctx context.Context, rdb *redis.Client) {
	ok, err := rdb.SetNX(ctx, leaderKey, nodeID, leaseTTL).Result()
	if err == nil && ok {
		isLeader.Store(true)
	} else {
		isLeader.Store(false)
	}
}

func renewLease(ctx context.Context, rdb *redis.Client) {
	const renewScript = `
		if redis.call("get", KEYS[1]) == ARGV[1] then
			return redis.call("pexpire", KEYS[1], ARGV[2])
		else
			return 0
		end
	`
	res, err := rdb.Eval(ctx, renewScript, []string{leaderKey}, nodeID, int64(leaseTTL/time.Millisecond)).Result()
	if err != nil {
		isLeader.Store(false)
		return
	}
	if val, ok := res.(int64); ok && val == 1 {
		isLeader.Store(true)
	} else {
		isLeader.Store(false)
	}
}

func IsLeader() bool {
	return isLeader.Load()
}
