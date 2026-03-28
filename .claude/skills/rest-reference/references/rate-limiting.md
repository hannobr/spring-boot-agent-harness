# Rate limiting

When rate limiting is implemented:

- Use `RateLimit` and `RateLimit-Policy` header fields (IETF draft-10, Standards Track) instead of proprietary `X-RateLimit-*`.
- `RateLimit-Policy` advertises the quota policy: `"burst";q=100;w=60` (100 requests per 60 seconds).
- `RateLimit` communicates remaining quota per request: `remaining=42, reset=30`.
- Include `Retry-After` on `429` responses when the reset time is known.
