[日本語](README_ja.md)

# Quick Menu (Renewed)

A Fabric mod for Minecraft that lets you register commands and keybindings to buttons and execute them quickly from a menu.

**A reconstruction of the original Fork by ImCodist, built without heavy dependencies like `owo-lib`.**

---

## Features

| Feature | Description |
|---------|-------------|
| **Folder navigation** | Organize buttons into folders |
| **Breadcrumb list** | Visual display of folder hierarchy |
| **Search (F key)** | Search button names and execute instantly |
| **Edit mode (E key)** | Add, edit, and delete buttons |
| **Quick reorder** | Ctrl+click in edit mode |
| **Quick delete** | Shift+click in edit mode |
| **Release to execute** | Execute action the moment you release the key |
| **Import / Export** | Share button layouts via clipboard (JSON + Base64) |

---

## Usage

1. **Open menu**: `G` (default)
2. **Edit mode**: `E` key or pencil icon
3. **Search**: `F` key or magnifying glass icon
4. **Navigate folders**: Click folder icon
5. **Go back**: Click `Root` in the breadcrumb list
6. **Export**: In edit mode, click `Export` to copy your button layout to the clipboard
7. **Import**: In edit mode, click `Import` to load a layout from the clipboard (mode selectable)

---

## Configuration

- Buttons per row
- Number of visible rows
- Close on key release
- Close after action execution
- Hide edit icon
- Default import mode (`Ask Each Time` / `Replace All` / `Merge by Name` / `Add Only`)

---

## Technical Specs

- **Dependencies**: Fabric Loader, Fabric API
- **Recommended**: YACL, ModMenu
- **Language**: Kotlin
- **Data storage**: JSON (Global)
- **License**: GPL-3.0

---

## Links

- [Modrinth](https://modrinth.com/mod/quick-menu-renewed)
- [Repository](https://github.com/tenkun0317/quick-menu)
- [Report Issues](https://github.com/tenkun0317/quick-menu/issues)
- [TODO](todo.md)
