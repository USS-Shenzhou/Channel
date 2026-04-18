"""Patch LASTCHANGE files for shallow-cloned WebRTC builds."""
import os

repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
util_dir = os.path.join(repo_root, "_build", "src", "build", "util")
os.makedirs(util_dir, exist_ok=True)

with open(os.path.join(util_dir, "LASTCHANGE.committime"), "w", encoding="utf-8", newline="\n") as f:
    f.write("1700000000\n")

with open(os.path.join(util_dir, "LASTCHANGE"), "w", encoding="utf-8", newline="\n") as f:
    f.write("LASTCHANGE=0000000000000000000000000000000000000000\n")

print("Patched LASTCHANGE files.")
