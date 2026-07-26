# S7: canvas on the watches

Date: 2026-07-26. Status: **approved** (Jay via @taOS-dev,
bus 1322). v2 - folds in the canvas internals from @taOS-dev (bus 1319) and
Jay's product framing (bus 1322). v1's open questions are all resolved.

Amends §6 of `DESIGN-taosc-companion-app.md` ("never the canvas" on watch).

## Jay's framing (shapes everything below)

The wrist canvas is for **fast, readable projection of existing boards** -
on-the-fly visual mini apps and deliverables, mockups/screenshots, and
**decision-making support**: pair the projection with Decisions actions where
a visual helps Jay choose. It is explicitly NOT wrist-side authoring.

## The constraint that shapes the architecture

Neither watch platform has a usable WebView: WKWebView does not exist on
watchOS, and Wear OS ships no WebView component. The phone trick - one web
canvas hosted in thin shells - is physically unavailable on the wrist. Watch
canvas is therefore a **native rendering of projected canvas state**.

## Data source (settled, bus 1319 - no canvas-side refactor needed)

The canonical store is `project_canvas_elements`: renderer-neutral rows
(`id, kind, x, y, w, h, rotation, z_index, payload JSON`) with kinds
`note / text / link / image / mermaid / flowchart / user_shape`. Read via
`GET /api/projects/{id}/canvas/elements`; live updates via the canvas SSE
stream (`canvas.*` events through the ProjectEventBroker). Precedent that
server-side projection into a foreign vocabulary works in pure data: the
`.tldr` snapshotter (`tinyagentos/projects/canvas/snapshotter.py`).

Constraints inherited from the canvas team (bus 1319):
1. `payload` is freeform JSON, NOT guaranteed to be a dict. Read every field
   defensively; one bad row degrades to a placeholder, never aborts the
   projection (hostile-row test precedent: `tests/test_canvas_tldr_export.py`).
2. `user_shape` rows carry raw tldraw blobs that will not project
   meaningfully - render a labelled placeholder.
3. #2132 migrates the desktop renderer tldraw→Excalidraw. The projection
   reads canonical rows so it is insulated; never couple to any
   renderer-specific payload shape.

## Primitive vocabulary v1 (per Jay's framing: readable projection first)

| canvas kind | watch primitive |
|---|---|
| `note`, `text` | text card |
| `image` | thumbnail (screenshots/mockups are a headline use) |
| `link` | labelled row (title + domain); open-on-phone handoff |
| `mermaid`, `flowchart` | labelled placeholder in v1 ("diagram - open on phone"); server-side rasterisation is a possible v2, not carded |
| `user_shape` | labelled placeholder (bus 1319 rule) |
| anything unknown/hostile | typed placeholder; projection never fails |

Ordering by `z_index`; the projection is versioned JSON (`{"version": 1, ...}`)
so renderers can refuse a future format they don't know.

**Decisions pairing:** when a Decision references canvas content, the watch
shows the projected visual alongside approve / deny / pick / quick-reply.
The actions themselves ride the existing Decisions path - the projection adds
context, not a new write surface.

## Interaction and transport

- Read-only projection, automatic for every canvas the device token can read
  (it is scoped like everything else; no per-agent opt-in needed for v1).
- Taps that act (Decisions) post through existing paths; no new write routes.
- Transport: fetch-on-open plus push-triggered refresh (a silent push over
  the S2 dispatch path nudges a re-fetch). Watches never hold the SSE stream;
  both platforms kill long-lived sockets.

## Alternatives rejected (unchanged from v1)

- **Server-rendered bitmaps of the whole canvas** - kills interaction and
  accessibility, costs battery, wrong on every watch size.
- **Phone-proxied rendering** - watch apps must work standalone; adds a
  fragile dependency for zero savings.

## Slices

- **S7a (server, platform-neutral, claimable now):** projection endpoint -
  canonical rows → v1 vocabulary, device-token scoped, snapshotter-style pure
  data. Core-repo work, no Mac. Carded as tsk child of tsk-duvb7b.
- **S7b (watchOS renderer)** and **S7c (Wear OS renderer):** parallel, same
  wire format, gated on S6's watch app shells existing.

S6 stays notifications + Decisions only and ships first; S7 builds on its
shells and push plumbing.
