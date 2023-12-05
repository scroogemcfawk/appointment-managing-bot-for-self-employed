## Appointment Management Telegram Bot


### About:
This bot is made for self employed people who provide
their service in person. It uses simple
contractor-client role model. At first, a contractor
creates a list of appointments (date and time records)
that are suitable for him/her (fit in his/her schedule).
Then, a client can make an appointment with the contractor
choosing the date-time record out of a list made by the
contractor. The only thing here that needed to be made
by contractor is to create these records.

For example, the contractor can be a manicurist, beauty master, tutor
or any other person who provides their service in
person and needs automatic appointment management.


### Config:
There are 2 important files that are used to configure the bot.
1. ```config.json``` - main config file, where you can set your bot token,
developer and contractor ids, path to locale etc.
2. ```locale.json``` - all interface mappings that are used during communication 
with bot users, including date format, commands, prompts and messages.

The default location for config files is ```src/main/kotlin/dev/scroogemcfawk/manicurebot/config```,
where you can also find ```example_config.json``` and ```example_locale.json``` - these
files show the basic structure, but are not complete.

In order to see full list of options, see ```Config.kt``` and ```Locale.kt```.

The minimum required fields are ```config.token``` and ```config.locale```, locale file must exist, but can be empty, 
in that case default values will be used.


### How to run:
Put ```config.json``` and ```locale.json``` into ```src/main/kotlin/dev/scroogemcfawk/manicurebot/config``` folder.
Both files have to have ```Config.kt``` and ```Locale.kt``` constructor parameter
names accordingly. If you don't know your dev and contractor chat id,
start the bot and use ```/_id``` command, id of your chat will be logged at
INFO level. If you leave ```locale.json``` empty, default english locale will
be used. Still, you have to specify ```config.json``` path as program argument.

When everything is ready, run main function with single argument 
(absolute path to ```config.json```) in ```src/main/kotlin/dev/scroogemcfawk/manicurebot/App.kt```

When you got your ids, set them in ```config.json``` and restart the bot.

### Note:
The code might have a lot of bugs and is structured
as is, because the person who wrote it is a
fucking brain-damaged retard (me), so, please,
keep it in mind when you use it.


### Terms Of Use:
You can basically do whatever you want with
this code, but keep sources open.
See ```LICENCE``` for more information.
