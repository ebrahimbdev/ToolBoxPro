# Google Play Release Checklist — ToolBox Pro

Package: `com.toolbox.pro` · Branch: `main` · Owner: ebrahimbdev

## 1. Pre-flight (repo)

- [ ] `version.properties` bumped (`VERSION_NAME`, `VERSION_CODE`) — release.yml does this automatically
- [ ] `API_BASE_URL` in `app/build.gradle.kts` points to the deployed Worker
- [ ] AdMob app/unit IDs real (`ca-app-pub-547239974100...`) and the AdMob app is linked to the Play package
- [ ] `npm run typecheck` passes in `worker/`
- [ ] Local build green:
      `./gradlew :app:assembleRelease :app:bundleRelease`
- [ ] Privacy policy live: https://ebrahimbdev.github.io/ToolBoxPro/ (GitHub Pages, `docs/`)

## 2. Signing (Play App Signing)

- [ ] Upload keystore generated **outside** the repo (`*.jks` / `*.keystore` are gitignored)
- [ ] Environment set (local or CI secrets):
  - `PLAY_KEYSTORE_PATH` — absolute path to keystore
  - `PLAY_KEYSTORE_PASSWORD`
  - `PLAY_KEYSTORE_ALIAS` (default `upload`)
  - `PLAY_KEYSTORE_KEY_PASSWORD` (falls back to store password)
- [ ] CI secret `PLAY_KEYSTORE_B64` = `base64 -w0 play.keystore` (optional; workflow restores it)
- [ ] Never commit the keystore or its passwords

## 3. Play Console setup

- [ ] Create app → package `com.toolbox.pro` → free app
- [ ] Complete **Data safety** form (matches `docs/index.html`):
      device ID (random), username (optional), payment note, AdMob advertising ID; no location/contacts/media
- [ ] **Privacy policy URL**: https://ebrahimbdev.github.io/ToolBoxPro/
- [ ] Content rating questionnaire → suitable rating
- [ ] Target audience: not directed at children under 13 (or declare accordingly)
- [ ] Ads declaration: contains ads (AdMob)
- [ ] Category: Tools · Contact email: ebrahimbdev@gmail.com

## 4. Store listing

- [ ] Short description (≤80 chars) + full description (≤4000 chars), fa/en
- [ ] App icon 512×512 PNG
- [ ] Feature graphic 1024×500
- [ ] At least 2 phone screenshots (1080×1920 or 1080×2400)
- [ ] App name ≤30 chars: `ToolBox Pro`

## 5. Upload & rollout

- [ ] Download `ToolBoxPro-vX.Y.Z.aab` from the GitHub Release (built by `release.yml`)
- [ ] Upload to Internal testing → close test with 2–3 testers
- [ ] Smoke test: QR generate/save/share, Home layout, Speed test server picker,
      Profile privacy link, payment dialogs (crypto/card), banner + interstitial ads
- [ ] Promote: Internal → Closed → Production (staged rollout 10% → 100%)
- [ ] Verify AdMob fills in production (real units, no test devices left in code)

## 6. Post-release

- [ ] Tag `vX.Y.Z` exists on GitHub with APK + AAB attached
- [ ] Monitor Android Vitals (ANR/crash rate) for 72h
- [ ] Monitor Worker logs (`npx wrangler tail`) for heartbeat/payment traffic
- [ ] Update `docs/PLAY_CHECKLIST.md` with anything learned

## Rollback

- Staged rollout → halt in Play Console (immediately stops new installs)
- Bad build → bump `VERSION_CODE` and ship a fix; never reuse a version code
