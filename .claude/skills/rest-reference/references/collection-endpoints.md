# Collection endpoints

- Paginate from the first public version when result sets can grow beyond a small bounded size.
- For opaque cursor pagination, return `nextPageToken` or `hasMore`. For page-number pagination, `totalPages` is acceptable.
- Document default and maximum page size. Missing or zero size -> documented default. Negative -> `400`.
- If a client exceeds max page size, coerce to max or reject -- do not vary per endpoint.
- Filtering, sorting, and field-selection parameters must be documented, validated, and safe. Unrecognized query parameters should not silently change semantics.
- Changing the default field set is a breaking change.
