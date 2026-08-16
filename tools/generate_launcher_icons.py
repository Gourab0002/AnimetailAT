from pathlib import Path

from PIL import Image, ImageEnhance, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parents[1]
SRC = Path(__file__).resolve().parent / "icon-source.jpg"
BG = (238, 220, 196, 255)

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}


def resize(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.Resampling.LANCZOS)


def make_foreground(src: Image.Image, canvas: int, scale: float = 0.84) -> Image.Image:
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    inner = max(1, int(canvas * scale))
    art = resize(src, inner)
    x = (canvas - inner) // 2
    y = (canvas - inner) // 2
    out.paste(art, (x, y), art)
    return out


def make_background(canvas: int) -> Image.Image:
    return Image.new("RGBA", (canvas, canvas), BG)


def make_monochrome(src: Image.Image, canvas: int, scale: float = 0.84) -> Image.Image:
    gray = ImageOps.grayscale(src)
    gray = ImageEnhance.Contrast(gray).enhance(2.2)
    gray = ImageOps.autocontrast(gray, cutoff=6)
    mask = gray.point(lambda p: 255 if p > 118 else 0)
    mask = mask.filter(ImageFilter.MedianFilter(size=3))
    white = Image.new("RGBA", src.size, (255, 255, 255, 0))
    white.putalpha(mask)
    return make_foreground(white, canvas, scale)


def save(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, format="PNG", optimize=True)


def main() -> None:
    src = Image.open(SRC).convert("RGBA")
    main_res = ROOT / "app" / "src" / "main" / "res"
    debug_res = ROOT / "app" / "src" / "debug" / "res"

    for name, factor in DENSITIES.items():
        legacy = int(48 * factor)
        layer = int(108 * factor)
        full = resize(src, legacy)
        fg = make_foreground(src, layer)
        bg = make_background(layer)
        mono = make_monochrome(src, layer)

        save(full, main_res / f"mipmap-{name}" / "ic_logo.png")
        save(bg, main_res / f"mipmap-{name}" / "ic_logo_background.png")
        save(fg, main_res / f"mipmap-{name}" / "ic_logo_foreground.png")
        save(mono, main_res / f"mipmap-{name}" / "ic_logo_monochrome.png")

        save(full, debug_res / f"mipmap-{name}" / "ic_launcher.png")
        save(bg, debug_res / f"mipmap-{name}" / "ic_launcher_background.png")
        save(fg, debug_res / f"mipmap-{name}" / "ic_launcher_foreground.png")
        save(mono, debug_res / f"mipmap-{name}" / "ic_logo_monochrome.png")

    playstore = resize(src, 512)
    save(playstore, ROOT / "app" / "src" / "main" / "ic_launcher-playstore.png")
    save(resize(src, 256), ROOT / ".github" / "assets" / "icon.png")

    banner_w, banner_h = 320, 180
    banner = Image.new("RGBA", (banner_w, banner_h), BG)
    scaled = src.resize((banner_h, banner_h), Image.Resampling.LANCZOS)
    banner.paste(scaled, ((banner_w - banner_h) // 2, 0), scaled)
    save(banner, main_res / "mipmap-xhdpi" / "ic_banner.png")
    save(banner, main_res / "mipmap-xhdpi" / "ic_banner_foreground.png")

    print("Generated launcher icons")


if __name__ == "__main__":
    main()
