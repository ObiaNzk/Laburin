// Command seed loads fictional demo data: the two demo accounts of docs/api/auth.md and offers around the default
// map center. It is idempotent: run it again and nothing is duplicated.
package main

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"errors"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/guregu/null/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/laburin/backend/internal/auth"
	"github.com/laburin/backend/internal/config"
	"github.com/laburin/backend/internal/model"
	"github.com/laburin/backend/internal/repository"
	"github.com/laburin/backend/internal/service"
	"github.com/redis/go-redis/v9"
	"github.com/rs/zerolog/log"
	"golang.org/x/crypto/bcrypt"
)

// Fictional demo password, documented in docs/api/auth.md. Never use it outside local development.
const demoPassword = "demo1234"

type seedOffer struct {
	id       string
	title    string
	category model.Category
	business string
	address  string
	lat, lng float64
	salary   *model.Salary
	closed   bool
}

// Same offers as the app's FakeBackend, with UUIDs; each at its own (fictional) business.
var offers = []seedOffer{
	{"5eed0000-0000-4000-8000-000000000001", "Ayudante de taller", model.CategoryMechanics, "Taller Ejemplo", "Av. Ejemplo 100", -34.6080, -58.3712, &model.Salary{Min: 650_000, Max: null.IntFrom(800_000), Period: model.SalaryPeriodMonth}, false},
	{"5eed0000-0000-4000-8000-000000000002", "Mozo/a fin de semana", model.CategoryGastronomy, "Bodegón Ejemplo", "Calle Ficticia 200", -34.5997, -58.3819, &model.Salary{Min: 6_500, Period: model.SalaryPeriodHour}, false},
	{"5eed0000-0000-4000-8000-000000000003", "Repositor/a", model.CategoryStock, "Autoservicio Ejemplo", "Av. Ejemplo 300", -34.6120, -58.3900, nil, false},
	{"5eed0000-0000-4000-8000-000000000004", "Vendedor/a", model.CategoryClothing, "Tienda Ejemplo", "Calle Ficticia 400", -34.6010, -58.3770, &model.Salary{Min: 700_000, Period: model.SalaryPeriodMonth}, false},
	{"5eed0000-0000-4000-8000-000000000005", "Cocinero/a", model.CategoryGastronomy, "Parrilla Ejemplo", "Av. Ejemplo 500", -34.5850, -58.3950, nil, false},
	{"5eed0000-0000-4000-8000-000000000006", "Mecánico/a", model.CategoryMechanics, "Lubricentro Ejemplo", "Calle Ficticia 600", -34.6400, -58.4200, nil, false},
	{"5eed0000-0000-4000-8000-000000000007", "Cajero/a", model.CategoryStock, "Kiosco Ejemplo", "Av. Ejemplo 700", -34.6050, -58.3800, nil, true},
}

func main() {
	cfg := config.Load()
	ctx := context.Background()
	pool, err := pgxpool.New(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Fatal().Err(err).Msg("postgres")
	}
	defer pool.Close()
	redisClient := redis.NewClient(&redis.Options{Addr: cfg.RedisAddr})
	defer redisClient.Close()

	postgres := repository.NewPostgres(pool)
	authService := service.NewAuthService(postgres, auth.NewTokens(cfg.JWTSecret, time.Hour, time.Now), bcrypt.DefaultCost)
	offerService := service.NewOfferService(postgres, repository.NewRedisGeo(redisClient))
	profile := service.NewProfileService(postgres)

	candidate := account(ctx, postgres, "postulante@demo.laburin", func() (service.Session, error) {
		params := service.SignUpCandidateParams{FirstName: "Postulante", LastName: "Demo", Email: "postulante@demo.laburin", Password: demoPassword}
		return authService.SignUpCandidate(ctx, params)
	})
	cv := model.CV{FileName: "cv-demo.pdf", Content: demoPDF("CV de ejemplo - [NOMBRE] [APELLIDO]")}
	must(profile.UploadCV(ctx, candidate.ID, cv), "demo CV")
	account(ctx, postgres, "comercio@demo.laburin", func() (service.Session, error) {
		params := service.SignUpBusinessParams{BusinessName: "Comercio Demo", Category: model.CategoryGastronomy, Schedule: "Lunes a viernes, 9 a 17 h", Address: "Av. Ejemplo 1000", Latitude: -34.6037, Longitude: -58.3816, Email: "comercio@demo.laburin", Password: demoPassword}
		return authService.SignUpBusiness(ctx, params)
	})

	for i, offer := range offers {
		email := fmt.Sprintf("seed-%d@demo.laburin", i+1)
		owner := account(ctx, postgres, email, func() (service.Session, error) {
			// Nobody logs in as these businesses: a random password nobody knows.
			params := service.SignUpBusinessParams{BusinessName: offer.business, Category: offer.category, Schedule: "Lunes a viernes, 9 a 17 h", Address: offer.address, Latitude: offer.lat, Longitude: offer.lng, Email: email, Password: randomPassword()}
			return authService.SignUpBusiness(ctx, params)
		})
		content := model.OfferContent{Title: offer.title, Category: offer.category, Schedule: "Lunes a viernes, 9 a 17 h", Description: "Buscamos una persona responsable y con ganas de aprender. No se requiere experiencia previa.", Salary: offer.salary}
		id := uuid.MustParse(offer.id)
		_, err := offerService.Put(ctx, owner.ID, id, content)
		if err != nil && !errors.Is(err, model.ErrConflict) { // conflict: already seeded and closed
			log.Fatal().Err(err).Str("offer", offer.id).Msg("seed offer")
		}
		if offer.closed {
			must(offerService.Close(ctx, owner.ID, id), "close seed offer")
		}
	}
	log.Info().Int("offers", len(offers)).Msg("seed done")
}

// account signs up the user, or returns the existing one when the email is taken.
func account(ctx context.Context, users *repository.Postgres, email string, signUp func() (service.Session, error)) model.User {
	session, err := signUp()
	if errors.Is(err, model.ErrConflict) {
		user, err := users.GetUserByEmail(ctx, email)
		must(err, "existing account")
		return user
	}
	must(err, "sign up")
	return session.User
}

func randomPassword() string {
	bytes := make([]byte, 24)
	_, err := rand.Read(bytes)
	must(err, "random password")
	return hex.EncodeToString(bytes)
}

func must(err error, what string) {
	if err != nil {
		log.Fatal().Err(err).Msg(what)
	}
}

// demoPDF builds a one-page PDF with a line of text, so the business can open the demo CV.
func demoPDF(text string) []byte {
	stream := fmt.Sprintf("BT /F1 18 Tf 72 720 Td (%s) Tj ET", text)
	objects := []string{
		"<< /Type /Catalog /Pages 2 0 R >>",
		"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
		"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
		fmt.Sprintf("<< /Length %d >>\nstream\n%s\nendstream", len(stream), stream),
		"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
	}
	pdf := "%PDF-1.4\n"
	offsets := make([]int, len(objects))
	for i, object := range objects {
		offsets[i] = len(pdf)
		pdf += fmt.Sprintf("%d 0 obj\n%s\nendobj\n", i+1, object)
	}
	xref := len(pdf)
	pdf += fmt.Sprintf("xref\n0 %d\n0000000000 65535 f \n", len(objects)+1)
	for _, offset := range offsets {
		pdf += fmt.Sprintf("%010d 00000 n \n", offset)
	}
	pdf += fmt.Sprintf("trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n", len(objects)+1, xref)
	return []byte(pdf)
}
