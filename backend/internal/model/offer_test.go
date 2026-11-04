package model

import (
	"testing"

	"github.com/guregu/null/v5"
	"github.com/stretchr/testify/require"
)

func TestNewSalary_SingleAmount(t *testing.T) {
	// when
	got, err := NewSalary(600_000, null.Int64{}, SalaryPeriodMonth)

	// then
	require.NoError(t, err)
	require.Equal(t, Salary{Min: 600_000, Period: SalaryPeriodMonth}, got)
}

func TestNewSalary_Range(t *testing.T) {
	// when
	got, err := NewSalary(5_000, null.IntFrom(7_000), SalaryPeriodHour)

	// then
	require.NoError(t, err)
	require.Equal(t, Salary{Min: 5_000, Max: null.IntFrom(7_000), Period: SalaryPeriodHour}, got)
}

func TestNewSalary_SameAmountAtBothEndsIsASingleAmount(t *testing.T) {
	// when
	got, err := NewSalary(600_000, null.IntFrom(600_000), SalaryPeriodMonth)

	// then
	require.NoError(t, err)
	require.Equal(t, Salary{Min: 600_000, Period: SalaryPeriodMonth}, got)
}

func TestNewSalary_Invalid(t *testing.T) {
	cases := map[string]struct {
		min    int64
		max    null.Int64
		period SalaryPeriod
	}{
		"zero minimum":      {0, null.Int64{}, SalaryPeriodMonth},
		"negative minimum":  {-5, null.Int64{}, SalaryPeriodMonth},
		"maximum below min": {700_000, null.IntFrom(600_000), SalaryPeriodMonth},
		"unknown period":    {600_000, null.Int64{}, "WEEK"},
		"empty period":      {600_000, null.Int64{}, ""},
	}
	for name, c := range cases {
		t.Run(name, func(t *testing.T) {
			// when
			_, err := NewSalary(c.min, c.max, c.period)

			// then
			require.ErrorIs(t, err, ErrInvalid)
		})
	}
}

func TestNewOfferContent_Valid(t *testing.T) {
	// begin
	salary := &Salary{Min: 600_000, Period: SalaryPeriodMonth}

	// when
	got, err := NewOfferContent(" Mozo ", CategoryGastronomy, "Sábados", "Atención en salón", salary)

	// then
	require.NoError(t, err)
	require.Equal(t, OfferContent{Title: "Mozo", Category: CategoryGastronomy, Schedule: "Sábados", Description: "Atención en salón", Salary: salary}, got)
}

func TestNewOfferContent_Invalid(t *testing.T) {
	cases := map[string]OfferContent{
		"blank title":       {Title: " ", Category: CategoryGastronomy, Schedule: "s", Description: "d"},
		"blank schedule":    {Title: "t", Category: CategoryGastronomy, Schedule: "", Description: "d"},
		"blank description": {Title: "t", Category: CategoryGastronomy, Schedule: "s", Description: "\n"},
		"unknown category":  {Title: "t", Category: "PLUMBING", Schedule: "s", Description: "d"},
	}
	for name, c := range cases {
		t.Run(name, func(t *testing.T) {
			// when
			_, err := NewOfferContent(c.Title, c.Category, c.Schedule, c.Description, nil)

			// then
			require.ErrorIs(t, err, ErrInvalid)
		})
	}
}
