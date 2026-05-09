import math
import os
import threading
import time

import cv2
from flask import Flask, jsonify
from ultralytics import YOLO

API_PORT = int(os.getenv("CAMERA_API_PORT", "5000"))
CAMERA_INDEX = int(os.getenv("CAMERA_INDEX", "0"))

YOLO_MODEL = os.getenv("YOLO_MODEL", "yolov8n.pt")
YOLO_IMGSZ = int(os.getenv("YOLO_IMGSZ", "320"))
CONFIDENCE_THRESHOLD = float(os.getenv("CAMERA_CONFIDENCE_THRESHOLD", "0.5"))
CAMERA_DEBUG_VIEW = os.getenv("CAMERA_DEBUG_VIEW", "false").lower() == "true"
DETECTION_FPS_LIMIT = float(os.getenv("CAMERA_DETECTION_FPS_LIMIT", "8"))

MOTION_DISTANCE_THRESHOLD = float(os.getenv("CAMERA_MOTION_DISTANCE_THRESHOLD", "35"))
MOTION_AREA_CHANGE_THRESHOLD = float(os.getenv("CAMERA_MOTION_AREA_CHANGE_THRESHOLD", "0.18"))
STATE_TTL_SECONDS = float(os.getenv("CAMERA_STATE_TTL_SECONDS", "2.0"))

FRAME_WIDTH = int(os.getenv("CAMERA_FRAME_WIDTH", "640"))
FRAME_HEIGHT = int(os.getenv("CAMERA_FRAME_HEIGHT", "480"))
FRAME_FPS = int(os.getenv("CAMERA_FRAME_FPS", "30"))

CAMERA_REOPEN_DELAY_SECONDS = float(os.getenv("CAMERA_REOPEN_DELAY_SECONDS", "1.0"))
CAMERA_MAX_CONSECUTIVE_FAILS = int(os.getenv("CAMERA_MAX_CONSECUTIVE_FAILS", "10"))
CAMERA_WARMUP_SECONDS = float(os.getenv("CAMERA_WARMUP_SECONDS", "1.0"))

app = Flask(__name__)

print("Loading YOLO model %s imgsz=%s..." % (YOLO_MODEL, YOLO_IMGSZ))
model = YOLO(YOLO_MODEL)
print("YOLO model loaded.")

camera_lock = threading.Lock()
detect_lock = threading.Lock()
state_lock = threading.Lock()

latest_frame = None
latest_frame_ts = 0.0
camera_ready = False
camera_error = None

last_person_centers = []
last_person_areas = []
last_detect_ts = 0.0
last_api_log_ts = 0.0
inference_count = 0
inference_window_start = time.time()
inference_fps = 0.0

latest_detection = {
    "human_detected": False,
    "human_count": 0,
    "max_confidence": 0.0,
    "motion_detected": False,
    "movement_score": 0.0,
    "boxes": [],
    "updated_at": 0.0,
}


def box_center_and_area(box):
    x1, y1, x2, y2 = box
    cx = (x1 + x2) / 2.0
    cy = (y1 + y2) / 2.0
    area = max(1.0, (x2 - x1) * (y2 - y1))
    return (cx, cy), area


def distance(p1, p2):
    return math.sqrt((p1[0] - p2[0]) ** 2 + (p1[1] - p2[1]) ** 2)


def analyze_motion(current_boxes):
    global last_person_centers, last_person_areas, last_detect_ts

    now = time.time()
    current_centers = []
    current_areas = []

    for box in current_boxes:
        center, area = box_center_and_area(box)
        current_centers.append(center)
        current_areas.append(area)

    with detect_lock:
        if not current_centers:
            if now - last_detect_ts > STATE_TTL_SECONDS:
                last_person_centers = []
                last_person_areas = []
            return False, 0.0

        if not last_person_centers:
            last_person_centers = current_centers
            last_person_areas = current_areas
            last_detect_ts = now
            return False, 0.0

        motion_detected = False
        movement_score = 0.0
        for i, center in enumerate(current_centers):
            nearest_dist = None
            nearest_area_change = 0.0

            for j, prev_center in enumerate(last_person_centers):
                d = distance(center, prev_center)
                prev_area = last_person_areas[j] if j < len(last_person_areas) else 1.0
                curr_area = current_areas[i]
                area_change = abs(curr_area - prev_area) / max(prev_area, 1.0)

                if nearest_dist is None or d < nearest_dist:
                    nearest_dist = d
                    nearest_area_change = area_change

            if nearest_dist is None:
                continue

            score = max(
                nearest_dist / max(MOTION_DISTANCE_THRESHOLD, 1),
                nearest_area_change / max(MOTION_AREA_CHANGE_THRESHOLD, 0.001),
            )
            movement_score = max(movement_score, score)

            if nearest_dist >= MOTION_DISTANCE_THRESHOLD or nearest_area_change >= MOTION_AREA_CHANGE_THRESHOLD:
                motion_detected = True

        last_person_centers = current_centers
        last_person_areas = current_areas
        last_detect_ts = now
        return motion_detected, movement_score


