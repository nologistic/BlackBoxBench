# Functional Topology — fossify_calculator

- session: `sess_20260916_085453_5cc78f`
- generated: 2026-09-16T10:12:57.258666+00:00
- coverage: 7 states · 21 features · 2 data · 16 edges (confirmed ratio 100%, 173 actions)

## Graph
```
fossify_calculator
├─ States
│  ├─ [✓] Calculator main screen (0.95) `state_calculator_main_screen`
│  │    ─REVEALS→ History dialog
│  │    ─TRANSITIONS_TO→ Unit converter home
│  │    ─REVEALS→ Overflow menu
│  │    ─PERSISTS_TO→ CalculationHistory
│  ├─ [✓] Settings screen (0.95) `state_settings_screen`
│  │    ─TRANSITIONS_TO→ Customize appearance screen
│  │    ─PERSISTS_TO→ AppSettings
│  ├─ [✓] Unit converter home (0.95) `state_unit_converter_home`
│  │    ─TRANSITIONS_TO→ Length unit conversion
│  │    ─TRANSITIONS_TO→ Area unit conversion
│  │    ─TRANSITIONS_TO→ Volume unit conversion
│  │    ─TRANSITIONS_TO→ Mass unit conversion
│  │    ─TRANSITIONS_TO→ Time unit conversion
│  │    ─TRANSITIONS_TO→ Speed unit conversion
│  │    ─TRANSITIONS_TO→ Pressure unit conversion
│  │    ─TRANSITIONS_TO→ Energy unit conversion
│  │    ─TRANSITIONS_TO→ Temperature unit conversion
│  ├─ [✓] About screen (0.90) `state_about_screen`
│  ├─ [✓] History dialog (0.90) `state_history_dialog`
│  ├─ [✓] Overflow menu (0.90) `state_overflow_menu`
│  │    ─TRANSITIONS_TO→ Settings screen
│  ├─ [✓] Customize appearance screen (0.90) `state_customize_appearance_screen`
├─ Features
│  ├─ [✓] Basic addition (0.95) `feature_basic_addition`
│  ├─ [✓] Left-to-right immediate evaluation (0.90) `feature_left_to_right_immediate_evaluation`
│  ├─ [✓] Backspace and clear-all (0.90) `feature_backspace_and_clear_all`
│  ├─ [✓] Calculation history (0.90) `feature_calculation_history`
│  ├─ [✓] History persistence (0.90) `feature_history_persistence`
│  ├─ [✓] Division by zero (0.85) `feature_division_by_zero`
│  ├─ [✓] Percent operator (0.90) `feature_percent_operator`
│  ├─ [✓] Length unit conversion (0.90) `feature_length_unit_conversion`
│  ├─ [✓] Temperature unit conversion (0.90) `feature_temperature_unit_conversion`
│  ├─ [✓] Area unit conversion (0.90) `feature_area_unit_conversion`
│  ├─ [✓] Volume unit conversion (0.90) `feature_volume_unit_conversion`
│  ├─ [✓] Mass unit conversion (0.85) `feature_mass_unit_conversion`
│  ├─ [✓] Time unit conversion (0.90) `feature_time_unit_conversion`
│  ├─ [✓] Speed unit conversion (0.90) `feature_speed_unit_conversion`
│  ├─ [✓] Pressure unit conversion (0.90) `feature_pressure_unit_conversion`
│  ├─ [✓] Energy unit conversion (0.90) `feature_energy_unit_conversion`
│  ├─ [✓] Subtraction with negative result (0.95) `feature_subtraction_with_negative_result`
│  ├─ [✓] Decimal division results (0.90) `feature_decimal_division_results`
│  ├─ [✓] Power operator (0.90) `feature_power_operator`
│  ├─ [✓] Square root (0.90) `feature_square_root`
│  ├─ [✓] Result stays editable after equals (0.85) `feature_result_stays_editable_after_equals`
├─ Data
│  ├─ [✓] CalculationHistory (0.90) `data_calculationhistory`
│  ├─ [✓] AppSettings (0.85) `data_appsettings`
```

## Features
### Basic addition `feature_basic_addition`
- status: ClaimStatus.CONFIRMED · confidence: 0.95
- Entering 7+8 and pressing = shows result 15, with the expression 7+8 displayed above.
- evidence: step 6 (frame 10 → 11)

### Left-to-right immediate evaluation `feature_left_to_right_immediate_evaluation`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Chained operations are evaluated left-to-right immediately: pressing an operator after 2+3 collapses the pending expression to 5, so 2+3×4 gives 20 (not 14). Expression line shows collapsed form "5x4".
- evidence: step 23 (frame 33 → 34)

### Backspace and clear-all `feature_backspace_and_clear_all`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Tap on C deletes the last character of the current number (20 becomes 2, 60 becomes 6, 15 becomes 1). A long-press on C clears the whole expression/result back to 0.
- evidence: step 24 (frame 35 → 36)

### Calculation history `feature_calculation_history`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Clock icon opens a 历史记录 (history) dialog listing past expressions with their results, newest first. Tapping an entry loads its result into the calculator input. It has 清除 (clear history) and 确定 (close) buttons.
- evidence: step 36 (frame 56 → 57)

