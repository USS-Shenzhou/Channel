"""Copy FFM wrapper files into the WebRTC source tree and register the build target."""
import os
import shutil

repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
webrtc_dir = os.path.join(repo_root, "webrtc")
build_src = os.path.join(repo_root, "_build", "src")
wrapper_dir = os.path.join(build_src, "ffm_wrapper")

os.makedirs(wrapper_dir, exist_ok=True)

for filename in ("BUILD.gn", "webrtc_ffm.h", "webrtc_ffm.cc"):
    shutil.copy2(os.path.join(webrtc_dir, filename), os.path.join(wrapper_dir, filename))
    print(f"Copied {filename} -> ffm_wrapper/")

root_gn = os.path.join(build_src, "BUILD.gn")
with open(root_gn, "a", encoding="utf-8") as f:
    f.write('\ngroup("ffm_build") {\n  deps = [ "//ffm_wrapper:webrtc_ffm" ]\n}\n')

print("Appended ffm_build group to root BUILD.gn.")
