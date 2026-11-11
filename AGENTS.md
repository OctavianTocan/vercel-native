# Vercel Native

Native Kotlin/Compose scaffold for a future Vercel dashboard. Dashboard work is not part of initial setup.

- Updates are mandatory, using the Inventory pattern. Do not add skip/later controls.
- CI runs on mini-002-netcup over Tailscale, label `vercel-linux`; never GitHub-hosted runners.
- Reuse the existing Inventory Google/password session as-is. No new scopes or auth backend.
- This project owns its publisher, signing key, bucket, versions and feed. Inventory only hosts the generic downloads page and authenticated serving route. Never use or change Inventory's publisher or manifest.
- No secrets, APKs, private history or Inventory library data in this public repository.
- Add a dated change file under android/changes for runtime/build changes. Version numbers are assigned when changes merge to main.
- Record approved design choices in CONTRACTS and docs/decisions.md; do not invent dashboard features.
- Verify the exact source commit, package, signing fingerprint, size and checksum when publishing. A green build is not phone installation proof.
- Never execute untrusted fork code on the persistent runner. PR verification must not access release secrets.
