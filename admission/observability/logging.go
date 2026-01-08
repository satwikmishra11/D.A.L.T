package observability

import (
	"encoding/json"
	"log"
	"time"
)

type Log struct {
	Level   string                 `json:"level"`
	Message string                 `json:"message"`
	Time    string                 `json:"time"`
	Fields  map[string]interface{} `json:"fields,omitempty"`
}

func Info(msg string, fields map[string]interface{}) {
	write("INFO", msg, fields)
}

func Error(msg string, fields map[string]interface{}) {
	write("ERROR", msg, fields)
}

func write(level, msg string, fields map[string]interface{}) {
	entry := Log{
		Level:   level,
		Message: msg,
		Time:    time.Now().UTC().Format(time.RFC3339),
		Fields:  fields,
	}
	b, _ := json.Marshal(entry)
	log.Println(string(b))
}
