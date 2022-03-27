# github-package-deleter

It's not easy to delete packages from the package registry of github.
It is also not easy to see what packages there are and what files they contain.
I gobbled together a little java tool to make it easier for me (and you).

I use Macs so I only make a Mac-app by default, but since it is swing-java app you can run it on any platform using the jar.

## How to use
- download the Mac app or start it from the jar file.
- you need to enter a _User_ or _Organization_ to see the packages from.
- you need a token with package authorisation (and package delete authorization if you want to delete packages)
- the user/organization is stored in your local prefs
- the token is only locally stored if you check the _Store_ checkbox (the token is not stored in plain text but can be reverse engeneered, so be careful)
- if you entered the correct data the list of packages will be shown
- you can right-click a version or a complete package and delete it from the popup menu
- you can selected multiple versions and packages to delete them in one go
- the Github GraphQL API will only allow a certain nember of calls per hour, the remaining number of calls is shown at the bottom right of the application window.

## Caution
- use at your own risk, I did my best but there might be bugs. I have used this app myself though ;-)
- your token can be reverse engeneered from the java-prefs
- deleted packages/versions are gone for eternity!

