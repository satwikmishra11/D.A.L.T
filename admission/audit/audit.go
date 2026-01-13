package audit

import (
	"admission/observability"
	"go.uber.org/zap"
)

type Event struct {
	OrgID  string
	Action string
	Reason string
}

var auditCh = make(chan Event, 1000)

func init() {
	go process()
}

func Record(org, action, reason string) {
	select {
	case auditCh <- Event{OrgID: org, Action: action, Reason: reason}:
	default:
		// Drop audit log if channel is full to prevent blocking critical path
		observability.Warn("Audit channel full, dropping event")
	}
}

func process() {
	for e := range auditCh {
		observability.Info("audit_log",
			zap.String("event_type", "AUDIT"),
			zap.String("org_id", e.OrgID),
			zap.String("action", e.Action),
			zap.String("reason", e.Reason),
		)
	}
}
