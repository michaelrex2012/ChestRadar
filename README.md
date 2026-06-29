Organizing your chests can be very time-consuming, especially in the late-game. This mod fixes that. It's just as simple as pressing 'Y'.
***
![Holding Netherite Ingot, then switching to a Netherite Block while Chest Radar is showing outlines and text](https://cdn.modrinth.com/data/cached_images/d8f8ec6be6da94f533ca3b02eb3acff51d33bd2d.gif)

## Features
* **Outlines:** By holding an item and activating the keybind, all nearby chests with your held item will be outlined.
* **Text:** Text with the number of items will also be rendered.
* **Color:** The color of text and the outline will change depending on the amount of items.
* **Full Support:** Works on all container types in Vanilla (e.g., Chests, Barrels, Shulker Boxes, and Crafters)
* **Customization:** Extensive settings for customization.

## Installation
Same as installing any other fabric mod.  
**Required Dependencies:**

* [Cloth Config API](https://modrinth.com/mod/cloth-config)
* [Fabric API](https://modrinth.com/mod/fabric-api)
* [Mod Menu](https://modrinth.com/mod/modmenu) (Not required, but highly recommended for setting access)

Multiplayer Note: If you want to play on a server or LAN with Chest Radar, it must be first installed on the server and client. If it is not installed on both the mods functionality will be disabled.

## Performance
Chest Radar is lightweight. When searching for chests the mod fetches the location of any container blocks from a Chunk Registry. This method is much faster and less straining than checking every block in the search radius to see if a block is a container. Additionally, the search radius can be adjusted for better performance. There is a 10-tick delay between each search to prevent overloading and spam.

## Compatibility
Chest Radar is almost always able to search for items added in other mods. Though as far as I know, Chest Radar cannot search in container blocks added by other mods (e.g., Tom's Simple Storage and Storage Drawers).

## FAQ
### **Does this mod have to be installed on servers?**
Yes, if you want Chest Radar to be enabled.  
### **How much lag is there?**
Almost none. There is no noticeable frame drop, freeze, or lag spike normally. It requires dozens of players actively running a search at the same time for there to be any lag.  
### **Can I include this in modpacks?**
Absolutely! But only if they are published on Modrinth or Curseforge.  
