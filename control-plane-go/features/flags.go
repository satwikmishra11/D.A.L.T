package features

var flags = map[string]map[string]bool{
	"free-tier": {
		"chaos-mode": false,
	},
	"enterprise": {
		"chaos-mode": true,
	},
}

func Enabled(org, feature string) bool {
	if f, ok := flags[org]; ok {
		return f[feature]
	}
	return false
}
