# Shortwave

A physical radio broadcasting system for Paper 1.21+. Players build tower structures that read from a Book & Quill and broadcast messages on a configurable frequency. Other players receive those messages with a tuned Recovery Compass or a speaker block placed anywhere in the world. Towers cost resources to activate and fuel to run. Range, broadcast speed, and lines-per-broadcast are each upgraded independently.

---

## Player Guide

### Building a Tower

Towers are a 4-block vertical structure. Stack from the ground up:

```
Lightning Rod        ← top
Chiseled Copper
Lectern
Chiseled Copper      ← base (this is what you interact with)
```

Any oxidation state of chiseled copper works for both copper positions. The structure must be exact — no gaps, no substitutions.

Once built, right-click the base chiseled copper to open the activation screen. You will be asked to pay a one-time activation cost (**16 Ender Eyes** by default). After paying, you configure the tower through the management GUI.

> Axes and honeycombs still work normally on tower copper — scraping and waxing pass through to vanilla.

### Writing Your Broadcast

Write a Book & Quill and place it in the Lectern. The tower treats the entire book as one continuous stream — all pages are flattened together and blank lines are ignored. Each broadcast cycle reads the next N lines (where N depends on your broadcast line tier) and advances the cursor. When it reaches the end it wraps back to the beginning.

If you change the number of broadcast lines or swap in a new book, the cursor resets to line 1 on the next cycle.

**Tips:**
- Write in multiples of your lines-per-broadcast setting for clean, gap-free cycles. A tower broadcasting 3 lines reads 1–3, then 4–6, then 7–9, then loops.
- Long lines show as-is in chat. Keep lines short for readability.
- Broadcasts are always delivered in book order, one window at a time — no randomness.

### Fuel

Towers need fuel to broadcast. Add it through the tower GUI. Fuel counts down by real time, not server uptime — a tower running out of fuel while the server is offline picks up from the correct timestamp when the server restarts.

| Fuel | Duration |
|------|----------|
| Coal | 1 minute |
| Charcoal | 1 minute 20 seconds |
| Magma Block | 1 minute 30 seconds |
| Blaze Rod | 20 minutes |

You can add multiple items at once. Shift-clicking in the fuel GUI adds your entire stack.

A tower with no fuel stops broadcasting but keeps all settings, book content, and upgrades intact.

### Range Upgrades

All towers start with a 500-block range. Upgrades are purchased through the tower GUI and must be purchased sequentially — you cannot skip tiers.

| Tier | Material | Cost | Range |
|------|----------|------|-------|
| 1 | — | Free | 500 blocks |
| 2 | Ender Eye | 20 | 1,000 blocks |
| 3 | Iron Block | 96 | 2,500 blocks |
| 4 | Iron Block | 192 | 5,000 blocks |
| 5 | Diamond Block | 16 | 10,000 blocks |
| 6 | Emerald Block | 24 | 20,000 blocks |

### Broadcast Line Upgrades

How many book lines are sent per broadcast cycle is upgraded separately from range.

| Level | Material | Cost | Lines per Broadcast |
|-------|----------|------|---------------------|
| 1 | — | Free | 1 |
| 2 | Lapis Block | 64 | 2 |
| 3 | Lapis Block | 128 | 3 |
| 4 | Lapis Block | 192 | 4 |
| 5 | Lapis Block | 256 | 5 |
| 6 | Emerald Block | 12 | 6 |

Within your unlocked maximum you can also choose to send fewer lines per cycle using the +/− controls in the GUI. Useful for shorter announcements that you want to loop faster.

### Broadcast Interval Upgrades

How frequently your tower broadcasts is also upgradeable. The interval is in seconds and can be set anywhere from your unlocked minimum up to 300 seconds.

| Tier | Material | Cost | Fastest Interval |
|------|----------|------|------------------|
| 1 | — | Free | 60 seconds |
| 2 | Gold Block | 64 | 45 seconds |
| 3 | Gold Block | 96 | 30 seconds |
| 4 | Ender Eye | 32 | 20 seconds |
| 5 | Ender Eye | 48 | 15 seconds |
| 6 | Ender Eye | 64 | 10 seconds |
| 7 | Emerald Block | 12 | 5 seconds |

