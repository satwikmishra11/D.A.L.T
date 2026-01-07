package policy

import "errors"

func Validate(users int32, duration int32, status string) error {
	if status != "APPROVED" {
		return errors.New("scenario not approved")
	}

	if orgId == "free-tier" && users > 1000 {
	return errors.New("org limit exceeded")
	}

	if users > 100_000 {
		return errors.New("user limit exceeded")
	}

	if duration > 3600 {
		return errors.New("duration exceeds 1 hour")
	}

	return nil
}
