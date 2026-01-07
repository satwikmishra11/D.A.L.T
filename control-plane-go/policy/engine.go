package policy

import "errors"

type Context struct {
	OrgID    string
	Users    int32
	Duration int32
}

type Rule func(ctx Context) error

type Engine struct {
	rules []Rule
}

func NewEngine(rules ...Rule) *Engine {
	return &Engine{rules: rules}
}

func (e *Engine) Evaluate(ctx Context) error {
	for _, rule := range e.rules {
		if err := rule(ctx); err != nil {
			return err
		}
	}
	return nil
}

var ErrDenied = errors.New("execution denied by policy")
