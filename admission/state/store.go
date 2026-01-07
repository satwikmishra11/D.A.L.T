package state

type Store interface {
	Get(key string) (string, bool)
	Set(key, value string)
}
