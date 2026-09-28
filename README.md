# Tablet Portals

Teleport tablets deserve better than a little blue sparkle. With Tablet Portals, breaking a teleport tablet
becomes a proper send-off:

1. **Smash.** Your character raises the tablet and slams it down, and it shatters in a burst of clay shards
   and dust.
2. **Portal.** A swirling portal tears open a tile in front of you, popping to full size with smoke rolling
   out around its base.
3. **Walk in.** You stride into the portal, vanish in a flash, and it snaps shut behind you.
4. **Step out.** At your destination a portal opens behind you and you walk out of it before it closes.

**The portal reflects where it leads.** Looking into the swirl you see the place you're going, rippling like
a reflection on choppy water. The exit portal shows the place you just left.

The game only loads the area around you, so a destination can't be drawn before you've been there. Instead,
the first time you arrive somewhere with a tablet, the plugin takes a small picture of the view (refreshed
once per login) and uses it for that tablet's portal from then on. Pictures are kept in
`.runelite/plugin-data/tablet-portals`; delete that folder to reset them.

Works with every teleport tablet: house tablets, Varrock/Lumbridge/Falador/Camelot/Ardougne, redirected house
tablets, Arceuus, Ancient and Lunar tablets. Other magic tablets (enchanting, Bones to bananas) are left alone.

## Settings

| Setting | What it does |
| --- | --- |
| Smash style | *Overhead slam* (default), *Quick crush*, or *Classic* (the game's own break motion, with the new effects) |
| Tablet shatters | Shards and dust when the tablet breaks |
| Portal colour | Classic purple, Arcane blue, Emerald, Inferno, Golden, Void or a custom colour |
| Portal size | 60–160% |
| Smoke | None, Light, Normal or Thick |
| Colour the smoke | Tint the smoke to match the portal |
| Step out at destination | Walk out of a portal where you arrive |
| Reflect the destination | Show the destination in the portal, like a reflection on water |
| Reflection strength | How clearly the destination shows through the swirl |
| Ripple | Calm, A little choppy (default), or Rough |

## Good to know

- It's purely cosmetic and only on your screen. Other players still see the normal tablet animation.
- The teleport itself is untouched: you arrive exactly when you would have anyway. The whole walk-in fits
  inside the time the game already takes to teleport you.
- If you start moving or doing something as soon as you arrive, the exit portal closes early and hands
  control straight back to you.
- The portal is the game's own swirl (the one inside house portals), and the smoke and shatter are the
  game's own effects, so it all fits the look of the game.

## Building

```
./gradlew build
./gradlew run   # starts RuneLite with the plugin loaded, in developer mode
```
