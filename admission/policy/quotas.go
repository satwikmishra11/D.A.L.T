package policy

import "errors"

var orgUserQuota = map[string]int32{
	"free-tier": 1000,
	"pro":       50_000,
}

func EnforceUserQuota() Rule {
	return func(ctx Context) error {
		limit, ok := orgUserQuota[ctx.OrgID]
		if ok && ctx.Users > limit {
			return errors.New("organization quota exceeded")
		}
		return nil
	}
}
