"""Generate the KonteksCek launcher icon set without external assets.

Run: python design/generate_icon.py
Outputs 1024px previews plus Android mipmaps (legacy, adaptive foreground,
and monochrome themed icon).
"""

from __future__ import annotations

import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
DESIGN = ROOT / "design"
RES = ROOT / "app" / "src" / "main" / "res"

S = 2  # supersampling factor
W = 1024 * S
CX = W / 2
SYMBOL_SCALE = 0.92

INK = (4, 18, 28)
MINT = (52, 211, 153)
LAVENDER = (199, 210, 254)
INDIGO = (99, 102, 241)


def srgb(hex_value: str) -> tuple[int, int, int]:
    value = hex_value.lstrip("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))


def gradient_rgb(stops: list[tuple[float, tuple[int, int, int]]], angle: float = 45.0) -> Image.Image:
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32)
    direction = np.array([math.cos(math.radians(angle)), math.sin(math.radians(angle))], dtype=np.float32)
    projection = xx * direction[0] + yy * direction[1]
    projection -= projection.min()
    projection /= max(projection.max(), 1.0)
    out = np.zeros((W, W, 3), dtype=np.float32)
    for index in range(len(stops) - 1):
        start, color_a = stops[index]
        end, color_b = stops[index + 1]
        mask = (projection >= start) & (projection <= end)
        local = (projection[mask] - start) / max(end - start, 1e-6)
        color_a = np.array(color_a, dtype=np.float32)
        color_b = np.array(color_b, dtype=np.float32)
        out[mask] = color_a[None, :] * (1.0 - local[:, None]) + color_b[None, :] * local[:, None]
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGB")


def radial(size: int, cx: float, cy: float, sigma: float) -> np.ndarray:
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    return np.exp(-(((xx - cx) ** 2 + (yy - cy) ** 2) / (2.0 * sigma**2)))


def background() -> Image.Image:
    yy, _ = np.mgrid[0:W, 0:W].astype(np.float32)
    t = np.clip(yy / W, 0.0, 1.0)
    top = np.array(srgb("#0E2036"), dtype=np.float32)
    mid = np.array(srgb("#0A1728"), dtype=np.float32)
    bottom = np.array(srgb("#050C16"), dtype=np.float32)
    base = np.where(
        t[..., None] < 0.55,
        top + (mid - top) * (t[..., None] / 0.55),
        mid + (bottom - mid) * ((t[..., None] - 0.55) / 0.45),
    )

    teal_glow = radial(W, W * 0.28, W * 0.78, W * 0.32)[..., None] * np.array(srgb("#0E7490"), dtype=np.float32) * 0.38
    indigo_glow = radial(W, W * 0.82, W * 0.20, W * 0.28)[..., None] * np.array(srgb("#4F46E5"), dtype=np.float32) * 0.30
    image = base + teal_glow + indigo_glow

    vignette = 1.0 - 0.35 * radial(W, CX, CX, W * 0.72)
    image *= vignette[..., None]
    return Image.fromarray(np.clip(image, 0, 255).astype(np.uint8), "RGB")


