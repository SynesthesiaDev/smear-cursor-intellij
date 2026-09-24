# ✏️ Smear Cursor

**Animate the cursor with a smear effect in IntelliJ IDEA and other JetBrains IDEs.**

Inspired by [Neovide's animated cursor](https://neovide.dev/features.html#animated-cursor) and the [smear-cursor.nvim](https://github.com/sphamba/smear-cursor.nvim) Neovim plugin.

---

✨ This is an up to date and optimized fork of [smear-cursor-intellij](https://github.com/ertugrulcetin/smear-cursor-intellij), originally created by [ Ertuğrul Çetin ](https://github.com/ertugrulcetin)

---

## ⚡ Features

- 🧈 **Smooth cursor animation** 
  - with spring physics!
- 📝 **Configurable animation speed** 
    - adjust stiffness, damping, and trailing behavior
- 🌈 **Color gradient trail effect** 
  - beautiful fading trail following your cursor
- 🖥️ **Works across all editor windows**  
  - consistent experience everywhere

## 📥 Installation

### From JetBrains Marketplace

1. Open IntelliJ IDEA
2. Go to **Settings/Preferences → Plugins → Marketplace**
3. Search for "Smear Cursor"
4. Click **Install**
5. Restart the IDE

### Manual Installation

1. Download the latest release from the [Releases](https://github.com/SynesthesiaDev/smear-cursor-intellij/releases) page
2. Go to **Settings/Preferences → Plugins**
3. Click the gear icon → **Install Plugin from Disk...**
4. Select the downloaded `.zip` file
5. Restart the IDE

## 📝 Configuration

Go to **Settings/Preferences → Editor → Smear Cursor** to configure.

## ▶️ Quick Toggle

- Right-click in the editor and select **Toggle Smear Cursor**
- Or use the action: `Toggle Smear Cursor` (searchable via Ctrl+Shift+A / Cmd+Shift+A)

---

## Building from Source

### Requirements
- JDK 21+
- IntelliJ IDEA 2025.3.5+

### Build
```bash
./gradlew build
```

### Run in Development
```bash
./gradlew runIde
```

### Package Plugin
```bash
./gradlew buildPlugin
```

The plugin ZIP will be in `build/distributions/`.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. **AI contributions WILL be rejected**

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Credits

- Original concept: [Neovide](https://neovide.dev/)
- Neovim implementation: [smear-cursor.nvim](https://github.com/sphamba/smear-cursor.nvim) by sphamba
- Original IntelliJ IDEA port: [ Ertuğrul Çetin ](https://github.com/ertugrulcetin)
- Up to date & optimized fork: [SynesthesiaDev](https://github.com/SynesthesiaDev)
