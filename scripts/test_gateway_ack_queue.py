"""
Smoke-test the gateway ACK queue without requiring backend availability.

Run with the same Python 3 environment used for gateway.py:
    python scripts/test_gateway_ack_queue.py
"""

import pathlib
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

import gateway  # noqa: E402


class FakeResponse:
    status_code = 200


def fake_proxy_post(url, payload, timeout):
    sent.append((url, payload, timeout))
    return FakeResponse()


sent = []
gateway.proxy_post = fake_proxy_post

assert gateway.enqueue_ack("ohstem-fan-ctrl-01", -1) is True
assert len(gateway.ACK_Q) == 0

assert gateway.enqueue_ack("ohstem-fan-ctrl-01", 42) is True
assert gateway.enqueue_ack("ohstem-fan-ctrl-01", 42) is True
assert len(gateway.ACK_Q) == 1

gateway.start_ack_worker_once()
deadline = time.time() + 2
while time.time() < deadline and not sent:
    time.sleep(0.02)

assert sent, "ACK worker did not forward queued ACK"
assert sent[0][1] == {"id": 42}
print("gateway ACK queue smoke test passed")
