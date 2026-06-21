# SillyServos-26-27

Silly Servos FTC robot codebase for the 2026-2027 season, built from the Pedro Pathing quickstart!

## What This Repo Contains

This project uses a minimal TeamCode structure:

- `config/HardwareNames.java`: all RC hardware map names
- `config/RobotConstants.java`: tunable values for drive and autonomous behavior
- `pedroPathing/PedroMecanumDrive.java`: wrapper around Pedro follower setup and mecanum control
- `opmodes/teleop/TemplateTeleOp.java`: driver control template
- `opmodes/auto/TemplateAuto.java`: autonomous template

The goal is to keep the base code easy to read.

## How The Robot Code Works

### 1) Hardware naming

`HardwareNames` is the single source of truth for drivetrain configuration names.


### 2) Tuning values

`RobotConstants` contains configurable values used by opmodes:

- drive speed limits
- teleop field-centric toggle
- simple autonomous forward speed/duration

### 3) Pedro mecanum drive wrapper

`PedroMecanumDrive` builds and owns the Pedro `Follower`.

It provides a small API used by opmodes:

- start teleop drive
- set drive powers (`forward`, `strafe`, `turn`)
- update follower each loop
- stop drivetrain safely
- read robot pose for telemetry

### 4) Opmodes

- `TemplateTeleOp`:
	- reads gamepad1 sticks
	- sends powers to Pedro drive
	- updates and reports pose telemetry
- `TemplateAuto`:
	- sets a starting pose
	- runs a simple timed forward movement
	- stops drive at the end

## Team Workflow

### Daily flow

1. Pull latest `main`.
2. Create a feature branch from `main`.
3. Implement and test in small increments.
4. Open a PR early, then iterate.
5. Merge after review and validation.

### Branch strategy

- `main`: always stable and competition-ready.
- Short-lived branches for active work.

Branch naming:

- `feat/<short-description>`
- `fix/<short-description>`
- `chore/<short-description>`
- `docs/<short-description>`
- `refactor/<short-description>`
- `test/<short-description>`

Examples:

- `feat/add-spline-auto`
- `fix/teleop-turn-scaling`
- `chore/update-gradle-wrapper`

## Commit Message Guidelines

Use Conventional Commit style:

`<type>: <short imperative summary>`

Common types used in this repo:

- `feat:` new robot capability or behavior
- `fix:` bug fix or behavior correction
- `chore:` maintenance, tooling, cleanup with no behavior change
- `docs:` README/comments/process docs
- `refactor:` code structure improvement without behavior change
- `test:` test additions or updates
- `perf:` performance improvement
- `build:` build system or dependency updates
- `ci:` CI workflow changes

Good examples:

- `feat: add field-centric toggle in teleop`
- `fix: clamp turn power for stable strafing`
- `chore: remove unused template files`
- `docs: add branch and PR process to root README`

## Pull Request Guidelines

### PR title format

Match Conventional Commit format:

- `feat: add red-side preload auto skeleton`
- `fix: correct starting pose heading`

### PR checklist

Before requesting review:

1. Branch is up to date with `main`.
2. Code is focused on one logical change.
3. Opmode behavior is tested on bot or simulator workflow.
4. Hardware names/constants changes are documented in PR description.
5. No unrelated file changes included.

### Review expectations

- At least one teammate review for normal changes.
- Author resolves feedback and re-requests review.

### Merge policy

- PR must pass required checks/review before merge.
- Do not push directly to `main` except emergency hotfixes.
- Always push to `dev` since that branch will later be merged with `main`.

## Quick Start For New Members

1. Clone repo and open in Android Studio.
2. Connect FTC SDK/Android environment.
3. Ask Jacob for futher instructions.