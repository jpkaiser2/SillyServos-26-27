# Silly Servos 2026-2027
Code repository for the Silly Servos Robotics Team #24213 at Highland Park High School
<br>
2026-2027 FTC BIOBUZZ
<br>
<img src="https://jacobkaiserman.com/sillyServos-2023-2024/images/hpRobotics.png" alt="Highland Park Robotics logo" width="30%">
<br>
<h2>Using GitHub</h2>
<h3>What is GitHub?</h3>
<p>Imagine GitHub as a digital workspace where you and others can collaborate on building things together. Think of it like a shared online notebook for writing code.</p>
<h3>What can you do with GitHub?</h3>
<p>On GitHub you can:</p>
<ul>
  <li>Store your code: Keep your code organized and safe in the cloud.</li>
  <li>Track changes: See who made what changes to your code and when.</li>
  <li>Collaborate with others: Work on coding projects with other people.</li>
  <li>Learn from others: Explore and learn from code written by others.</li>
</ul>
<p>Key terms:</p>
<ul>
  <li>Repository: This is like a folder where you store your code files.</li>
  <li>Commit: This is a snapshot of your code at a specific point in time.</li>
  <li>Branch: This is a copy of your code where you can make changes without affecting the original.</li>
  <li>Pull request: This is a way to suggest changes to someone else's code.</li>
</ul>
<p>Think of it like writing a book:</p>
<ul>
  <li>The repository is the book itself.</li>
  <li>Each chapter is a branch.</li>
  <li>Saving your progress is a commit.</li>
  <li>Asking a friend to review your chapter is a pull request.</li>
</ul>

<h1>Setting up your Computer For Development</h1>
<br>
<h2>If You Have a Mac, Windows, or GNU/Linux Computer:</h2>
<p>We use GitHub as our central code repository for the robot. This allows multiple team members to collaborate efficiently on the code. While GitHub can be used for basic editing, we primarily utilize an Integrated Development Environment (IDE) for code development. An IDE is a specialized software application that provides advanced features for writing, testing, and debugging code. Our team uses Android Studio, an IDE specifically designed for Java development, to create and refine our robot's software. Since Android Studio is a desktop application, it runs directly on your local computer and needs to be downloaded. To connect GitHub (on the web) to your IDE (locally on your computer), you will need to download GitHub Desktop. The GitHub desktop GUI is the easiest way to connect to GitHub. You can also use Git in the command line, but our team uses GitHub Desktop since it is easier for beginners.</p>
<h3>Downloading GitHub Desktop & Creating a GitHub Account</h3>
<p>You will need to create a GitHub account to contribute to this repository. Navigate to <a href="https://www.github.com">GitHub.com</a> and create a free account. Next, download <a href="https://github.com/apps/desktop">GitHub Desktop</a> and sign in to your newly created account. If you are using a Linux machine, you need to install it from your preferred package manager (but if you are a Linux user you probably don't need to read this guide). You now have GitHub all set up!</p>
<h3>Cloning the GitHub Repository</h3>
<p>To edit the in-progress code repository in Android Studio, you need to have your own local copy of it. This is called "cloning" a repository. In GitHub Desktop, click on the upper left-hand corner where it says "current repository". Click on the "Add" button and select "clone repository" from the dropdown menu. Then, click the "URL" tab and enter <code>https://github.com/jpkaiser2/SillyServosInProgress</code>. Below that, choose a local path. This is where you will access the code files from your computer. Choose somewhere you will remember, such as your desktop. Click the "clone" button and you should be done!</p>
<h3>Installing Android Studio and Opening the Cloned Repository</h3>
<p>You will now need to download <a href="https://developer.android.com/studio">Android Studio</a>. Once it is installed, open the app and click on File>Open. You can then select the path of the cloned repository (from the previous step). The repository is now editable in Android Studio!</p>
<h3>Editing the Code</h3>
<p>You can edit the code in Android Studio. More information on the code structure can be found farther down on this page. Once you edit the code, you will need to push it to GitHub to publish your changes. If you haven't already, save your code in Android Studio with command+s (Mac) or ctrl+s (Windows & GNU/Linux). Navigate to GitHub Desktop and write a short summary of your code. It is a good idea to document your code, especially if you are working on it with others. Then, click "commit to main" to add your changes. Then click "push origin" to add it to GitHub. If you do not see the "push origin" button, that means someone else has made a change to the repository. Click on "pull origin" to get their changes and then click "push origin" to add your changes.</p>
<br>

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
