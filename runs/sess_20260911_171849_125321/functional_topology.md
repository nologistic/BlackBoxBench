# Functional Topology — fossify_calendar

- session: `sess_20260911_171849_125321`
- generated: 2026-09-11T18:20:07.307916+00:00
- coverage: 43 states · 19 features · 4 data · 3 edges (confirmed ratio 100%, 132 actions)

## Graph
```
fossify_calendar
├─ States
│  ├─ [✓] Day view empty agenda (0.98) `state_day_view_empty_agenda`
│  ├─ [✓] Calendar view selector (0.99) `state_calendar_view_selector`
│  ├─ [✓] Month view (0.99) `state_month_view`
│  ├─ [✓] Add item menu (0.99) `state_add_item_menu`
│  ├─ [✓] New activity editor (0.99) `state_new_activity_editor`
│  ├─ [✓] Reminder disclaimer (0.99) `state_reminder_disclaimer`
│  ├─ [✓] Activity repeat choices (0.99) `state_activity_repeat_choices`
│  ├─ [✓] Custom activity recurrence (0.99) `state_custom_activity_recurrence`
│  ├─ [✓] Activity attendance status choices (0.99) `state_activity_attendance_status_choices`
│  ├─ [✓] Activity color picker (0.99) `state_activity_color_picker`
│  ├─ [✓] Notification permission prompt (0.99) `state_notification_permission_prompt`
│  ├─ [✓] Day agenda with activity (0.99) `state_day_agenda_with_activity`
│  ├─ [✓] Edit activity (0.99) `state_edit_activity`
│  ├─ [✓] New task editor (0.99) `state_new_task_editor`
│  ├─ [✓] Edit task (0.99) `state_edit_task`
│  ├─ [✓] Search results (0.99) `state_search_results`
│  ├─ [✓] Settings overview (0.98) `state_settings_overview`
│  ├─ [✓] Appearance settings (0.99) `state_appearance_settings`
│  ├─ [✓] Custom icon compatibility warning (0.95) `state_custom_icon_compatibility_warning`
│  ├─ [✓] App theme choices (0.99) `state_app_theme_choices`
│  ├─ [✓] Unsaved appearance confirmation (0.99) `state_unsaved_appearance_confirmation`
│  ├─ [✓] Manage calendars (0.99) `state_manage_calendars`
│  ├─ [✓] Add calendar dialog (0.99) `state_add_calendar_dialog`
│  ├─ [✓] Reminder and sync settings (0.99) `state_reminder_and_sync_settings`
│  ├─ [✓] CalDAV calendar selection empty (0.99) `state_caldav_calendar_selection_empty`
│  ├─ [✓] Calendar view behavior settings (0.98) `state_calendar_view_behavior_settings`
│  ├─ [✓] List widget and item settings (0.99) `state_list_widget_and_item_settings`
│  ├─ [✓] Backup and migration settings (0.99) `state_backup_and_migration_settings`
│  ├─ [✓] Export activities dialog (0.99) `state_export_activities_dialog`
│  ├─ [✓] Import iCalendar file picker (0.99) `state_import_icalendar_file_picker`
│  ├─ [✓] Month view with multiple calendars (0.98) `state_month_view_with_multiple_calendars`
│  ├─ [✓] Calendar visibility filter (0.99) `state_calendar_visibility_filter`
│  ├─ [✓] Main overflow menu (0.99) `state_main_overflow_menu`
│  ├─ [✓] Go to month dialog (0.99) `state_go_to_month_dialog`
│  ├─ [✓] state_holiday_country_selector (0.99) `state_holiday_country_selector`
│  ├─ [✓] state_contact_birthday_import_options (0.99) `state_contact_birthday_import_options`
│  ├─ [✓] state_contact_anniversary_import_options (0.99) `state_contact_anniversary_import_options`
│  ├─ [✓] state_about_page (0.99) `state_about_page`
│  ├─ [✓] state_week_view (0.99) `state_week_view`
│  ├─ [✓] state_month_and_day_view (1.00) `state_month_and_day_view`
│  ├─ [✓] state_year_view (1.00) `state_year_view`
│  ├─ [✓] state_simple_event_list (1.00) `state_simple_event_list`
│  ├─ [✓] state_delete_activity_confirmation (1.00) `state_delete_activity_confirmation`
├─ Features
│  ├─ [✓] Switch calendar layout (0.99) `feature_switch_calendar_layout`
│  │    ─REVEALS→ Calendar view selector
│  ├─ [✓] Create activity (0.99) `feature_create_activity`
│  │    ─MUTATES→ Activity
│  ├─ [✓] Persist activity across restart (0.99) `feature_persist_activity_across_restart`
│  │    ─PERSISTS_TO→ Activity
│  ├─ [✓] Duplicate activity (0.99) `feature_duplicate_activity`
│  ├─ [✓] Create task (0.99) `feature_create_task`
│  ├─ [✓] Complete task (0.99) `feature_complete_task`
│  ├─ [✓] Search activities and tasks (0.98) `feature_search_activities_and_tasks`
│  ├─ [✓] Change app theme (0.99) `feature_change_app_theme`
│  ├─ [✓] Create local calendar (0.99) `feature_create_local_calendar`
│  ├─ [✓] Manage individual calendars (0.99) `feature_manage_individual_calendars`
│  ├─ [✓] Enable CalDAV synchronization (0.99) `feature_enable_caldav_synchronization`
│  ├─ [✓] Configure iCalendar export (0.99) `feature_configure_icalendar_export`
│  ├─ [✓] Filter calendar visibility (0.99) `feature_filter_calendar_visibility`
│  ├─ [✓] Quick toggle calendar (0.98) `feature_quick_toggle_calendar`
│  ├─ [✓] feature_add_country_holidays (0.97) `feature_add_country_holidays`
│  ├─ [✓] feature_import_contact_birthdays (0.98) `feature_import_contact_birthdays`
│  ├─ [✓] feature_import_contact_anniversaries (0.98) `feature_import_contact_anniversaries`
│  ├─ [✓] feature_delete_activity_with_confirmation (1.00) `feature_delete_activity_with_confirmation`
│  ├─ [✓] feature_all_day_activity (1.00) `feature_all_day_activity`
├─ Data
│  ├─ [✓] Activity (0.99) `data_activity`
│  ├─ [✓] Task (0.99) `data_task`
│  ├─ [✓] Calendar (0.99) `data_calendar`
│  ├─ [✓] data_app_identity (1.00) `data_app_identity`
```

