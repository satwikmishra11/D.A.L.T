package observability

import "log"

func Info(msg string, fields map[string]interface{}) {
	log.Printf("[INFO] %s %+v", msg, fields)
}

func Error(msg string, fields map[string]interface{}) {
	log.Printf("[ERROR] %s %+v", msg, fields)
}
