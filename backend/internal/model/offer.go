package model

import (
	"fmt"
	"strings"

	"github.com/guregu/null/v5"
)

// NewSalary validates a salary: positive whole pesos, a known period and max >= min. A max equal to the min is a
// single amount.
func NewSalary(min int64, max null.Int64, period SalaryPeriod) (Salary, error) {
	if min <= 0 {
		return Salary{}, fmt.Errorf("%w: salary min must be positive", ErrInvalid)
	}
	if !period.Valid() {
		return Salary{}, fmt.Errorf("%w: unknown salary period %q", ErrInvalid, period)
	}
	if max.Valid && max.Int64 < min {
		return Salary{}, fmt.Errorf("%w: salary max below min", ErrInvalid)
	}
	if max.Valid && max.Int64 == min {
		max = null.Int64{}
	}
	return Salary{Min: min, Max: max, Period: period}, nil
}

// NewOfferContent validates what a business writes: every text field is required and the category must be known.
func NewOfferContent(title string, category Category, schedule, description string, salary *Salary) (OfferContent, error) {
	content := OfferContent{Title: strings.TrimSpace(title), Category: category, Schedule: strings.TrimSpace(schedule), Description: strings.TrimSpace(description), Salary: salary}
	if content.Title == "" || content.Schedule == "" || content.Description == "" {
		return OfferContent{}, fmt.Errorf("%w: title, schedule and description are required", ErrInvalid)
	}
	if !category.Valid() {
		return OfferContent{}, fmt.Errorf("%w: unknown category %q", ErrInvalid, category)
	}
	return content, nil
}
