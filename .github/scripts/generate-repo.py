#!/usr/bin/env python3
"""
Genera repo/index.json e index.min.json leyendo los metadatos reales
de cada extension (build.gradle) en vez de tenerlos escritos a mano.
Esto evita que el indice quede desactualizado si cambia el nombre,
la version o se agregan nuevas extensiones.
"""
import json
import re
import shutil
from pathlib import Path

root = Path(__file__).resolve().parents[2]
out = root / "repo"
out.mkdir(exist_ok=True)
(out / "apk").mkdir(exist_ok=True)
(out / "icon").mkdir(exist_ok=True)

NAME_RE = re.compile(r"extName\s*=\s*['\"]([^'\"]+)['\"]")
VERSION_CODE_RE = re.compile(r"extVersionCode\s*=\s*(\d+)")
NSFW_RE = re.compile(r"isNsfw\s*=\s*true", re.IGNORECASE)

entries = []

for gradle_file in sorted(root.glob("src/*/*/build.gradle*")):
    ext_dir = gradle_file.parent
    category = ext_dir.parent.name       # ej: "all"
    ext_id = ext_dir.name                # ej: "mangadexsafe"
    pkg = f"eu.kanade.tachiyomi.extension.{category}.{ext_id}"

    text = gradle_file.read_text(encoding="utf-8")
    name_match = NAME_RE.search(text)
    version_code_match = VERSION_CODE_RE.search(text)
    if not name_match or not version_code_match:
        print(f"AVISO: no se pudieron leer metadatos de {gradle_file}, se omite.")
        continue

    ext_name = name_match.group(1)
    version_code = int(version_code_match.group(1))
    version_name = f"16.{version_code}"
    is_nsfw = 1 if NSFW_RE.search(text) else 0

    apk_glob = list((ext_dir / "build" / "outputs" / "apk" / "release").glob("*.apk"))
    if not apk_glob:
        print(f"AVISO: no se encontro APK compilado para {ext_id}, se omite del indice.")
        continue

    apk_path = apk_glob[0]
    shutil.copy2(apk_path, out / "apk" / apk_path.name)

    entries.append({
        "name": ext_name,
        "pkg": pkg,
        "apk": apk_path.name,
        "lang": category,
        "code": version_code,
        "version": version_name,
        "nsfw": is_nsfw,
        "sources": [{
            "name": ext_name,
            "lang": category,
            "id": ext_id,
            "baseUrl": "",
        }],
    })

entries.sort(key=lambda e: e["pkg"])

with (out / "index.json").open("w", encoding="utf-8") as f:
    json.dump(entries, f, ensure_ascii=False, indent=2)

with (out / "index.min.json").open("w", encoding="utf-8") as f:
    json.dump(entries, f, ensure_ascii=False, separators=(",", ":"))

print(f"Se genero el indice del repositorio con {len(entries)} extension(es).")