def open_camera():
    cap = cv2.VideoCapture(CAMERA_INDEX, cv2.CAP_DSHOW)
    if not cap.isOpened():
        return cap

    cap.set(cv2.CAP_PROP_FRAME_WIDTH, FRAME_WIDTH)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, FRAME_HEIGHT)
    cap.set(cv2.CAP_PROP_FPS, FRAME_FPS)
    try:
        cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
    except Exception:
        pass
    return cap


def set_camera_status(ready, error=None):
    global camera_ready, camera_error
    with state_lock:
        camera_ready = ready
        camera_error = error


def update_latest_frame(frame):
    global latest_frame, latest_frame_ts
    with camera_lock:
        latest_frame = frame.copy()
        latest_frame_ts = time.time()


def read_latest_frame():
    with camera_lock:
        if latest_frame is None:
            return None, latest_frame_ts
        return latest_frame.copy(), latest_frame_ts


def update_latest_detection(human_detected, human_count, max_confidence, motion_detected, movement_score, boxes):
    global latest_detection
    with state_lock:
        latest_detection = {
            "human_detected": human_detected,
            "human_count": human_count,
            "max_confidence": max_confidence,
            "motion_detected": motion_detected,
            "movement_score": movement_score,
            "boxes": boxes,
            "updated_at": time.time(),
        }


def get_latest_detection():
    with state_lock:
        return dict(latest_detection)


def reset_detection_if_stale():
    with state_lock:
        updated_at = latest_detection["updated_at"]
        if updated_at == 0.0:
            return

        if time.time() - updated_at > STATE_TTL_SECONDS * 2:
            latest_detection.update(
                {
                    "human_detected": False,
                    "human_count": 0,
                    "max_confidence": 0.0,
                    "motion_detected": False,
                    "movement_score": 0.0,
                    "boxes": [],
                    "updated_at": time.time(),
                }
            )


def process_frame(frame):
    results = model(frame, classes=[0], conf=CONFIDENCE_THRESHOLD, imgsz=YOLO_IMGSZ, verbose=False)
    boxes_obj = results[0].boxes

    person_boxes = []
    max_confidence = 0.0

    if boxes_obj is not None and len(boxes_obj) > 0:
        xyxy = boxes_obj.xyxy.cpu().numpy()
        confs = boxes_obj.conf.cpu().numpy()

        for i in range(len(xyxy)):
            x1, y1, x2, y2 = xyxy[i].tolist()
            person_boxes.append([float(x1), float(y1), float(x2), float(y2)])

        max_confidence = float(confs.max()) if len(confs) > 0 else 0.0

    human_count = len(person_boxes)
    motion_detected, movement_score = analyze_motion(person_boxes)

    update_latest_detection(
        human_detected=human_count > 0,
        human_count=human_count,
        max_confidence=max_confidence,
        motion_detected=motion_detected,
        movement_score=movement_score,
        boxes=person_boxes,
    )

    if not CAMERA_DEBUG_VIEW:
        return None

    annotated_frame = results[0].plot()
    cv2.putText(
        annotated_frame,
        "Humans: %s | Conf: %.2f" % (human_count, max_confidence),
        (10, 30),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.8,
        (0, 255, 0),
        2,
    )
    cv2.putText(
        annotated_frame,
        "Motion: %s | Score: %.2f" % (motion_detected, movement_score),
        (10, 65),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.8,
        (0, 255, 255),
        2,
    )
    return annotated_frame


