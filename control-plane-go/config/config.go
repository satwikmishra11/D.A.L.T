package config

import (
	"encoding/json"
	"os"
	"sync"
)

var (
	mu     sync.RWMutex
	config map[string]interface{}
)

func Load(path string) error {
	data, err := os.ReadFile(path)
	if err != nil {
		return err
	}

	mu.Lock()
	defer mu.Unlock()
	return json.Unmarshal(data, &config)
}

func Get(key string) interface{} {
	mu.RLock()
	defer mu.RUnlock()
	return config[key]
}
