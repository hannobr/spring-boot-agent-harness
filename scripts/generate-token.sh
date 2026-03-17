#!/usr/bin/env bash
# Generates a JWT signed with the dev HMAC key for local development.
# Requires: python3 with PyJWT
# Usage: ./scripts/generate-token.sh [subject]

set -euo pipefail

SUBJECT="${1:-dev}"
SECRET="demo-signing-key-must-be-at-least-256-bits-for-hmac-sha256"

if command -v python3 &>/dev/null && python3 -c "import jwt" 2>/dev/null; then
    TOKEN=$(python3 -c "
import jwt, time
payload = {
    'sub': '$SUBJECT',
    'iat': int(time.time()),
    'exp': int(time.time()) + 86400,
    'iss': 'dev'
}
print(jwt.encode(payload, '$SECRET', algorithm='HS256'))
")
    echo "JWT Token (valid 24h):"
    echo "$TOKEN"
    echo ""
    echo "Use it against any protected endpoint you add to the template, for example:"
    echo "  curl -H 'Authorization: Bearer $TOKEN' http://localhost:8080/api/your-endpoint"
else
    echo "Error: python3 with PyJWT is required."
    echo "Install: pip3 install PyJWT"
    exit 1
fi
