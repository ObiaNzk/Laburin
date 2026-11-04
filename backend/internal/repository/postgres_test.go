package repository

import (
	"context"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/guregu/null/v5"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/testutil"
	"github.com/stretchr/testify/require"
)

type fixture struct {
	repo     *Postgres
	ctx      context.Context
	business model.Business
}

// newFixture starts from an empty database holding one business.
func newFixture(t *testing.T) fixture {
	repo := NewPostgres(testutil.Postgres(t))
	ctx := context.Background()
	user := model.User{ID: uuid.New(), Role: model.RoleBusiness, Email: "[comercio]@test.laburin", PasswordHash: "hash", DisplayName: "[COMERCIO]"}
	business := model.Business{UserID: user.ID, Name: "[COMERCIO]", Category: model.CategoryGastronomy, Schedule: "9 a 17", Address: "[DIRECCION]", Latitude: -34.6037, Longitude: -58.3816}
	require.NoError(t, repo.CreateBusiness(ctx, user, business))
	return fixture{repo: repo, ctx: ctx, business: business}
}

func (f fixture) candidate(t *testing.T, email string) model.User {
	user := model.User{ID: uuid.New(), Role: model.RoleCandidate, Email: email, PasswordHash: "hash", DisplayName: "[NOMBRE]"}
	require.NoError(t, f.repo.CreateCandidate(f.ctx, user))
	return user
}

func (f fixture) offer(t *testing.T, content model.OfferContent) uuid.UUID {
	id := uuid.New()
	require.NoError(t, f.repo.CreateOffer(f.ctx, id, f.business.UserID, content))
	return id
}

var testContent = model.OfferContent{Title: "Mozo", Category: model.CategoryGastronomy, Schedule: "Sábados", Description: "Atención en salón"}

func TestGetUserByEmail_IsCaseInsensitive(t *testing.T) {
	// begin
	f := newFixture(t)
	user := f.candidate(t, "[postulante]@test.laburin")

	// when
	got, err := f.repo.GetUserByEmail(f.ctx, "[POSTULANTE]@Test.Laburin")

	// then
	require.NoError(t, err)
	require.Equal(t, user, got)
}

func TestGetUserByEmail_Unknown(t *testing.T) {
	// begin
	f := newFixture(t)

	// when
	_, err := f.repo.GetUserByEmail(f.ctx, "[nadie]@test.laburin")

	// then
	require.ErrorIs(t, err, model.ErrNotFound)
}

func TestCreateCandidate_EmailTakenInAnyCase(t *testing.T) {
	// begin
	f := newFixture(t)
	f.candidate(t, "[postulante]@test.laburin")
	again := model.User{ID: uuid.New(), Role: model.RoleCandidate, Email: "[POSTULANTE]@test.laburin", PasswordHash: "hash", DisplayName: "x"}

	// when
	err := f.repo.CreateCandidate(f.ctx, again)

	// then
	require.ErrorIs(t, err, model.ErrConflict)
}

func TestCreateBusiness_EmailTakenLeavesNoBusinessBehind(t *testing.T) {
	// begin
	f := newFixture(t)
	user := model.User{ID: uuid.New(), Role: model.RoleBusiness, Email: "[comercio]@test.laburin", PasswordHash: "hash", DisplayName: "otro"}
	business := model.Business{UserID: user.ID, Name: "otro", Category: model.CategoryStock, Schedule: "s", Address: "a"}

	// when
	err := f.repo.CreateBusiness(f.ctx, user, business)

	// then
	require.ErrorIs(t, err, model.ErrConflict)
	_, err = f.repo.GetBusiness(f.ctx, user.ID)
	require.ErrorIs(t, err, model.ErrNotFound)
}

func TestGetBusiness_ReturnsTheProfile(t *testing.T) {
	// begin
	f := newFixture(t)

	// when
	got, err := f.repo.GetBusiness(f.ctx, f.business.UserID)

	// then
	require.NoError(t, err)
	require.Equal(t, f.business, got)
}

func TestOffer_CreateAndGetWithTheBusinessLocation(t *testing.T) {
	// begin
	f := newFixture(t)
	salaried := testContent
	salaried.Salary = &model.Salary{Min: 600_000, Max: null.IntFrom(750_000), Period: model.SalaryPeriodMonth}
	before := time.Now()

	// when
	id := f.offer(t, salaried)
	got, err := f.repo.GetOffer(f.ctx, id)

	// then
	require.NoError(t, err)
	require.WithinRange(t, got.UpdatedAt, before.Add(-time.Second), time.Now().Add(time.Second))
	expected := model.Offer{
		ID: id, BusinessID: f.business.UserID, Content: salaried, BusinessName: "[COMERCIO]", Address: "[DIRECCION]",
		Latitude: -34.6037, Longitude: -58.3816, Status: model.OfferStatusActive, UpdatedAt: got.UpdatedAt,
	}
	require.Equal(t, expected, got)
}

