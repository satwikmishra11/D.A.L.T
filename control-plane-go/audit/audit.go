package audit

import (
	"log"
	"time"
)

func Record(org, action, reason string) {
	log.Printf(
		"[AUDIT] org=%s action=%s reason=%s ts=%s",
		org,
		action,
		reason,
		time.Now().UTC(),
	)
}
