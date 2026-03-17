---
name: crwl
description: "Fetch web page content using the crwl CLI tool. Use this skill as the primary method whenever you need to read, fetch, or retrieve the content of a specific URL. This replaces WebFetch for all URL content retrieval — use crwl instead of WebFetch. Also use this when WebSearch returns results but you need to read the actual page content, when you encounter 403/blocked/timeout errors fetching a page, or when the user asks you to crawl, scrape, read, or check any website. If a URL is involved and you need its content, this skill applies."
---

# crwl — Web content fetcher

`crwl` is a CLI tool installed on this machine that fetches web pages and returns clean markdown. It handles sites that block typical API-based fetchers (news sites, pages behind bot protection, etc.), making it more reliable than built-in fetch tools.

Use `crwl` as the default way to retrieve content from URLs.

## Basic usage

Fetch a single page as markdown:

```bash
crwl <url> -o markdown
```

**Example:**
```bash
crwl https://www.telegraaf.nl -o markdown
```

## Deep crawl

When a single page isn't enough — for instance the user wants to understand a site's documentation, find information that might be spread across multiple pages, or explore a section of a site — use deep crawl:

```bash
crwl <url> --deep-crawl bfs --max-pages <N> -o markdown
```

Strategies:
- `bfs` (breadth-first) — good default, explores pages level by level from the starting URL
- `dfs` (depth-first) — follows links deep before going wide, useful for hierarchical docs
- `best-first` — prioritizes pages that seem most relevant

Choose `--max-pages` based on scope: 5-10 for a quick scan, 10-25 for thorough documentation reads. Start small — you can always crawl more if needed.

**Example:**
```bash
crwl https://docs.crawl4ai.com --deep-crawl bfs --max-pages 10 -o markdown
```

## Structured data extraction

To extract structured data from a page using an LLM:

```bash
crwl <url> -j "Extract all product names and prices" -o json
```

Note: for structured extraction, `-o json` makes more sense than markdown. This is the one exception to the "always markdown" default.

## When to use deep crawl vs. single page

- **Single page**: user gives you a specific URL, you need the content of that page
- **Deep crawl**: user says things like "read their docs", "find X on their site", "what does this site say about Y" — situations where the answer might span multiple pages or you're not sure which page has it

## Output handling

**NEVER truncate output with `head -500` or similar low limits.** Most documentation pages are 800-2000+ lines of markdown — cutting at 500 loses the majority of content.

| Approach | When | Why |
|---|---|---|
| No truncation | Single pages in a subagent | Subagent context is disposable — let it consume the full output and summarize |
| `-O /tmp/crwl-<name>.md` + Read tool | Large pages or deep crawls | Write to file, then read specific sections with the Read tool |
| `head -5000` as last resort | Only if none of the above apply AND in main context | Even then, 5000 minimum — never less |

**Preferred pattern for large content:**
```bash
crwl <url> -o markdown -O /tmp/crwl-spring-modulith-docs.md
```
Then use the Read tool to read the file (with offset/limit if needed).

## Subagent usage

Web research with crwl should happen in **subagents**. This keeps large web content out of main context and lets the subagent consume and summarize the full page. Only fetch in the main context if the result is small and immediately needed for an edit.

## Known limitations

- The `-q` (question) flag does **NOT** work in this project. Do not use it.

## Tips

- If a page returns mostly cookie consent / login walls, the content is likely behind authentication. Let the user know rather than retrying.
- `crwl` output goes to stdout by default. For large pages or deep crawls, always use `-O <filepath>` to write to a file instead.
- Use descriptive filenames: `-O /tmp/crwl-spring-modulith-events.md`, not `-O /tmp/output.md`.