func TestOffer_WithoutSalaryHasNilSalary(t *testing.T) {
	// begin
	f := newFixture(t)
	id := f.offer(t, testContent)

	// when
	got, err := f.repo.GetOffer(f.ctx, id)

	// then
	require.NoError(t, err)
	require.Nil(t, got.Content.Salary)
}

func TestGetOffer_Unknown(t *testing.T) {
	// begin
	f := newFixture(t)

	// when
	_, err := f.repo.GetOffer(f.ctx, uuid.New())

	// then
	require.ErrorIs(t, err, model.ErrNotFound)
}

func TestUpdateOfferContent_ReplacesEverythingIncludingTheSalary(t *testing.T) {
	// begin
	f := newFixture(t)
	salaried := testContent
	salaried.Salary = &model.Salary{Min: 5_000, Period: model.SalaryPeriodHour}
	id := f.offer(t, salaried)
	edited := model.OfferContent{Title: "Cocinero", Category: model.CategoryGastronomy, Schedule: "Noches", Description: "Parrilla"}

	// when
	err := f.repo.UpdateOfferContent(f.ctx, id, edited)

	// then
	require.NoError(t, err)
	got, err := f.repo.GetOffer(f.ctx, id)
	require.NoError(t, err)
	require.Equal(t, edited, got.Content)
}

func TestCloseOffer_ClosesIt(t *testing.T) {
	// begin
	f := newFixture(t)
	id := f.offer(t, testContent)

	// when
	err := f.repo.CloseOffer(f.ctx, id)

	// then
	require.NoError(t, err)
	got, err := f.repo.GetOffer(f.ctx, id)
	require.NoError(t, err)
	require.Equal(t, model.OfferStatusClosed, got.Status)
}

func TestListActiveOffersByIDs_SkipsClosedAndFiltersCategory(t *testing.T) {
	// begin
	f := newFixture(t)
	gastronomy := f.offer(t, testContent)
	stockContent := testContent
	stockContent.Category = model.CategoryStock
	stock := f.offer(t, stockContent)
	closed := f.offer(t, testContent)
	require.NoError(t, f.repo.CloseOffer(f.ctx, closed))
	ids := []uuid.UUID{gastronomy, stock, closed, uuid.New()}
	category := model.CategoryGastronomy

	// when
	all, errAll := f.repo.ListActiveOffersByIDs(f.ctx, ids, nil)
	filtered, errFiltered := f.repo.ListActiveOffersByIDs(f.ctx, ids, &category)

	// then
	require.NoError(t, errAll)
	require.NoError(t, errFiltered)
	require.ElementsMatch(t, []uuid.UUID{gastronomy, stock}, offerIDs(all))
	require.Equal(t, []uuid.UUID{gastronomy}, offerIDs(filtered))
}

func TestListBusinessOffers_AnyStatusNewestFirst(t *testing.T) {
	// begin
	f := newFixture(t)
	first := f.offer(t, testContent)
	second := f.offer(t, testContent)
	require.NoError(t, f.repo.CloseOffer(f.ctx, first))

	// when
	got, err := f.repo.ListBusinessOffers(f.ctx, f.business.UserID)

	// then
	require.NoError(t, err)
	require.Equal(t, []uuid.UUID{first, second}, offerIDs(got))
}

func TestListActiveOfferLocations_OnlyActive(t *testing.T) {
	// begin
	f := newFixture(t)
	active := f.offer(t, testContent)
	closed := f.offer(t, testContent)
	require.NoError(t, f.repo.CloseOffer(f.ctx, closed))

	// when
	got, err := f.repo.ListActiveOfferLocations(f.ctx)

	// then
	require.NoError(t, err)
	require.Equal(t, []model.OfferLocation{{ID: active, Latitude: -34.6037, Longitude: -58.3816}}, got)
}

func TestApplication_CreateGetAndOnePerOffer(t *testing.T) {
	// begin
	f := newFixture(t)
	candidate := f.candidate(t, "[postulante]@test.laburin")
	offerID := f.offer(t, testContent)
	id := uuid.New()

	// when
	err := f.repo.CreateApplication(f.ctx, id, offerID, candidate.ID)
	again := f.repo.CreateApplication(f.ctx, uuid.New(), offerID, candidate.ID)

	// then
	require.NoError(t, err)
	require.ErrorIs(t, again, model.ErrConflict)
	got, err := f.repo.GetApplication(f.ctx, id)
	require.NoError(t, err)
	require.Equal(t, model.Application{ID: id, OfferID: offerID, CandidateID: candidate.ID, Status: model.ApplicationStatusSent, CreatedAt: got.CreatedAt}, got)
	_, err = f.repo.GetApplication(f.ctx, uuid.New())
	require.ErrorIs(t, err, model.ErrNotFound)
}