def bubble_mask() -> Image.Image:
    mask = Image.new("L", (W, W), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle(
        [250 * S, 270 * S, 774 * S, 688 * S],
        radius=176 * S,
        fill=255,
    )
    # Rounded chat tail: a clean triangle whose base sits inside the bubble.
    draw.polygon(
        [(408 * S, 628 * S), (322 * S, 768 * S), (548 * S, 650 * S)],
        fill=255,
    )
    return mask


def check_mask() -> Image.Image:
    mask = Image.new("L", (W, W), 0)
    draw = ImageDraw.Draw(mask)
    draw.line(
        [(392 * S, 505 * S), (492 * S, 605 * S), (660 * S, 395 * S)],
        fill=255,
        width=68 * S,
        joint="curve",
    )
    for x, y in ((392, 505), (660, 395)):
        draw.ellipse([(x - 34) * S, (y - 34) * S, (x + 34) * S, (y + 34) * S], fill=255)
    return mask


def star(size: int, points: int = 4) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    center = size / 2
    outer = size * 0.48
    inner = size * 0.13
    coords = []
    for i in range(points * 2):
        radius = outer if i % 2 == 0 else inner
        angle = -math.pi / 2 + i * math.pi / points
        coords.append((center + radius * math.cos(angle), center + radius * math.sin(angle)))
    draw.polygon(coords, fill=255)
    return mask


def add_glow(layer: Image.Image, mask: Image.Image, color: tuple[int, int, int], blur: int, strength: float) -> None:
    alpha = mask.filter(ImageFilter.GaussianBlur(blur)).point(lambda value: int(value * strength))
    overlay = Image.new("RGBA", layer.size, (*color, 255))
    overlay.putalpha(alpha)
    layer.alpha_composite(overlay)


def build_symbol() -> Image.Image:
    layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    bubble = bubble_mask()

    shadow = Image.new("L", (W, W), 0)
    shadow.paste(bubble, (0, 20 * S))
    shadow = shadow.filter(ImageFilter.GaussianBlur(34 * S)).point(lambda value: int(value * 0.55))
    shadow_layer = Image.new("RGBA", (W, W), (*INK, 255))
    shadow_layer.putalpha(shadow)
    layer.alpha_composite(shadow_layer)

    bubble_rgb = gradient_rgb(
        [
            (0.0, srgb("#67E8F9")),
            (0.42, srgb("#22D3EE")),
            (0.74, srgb("#2DD4BF")),
            (1.0, srgb("#34D399")),
        ],
        angle=35,
    ).convert("RGBA")
    bubble_rgb.putalpha(bubble)
    layer.alpha_composite(bubble_rgb)

    rim = Image.new("L", (W, W), 0)
    ImageDraw.Draw(rim).rounded_rectangle(
        [260 * S, 280 * S, 764 * S, 678 * S],
        radius=166 * S,
        outline=255,
        width=4 * S,
    )
    rim = Image.composite(rim, Image.new("L", (W, W), 0), bubble)
    rim = rim.point(lambda value: int(value * 40 / 255))
    rim_layer = Image.new("RGBA", (W, W), (255, 255, 255, 255))
    rim_layer.putalpha(rim)
    layer.alpha_composite(rim_layer)

    check = check_mask()
    check_shadow = check.filter(ImageFilter.GaussianBlur(9 * S)).point(lambda value: int(value * 0.45))
    check_shadow_layer = Image.new("RGBA", (W, W), (*INK, 255))
    check_shadow_layer.putalpha(check_shadow)
    layer.alpha_composite(check_shadow_layer)
    check_layer = Image.new("RGBA", (W, W), (*INK, 255))
    check_layer.putalpha(check)
    layer.alpha_composite(check_layer)

    big = star(190 * S)
    big_mask = Image.new("L", (W, W), 0)
    big_mask.paste(big, (715 * S, 160 * S))
    add_glow(layer, big_mask, INDIGO, 40 * S, 0.55)
    big_layer = Image.new("RGBA", (W, W), (*LAVENDER, 255))
    big_layer.putalpha(big_mask)
    layer.alpha_composite(big_layer)

    small = star(66 * S)
    small_mask = Image.new("L", (W, W), 0)
    small_mask.paste(small, (826 * S, 302 * S))
    small_layer = Image.new("RGBA", (W, W), (255, 255, 255, 235))
    small_layer.putalpha(small_mask)
    layer.alpha_composite(small_layer)

    return layer


def scale_symbol(symbol: Image.Image) -> Image.Image:
    size = int(W * SYMBOL_SCALE)
    scaled = symbol.resize((size, size), Image.LANCZOS)
    canvas = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    canvas.alpha_composite(scaled, ((W - size) // 2, (W - size) // 2))
    return canvas


def build_master() -> Image.Image:
    base = background().convert("RGBA")
    base.alpha_composite(scale_symbol(build_symbol()))
    return base.resize((1024, 1024), Image.LANCZOS)


def rounded_mask(size: int, ratio: float = 0.225) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * ratio), fill=255)
    return mask


def circle_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    return mask


def build_legacy(master: Image.Image, round_icon: bool) -> Image.Image:
    output = Image.new("RGBA", master.size, (0, 0, 0, 0))
    output.paste(master, (0, 0))
    mask = circle_mask(master.width) if round_icon else rounded_mask(master.width)
    output.putalpha(mask)
    return output


def build_monochrome() -> Image.Image:
    # Solid speech bubble with a check-shaped cutout + sparkles.
    mask = bubble_mask()
    check = check_mask()
    mask = Image.composite(Image.new("L", (W, W), 0), mask, check)

    big = star(190 * S)
    mask.paste(255, (715 * S, 160 * S), big)
    small = star(66 * S)
    mask.paste(255, (826 * S, 302 * S), small)

    full = mask.convert("RGBA")
    scaled = full.resize((int(W * SYMBOL_SCALE), int(W * SYMBOL_SCALE)), Image.LANCZOS)
    canvas = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    canvas.alpha_composite(scaled, ((W - scaled.width) // 2, (W - scaled.height) // 2))
    return canvas.resize((1024, 1024), Image.LANCZOS)


def save_resized(image: Image.Image, path: Path, size: int, fmt: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    resized = image.resize((size, size), Image.LANCZOS)
    if fmt == "webp":
        resized.save(path, "WEBP", quality=95, method=6)
    else:
        resized.save(path, "PNG", optimize=True)


def main() -> None:
    DESIGN.mkdir(parents=True, exist_ok=True)
    master = build_master()
    legacy = build_legacy(master, round_icon=False)
    round_icon = build_legacy(master, round_icon=True)
    monochrome = build_monochrome()

    master.save(DESIGN / "logo-preview.png")
    legacy.save(DESIGN / "logo-legacy.png")
    round_icon.save(DESIGN / "logo-round.png")
    monochrome.save(DESIGN / "logo-monochrome.png")

    legacy_sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    foreground_sizes = {
        "mipmap-mdpi": 108,
        "mipmap-hdpi": 162,
        "mipmap-xhdpi": 216,
        "mipmap-xxhdpi": 324,
        "mipmap-xxxhdpi": 432,
    }

    for folder, size in legacy_sizes.items():
        save_resized(legacy, RES / folder / "ic_launcher.webp", size, "webp")
        save_resized(round_icon, RES / folder / "ic_launcher_round.webp", size, "webp")
    for folder, size in foreground_sizes.items():
        save_resized(master, RES / folder / "ic_launcher_foreground.png", size, "png")
        save_resized(monochrome, RES / folder / "ic_launcher_monochrome.png", size, "png")

    print("Generated icon assets.")
    for path in sorted(DESIGN.glob("logo-*.png")):
        print(" ", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
