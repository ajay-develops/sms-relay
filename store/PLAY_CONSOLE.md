# Play Console answer sheet — SMS Relay

Everything needed to fill in Play Console for `com.soulredemption.smsrelay`.

## Files

| Item | File |
|---|---|
| App bundle (upload this) | `app/build/outputs/bundle/release/app-release.aab` (versionCode 1, 1.0.0) |
| App icon 512×512 | `store/play/icon-512.png` |
| Feature graphic 1024×500 | `store/play/feature-graphic.png` |
| Phone screenshots (1080×1920) | `store/play/screenshot-1.png` … `screenshot-5.png` |
| Demo video (SMS permission declaration) | https://github.com/ajay-develops/sms-relay/releases/download/v1.0.0/sms-relay-demo.mp4 |

Upload key: `~/.config/sms-relay/sms-relay-release.jks` (SHA-256 `dfea0d98bb6e0ebd7037a5eae21f1f6b2cba1706142eecc691a8f1b67b4c10c1`). Use Play App Signing.

## Store listing

- **App name:** SMS Relay
- **Short description (≤80):** Forward the texts you choose to another phone. Private and dual-SIM aware.
- **Full description:** `fastlane/metadata/android/en-US/full_description.txt` (replace `<b>` tags with plain headings if Play rejects them — Play supports `<b>`).
- **Category:** Communication (Tools also fits)
- **Tags:** SMS, Messaging tools
- **Contact email:** saulredemptionservicesllc@gmail.com
- **Website:** https://ajay-develops.github.io/sms-relay/
- **Privacy policy URL:** https://ajay-develops.github.io/sms-relay/privacy.html

## App content

| Section | Answer |
|---|---|
| Privacy policy | https://ajay-develops.github.io/sms-relay/privacy.html |
| App access | All functionality available without special access (no login) |
| Ads | No ads |
| Content rating | Category: Utility/Productivity/Communication/Other. Violence, sexuality, language, drugs, gambling: No. Users can interact/exchange content: **No** (app forwards the user's own SMS via carrier; no user-to-user platform). Shares location: No. Digital purchases: No. → expected rating: Everyone / PEGI 3 |
| Target audience | 18+ (avoid "Designed for Families" requirements; SMS permissions) |
| News app | No |
| COVID-19 | No |
| Data safety | **No data collected, no data shared.** Rationale: Play defines "collected" as transmitted off device by the developer. SMS Relay has no INTERNET permission; message content only leaves the device as an SMS the user explicitly configured, initiated on-device to a user-chosen recipient (user-initiated transfer, excluded). Encryption in transit: N/A. Deletion: N/A (nothing collected). |
| Government app | No |
| Financial features | None |
| Health | None |
| Advertising ID | Does not use advertising ID |

## Sensitive permissions — SMS and Call Log declaration

- **Permissions declared:** `RECEIVE_SMS`, `SEND_SMS`
- **Core functionality / permitted use case:** *SMS forwarding / device automation* — the app's sole purpose is to automatically forward incoming SMS that match user-defined rules to phone numbers the user chooses. If the form lists "Cross-device synchronization or transfer of SMS or calls", select that; otherwise "Device automation".
- **Default handler:** The app is not a default SMS handler and does not need to be; it never reads the SMS inbox.
- **Justification text:**

  > SMS Relay is a single-purpose utility that forwards incoming text messages to another phone number chosen by the user, for example forwarding bank alerts from a work SIM to a personal phone. RECEIVE_SMS is required to detect incoming messages and match them against the user's rules (sender, SIM slot). SEND_SMS is required to send the forwarded copy from the user's own SIM. Forwarding only happens for rules the user creates, to recipients the user selects, and the app shows every forward in an on-device activity log. The app has no INTERNET permission, never reads the SMS inbox, and stores nothing off device. Without these two permissions the app has no functionality.

- **Video:** https://github.com/ajay-develops/sms-relay/releases/download/v1.0.0/sms-relay-demo.mp4 (shows permission prompt, creating a rule, a matching SMS being forwarded and a non-matching SMS ignored)
- **Prominent disclosure:** First-run setup screen explains each permission before the system prompt.

`READ_PHONE_STATE` is used only to list SIM slots (SubscriptionManager) and send from a chosen SIM; it is not a restricted permission but is disclosed in the privacy policy.

## Release

- Track: **Production**, countries: all available.
- Release name: 1.0.0
- Release notes: `fastlane/metadata/android/en-US/changelogs/1.txt`