func TestListCandidateApplications_JoinsTheOffer(t *testing.T) {
	// begin
	f := newFixture(t)
	candidate := f.candidate(t, "[postulante]@test.laburin")
	offerID := f.offer(t, testContent)
	id := uuid.New()
	require.NoError(t, f.repo.CreateApplication(f.ctx, id, offerID, candidate.ID))
	require.NoError(t, f.repo.CloseOffer(f.ctx, offerID))

	// when
	got, err := f.repo.ListCandidateApplications(f.ctx, candidate.ID)

	// then
	require.NoError(t, err)
	require.Len(t, got, 1)
	expected := model.CandidateApplication{ID: id, OfferID: offerID, OfferTitle: "Mozo", OfferStatus: model.OfferStatusClosed, BusinessName: "[COMERCIO]", Status: model.ApplicationStatusSent, CreatedAt: got[0].CreatedAt}
	require.Equal(t, expected, got[0])
}

func TestApplicants_IncludeCVAndOwnerAndReflectTheReview(t *testing.T) {
	// begin
	f := newFixture(t)
	withCV := f.candidate(t, "[uno]@test.laburin")
	withoutCV := f.candidate(t, "[dos]@test.laburin")
	require.NoError(t, f.repo.SaveCV(f.ctx, withCV.ID, model.CV{FileName: "cv.pdf", Content: []byte("%PDF-1")}))
	offerID := f.offer(t, testContent)
	first, second := uuid.New(), uuid.New()
	require.NoError(t, f.repo.CreateApplication(f.ctx, first, offerID, withCV.ID))
	require.NoError(t, f.repo.CreateApplication(f.ctx, second, offerID, withoutCV.ID))

	// when
	require.NoError(t, f.repo.UpdateApplicationStatus(f.ctx, first, model.ApplicationStatusApproved))
	list, errList := f.repo.ListApplicants(f.ctx, offerID)
	one, errOne := f.repo.GetApplicant(f.ctx, first)

	// then
	require.NoError(t, errList)
	require.NoError(t, errOne)
	approved := model.Applicant{ApplicationID: first, OfferID: offerID, CandidateID: withCV.ID, BusinessID: f.business.UserID, CandidateName: "[NOMBRE]", CandidateEmail: "[uno]@test.laburin", CVFileName: null.StringFrom("cv.pdf"), Status: model.ApplicationStatusApproved}
	pending := model.Applicant{ApplicationID: second, OfferID: offerID, CandidateID: withoutCV.ID, BusinessID: f.business.UserID, CandidateName: "[NOMBRE]", CandidateEmail: "[dos]@test.laburin", Status: model.ApplicationStatusSent}
	require.Equal(t, []model.Applicant{approved, pending}, list)
	require.Equal(t, approved, one)
	_, err := f.repo.GetApplicant(f.ctx, uuid.New())
	require.ErrorIs(t, err, model.ErrNotFound)
}

func TestCV_SaveReplacesAndReads(t *testing.T) {
	// begin
	f := newFixture(t)
	candidate := f.candidate(t, "[postulante]@test.laburin")
	hasBefore, err := f.repo.HasCV(f.ctx, candidate.ID)
	require.NoError(t, err)
	_, errName := f.repo.GetCVFileName(f.ctx, candidate.ID)
	_, errCV := f.repo.GetCV(f.ctx, candidate.ID)

	// when
	require.NoError(t, f.repo.SaveCV(f.ctx, candidate.ID, model.CV{FileName: "viejo.pdf", Content: []byte("%PDF-old")}))
	require.NoError(t, f.repo.SaveCV(f.ctx, candidate.ID, model.CV{FileName: "nuevo.pdf", Content: []byte("%PDF-new")}))

	// then
	require.False(t, hasBefore)
	require.ErrorIs(t, errName, model.ErrNotFound)
	require.ErrorIs(t, errCV, model.ErrNotFound)
	has, err := f.repo.HasCV(f.ctx, candidate.ID)
	require.NoError(t, err)
	require.True(t, has)
	name, err := f.repo.GetCVFileName(f.ctx, candidate.ID)
	require.NoError(t, err)
	require.Equal(t, "nuevo.pdf", name)
	cv, err := f.repo.GetCV(f.ctx, candidate.ID)
	require.NoError(t, err)
	require.Equal(t, model.CV{FileName: "nuevo.pdf", Content: []byte("%PDF-new")}, cv)
}

func TestSavePushToken_MovesATokenToTheLastUser(t *testing.T) {
	// begin
	f := newFixture(t)
	first := f.candidate(t, "[uno]@test.laburin")
	second := f.candidate(t, "[dos]@test.laburin")

	// when
	require.NoError(t, f.repo.SavePushToken(f.ctx, first.ID, "fcm-1"))
	require.NoError(t, f.repo.SavePushToken(f.ctx, second.ID, "fcm-1"))

	// then
	var owner uuid.UUID
	require.NoError(t, f.repo.pool.QueryRow(f.ctx, "SELECT user_id FROM push_tokens WHERE token = 'fcm-1'").Scan(&owner))
	require.Equal(t, second.ID, owner)
}

func offerIDs(offers []model.Offer) []uuid.UUID {
	ids := make([]uuid.UUID, len(offers))
	for i, offer := range offers {
		ids[i] = offer.ID
	}
	return ids
}
