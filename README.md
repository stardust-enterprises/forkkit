# Forkkit!

###### Forge + Bukkit! Forkkit!

## What the hell is this?!

> :warning: **Note**: This is a library/technical mod, not a "content"-mod. This is meerly a tool for modders and server owners to extend their server's functionality.

Forkkit is a "Forge mod" *(technically a [ModLauncher](https://github.com/McModLauncher/modlauncher/tree/main-8.1.x) service)* that hooks into Bukkit's classloading mechanisms to allow:
- other Forge mods to access Bukkit plugins' classes.
- ModLauncher transformation services to transform Bukkit plugins' classes.
  - For now, this doesn't make [Mixin](https://github.com/SpongePowered/Mixin) work. It's definitely possible with some *smart code*, but I'll leave that to someone else.
 
## How do I use this?????

Simply drop the Forkkit jar into your server's `mods` folder, and you're good to go!  
Now you can depend on plugin classes when developing your mods :)

## Compatibility

Forkkit has been developped for ModLauncher 8.1.x while targetting [Mohist](https://github.com/MohistMC/Mohist) 1.16.5, and is not guaranteed to work on any other version, but you're welcome to try it!

ModLauncher 9.x *(basically 1.17+)* is not supported and probably won't ever be since it's vastly different from 8.x and would *probably* require major code changes.

## License

Forkkit is licensed under the [BSD Zero Clause license](./LICENSE), go wild!
