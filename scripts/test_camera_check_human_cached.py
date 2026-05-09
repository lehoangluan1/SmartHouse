"""
Static smoke-test for camera_interface.py.

It verifies that /check_human reads cached detection state instead of calling
YOLO/model inference from the request handler. Run with Python 3:
    python scripts/test_camera_check_human_cached.py
"""

import ast
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
source = (ROOT / "camera_interface.py").read_text(encoding="utf-8")
tree = ast.parse(source)

check_human = next(
    node for node in tree.body
    if isinstance(node, ast.FunctionDef) and node.name == "check_human"
)

called_names = {
    node.func.id
    for node in ast.walk(check_human)
    if isinstance(node, ast.Call) and isinstance(node.func, ast.Name)
}

assert "process_frame" not in called_names
assert "model" not in called_names
assert "get_latest_detection" in called_names
print("camera /check_human cached smoke test passed")
