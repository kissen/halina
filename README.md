halina
======

`halina` is an English dictionary for Android 8.1 and up. All records are stored
offline on the phone. `halina` is my favorite companion when reading books in
the park

Features
--------

* `halina` uses the [Wiktionary][1] dictionary ripped with [wikidictools][2].
* Efficient indexing of the dictionary results in quick look up times. On my
  trashy old phone it is rare that lookup takes more than a second.
* No ads, no trackers, all offline.

Screenshots
-----------

<p float="left">
  <img src="/doc/search.png" width="20%" />
  <img src="/doc/word.png" width="20%" />
</p>


Current State
-------------

I have been using `halina` for a month and it has helped me a lot when reading
old English classics. That said, `halina` is not exactly ready for shipping.

* Currently the dictionary is embedded into the APK. That results in a huge binary.
  It would be better to download a recent rip on first run.
* There is no logo.
* There is no public release yet. It would be nice if we could publish `halina`
  on [F-Droid][5], but right now it is too early.

Because `halina` already works perfectly for me, development is slow.

Credit
------

* `halina` is free software: you can redistribute it and/or modify it under the
  terms of the [GNU General Public License][4] as published by the Free Software
  Foundation, either version 3 of the License, or (at your option) any later
  version.

* This repository contains an unofficial rip of the Wiktionary in file
  `/app/src/main/assets/wiki.sqlite3`. `wiki.sqlite3` is a derivative work of
  Wiktionary and licensed according to the [GNU Free Documentation License][3].

[1]: https://www.wiktionary.org/
[2]: https://github.com/kissen/wikidictools
[3]: https://en.wiktionary.org/wiki/Wiktionary:Text_of_the_GNU_Free_Documentation_License
[4]: https://www.gnu.org/licenses/gpl-3.0.en.html
[5]: https://www.f-droid.org/
