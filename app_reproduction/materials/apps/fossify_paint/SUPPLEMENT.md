# Fossify Paint reproduction supplementary information (non-entity materials)

## Brushes and colors

- Brush: drag the slider to change thickness, **taking effect from the next stroke**; existing strokes stay.
- Eraser: dragging clears the area back to the canvas background color (not transparent).
- Color: tap the current color swatch → color picker → preset palette + 2D palette/hue bar
  → after confirming, the swatch updates and later strokes use the new color.
- Custom colors are saved to "Recently used" and are still there after reopening.

## Canvas and undo

- The canvas starts as a solid background (the background color is changeable; existing strokes stay in the foreground).
- Undo/redo is per stroke; clearing the canvas asks for confirmation.

## Import and export

- "Open file": load a PNG/JPG from the file picker as new canvas content.
- "Import as background": the image sits underneath and new strokes are drawn on top.
- Save/export: enter a file name → PNG or JPG; after saving it is visible in the system gallery/files
  (in the sandbox it can save to the app-private directory and show a success state).
- Share: opens the system share sheet (in the sandbox, showing the sheet or a success notice is fine).

## Key reproduction behaviors

- Draw a stroke → it appears on screen; change thickness/color → draw again and the two strokes differ in style;
  eraser → only the area it passes is erased; undo → the last stroke disappears.
- Background-color change + image import + export are the key observation points of the write-path loop.
