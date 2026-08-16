<div align="center">

<a href="https://github.com/Gourab0002/AnimetailAT">
    <img src="./.github/assets/icon.png" alt="animetail logo" title="animetail logo" width="80"/>
</a>

# AnimetailAT

### Animetail with AI page translation for raw manga, manhwa, and manhua.
Fork of [Animetail](https://github.com/Animetailapp/Animetail) (Mihon / Aniyomi). Watch anime and read comics, including untranslated pages.

[![CI](https://img.shields.io/github/actions/workflow/status/Gourab0002/AnimetailAT/build_push.yml?branch=main&label=CI&labelColor=27303D)](https://github.com/Gourab0002/AnimetailAT/actions/workflows/build_push.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/Gourab0002/AnimetailAT?labelColor=27303D&color=818cf8)](/LICENSE)

## Download

Every push to `main` builds an installable debug APK.

1. Open the latest [CI run](https://github.com/Gourab0002/AnimetailAT/actions/workflows/build_push.yml).
2. Download the **animetail-debug-…** artifact.
3. Install `app-universal-debug.apk` on your phone.

The debug build uses package id `com.dark.animetailv2.dev`, so it can sit next to an official Animetail install.

*Requires Android 8.0 or higher.*

## Page translation

Read raw Japanese, Korean, and Chinese pages in the reader.

1. Open **Settings → Page translation**.
2. Choose a translator:
   * **xAI Grok**, **OpenRouter**, **Google Gemini**, or **OpenAI** — paste your own API key.
   * **On-device (ML Kit)** — no key. Uses Google Play services; first use downloads language models.
3. Set **Translate from** (Auto, Japanese, Korean, Simplified Chinese, Traditional Chinese) and **Translate to**.
4. In the reader, tap the translate icon on the bottom bar.

Translated text is drawn over speech bubbles. Original chapter files are not overwritten. Results are cached on the device.

**Notes**
* Cloud providers send the current page image to the service you choose. Some refuse adult images; if a page stays raw, try xAI or On-device.
* On-device Chinese OCR reads simplified and traditional text; on-device output is Simplified Chinese.
* This is a first version: boxes over text, not a full typeset scanlation. Failures currently fall back to the original page.

## Features

<div align="left">

Features include:
* AnimetailAT:
    * AI / on-device page translation for raw manga, manhwa, manhua, and similar image comics
    * User-supplied API keys (xAI, OpenRouter, Gemini, OpenAI)
    * GitHub Actions APK builds on every push
* Animetail:
    * Multimedia Home Feed with Movies, Series, Anime, and Manga recommendations
    * Hero Media Carousel with auto-scrolling (4s), indicator dots, and TMDB/AniList trends integration
    * Direct launch from "Continue watching & reading" cards with exact progress formatting
    * Real tracking scores (MAL/AniList/TMDB) and rating badges on media items
    * Support for Cast functionality
    * Support themes monet
    * Android tv compatibility (only banner)
    * Optimized Discord Rich Presence for Manga and Anime, no external API.

* Kuukiyomi:
    * Torrent support(Needs right extensions) (@Diegopyl1209)
    * Custom Theme support
    * resmush.it(Data Saver Provider)
    * Group by tags in library
    * Discord Rich Presence for Manga
* Aniyomi:
    * Watching videos
    * View images
    * Torrent streaming support
    * Support for thumbnail preview when seeking in player
    * Embedded HTTP server for extensions
    * Local reading/watching of downloaded content
    * A configurable reader with multiple viewers, reading directions and other settings.
    * A configurable player built on mpv-android with multiple options and settings
    * Tracker support: [MyAnimeList](https://myanimelist.net/), [AniList](https://anilist.co/), [Kitsu](https://kitsu.app/), [MangaUpdates](https://mangaupdates.com), [Shikimori](https://shikimori.one), [Bangumi](https://bgm.tv/), and [Hikka](https://hikka.io/)
    * Categories to organize your library
    * Light and dark themes
    * Create backups locally to read/watch offline or to your desired cloud service
* Mihon:
    * Advanced library search supporting logical/comparison operators, field-specific prefixes, and nested expressions
    * Vertical chapter navigator for long strip mode with customizable height and reader settings
    * Resumable image downloads
    * Split extension lists support with `index.pb` format
* Other fork features:
    * TachiyomiSY:
        * Data Saver
        * Edit Info
        * Library Grouping
        * Double Pages
    * Animiru:
        * Discord Rich Presence
    * TachiyomiJ2K:
        * Page Preload
    * Komikku:
        * Repository visibility toggle, icon support, and name display

</div>

## Contributing

[Code of conduct](./CODE_OF_CONDUCT.md) · [Contributing guide](./CONTRIBUTING.md)

Pull requests are welcome. For page translation changes, open an issue on [this repo](https://github.com/Gourab0002/AnimetailAT/issues).

Upstream Animetail docs: [FAQ](https://aniyomi.org/docs/faq/general) · [changelog](https://aniyomi.org/changelogs/) · [issues](https://github.com/Animetailapp/animetail/issues)

### Repositories

[![aniyomiorg/aniyomi-website - GitHub](https://github-readme-stats.vercel.app/api/pin/?username=aniyomiorg&repo=aniyomi-website&bg_color=161B22&text_color=c9d1d9&title_color=818cf8&icon_color=818cf8&border_radius=8&hide_border=true&description_lines_count=2)](https://github.com/aniyomiorg/aniyomi-website/)
[![aniyomiorg/aniyomi-mpv-lib - GitHub](https://github-readme-stats.vercel.app/api/pin/?username=aniyomiorg&repo=aniyomi-mpv-lib&bg_color=161B22&text_color=c9d1d9&title_color=818cf8&icon_color=818cf8&border_radius=8&hide_border=true&description_lines_count=2)](https://github.com/aniyomiorg/aniyomi-mpv-lib/)

### Credits

Thank you to everyone who contributed to Tachiyomi, Mihon, Aniyomi, and [Animetail](https://github.com/Animetailapp/Animetail).

<a href="https://github.com/Animetailapp/Animetail/graphs/contributors">
    <img src="https://contrib.rocks/image?repo=Animetailapp/Animetail" alt="Animetail app contributors" title="Animetail app contributors" width="800"/>
</a>

### Disclaimer

The developer(s) of this application does not have any affiliation with the content providers available, and this application hosts zero content.

### License

<pre>
Copyright © 2015 Javier Tomás
Copyright © 2024 Mihon Open Source Project
Copyright © 2024 Aniyomi Open Source Project
Copyright © 2024 The Animetail Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>

</div>
