<div align="center">

<img src="./.github/assets/icon.png" alt="AnimetailAT" width="96"/>

# AnimetailAT

**Watch anime and read manga, manhwa, and manhua — including raw, untranslated pages.**

A fork of [Animetail](https://github.com/Animetailapp/Animetail), built on Mihon and Aniyomi.

[![License: Apache-2.0](https://img.shields.io/github/license/Gourab0002/AnimetailAT?labelColor=27303D&color=818cf8)](/LICENSE)

*Requires Android 8.0 or higher.*

</div>

## Page translation

AnimetailAT can translate text on comic pages so you can read Japanese, Korean, and Chinese sources in your language.

**Supported translators**

| Provider | Notes |
|---|---|
| xAI Grok | Your API key |
| OpenRouter | Your API key |
| Google Gemini | Your API key |
| OpenAI | Your API key |
| On-device (ML Kit) | No API key. Language models download on first use and require Google Play services. |

**How to use**

1. Open **Settings → Page translation**.
2. Choose a translator and, if required, add your API key.
3. Set the source language (or leave Auto) and the target language.
4. In the reader, tap the translate icon.

Translations are drawn over the original text. Source files are never modified. Completed pages are cached locally.

Source languages include Japanese, Korean, Simplified Chinese, and Traditional Chinese. On-device Chinese output is Simplified Chinese.

Cloud translators send the current page image to the provider you select. Some providers may refuse adult images; if a page is not translated, switch provider or use on-device.

## Features

**AnimetailAT**
- In-reader page translation for raw manga, manhwa, and manhua
- User-owned API keys; keys stay on the device
- On-device OCR and translation as an offline option

**Library and discovery**
- Home feed for movies, series, anime, and manga
- Continue watching and reading with saved progress
- Tracker scores from MyAnimeList, AniList, TMDB, and others
- Categories, tags, and advanced library search

**Reader**
- Multiple viewing modes and reading directions
- Webtoon / long-strip layout with a vertical chapter navigator
- Dual-page, crop, data saver, and local / downloaded reading

**Player**
- mpv-based player with audio, subtitle, and decoder options
- Thumbnail preview while seeking
- Torrent streaming (with compatible extensions)
- Cast support

**Tracking and extras**
- MyAnimeList, AniList, Kitsu, MangaUpdates, Shikimori, Bangumi, and Hikka
- Local backups
- Discord Rich Presence
- Light, dark, and custom themes

## Contributing

[Code of conduct](./CODE_OF_CONDUCT.md) · [Contributing guide](./CONTRIBUTING.md)

Pull requests are welcome. Please open an issue first for larger changes.

## Credits

AnimetailAT is based on [Animetail](https://github.com/Animetailapp/Animetail), which builds on Tachiyomi, Mihon, and Aniyomi.

<a href="https://github.com/Animetailapp/Animetail/graphs/contributors">
    <img src="https://contrib.rocks/image?repo=Animetailapp/Animetail" alt="Animetail contributors" width="800"/>
</a>

## Disclaimer

The developers of this application are not affiliated with any content providers. The application hosts no content.

## License

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
