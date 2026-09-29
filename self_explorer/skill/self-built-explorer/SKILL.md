---
name: self-built-explorer
description: In an isolated blank environment, explore an unknown website seriously in a pure black-box, human-like way and reproduce its core features.
---

Explore an unknown website as seriously and thoroughly as possible in an isolated environment, and understand its observable core features. The target browser has already been started by the isolated environment and is reachable through the browser I/O root returned by `begin_workspace`; there is no browser to start in the workspace, nor is one needed. That interface only connects to the target browser's visible pixels and human-like input; it provides no ready-made operation tools, tool implementations, page semantics, exploration procedure, or output specification.
The start URL is an entry that the task must provide explicitly: if the user has not given one, ask the user for it first — never guess or use a default target. Once received, call `begin_workspace` once at the start and pass it as `target_url` verbatim; when done, call `finish_workspace`. That URL is not a navigation whitelist: when the target flow redirects, changes domain, opens new pages, or chains through multiple URLs, keep exploring by what is visible.

Strict restrictions:

- Use only the isolated-workspace operations provided by `blackboxbench-self-built`; do not use files, programs, services, or information sources outside the workspace.
- Do not read the target's source code, page source, DOM, accessibility tree, selectors, API responses, network requests, cookies, storage, or any other web-semantic channel; do not search for, download, or reuse the original implementation, and do not touch this project's other exploration conditions, their implementations, or their artifacts.
- The start URL only specifies the page the isolated environment opens initially. The target browser may load dependencies, follow automatic redirects, and reach other URLs through human-like operation in a real visible flow; do not stop merely because the address or domain changes.
- The workspace may have ordinary network access, but the network may only be used to fetch generic dependencies unrelated to the target. Do not request the target or associated sites directly, do not search for the target or its original implementation, do not download web pages, page source, API responses, or site assets, and never use any network search result as an exploration conclusion. Judgments about the target may only come from visible results in the isolated environment.
- Exploration must stay pure black-box and human-like; knowledge of the target may only come from what is actually visible on screen.
- Do not fabricate or guess target features from training data, prior knowledge, or public assets; the reproduction may only rely on what you actually explored this time. Without real exploration you must not generate a website, and you must explicitly report failure.
- The target pages may display private information of real accounts; such content is not part of the reproduction goal. Do not copy or hard-code real names, accounts, document titles, bodies, comments, messages, or other private content when reproducing; reproduce only the actually observed page structure and features, and replace private data with the public fictional material provided by the reproduction environment.
- The workspace content must come entirely from your own exploration; at finalization it is compared against the benchmark's private content, and any content of unknown origin will cause `finish_workspace` to be rejected.

When you judge on your own that you have explored as seriously and thoroughly as possible, call `finish_workspace`.
If the returned `stage` is `reproduction`, reproduce the core features of the target website in the current isolated environment based on what you actually explored this time; do not add target behavior you did not explore. When done, call `finish_workspace` again to end the task.