### Jingles

A jingle is a short sound that plays for nearby players with a tuned receiver at the start of each broadcast. Select one in the tower GUI. The current options are:

| Name | Sound |
|------|-------|
| The Alert | Bell |
| The Tech | Beacon activate |
| The Magic | Amethyst chime |
| The Classic | Note block pling |
| The Retro | XP orb pickup |
| The Heavy | Anvil land |
| The Victory | Level up |

### Oxidation and Signal Quality

Tower copper oxidizes very slowly over time (roughly 2% of vanilla speed by default). As it oxidizes, the broadcast signal gets garbled.

| Copper State | Signal Garble |
|--------------|--------------|
| Unaffected (fresh) | None |
| Exposed | 10% of characters garbled |
| Weathered | 30% garbled |
| Oxidized | 100% — entire message becomes `#### ## ####` |

Scrape the base copper with an axe to reset oxidation. Waxing it with a honeycomb locks it at the current state permanently.

Multiple towers on the same frequency within range of a receiver also add garble on top of oxidation. Each competing tower stacks an additional 10% (configurable), capped at 90%.

### Voice Broadcasting (SimpleVoiceChat)

If the server has SimpleVoiceChat installed, each tower has a **Voice Mode** toggle in the GUI. When enabled, players within the tower's range can transmit voice audio on that tower's frequency — heard by any player with a receiver tuned to the matching frequency, also within range.

Voice mode is independent of fuel and book content. A tower can broadcast text, voice, both, or neither.

### Receiving Broadcasts

**Handheld receiver (Recovery Compass)**

Right-click a Recovery Compass to tune it. You will be prompted to type a frequency in chat (format: `104.50`, range: `88.00`–`108.00`). Once tuned, hold it in your hand or hotbar — broadcasts from any tower on that frequency within range will appear in your chat, preceded by the tower's jingle sound.

After tuning, the compass also tells you which active towers on that frequency are currently within range and their coordinates, so you know what you can receive.

**Speaker blocks**

Build a speaker by placing any copper block on the ground with a Decorated Pot on top. Right-click the pot. The first time you activate a speaker it costs **8 Iron Blocks**. After paying, type a frequency in chat to tune it.

When a tower on the matching frequency broadcasts and the speaker is within range, a floating text display appears above the pot showing the message for 5 seconds. Only one hologram is shown at a time — if a new broadcast arrives before the previous one expires, it replaces it immediately.

Re-tuning an existing speaker to a different frequency is always free — just right-click the pot again.

Speakers only produce holograms when their chunk is loaded.

### Structure Resilience

Towers and speakers are designed to survive partial damage.

**Towers** — the base chiseled copper is the anchor:
- Breaking the base copper **destroys** the tower registration and all its data.
- Breaking the lectern, top copper, or lightning rod **damages** the tower (broadcasting stops). The registration and all upgrades/settings are kept.
- Placing the missing blocks back **restores** the tower automatically — no interaction needed, no cost. Broadcasting resumes on the next cycle.

**Speakers** — the copper block is the anchor:
- Breaking the copper block **destroys** the speaker registration.
- Breaking the pot **disables** the speaker (no holograms appear) but the registration is kept.
- Placing the pot back **restores** it automatically. No re-activation cost.

### Reinforcement and Access

If a tower or speaker copper block is reinforced to a NameLayer group via Citadel, only members of that group with the `USE_RADIO` permission can open the tower GUI or activate speakers near it. Members and above have this permission by default. Unreinforced towers and speakers are accessible to anyone.

---

## Admin Reference

### Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/shortwave clearholograms` | `shortwave.admin` | Removes all stuck speaker holograms across all loaded worlds |

Hologram cleanup also runs automatically 1 tick after the plugin enables to clear any leftover displays from a previous session.

### Configuration

All values are in `config.yml`. Every field is commented. Key non-obvious settings:

