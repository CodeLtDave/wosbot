# Privacy Policy

This Privacy Policy describes the information Frostguard can transfer to
networked systems and the choices available to users.

## 1. Information We Collect

Optional usage telemetry is disabled by default. Frostguard sends telemetry to
Mixpanel only after the user explicitly enables **Share pseudonymous usage data
with Mixpanel** in Configuration. Existing installations without a recorded
consent version are treated as opted out, even if an older release stored the
legacy enabled value.

The analytics data we collect may include:
- **Application events:** application launch/shutdown, bot start/stop, task name
  and completion status.
- **Usage metrics:** task duration, session duration, enabled task names, and
  configured profile count.
- **System information:** operating system and architecture, Java version and
  vendor, Frostguard version, emulator type, and headless-mode state.

Events are associated with a randomly generated UUID that is not derived from
hardware. Frostguard does not intentionally send names, email addresses, exact
locations, game-account identifiers, file paths, screenshots, or logs. Because
the random UUID distinguishes one installation over time, this policy calls the
data pseudonymous rather than anonymous.

## 2. How We Use Your Information

The optional telemetry is used solely for the following purposes:
- **Product Improvement:** Analyzing usage patterns to enhance existing features and prioritize future development.
- **Bug Fixing:** Identifying, diagnosing, and resolving application errors to improve overall stability.
- **Performance Optimization:** Monitoring performance metrics to ensure a smooth and efficient user experience.

## 3. Third-Party Analytics Services

We use **Mixpanel** to process optional telemetry. Users can withdraw consent at
any time by clearing the Mixpanel checkbox in Configuration; subsequent events
are not sent.

In the spirit of full transparency, we make our high-level analytics dashboard publicly available. You can view the actual metrics we track and analyze on our public Mixpanel board:

**[View Public Analytics Dashboard](https://mixpanel.com/p/5hWV4q4ha1RguGTtxBfCMF)**

For more information on how Mixpanel handles data, please review the [Mixpanel Privacy Policy](https://mixpanel.com/legal/privacy-policy).

## 4. Other Network Connections

Frostguard accesses other networked systems only for features selected or
configured by the user. These include GitHub for release information and
updates, Telegram for the optional watcher, and web links opened by the user.
The optional gift-code feature retrieves codes from
`gift-code-api.whiteout-bot.com` and submits the user-provided player ID and
gift code to Century Games at `wos-giftcode-api.centurygame.com`. Frostguard
also connects to the locally configured Android emulator. Those services apply
their own privacy terms.

## 5. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect changes in our practices or for other operational, legal, or regulatory reasons. We encourage you to review this document periodically.

## 6. Contact Us

If you have any questions or concerns about this Privacy Policy, the telemetry we collect, or our data practices, please reach out by opening an issue in our project repository. Or by joining our [Discord server](https://discord.com/invite/sUthSHRVvU).