### History persistence `feature_history_persistence`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- The calculation history survives an app restart: after restart the same entries (5x4=20, 2+3=5, 15x4=60, 12+3=15, 7+8=15) are still listed.
- evidence: step 38 (frame 60 → 61)

### Division by zero `feature_division_by_zero`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- Dividing by zero (2÷0 then =) leaves the expression 2÷0 on screen and shows no numeric result; no error text is displayed.
- evidence: step 49 (frame 74 → 75)

### Percent operator `feature_percent_operator`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Percent is evaluated relative to the preceding operand: 200+10% = 220 (i.e. 200 + 10% of 200). A trailing % alone (e.g. 60%) stays displayed with the percent sign.
- evidence: step 88 (frame 120 → 121)

### Length unit conversion `feature_length_unit_conversion`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Length converter: two fields (from/to) with unit dropdowns and a numeric keypad. Typing a value converts live (1 km -> 1000 m). Changing the unit re-converts (1 mi -> 1609.344 m). A swap button in the middle swaps the two units.
- evidence: step 92 (frame 127 → 128)

### Temperature unit conversion `feature_temperature_unit_conversion`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Temperature converter between 摄氏度 (°C) and 开尔文 (K): 0°C shows 273.15 K, -4°C shows 269.15 K. Its keypad adds a +/- sign-toggle button.
- evidence: step 97 (frame 136 → 137)

### Area unit conversion `feature_area_unit_conversion`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Area converter (面积) with unit list: 平方千米(km²), 平方米(m²), 平方厘米(cm²), 平方毫米(mm²), 平方英里(sq mi), 平方码(sq yd), 平方英尺(sq ft), 平方英寸(sq in), 英亩(ac), 公顷(ha).
- evidence: step 127 (frame 177 → 178)

### Volume unit conversion `feature_volume_unit_conversion`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Volume converter (体积) with ~25 units: 立方米/立方分米/立方厘米/立方毫米/升/厘升/分升/毫升/英亩-英尺/立方英尺/立方英寸 plus US and UK 桶/加仑/夸脱/品脱/杯/吉耳/液量盎司.
- evidence: step 131 (frame 183 → 184)

### Mass unit conversion `feature_mass_unit_conversion`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- Mass converter (质量): default 磅(lb) -> 千克(kg). Units: 克(g), 千克(kg), 毫克(mg), 微克, 公吨(t), 磅(lb), 盎司(oz), 格令(gr), 打兰(dr), 英石(st), 长吨(ton), 短吨(sh tn), 克拉(kt), 克拉（公制）(ct).
- evidence: step 139 (frame 195 → 196)

### Time unit conversion `feature_time_unit_conversion`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Time converter (时间): default 小时(h) -> 秒(s). Units: 小时(h), 分钟(m), 秒(s), 毫秒(ms), 天(d), 周(wk), 年(y).
- evidence: step 145 (frame 204 → 205)

### Speed unit conversion `feature_speed_unit_conversion`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Speed converter (速度): default 千米/时(km/h) -> 英里/时(mph). Units: 米/秒(m/s), 千米/秒(km/s), 千米/时(km/h), 英里/时(mph), 节(kn), 英尺/秒(ft/s), 马赫(Ma), 光速(c).
- evidence: step 150 (frame 211 → 212)

### Pressure unit conversion `feature_pressure_unit_conversion`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Pressure converter (压强): default 巴(bar) -> 磅/平方英寸(psi). Units: 帕斯卡(Pa), 千帕(kPa), 兆帕(MPa), 巴(bar), 豪巴(mbar), 气压(atm), 磅/平方英寸(psi), 托尔(Torr), 毫米汞柱(mmHg), 英寸汞柱(inHg).
- evidence: step 155 (frame 218 → 219)

### Energy unit conversion `feature_energy_unit_conversion`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Energy converter (能量): default 千卡(kcal) -> 千焦(kJ). Units: 焦耳(J), 千焦(kJ), 兆焦(MJ), 吉焦(GJ), 卡路里(cal), 千卡(kcal), 瓦时(Wh), 千瓦时(kWh), 兆瓦时(MWh), 电子伏特(eV), 英制热量单位(BTU), 热单位(thm), 英尺磅(ft·lbf), 尔格(erg).
- evidence: step 160 (frame 225 → 226)

### Subtraction with negative result `feature_subtraction_with_negative_result`
- status: ClaimStatus.CONFIRMED · confidence: 0.95
- Subtraction supports negative results: 5-8 shows -3 on the result line.
- evidence: step 108 (frame 151 → 152)

### Decimal division results `feature_decimal_division_results`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Division produces decimal results: 10÷4 = 2.5, 1÷3 = 0.3333333333333333 (about 16 significant digits shown).
- evidence: step 115 (frame 160 → 161)

### Power operator `feature_power_operator`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Power operator (^): 2^3 = 8, expression shown as 2^3.
- evidence: step 65 (frame 93 → 94)

### Square root `feature_square_root`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- Square root key: pressing √ then a number computes its square root; 1√9=3 and 1√16=4 (the display shows a leading 1 before the radical).
- evidence: step 69 (frame 98 → 99)

### Result stays editable after equals `feature_result_stays_editable_after_equals`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- After pressing =, the result stays editable: tapping a digit appends to it (result -3 then 7 -> -37) instead of starting a fresh number. Pressing = again keeps the same result.
- evidence: step 109 (frame 153 → 154)