def capture_thread():
    cap = None
    fail_count = 0

    while True:
        if cap is None or not cap.isOpened():
            print("Opening camera...")
            cap = open_camera()
            if not cap.isOpened():
                set_camera_status(False, "Cannot open camera.")
                time.sleep(CAMERA_REOPEN_DELAY_SECONDS)
                continue

            set_camera_status(True, None)
            print("Camera started.")
            time.sleep(CAMERA_WARMUP_SECONDS)

        ret, frame = cap.read()
        if not ret or frame is None:
            fail_count += 1
            set_camera_status(False, "Cannot read frame from camera.")

            if fail_count >= CAMERA_MAX_CONSECUTIVE_FAILS:
                try:
                    cap.release()
                except Exception:
                    pass
                cap = None
                fail_count = 0
                reset_detection_if_stale()
                time.sleep(CAMERA_REOPEN_DELAY_SECONDS)

            time.sleep(0.05)
            continue

        fail_count = 0
        set_camera_status(True, None)
        update_latest_frame(frame)


def inference_thread():
    global inference_count, inference_window_start, inference_fps

    min_interval = 1.0 / max(DETECTION_FPS_LIMIT, 0.1)
    last_infer_ts = 0.0
    last_frame_ts = 0.0
    window_name = "YOLO Human Motion Detection"

    if CAMERA_DEBUG_VIEW:
        cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)

    while True:
        now = time.time()
        if now - last_infer_ts < min_interval:
            time.sleep(0.01)
            continue

        frame, frame_ts = read_latest_frame()
        if frame is None or frame_ts <= last_frame_ts:
            reset_detection_if_stale()
            time.sleep(0.02)
            continue

        last_frame_ts = frame_ts
        last_infer_ts = now

        try:
            annotated = process_frame(frame)
            inference_count += 1
            elapsed = time.time() - inference_window_start
            if elapsed >= 2.0:
                inference_fps = inference_count / elapsed
                inference_count = 0
                inference_window_start = time.time()

            if CAMERA_DEBUG_VIEW and annotated is not None:
                cv2.imshow(window_name, annotated)
                if (cv2.waitKey(1) & 0xFF) == ord("q"):
                    break
        except Exception as exc:
            print("Frame inference error: %s" % exc)
            time.sleep(0.1)

    if CAMERA_DEBUG_VIEW:
        cv2.destroyAllWindows()


threading.Thread(target=capture_thread, daemon=True).start()
threading.Thread(target=inference_thread, daemon=True).start()


@app.route("/check_human", methods=["GET"])
def check_human():
    global last_api_log_ts

    reset_detection_if_stale()
    detection = get_latest_detection()

    with state_lock:
        ready = camera_ready
        err = camera_error

    if not ready and detection["updated_at"] == 0.0:
        return jsonify({"status": "error", "message": err or "Camera is not ready."}), 503

    now = time.time()
    age = None if detection["updated_at"] == 0.0 else now - detection["updated_at"]
    if now - last_api_log_ts >= 2.0:
        print(
            "[API] human=%s count=%s motion=%s score=%.2f conf=%.1f%% age=%s"
            % (
                detection["human_detected"],
                detection["human_count"],
                detection["motion_detected"],
                detection["movement_score"],
                detection["max_confidence"] * 100,
                "n/a" if age is None else "%.3fs" % age,
            )
        )
        last_api_log_ts = now

    return jsonify(
        {
            "status": "success",
            "camera_ready": ready,
            "human_detected": detection["human_detected"],
            "human_count": detection["human_count"],
            "max_confidence": detection["max_confidence"],
            "motion_detected": detection["motion_detected"],
            "movement_score": detection["movement_score"],
            "boxes": detection["boxes"],
            "updated_at": detection["updated_at"],
            "detection_age_seconds": None if age is None else round(age, 3),
        }
    ), 200


@app.route("/health", methods=["GET"])
def health():
    now = time.time()
    with camera_lock:
        ready_frame = latest_frame is not None
        frame_age = None if latest_frame_ts == 0 else round(now - latest_frame_ts, 3)

    with state_lock:
        ready = camera_ready
        err = camera_error
        detection_updated_at = latest_detection["updated_at"]

    detection_age = None if detection_updated_at == 0 else round(now - detection_updated_at, 3)

    return jsonify(
        {
            "status": "success",
            "camera_ready": ready,
            "frame_available": ready_frame,
            "model_loaded": True,
            "model": YOLO_MODEL,
            "imgsz": YOLO_IMGSZ,
            "debug_view": CAMERA_DEBUG_VIEW,
            "last_frame_age_seconds": frame_age,
            "detection_age_seconds": detection_age,
            "inference_fps": round(inference_fps, 2),
            "camera_error": err,
        }
    ), 200


if __name__ == "__main__":
    print("\nYOLO API server listening on port %s." % API_PORT)
    app.run(host="0.0.0.0", port=API_PORT, debug=False, use_reloader=False, threaded=True)