## Features
### Switch calendar layout `feature_switch_calendar_layout`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The grid button opens a six-choice radio menu; choosing Month view closes the menu and redraws the calendar as a full month grid.
- evidence: step 8 (frame 15 → 16)

### Create activity `feature_create_activity`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The add menu opens a form for an activity. Confirming a filled form requests notification permission on first use and then adds a colored activity bar to its date in month view.
- evidence: step 11 (frame 21 → 22)

### Persist activity across restart `feature_persist_activity_across_restart`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- A saved activity remains visible on its date after the app is restarted.
- evidence: step 32 (frame 55 → 56)

### Duplicate activity `feature_duplicate_activity`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The duplicate icon opens a new-activity form prefilled with all values from the existing activity, ready to save as another item.
- evidence: step 37 (frame 64 → 65)

### Create task `feature_create_task`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Choosing Task opens a task-specific form. Saving a filled form adds a task card with a check-circle indicator to the selected day.
- evidence: step 40 (frame 69 → 70)

### Complete task `feature_complete_task`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The bottom completion action returns to the day agenda and renders the task in muted colors with strikethrough title while retaining its card.
- evidence: step 53 (frame 88 → 89)

### Search activities and tasks `feature_search_activities_and_tasks`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- Entering a query filters calendar items and shows matching result cards grouped under their month and date.
- evidence: step 57 (frame 95 → 96)

### Change app theme `feature_change_app_theme`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Selecting Dark theme immediately restyles the screen with dark surfaces and reveals separate text, background, primary, and icon color controls.
- evidence: step 65 (frame 110 → 111)

### Create local calendar `feature_create_local_calendar`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The add action opens a title-and-color dialog; confirming a title adds the calendar to the management list.
- evidence: step 71 (frame 121 → 122)

### Manage individual calendars `feature_manage_individual_calendars`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Each calendar has a menu with Edit and Delete actions.
- evidence: step 75 (frame 128 → 129)

### Enable CalDAV synchronization `feature_enable_caldav_synchronization`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Turning on CalDAV first requests calendar access, then opens a chooser for synchronizable calendars. The observed device had no calendars available and displayed an empty-state message.
- evidence: step 79 (frame 134 → 135)

### Configure iCalendar export `feature_configure_icalendar_export`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Exporting to .ics opens a dialog to name the file and choose activities, tasks, past entries, and calendars.
- evidence: step 85 (frame 145 → 146)

### Filter calendar visibility `feature_filter_calendar_visibility`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Unchecking Local calendar and confirming removes its activity and task from the month grid; the bottom calendar indicator becomes muted while Work remains active.
- evidence: step 92 (frame 157 → 158)

### Quick toggle calendar `feature_quick_toggle_calendar`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- Tapping a muted calendar name in the bottom bar immediately restores that calendar and its items without opening the filter dialog.
- evidence: step 94 (frame 160 → 161)

### feature_add_country_holidays `feature_add_country_holidays`
- status: ClaimStatus.CONFIRMED · confidence: 0.97
- 从更多菜单进入国家或地区单选列表，可选择并添加对应公共节假日。
- evidence: step 105 (frame 178 → 179)

### feature_import_contact_birthdays `feature_import_contact_birthdays`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 可读取通讯录生日并设定生日活动提醒，也能选择自动添加以后新增的生日。
- evidence: step 108 (frame 183 → 184)

### feature_import_contact_anniversaries `feature_import_contact_anniversaries`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 可读取通讯录纪念日并配置提醒及自动添加新纪念日。
- evidence: step 113 (frame 191 → 192)

### feature_delete_activity_with_confirmation `feature_delete_activity_with_confirmation`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 活动可删除，操作前要求二次确认。
- evidence: step 128 (frame 215 → 216)

### feature_all_day_activity `feature_all_day_activity`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 活动可切换为全天；开启后开始与结束时间隐藏，仅保留开始和结束日期。
- evidence: step 130 (frame 218 → 219)
