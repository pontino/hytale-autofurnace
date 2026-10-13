# AutoFurnace

A server-side Hytale mod: press **Turn On** once and your furnace (or any fuel-based processing bench)
**relights itself** whenever it has something to process.

- **Turn On** → the furnace starts as usual and switches to **auto mode**. When it runs out of input it goes idle,
  and as soon as you add more it starts again by itself.
- **Turn Off** → the furnace stops and auto mode is disabled.
- A chat message confirms when auto mode is enabled or disabled.
- The setting is saved per furnace, so it survives restarts and unloaded chunks.

The furnace only relights when it can actually work: input to process, fuel in the fuel slot and room in the output.

> **Why not a separate "Auto" button?** The furnace window is drawn by the game client (`ProcessingPanel.ui` + C# code),
> and server plugins can't add elements to it. AutoFurnace instead recognises Turn On / Turn Off by watching the
> furnace state while its window is open.

## Installation

- **Single player:** put the jar in `%APPDATA%\Hytale\UserData\Mods` (or install it from CurseForge) and enable it
  in your world's **Mods** settings (external mods are disabled by default in single player).
- **Dedicated server:** drop the jar into the server's `mods/` folder and restart.

## Building

Requirements: Windows, Hytale installed (the build compiles against its `HytaleServer.jar`) and a JDK 25 in
`.tools\jdk-25` (not committed). For example, download the Eclipse Temurin JDK 25 zip for Windows x64 from
[adoptium.net](https://adoptium.net/) and extract it so that `.tools\jdk-25\bin\javac.exe` exists.

```powershell
.\build.ps1                          # -> build\AutoFurnace-<version>.jar (against the release patchline)
.\build.ps1 -Install                 # also copies it to %APPDATA%\Hytale\UserData\Mods
.\build.ps1 -Patchline pre-release   # compile against the pre-release server
```

`start-testserver.ps1 [-Patchline pre-release]` starts a local dedicated server in `testserver/` with the mod
installed (connect to `localhost:5520`; on first start run `auth login device` in the server console).

## Code layout

- `AutoFurnacePlugin` – registers the component and the system
- `AutoFurnaceBlock` – persistent per-furnace auto-mode flag (chunk component)
- `AutoIgniteSystem` – detects Turn On / Turn Off (state changes while the window is open) and relights furnaces in auto mode

## License

[MIT](LICENSE)
