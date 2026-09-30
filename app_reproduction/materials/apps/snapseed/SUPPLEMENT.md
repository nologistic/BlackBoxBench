# Snapseed reproduction supplementary information (non-entity materials)

## Non-destructive edit stack (core semantics)

- Workflow: open an image → pick a tool/Look → **live-adjust the preview** → confirm (√)/
  cancel (×) → back to the main screen → export.
- Every confirmed edit becomes a layer stacked on the edit stack; "View edits" lets you review
  each layer, undo/redo any layer (fine-tune), or revert to the original.
- Exiting unconfirmed (×) = this adjustment does not take effect.
- Export creates a **new file** (the original is not overwritten; save-as-copy allowed).

## Tools list and adjustment dimensions

| Tool | Interaction model | Parameter dimensions |
|---|---|---|
| Tune image | Slide up/down to pick a dimension, left/right to adjust | Brightness/contrast/saturation/ambiance/highlights/shadows/warmth |
| Details | Slide up/down to switch | Structure/sharpen |
| Crop | Drag frame + aspect lock | Free/original/1:1/3:2/4:3/16:9/DIN |
| Rotate | Gesture rotation + straighten bar | Angle/mirror flip |
| Perspective | Drag corners for tilt correction | Tilt/zoom |
| Selective | Tap to place control points | Brightness/contrast/saturation/structure (radius) |
| Brush | Finger painting | Dodge/burn/exposure/temperature/saturation |
| HDR Scape | Pick a style then adjust | Filter strength/brightness/saturation |
| Glamour glow | Pick a style | Glow strength |
| Tonal contrast | Adjust by zone | Highlights/midtones/shadows/protect |
| Drama | Pick a style | Filter strength/saturation |
| Vintage | Pick a film | Brightness/saturation/vignette strength/style strength |
| Grunge | Pick a style | Texture/style strength/brightness/saturation |
| Black & white | Pick a preset | Brightness/contrast/texture |
| Portrait | Face detection (may be simplified to whole-image) | Skin tone/face brightening/eye clarity |
| Lens blur | Drag the focus area | Blur strength/transition/shape |
| Vignette | Drag the center point | Inner/outer brightness |
| Text | Type text | Font/color/style/opacity |
| Frame | Pick a frame style | Frame width/color |
| Expand | Smart edge extension | Black/white/smart fill |

## Looks

- Preset filters (Smooth/Accentuate/Early morning/…), one-finger preview, tap to apply;
- Strength is globally adjustable; Looks and tool edits share the same edit stack.

## Export

- Parameters: quality (high/medium/low), size (original/reduced), format (JPG/PNG);
- After export it goes to the album (in the sandbox: app-private directory + success state);
- "Share" opens the system share sheet.

## Key reproduction behaviors

- Tool → adjust → preview changes → confirm to push a layer → enter the next tool (stack grows) →
  View edits to roll back a layer — this state machine is the core observation point.
- Parameter direction semantics (brightness+ = brighter) and the visible change after applying a filter must be perceptible;
  precision is not required.
