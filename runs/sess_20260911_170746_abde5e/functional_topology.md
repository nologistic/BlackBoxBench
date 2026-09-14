# Functional Topology — fossify_paint

- session: `sess_20260911_170746_abde5e`
- generated: 2026-09-11T18:02:57.854104+00:00
- coverage: 15 states · 21 features · 2 data · 16 edges (confirmed ratio 100%, 98 actions)

## Graph
```
fossify_paint
├─ States
│  ├─ [✓] Blank drawing canvas (0.99) `state_blank_drawing_canvas`
│  │    ─TRANSITIONS_TO→ Overflow menu
│  │    ─TRANSITIONS_TO→ Color picker dialog
│  │    ─TRANSITIONS_TO→ Clear-canvas confirmation
│  │    ─TRANSITIONS_TO→ Save As dialog
│  ├─ [✓] Overflow menu (0.99) `state_overflow_menu`
│  │    ─TRANSITIONS_TO→ Settings screen
│  │    ─TRANSITIONS_TO→ System photo picker
│  │    ─TRANSITIONS_TO→ About screen
│  ├─ [✓] Settings screen (0.99) `state_settings_screen`
│  │    ─TRANSITIONS_TO→ Custom appearance settings
│  ├─ [✓] Custom appearance settings (0.99) `state_custom_appearance_settings`
│  │    ─TRANSITIONS_TO→ Theme customization preview
│  ├─ [✓] Theme customization preview (0.99) `state_theme_customization_preview`
│  ├─ [✓] Color picker dialog (0.99) `state_color_picker_dialog`
│  ├─ [✓] Clear-canvas confirmation (0.99) `state_clear_canvas_confirmation`
│  ├─ [✓] Save As dialog (0.99) `state_save_as_dialog`
│  │    ─TRANSITIONS_TO→ System export file picker
│  ├─ [✓] System export file picker (0.99) `state_system_export_file_picker`
│  ├─ [✓] System photo picker (0.99) `state_system_photo_picker`
│  │    ─TRANSITIONS_TO→ Painted canvas with mixed edits
│  ├─ [✓] About screen (0.99) `state_about_screen`
│  │    ─TRANSITIONS_TO→ Third-party licenses screen
│  ├─ [✓] Third-party licenses screen (0.99) `state_third_party_licenses_screen`
│  ├─ [✓] Painted canvas with mixed edits (0.99) `state_painted_canvas_with_mixed_edits`
│  ├─ [✓] Blue background canvas (0.99) `state_blue_background_canvas`
│  ├─ [✓] Application font chooser (0.99) `state_application_font_chooser`
├─ Features
│  ├─ [✓] Appearance editor save guard (0.99) `feature_appearance_editor_save_guard`
│  ├─ [✓] Open settings from drawing workspace (0.99) `feature_open_settings_from_drawing_workspace`
│  ├─ [✓] Application theme selection (0.99) `feature_application_theme_selection`
│  ├─ [✓] Freehand drawing (0.99) `feature_freehand_drawing`
│  ├─ [✓] Brush size adjustment (0.99) `feature_brush_size_adjustment`
│  ├─ [✓] Undo and redo drawing edits (0.99) `feature_undo_and_redo_drawing_edits`
│  ├─ [✓] Change drawing color (0.99) `feature_change_drawing_color`
│  ├─ [✓] Flood fill connected canvas area (0.99) `feature_flood_fill_connected_canvas_area`
│  ├─ [✓] Erase with adjustable width (0.99) `feature_erase_with_adjustable_width`
│  ├─ [✓] Pick color from canvas (0.99) `feature_pick_color_from_canvas`
│  ├─ [✓] Clear canvas with confirmation (0.99) `feature_clear_canvas_with_confirmation`
│  ├─ [✓] Export drawing as PNG (0.99) `feature_export_drawing_as_png`
│  │    ─MUTATES→ Exported drawing image
│  ├─ [✓] Open an image as canvas (0.99) `feature_open_an_image_as_canvas`
│  │    ─DEPENDS_ON→ Exported drawing image
│  ├─ [✓] Share action device behavior (0.95) `feature_share_action_device_behavior`
│  ├─ [✓] Print action device behavior (0.90) `feature_print_action_device_behavior`
│  ├─ [✓] Open About and license information (0.99) `feature_open_about_and_license_information`
│  ├─ [✓] Live canvas resets but tool preferences persist on restart (0.99) `feature_live_canvas_resets_but_tool_preferences_persist_on_restart`
│  ├─ [✓] Change persistent canvas background color (0.99) `feature_change_persistent_canvas_background_color`
│  │    ─MUTATES→ Drawing preferences
│  ├─ [✓] Toggle brush-size tool visibility (0.99) `feature_toggle_brush_size_tool_visibility`
│  │    ─MUTATES→ Drawing preferences
│  ├─ [✓] Toggle keep-awake preference (0.99) `feature_toggle_keep_awake_preference`
│  ├─ [✓] Preview application font choice (0.99) `feature_preview_application_font_choice`
├─ Data
│  ├─ [✓] Exported drawing image (0.99) `data_exported_drawing_image`
│  │    ←DEPENDS_ON─ Open an image as canvas
│  ├─ [✓] Drawing preferences (0.99) `data_drawing_preferences`
```

## Features
### Appearance editor save guard `feature_appearance_editor_save_guard`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Pressing back after changing the application theme opens a confirmation dialog with 丢弃 and 保存 actions; the edited theme remains previewed behind the modal.
- evidence: step 9 (frame 17 → 18)

