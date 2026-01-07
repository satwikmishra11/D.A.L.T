package lifecycle

import (
	"context"
	"os"
	"os/signal"
	"syscall"
)

func Wait(ctx context.Context) {
	sig := make(chan os.Signal, 1)
	signal.Notify(sig, syscall.SIGTERM, syscall.SIGINT)
	<-sig
}
