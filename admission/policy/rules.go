package policy

import "errors"

func RequireApproval(status string) Rule {
	return func(ctx Context) error {
		if status != "APPROVED" {
			return errors.New("scenario not approved")
		}
		return nil
	}
}

func MaxDuration(seconds int32) Rule {
	return func(ctx Context) error {
		if ctx.Duration > seconds {
			return errors.New("duration limit exceeded")
		}
		return nil
	}
}
