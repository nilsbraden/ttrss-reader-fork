# TTRSS-Reader
TTRSS-Reader (or ttrss-reader-fork) is a fork of the original Project from [Tiny Tiny RSS Android client](https://github.com/tt-rss/tt-rss-android). I took the sourcecode and continued the development because the original owner didn't reply to any messages back then (and has not done so as of today).

It is a client application for the RSS-Reader [Tiny Tiny RSS](http://tt-rss.org/), a PHP-based online feedreader which runs on your own webspace. It is accessible with any browser and even supports keyboard shortcuts through javascript.
Download

Since google announced that the downloads from google-code will not be available after January 14th, 2014 I started to move all downloads to google-drive. The commentary with the current changelog will be removed in the process but the full changelog can be viewed in the Wiki.

[<img src="https://github.com/machiav3lli/oandbackupx/blob/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png"
    alt="Get it on GitHub"
    height="80">](https://github.com/nilsbraden/ttrss-reader-fork/releases)
[<img src="https://www.nilsbraden.de/android/tt-rss/google-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=org.ttrssreader)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/org.ttrssreader/)
[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/refs/heads/main/assets/graphics/badge_obtainium.png"
    alt="Get it with Obtanium"
    height="80">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22org.ttrssreader%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fnilsbraden%2Fttrss-reader-fork%22%2C%22author%22%3A%22nilsbraden%22%2C%22name%22%3A%22TTRSS-Reader%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22includeTarballs%5C%22%3Afalse%2C%5C%22tarballedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3A%22GitHub%22%7D)

 * ([**Changelog**](https://github.com/nilsbraden/ttrss-reader-fork/wiki/Changelog))

# Features
 * Use Swipe for Next/Previous Article
 * Swipe-Area in bottom area of the Article-View to avoid swiping while scrolling
 * Show or download images/videos/music attached to Articles
 * Share URL via Twitter/SMS/Mail
 * Mark everything (whole category or all categories) as read
 * Navigate through articles with volume-buttons
 * Read Labels
 * Cache articles in local-database
 * Cache images for offline browsing
 * SSL-Supoprt with self-signed certificates
 * Scheduled updates via Tasker/Locale
 * Fast JSON-Parsing with stream-based parser GSON 

See [Changelog](https://github.com/nilsbraden/ttrss-reader-fork/wiki/Changelog) for detailed information.

# Note regarding support and F-Droid
Since [F-Droid](https://f-droid.org/repository/browse/?fdid=org.ttrssreader) is often several months late with the build I have to make this clear: I won't provide support for versions which are not exactly or at least near to the version which is stated above on this site ("Current Version"). Please provide the full version information from the About-box within the app when reporting an issue!

# Changelog
Please follow this [link](https://github.com/nilsbraden/ttrss-reader-fork/wiki/Changelog)

# Contact
Please feel free to contact me via E-Mail (ttrss /at/ nilsbraden.de) or via the Bugtracker (Issues). If you have questions specifically about Tiny Tiny RSS you may ask me or visit the [TT-RSS Discussion page](https://github.com/tt-rss/tt-rss/discussions). If I don't answer fast enough or am not willing to implement your suggestion please keep in mind that this is only a hobby for me and I'm unwilling to implement every little feature just beacuse you suggested I do so. You may implement it and send me pull-requests so I can include the features but I can't guarantee I will do this for every thing you want to have.

# Donations
If you would like to support my work and the further developement of TTRSS-reader you may submit a donation via Paypal here:
 * Paypal: [![paypal.com][1]][2]<br><br>

For more information you may want to visit [Donations](https://github.com/nilsbraden/ttrss-reader-fork/wiki/Donations).

# Screenshots
![Screenshot 1](http://nilsbraden.de/android/screenshots/small/device-2014-01-02-163718_1.png)
![Screenshot 2](http://nilsbraden.de/android/screenshots/small/device-2014-01-02-163709_1.png)
![Screenshot 3](http://nilsbraden.de/android/screenshots/small/device-2014-01-02-163755_1.png)
![Screenshot 4](http://nilsbraden.de/android/screenshots/small/device-2014-01-02-163740_1.png)
![Screenshot 5](http://nilsbraden.de/android/screenshots/small/Screenshot_2014-01-02-16-34-03_1.png)
![Screenshot 6](http://nilsbraden.de/android/screenshots/small/Screenshot_2014-01-02-16-34-16_1.png)
![Screenshot 7](http://nilsbraden.de/android/screenshots/small/Screenshot_2014-01-02-16-34-39_1.png)
![Screenshot 8](http://nilsbraden.de/android/screenshots/small/Screenshot_2014-01-02-16-35-11_1.png)
![Screenshot 9](http://nilsbraden.de/android/screenshots/small/Screenshot_2014-01-02-16-35-30_1.png)

[1]: https://nilsbraden.de/android/tt-rss/btn_donateCC_LG.gif
[2]: https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=SD4AN4APNW6B4
[3]: https://www.nilsbraden.de/android/tt-rss/google-play-badge.png
[4]: https://play.google.com/store/apps/details?id=org.ttrssreader
[5]: https://www.nilsbraden.de/android/tt-rss/qrcode.png
