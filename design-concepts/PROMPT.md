# Reusable design prompt — TwinWatch splash + app icon

Copy everything in the fenced block into another AI agent to generate a fresh, uncommitted concept set.

```text
Generate 4–6 distinct visual directions for the Android splash screen and launcher icon of TwinWatch.

Product context:
- TwinWatch is an Android app for friends to track TV shows together: shared circles, member progress, watched episodes, season notes, public/private notes, activity feed, invitations, and optional FCM notifications.
- Name: TwinWatch. Wordmark must read exactly "TwinWatch" (or stacked TWIN / WATCH). Do not rename the product.
- Current app uses a punchy editorial UI: warm paper/ink palette, heavy uppercase labels, square-ish cards, amber/mint/coral accents. Treat that as context, not as a constraint to copy.
- The visual brief is "Twin Peaks-inspired but original". Borrow atmosphere only: velvet curtains, checkerboard thresholds, pine forest, CRT/static signals, late-night diner/receipt paper, red-room lighting, index-card mystery. Do NOT use or imitate protected characters, logos, fonts, exact props, quotes, or recognizable stills. No Dale Cooper, Black Lodge logo, coffee motif, red room photograph, or series title treatment.

For each direction, deliver:
1. A short name (2–4 words).
2. Palette: 4–6 named hex colors.
3. Type direction: one characterful display face and one restrained body/utility face. Prefer system/Google fonts with clear fallbacks; explain why they fit.
4. Layout: one-sentence description plus a small ASCII wireframe of the splash screen.
5. Signature: the single memorable element that makes this direction specific to TwinWatch.
6. A self-contained HTML mockup showing:
   - a phone-shaped splash screen (9:19.5-ish),
   - the app icon preview at launcher size,
   - the direction name and one-sentence rationale.
7. The HTML must be dependency-free: inline CSS/SVG only, no external fonts, images, CDNs, or JS unless essential. It must respect prefers-reduced-motion.

Quality bar:
- Make the directions genuinely different, not recolors of the same layout.
- Each splash should feel like a real Android launch moment: centered identity, restrained copy, no fake navigation.
- Each app icon must remain legible at 48dp: simple silhouette, strong negative space, no tiny details.
- Avoid generic AI defaults: no purple-blue gradients, no neon cyberpunk unless justified, no glassmorphism, no random tech grid.
- Spend the boldness in one signature element per direction; keep everything else quiet.
- Use plain, user-facing copy. Useful tagline examples: "Watch together, episode by episode.", "Two screens, one story.", "Find the signal. Watch together."
- Do not generate final production assets yet. These are review concepts only.

File output:
- Create a new uncommitted folder named design-concepts/ (or design-concepts-v2/ if one exists).
- Write one preview.html containing all directions in a responsive grid.
- Write README.md listing each direction and its palette/signature.
- Do not commit, stage, push, or modify app source files.
- Return only: files created, one-line description of each direction, and which direction you recommend and why.
```
