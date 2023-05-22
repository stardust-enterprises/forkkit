# Fukkit!

###### Forge + Bukkit! Fukkit!

## What the hell is this?!

Fukkit is a "Forge mod" *(technically a [ModLauncher](https://github.com/McModLauncher/modlauncher/tree/main-8.1.x) service)* that hooks into Bukkit's classloading mechanisms to allow:
- other Forge mods to access Bukkit plugins' classes.
- ModLauncher transformation services to transform Bukkit plugins' classes.
    - For now, this doesn't make [Mixin](https://github.com/SpongePowered/Mixin) work, but it's definitely possible in the future, tho not in my roadmap.
 
## How do I use this?????

Simply drop the Fukkit jar into your server's `mods` folder, and you're good to go!

## Compatability

Fukkit has been developped for ModLauncher 8.1.x targetting [Mohist](https://github.com/MohistMC/Mohist) 1.16.5, and is not guaranteed to work on any other version, but you're welcome to try it.

ModLauncher 9.x (basically 1.17+) is not supported and probably won't ever be since it's vastly different from 8.x and would *probably* require major code changes.

## License

Fukkit is licensed under the [CC0 1.0 Universal license](./LICENSE), go wild!