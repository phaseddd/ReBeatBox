---
wave: 1
depends_on: []
files_modified:
  - build.gradle
  - settings.gradle
  - src/main/java/com/rebeatbox/App.java
  - src/main/resources/soundfonts/
requirements:
  - UI-01
autonomous: true
---

# Plan 01: Project Scaffold & SoundFont

**Objective:** Initialize Gradle project with all dependencies, create package structure, and bundle FluidR3_GM.sf2 SoundFont. After this plan, the project compiles and an empty NightShade-themed window can launch.

## Tasks

<task id="01-01" type="execute">
<objective>Create Gradle project with Radiance and MIDI dependencies</objective>

<read_first>
- .planning/research/STACK.md (Radiance artifact list, versions, Java 17 requirement)
- .planning/research/ARCHITECTURE.md (package structure recommendation)
</read_first>

<action>
Create the following files:

1. `settings.gradle`:
```groovy
rootProject.name = 'ReBeatBox'
```

2. `build.gradle` with:
- Java 17 source compatibility
- Radiance 8.5.0 dependencies (radiance-common, radiance-animation, radiance-theming, radiance-component)
- Application plugin with main class `com.rebeatbox.App`
- Jar manifest with Main-Class attribute

Radiance Maven coordinates (groupId: `org.pushing-pixels`):
- `radiance-common:8.5.0`
- `radiance-animation:8.5.0`
- `radiance-theming:8.5.0`
- `radiance-component:8.5.0`

3. `src/main/java/com/rebeatbox/App.java`:
- Main class with `SwingUtilities.invokeLater()`
- Create empty JFrame with NightShade skin applied
- Set title "ReBeatBox", size 1100x700, default close EXIT_ON_CLOSE
- Window centered on screen

4. `src/main/java/com/rebeatbox/engine/` (empty package via .gitkeep or placeholder)
5. `src/main/java/com/rebeatbox/ui/` (empty package)
6. `src/main/java/com/rebeatbox/live/` (empty package, reserved for Phase 3)
7. `src/main/java/com/rebeatbox/visual/` (empty package, reserved for Phase 2)
</action>

<acceptance_criteria>
- `build.gradle` contains `radiance-theming:8.5.0` dependency
- `build.gradle` contains `radiance-animation:8.5.0` dependency
- `build.gradle` contains `radiance-component:8.5.0` dependency
- `build.gradle` contains `radiance-common:8.5.0` dependency
- `build.gradle` sets `sourceCompatibility = JavaVersion.VERSION_17`
- `App.java` exists at `src/main/java/com/rebeatbox/App.java`
- `App.java` calls `SwingUtilities.invokeLater()`
- `App.java` sets `RadianceThemingCortex.GlobalScope.setSkin(new NightShadeSkin())` or equivalent Radiance skin init
- `gradlew compileJava` exits 0
</acceptance_criteria>
</task>

<task id="01-02" type="execute">
<objective>Download and bundle FluidR3_GM.sf2 SoundFont</objective>

<read_first>
- .planning/phases/01-foundation/01-CONTEXT.md § D-13, D-14 (SoundFont decisions)
</read_first>

<action>
1. Create directory: `src/main/resources/soundfonts/`
2. Download FluidR3_GM.sf2 from https://musical-artifacts.com/artifacts/3 (FluidR3_GM.sf2, ~141MB, MIT licensed)
   Alternative source: https://archive.org/download/fluidr3-gm-gs/FluidR3_GM.sf2
3. Place file at: `src/main/resources/soundfonts/FluidR3_GM.sf2`
4. Verify the file is > 100MB (not a truncated download)
5. Add a `.gitattributes` or note in `.gitignore` if needed — the 141MB file should be tracked by git (it's a project asset, not a build artifact)
</action>

<acceptance_criteria>
- `src/main/resources/soundfonts/FluidR3_GM.sf2` exists
- File size > 100MB (check with `ls -la` or `wc -c`)
</acceptance_criteria>
</task>

## Verification

- [ ] `gradlew compileJava` succeeds
- [ ] App launches with NightShade dark theme window (no MIDI functionality yet)
- [ ] FluidR3_GM.sf2 present in resources
- [ ] Package structure matches ARCHITECTURE.md layout

## must_haves

- `truths`: ["Project compiles with Java 17 and Radiance 8.5.0", "NightShade skin is the application-wide default theme", "SoundFont is bundled as a classpath resource at soundfonts/FluidR3_GM.sf2"]
