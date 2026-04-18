"""Write GN args file to avoid shell quoting issues across platforms."""
import os
import sys

target_os = sys.argv[1]
target_cpu = sys.argv[2]

args_dir = os.path.join("_build", "src", "out", "Release")
os.makedirs(args_dir, exist_ok=True)

args = f"""\
target_os = "{target_os}"
target_cpu = "{target_cpu}"
is_debug = false
is_component_build = false
rtc_include_tests = false
rtc_build_examples = false
rtc_build_tools = false
rtc_enable_protobuf = false
rtc_use_x11 = false
rtc_include_ilbc = false
rtc_include_opus = false
rtc_include_internal_audio_device = false
clang_use_chrome_plugins = false
symbol_level = 0
use_thin_lto = true
"""

# macOS: disable C++ modules to avoid missing .modulemap files in SDK
if target_os == "mac":
    args += "use_libcxx_modules = false\n"

args_path = os.path.join(args_dir, "args.gn")
with open(args_path, "w", encoding="utf-8") as f:
    f.write(args)

print(f"Wrote {args_path}")
print(args)
