# Changelog
Date format: (YYYY-MM-DD)  

## Next release
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
* 

## v2.0 Release (2018-05-29)
### Supported MC versions: 1.12, 1.11, 1.10, 1.9, 1.8
This update is not backwards compatible. If you have run a previous version of Billboards in the past, please remove the old Billboards plugin folder before running this versions.
* UUID update: All players are now identifier by uuids instead of names. Make sure that your economy plugin supports Vault's OfflinePlayer (UUID) API.
* Some messages have slightly changed and new ones have been added for unknown names, unknown uuid and server owner name. Message parameters for creator and owner uuids have been added.
* Signs data is now stored in a separate file (billboards.yml). Old data does not get imported!
* The config, billboards storage and message format have changed! Please remove any old data before updating!
* Fixed support for Bukkit 1.12.1 and above.
* Fix: Players can only break signs they are the creator of if the sign isn't currently owned by some other player.
* Added 'billboards' command alias.
* Major internal refactoring and cleanup.

## v1.6 Release (2015-07-26)
### Supported MC versions: 1.8, 1.7.9
* Changed: Moved SignChangeEvent handler to priority HIGH to give other plugins a better chance to modify the sign content before Billboards applies it to the Billboard sign.
* Changed: Billboards is by default no longer ignoring other plugins preventing sign changes (like anti-swearing plugins).
* Added: Setting 'BypassSignChangeBlocking' (default false), to forcefully bypass other plugins which cancel the SignChangeEvent.

## v1.5 Release (2014-12-14)
### Supported MC versions: 1.8, 1.7.9
* Fixed: -1 not being handled as 'infinite' for the max-rent setting.
* Updated a few default messages: the 'RENT_SIGN_LINE_2' text did show the price instead of the owner name and the messages containing instruction on how to edit the signs were updated because in MC 1.8 it is required to additionally sneak in order to place a sign against another sign.

Storage is still playername-based.

## v1.4 Release (2014-02-09)
### Supported MC versions: 1.7.4, 1.6.4, 1.5.2, 1.4.7
*Added: maxRentPerPlayer setting, which limits the amount of signs a player can rent. Also added a new message which prints that limit to the player when he reaches it.

## v1.3 Release (2013-10-08)
### Supported MC versions: 1.7.4, 1.6.4, 1.5.2, 1.4.7
* Added: different sign text after the sign was rented
* Changed: a lot of default messages and colors. I suggest to remove the messages.yml file to let it regenerate, if you are upgrading! 
* Added (experimental!): player signs (very experimental state!)
  * Added permission node: 'billboards.create' which allows creating of billboard signs (default: false)
  * Signs can now have a "creator", who gets the money when his sign is rent by someone. If the deposit fails, it will try to refund the withdraw to the person which tries to rent the sign.
  * Admins do by default create signs for the server instead of themselves, like the players without the admin permission do
  * The command has a new optional argument: /billboard [<price> <duration>] [creator] <- this is for admins to create player signs for themselves or others

The player signs are EXPERIMENTAL: The plugins has no sign protection built-in yet (signs can be broken, if they are not in some sort of protected region (this can also be abused by players selling signs in their land)) and players (with the billboards.create permission) can currently transform ANY sign EVERYWHERE to a billboard sign.

These disadvantages make the player signs currently impractical.

## v1.2 Release (2013-08-20)
### Supported MC versions: 1.6.2, 1.5.2, 1.4.7
* Fixed more mistakes in the default messages.
* Changed: early cancelling of the BlockPlaceEvents in order to prevent missleading messages of Block Protection plugins like WorldGuard, if they ignore cancelled events.

## v1.1 Release (2013-08-18)
### Supported MC versions: 1.6.2, 1.5.2, 1.4.7
* Fixing missing translation for the date format.
* Moved formats into the messages file.
* Internal change: renamed adsign everywhere to billboard.
* The billboard command now optionally accepts two arguments for the price and the duration of an individual billboard sign.

## v1.0 Release (2013-08-09)
### Supported MC versions: 1.6.2, 1.5.2, 1.4.7
* Initial release.
