#!/usr/bin/env python3
"""Patch AArch64/x86_64 ELF LOAD segment alignment to 16 KB for Android 15+."""

from __future__ import annotations

import struct
import sys
from pathlib import Path

PT_LOAD = 1
EI_CLASS = 4
ELFCLASS32 = 1
ELFCLASS64 = 2
EM_AARCH64 = 183
EM_ARM = 40
EM_X86_64 = 62
PAGE_ALIGN = 0x4000


def patch_elf64(data: bytearray, path: Path) -> int:
    e_phoff = struct.unpack_from("<Q", data, 0x20)[0]
    e_phentsize = struct.unpack_from("<H", data, 0x36)[0]
    e_phnum = struct.unpack_from("<H", data, 0x38)[0]
    patched = 0
    for i in range(e_phnum):
        off = e_phoff + i * e_phentsize
        p_type = struct.unpack_from("<I", data, off)[0]
        if p_type != PT_LOAD:
            continue
        align_off = off + 0x30
        current = struct.unpack_from("<Q", data, align_off)[0]
        if current < PAGE_ALIGN:
            struct.pack_into("<Q", data, align_off, PAGE_ALIGN)
            patched += 1
    return patched


def patch_elf32(data: bytearray, path: Path) -> int:
    e_phoff = struct.unpack_from("<I", data, 0x1C)[0]
    e_phentsize = struct.unpack_from("<H", data, 0x2A)[0]
    e_phnum = struct.unpack_from("<H", data, 0x2C)[0]
    patched = 0
    for i in range(e_phnum):
        off = e_phoff + i * e_phentsize
        p_type = struct.unpack_from("<I", data, off)[0]
        if p_type != PT_LOAD:
            continue
        align_off = off + 0x1C
        current = struct.unpack_from("<I", data, align_off)[0]
        if current < PAGE_ALIGN:
            struct.pack_into("<I", data, align_off, PAGE_ALIGN)
            patched += 1
    return patched


def patch_elf(path: Path) -> None:
    data = bytearray(path.read_bytes())
    if data[:4] != b"\x7fELF":
        raise ValueError(f"{path}: not an ELF file")

    elf_class = data[EI_CLASS]
    if elf_class == ELFCLASS64:
        patched = patch_elf64(data, path)
    elif elf_class == ELFCLASS32:
        patched = patch_elf32(data, path)
    else:
        raise ValueError(f"{path}: unsupported ELF class {elf_class}")

    if patched == 0:
        print(f"{path}: already 16 KB aligned")
        return

    path.write_bytes(data)
    print(f"{path}: patched {patched} LOAD segment(s) to 16 KB")


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(f"usage: {argv[0]} <lib.so> [more.so ...]", file=sys.stderr)
        return 1

    for arg in argv[1:]:
        patch_elf(Path(arg))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
