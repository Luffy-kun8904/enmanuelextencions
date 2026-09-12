#!/usr/bin/env python3
import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
out = root / "repo"
out.mkdir(exist_ok=True)
(out / "apk").mkdir(exist_ok=True)
(out / "icon").mkdir(exist_ok=True)

apks = sorted((root / "src").glob("**/build/outputs/apk/release/*.apk"))
entries = []
for apk in apks:
    import shutil
    shutil.copy2(apk, out / "apk" / apk.name)
    entries.append({
        "name": "Aniyomi Manga Extension",
        "pkg": "eu.kanade.tachiyomi.extension.all.mangadexsafe",
        "apk": apk.name,
        "lang": "all",
        "code": 1,
        "version": "16.1",
        "nsfw": 0,
        "sources": [{
            "name": "MangaDex Safe",
            "lang": "all",
            "id": "mangadexsafe",
            "baseUrl": "https://mangadex.org"
        }]
    })

with (out / "index.json").open("w", encoding="utf-8") as f:
    json.dump(entries, f, ensure_ascii=False, indent=2)

with (out / "index.min.json").open("w", encoding="utf-8") as f:
    json.dump(entries, f, ensure_ascii=False, separators=(",", ":"))

print(f"Generated repository metadata for {len(entries)} APK(s).")
