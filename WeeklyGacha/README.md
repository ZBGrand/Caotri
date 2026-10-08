# WeeklyGacha

Paper 1.21.x / Java 21 / Maven plugin. Banners rotate in config order every 14 days from a persisted timestamp.

## Build
1. Install JDK 21 and Maven.
2. Open a terminal in this directory.
3. Run `mvn clean package`.
4. Copy `target/WeeklyGacha-1.0.0.jar` into your server's `plugins/` directory.
5. Install Vault and a Vault-compatible economy plugin (for example EssentialsX Economy), then restart.
6. Edit `plugins/WeeklyGacha/config.yml` and run `/gacha admin reload`.

## Commands
- `/gacha` — open GUI
- `/gacha history` — show last 10 entries (up to 100 stored)
- `/gacha admin list` — list banner IDs
- `/gacha admin set <bannerId>` — switch banner and restart its 14-day timer
- `/gacha admin reload` — reload config

## Permissions
- `weeklygacha.use` (default true)
- `weeklygacha.admin` (default op)

## MMOItems / ItemsAdder rewards
Rewards are delivered by configured console commands rather than compile-time API dependencies. Edit command examples and item IDs in `config.yml` to match the command syntax and item IDs on your server. Vault is a provided dependency and must be installed with a compatible economy provider for paid pulls.

`weight` is a relative weight, not a percentage. Pity is per player and per banner. Mark desired featured rewards with `pity: true`. Back up data and test on a staging server before production use.