- **`default-tower-range`** — range for new towers before any upgrades are purchased.
- **`interference-garble-per-tower`** — additional garble percentage each competing tower on the same frequency adds, stacked and capped at 90%.
- **`frequency.decimal-places`** — `1` gives format `104.5`, `2` gives `104.53`. Changing this after towers are registered will break existing tuned receivers (frequency strings won't match).
- **`tower-oxidation-cancel-chance`** — probability (0.0–1.0) that a natural oxidation tick on tower copper is suppressed. Default `0.98` = ~2% of vanilla oxidation speed.
- **`fuel.duration-seconds`** — real-time seconds, not ticks. Fuel runs down by wall clock whether the server is on or not.
- **`auto-save-interval-ticks`** — periodic save cadence. Data is also saved immediately on any state change (fuel added, upgrade purchased, frequency changed, etc.).

### Data Persistence

| File | Contents |
|------|----------|
| `plugins/Shortwave/towers.json` | All tower data |
| `plugins/Shortwave/speakers.json` | Speaker locations and tuned frequencies |

**Tower data saved:** location, frequency, fuel end timestamp (Unix ms), current book cursor position, jingle, range, broadcast line tier and selected value, broadcast interval tier and selected value, cached book pages, cached oxidation level, voice mode state.

**Speaker data saved:** location, tuned frequency.

Fuel uses a Unix millisecond timestamp (`System.currentTimeMillis() + duration`) so it drains correctly while the server is offline.

The broadcast cursor (`currentPage`) is saved to disk. If the server restarts mid-cycle, the next cycle picks up at the saved line rather than restarting from line 1 — unless the book or line count changed, in which case it resets to 1.

### Caching Architecture

`RadioTower` holds three cached values that the broadcast task reads without touching any blocks:

- **`cachedPages`** — book pages, updated when a player places a book in the lectern (via `InteractionListener`) and refreshed on GUI open. Saved to disk so it survives restarts without block access.
- **`cachedOxidation`** — copper oxidation level, updated by `OxidationListener` on `BlockFadeEvent` and refreshed on GUI open. Saved to disk.
- **`structureIntact`** — defaults to `true` on load. Set to `false` when any non-anchor structure block breaks. Re-validated against actual block state when a player places blocks back (via `BlockPlaceEvent`) or opens the GUI.

`BroadcastTask` does zero block reads. Towers broadcast correctly even when their chunk is unloaded since all required data is in the model. Speakers intentionally skip unloaded chunks — `SpeakerManager.getLoadedSpeakersOnFrequency()` checks `world.isChunkLoaded()` before including a speaker.

### Code Structure

```
world.edenmc.shortwave/
├── ShortwavePlugin.java              plugin entry point, command handler, hologram cleanup
├── models/
│   └── RadioTower.java               tower data model and all cached state
├── managers/
│   ├── TowerManager.java             tower registry and JSON persistence
│   ├── SpeakerManager.java           speaker registry and JSON persistence
│   ├── ConfigManager.java            typed config access
│   └── GUIManager.java               all GUI screens (CivModCore ClickableInventory)
├── listeners/
│   ├── InteractionListener.java      right-click, chat input, block break/place
│   └── OxidationListener.java        BlockFadeEvent — updates cached oxidation on tower copper
├── tasks/
│   ├── BroadcastTask.java            BukkitRunnable firing every second (per-tower intervals apply)
│   └── ParticleTask.java             visual indicators for towers and speakers (SVC only)
└── voice/
    └── VoiceManager.java             SimpleVoiceChat integration (soft dependency)
```

### Dependencies

| Dependency | Required | Purpose |
|------------|----------|---------|
| CivModCore | Yes | GUI framework (`ClickableInventory`) |
| Citadel | Yes | Reinforcement access checks |
| NameLayer | Yes | `USE_RADIO` permission type registration |
| SimpleVoiceChat | No | Voice relay between tower transmitters and receivers |

Built with Gradle (`build.gradle.kts`) following the standard EdenMC plugin structure.
