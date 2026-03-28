# Definition of Done — Worked Examples

These examples show what a good exit condition statement looks like for each
risk tier. Use them as templates, not as rules to follow literally.

---

## CRITICAL Tier Examples

### Bug fix: incorrect rounding on currency conversion

```
TASK: Fix EUR→USD conversion rounding that truncates instead of HALF_UP
RISK TIER: CRITICAL
EXIT CONDITION:
  1. New test CurrencyConversionTest#shouldRoundHalfUp asserts
     convert(EUR 10.005, USD) == USD 11.83 (not 11.82)
  2. Test fails before fix, passes after
  3. mvn test -pl payments-core -Dtest=CurrencyConversionTest
RATIONALE: Money calculation — silent rounding error affects balances
```

### New feature: PSD2 strong customer authentication check

```
TASK: Add SCA requirement check before payment initiation above €30
RISK TIER: CRITICAL
EXIT CONDITION:
  1. ScaRequirementTest#shouldRequireScaAboveThreshold — payment of €31
     without SCA token returns 403
  2. ScaRequirementTest#shouldAllowBelowThreshold — payment of €29
     without SCA token returns 200
  3. Both tests fail before implementation, pass after
  4. mvn test -pl payment-initiation -Dtest=ScaRequirementTest
RATIONALE: Regulatory requirement (PSD2) — wrong behavior = compliance violation
```

### Jurido: ECLI citation extraction from court ruling text

```
TASK: Extract ECLI identifiers from unstructured Dutch court ruling paragraphs
RISK TIER: CRITICAL
EXIT CONDITION:
  1. EcliExtractorTest#shouldExtractStandardEcli — input containing
     "ECLI:NL:HR:2023:1234" returns exactly that identifier
  2. EcliExtractorTest#shouldHandleMultipleEclis — input with 3 ECLI
     references returns all 3 in order
  3. EcliExtractorTest#shouldNotFalsePositive — input with "ECLI:" followed
     by malformed suffix returns empty list
  4. All fail before, pass after
  5. mvn test -pl jurido-analysis -Dtest=EcliExtractorTest
RATIONALE: Legal citation accuracy — wrong ECLI = wrong legal advice
```

---

## HIGH Tier Examples

### New endpoint: GET /api/v1/accounts/{id}/transactions

```
TASK: Add paginated transaction history endpoint
RISK TIER: HIGH
EXIT CONDITION:
  1. Run existing AccountControllerIntegrationTest — confirm green baseline
  2. Add TransactionHistoryEndpointTest with:
     - 200 with valid account and pagination params
     - 404 for nonexistent account
     - 400 for invalid page size
  3. mvn test -pl account-service -Dtest=TransactionHistory*
RATIONALE: New API surface with business logic, but no money movement
```

### Database migration: add index on transaction_date

```
TASK: Add composite index (account_id, transaction_date) to transactions table
RISK TIER: HIGH
EXIT CONDITION:
  1. Flyway migration V42__add_transaction_date_index.sql applies cleanly
  2. mvn flyway:migrate -pl account-service succeeds
  3. Existing TransactionRepositoryTest suite passes (no broken queries)
  4. mvn test -pl account-service -Dtest=TransactionRepositoryTest
RATIONALE: Schema change — bad migration blocks all subsequent deployments
```

### Refactor: extract payment validation into Spring Modulith module

```
TASK: Move payment validation from payment-processing into new payment-validation module
RISK TIER: HIGH
EXIT CONDITION:
  1. mvn test -pl payment-processing — existing tests still green
  2. mvn test -pl payment-validation — extracted tests pass in new location
  3. mvn test -Dtest="*ArchitectureTest*" — module boundaries respected
  4. mvn test -Dtest="*ModularityTests*" — Spring Modulith verification passes
RATIONALE: Structural change across module boundaries — ArchUnit must validate
```

---

## MEDIUM Tier Examples

### Configuration: add new Spring profile for staging environment

```
TASK: Add application-staging.yml with staging-specific datasource and feature flags
RISK TIER: MEDIUM
EXIT CONDITION:
  1. mvn compile — no syntax errors in YAML
  2. mvn test -pl <module> — existing tests unaffected
  3. curl localhost:8080/actuator/env after boot with --spring.profiles.active=staging
     shows expected datasource URL
RATIONALE: Config only — type system and existing tests cover behavior
```

### DTO change: add field to AccountSummaryResponse

```
TASK: Add availableBalance field to AccountSummaryResponse DTO
RISK TIER: MEDIUM
EXIT CONDITION:
  1. mvn compile -pl account-service — NullAway confirms @NonNull annotation
  2. mvn test -pl account-service — existing serialization and controller
     tests catch mapping issues
RATIONALE: DTO change covered by existing integration tests and null-safety checks
```

---

## LOW Tier Examples

### Scaffold new module

```
TASK: Create empty notification-service module with standard package structure
RISK TIER: LOW
EXIT CONDITION: mvn compile -pl notification-service
RATIONALE: Boilerplate only — nothing to test yet
```

### Add logging to payment retry flow

```
TASK: Add structured log statements at WARN level for payment retry attempts
RISK TIER: LOW
EXIT CONDITION: mvn compile -pl payment-processing
RATIONALE: Logging only, no behavior change, non-audit
```

### Documentation update

```
TASK: Update README with local development setup instructions
RISK TIER: LOW
EXIT CONDITION: none (markdown only)
RATIONALE: No code change
```
