# Demo Ekip ve Hizmet Seed Uygulama Plani

> **For agentic workers:** Implement task-by-task with a test checkpoint after each task.

**Goal:** Expand the ten development businesses with varied service catalogs and photographed multi-service teams.

**Architecture:** Represent each seeded service and employee explicitly in `DevDataSeeder`; upsert by business slug, service name, and employee full name. Keep the seed additive and avoid running destructive integration tests on the shared database.

**Tech Stack:** Spring Boot, JPA, PostgreSQL, JUnit 5.

## Global Constraints

- Do not delete existing database rows.
- Each seeded venue has 3-5 services and 2-4 employees, with varied team sizes.
- Every seeded employee has a Pexels profile URL and can be linked to multiple services.
- Re-running the development seed must not duplicate services, employees, or working hours.
- Do not run integration tests that target the persistent `localhost:5433/randevu` database.

---

### Task 1: Specify Seed Catalog Shape

**Files:**
- Test: `backend/src/test/java/com/randevupazaryeri/config/DevDataSeederTest.java`
- Modify: `backend/src/main/java/com/randevupazaryeri/config/DevDataSeeder.java`

- [x] Write database-free tests requiring nine nearby venue definitions, 3-5 services, 2-4 staff with varied counts, Pexels photos, valid multi-service assignments, and Turkish/ASCII name matching.
- [x] Run `./mvnw -Dtest=DevDataSeederTest test` and confirm the tests fail before the seed catalog and name normalizer are implemented.
- [x] Add explicit service/team definitions and pass both tests.

### Task 2: Additive Seed Existing and Missing Rows

**Files:**
- Modify: `backend/src/main/java/com/randevupazaryeri/config/DevDataSeeder.java`

- [x] Upsert the known businesses, services, and employees without deleting existing rows.
- [x] Merge configured service links onto each employee and add hours only where no schedule exists; deactivate only duplicate spelling aliases, without deleting rows.
- [x] Re-run the two database-free tests and compile/package with tests skipped.
- [x] Run the dev seed twice and verify the counts remain 10 businesses, 33 services, and 28 active staff; verify photos exist and working hours are not duplicated.