### Open settings from drawing workspace `feature_open_settings_from_drawing_workspace`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The overflow menu exposes a 设置 item that transitions from the drawing canvas to the settings page.
- evidence: step 3 (frame 5 → 6)

### Application theme selection `feature_application_theme_selection`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The app theme chooser lists system default, light, dark, dark red, white, black-and-white, and custom options. Selecting a concrete theme immediately previews editable text/background/primary/icon colors and requires confirmation.
- evidence: step 8 (frame 15 → 16)

### Freehand drawing `feature_freehand_drawing`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Dragging across the white canvas paints a rounded stroke in the current color; after the first stroke an undo control appears.
- evidence: step 14 (frame 23 → 24)

### Brush size adjustment `feature_brush_size_adjustment`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Tapping along the bottom horizontal slider moves its thumb, enlarges the circular brush preview, and subsequent strokes become much thicker.
- evidence: step 15 (frame 25 → 26)

### Undo and redo drawing edits `feature_undo_and_redo_drawing_edits`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Undo removes the most recent stroke and reveals a redo control; tapping redo restores that stroke.
- evidence: step 17 (frame 29 → 30)

### Change drawing color `feature_change_drawing_color`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Selecting a preset in the color picker previews its new hex value and color, and confirming updates both the toolbar swatch and brush preview; new strokes use the chosen color while existing strokes remain unchanged.
- evidence: step 19 (frame 33 → 34)

### Flood fill connected canvas area `feature_flood_fill_connected_canvas_area`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Selecting the fill bucket hides brush-size controls; tapping the white canvas fills its connected background with the current red color while differently colored strokes remain visible. Undo restores the prior background.
- evidence: step 23 (frame 41 → 42)

### Erase with adjustable width `feature_erase_with_adjustable_width`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Selecting the eraser highlights it and retains the size preview/slider; dragging across a colored stroke removes pixels along a wide rounded path, revealing the white canvas.
- evidence: step 26 (frame 47 → 48)

### Pick color from canvas `feature_pick_color_from_canvas`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Selecting the eyedropper hides size controls; tapping a green painted area changes the current-color swatch from red to that sampled green while the eyedropper stays active.
- evidence: step 28 (frame 51 → 52)

### Clear canvas with confirmation `feature_clear_canvas_with_confirmation`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Trash first asks for confirmation. Choosing No keeps the drawing. Choosing Yes blanks the canvas, and the resulting clear action itself can be undone to restore the drawing.
- evidence: step 30 (frame 55 → 56)

### Export drawing as PNG `feature_export_drawing_as_png`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Save opens a naming/format dialog supporting PNG, SVG, and JPG. Confirming PNG opens the Android document picker; tapping its Save button writes the timestamped PNG and returns to the unchanged canvas.
- evidence: step 35 (frame 64 → 65)

### Open an image as canvas `feature_open_an_image_as_canvas`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Open File invokes the Android photo picker. Choosing the recently saved drawing loads it onto the canvas; after clearing the live canvas, choosing the same saved preview restores the saved green/red/erased picture.
- evidence: step 39 (frame 71 → 72)

### Share action device behavior `feature_share_action_device_behavior`
- status: ClaimStatus.CONFIRMED · confidence: 0.95
- On this registered device, tapping Share with a drawn stroke visible returned to the drawing workspace with a blank canvas and no share destination UI left on screen. This happened twice.
- evidence: step 49 (frame 89 → 90)

### Print action device behavior `feature_print_action_device_behavior`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Choosing 打印 from the overflow menu returned directly to the unchanged blue drawing workspace; no print dialog remained visible on this registered device.
- evidence: step 57 (frame 103 → 104)

### Open About and license information `feature_open_about_and_license_information`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The overflow menu opens the About page, and its 第三方许可 row opens a separate long, scrollable license text screen. Back returns through About to the canvas.
- evidence: step 59 (frame 106 → 107)

### Live canvas resets but tool preferences persist on restart `feature_live_canvas_resets_but_tool_preferences_persist_on_restart`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Restarting after loading/drawing clears the active painted pixels and removes undo history. The chosen green color and maximum brush size persist; after changing the background to blue, a later restart also preserves the blue background.
- evidence: step 46 (frame 83 → 84)

### Change persistent canvas background color `feature_change_persistent_canvas_background_color`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Change Background Color opens the color chooser initialized from white. Selecting blue previews #1976D2; confirming recolors the full canvas blue, changes tool icons to white for contrast, does not create undo history, and the blue background survives app restart.
- evidence: step 53 (frame 96 → 97)

### Toggle brush-size tool visibility `feature_toggle_brush_size_tool_visibility`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Turning off 显示画笔大小工具 changes its switch to off. Returning to the canvas removes the bottom brush preview and size slider entirely. Turning the switch back on restores the enabled setting.
- evidence: step 74 (frame 129 → 130)

### Toggle keep-awake preference `feature_toggle_keep_awake_preference`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The prevent-auto-sleep switch can be turned off and back on from Settings; its visual switch state updates immediately.
- evidence: step 73 (frame 127 → 128)

### Preview application font choice `feature_preview_application_font_choice`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Choosing 等宽字体 closes the chooser, updates the displayed application-font value, previews the different lettering, and reveals a top checkmark for saving. Leaving by Back invokes the existing discard-or-save guard.
- evidence: step 89 (frame 147 → 148)
