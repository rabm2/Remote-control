# Bravia Mini Remote

Tiny Android remote for compatible Sony BRAVIA TVs.

## Controls
- Power
- Volume +
- Volume -
- Mute

## Setup
1. Put the phone and TV on the same Wi-Fi.
2. On the TV, enable IP control and set a Pre-Shared Key if required.
3. Build/install the app.
4. On first launch enter the TV's local IP address and PSK.
5. Long-press "SONY BRAVIA" to change the settings later.

The app uses Sony's local JSON-RPC BRAVIA control endpoints:
- /sony/system
- /sony/audio

Note: the Power button in this first version sends Power ON. The next version can query the TV's power state and toggle it correctly.
