# LMonitor

LMonitor mirrors your Android phone's screen to your car's Android Auto display,
turning it into an external monitor while parked.

This is a personal fork of [MirrorMobile](https://github.com/chenxiaolong/MirrorMobile)
by Andrew Gunnerson, adapted for my own Leapmotor B10.

## What's different from upstream

- **Adjustable driving-speed cutoff**: instead of a fixed `0.001 m/s` threshold, the
  pause-while-driving speed is now a tappable preference in Settings, cycling through
  a few presets (`0.001` / `0.5` / `1.0` / `1.5` / `2.0` m/s). The pause-while-driving
  behavior itself is unchanged and cannot be disabled — see upstream's README for why.

More changes will land here as this fork evolves for B10-specific use.

## Features (inherited from upstream)

- Mirrors the phone screen to Android Auto
- Pauses screen mirroring when the car is driven
  - **NOTE**: If the vehicle doesn't report its speed to Android Auto, LMonitor can't
    be used at all

See [upstream's README](https://github.com/chenxiaolong/MirrorMobile#readme) for the
full list of features, limitations, and known Android Auto rendering bugs that affect
this fork too, since they live in the platform, not in the app.

## License

LMonitor is licensed under GPL-3.0-only, same as upstream. See [LICENSE](LICENSE).

## Credit

All the hard work here is Andrew Gunnerson's. This fork exists to scratch a personal
itch (Leapmotor B10 integration) — if you're not me, you almost certainly want
[the original project](https://github.com/chenxiaolong/MirrorMobile) instead.
