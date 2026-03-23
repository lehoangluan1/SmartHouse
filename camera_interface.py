import cv2
import threading
import time
import math
from flask import Flask, jsonify
from ultralytics import YOLO

API_PORT = 5000
DEBUG_CAMERA = False
CAMERA_INDEX = 0

CONFIDENCE_THRESHOLD = 0.5
MOTION_DISTANCE_THRESHOLD = 35
MOTION_AREA_CHANGE_THRESHOLD = 0.18
STATE_TTL_SECONDS = 2.0

latest_frame = None
latest_frame_ts = 0.0
camera_lock = threading.Lock()
app = Flask(__name__)

print("Đang tải mô hình YOLOv8...")
model = YOLO("yolov8m.pt")

detect_lock = threading.Lock()
last_person_centers = []
last_person_areas = []
last_detect_ts = 0.0
last_api_log_ts = 0.0


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

    motion_detected = False
    movement_score = 0.0

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


def camera_thread():
    global latest_frame, latest_frame_ts, DEBUG_CAMERA

    cap = cv2.VideoCapture(CAMERA_INDEX)
    if not cap.isOpened():
        print("LỖI: Không thể mở camera!")
        return

    print("Camera đã khởi động ngầm...")
    window_name = "YOLO Human Motion Detection"
    window_open = False

    while True:
        ret, frame = cap.read()
        if not ret:
            time.sleep(0.05)
            continue

        with camera_lock:
            latest_frame = frame.copy()
            latest_frame_ts = time.time()

        if DEBUG_CAMERA:
            if not window_open:
                cv2.namedWindow(window_name)
                window_open = True

            results = model(frame, classes=[0], conf=CONFIDENCE_THRESHOLD, verbose=False)
            annotated_frame = results[0].plot()
            cv2.imshow(window_name, annotated_frame)
            cv2.waitKey(1)
        else:
            if window_open:
                cv2.destroyWindow(window_name)
                window_open = False


threading.Thread(target=camera_thread, daemon=True).start()


@app.route("/check_human", methods=["GET"])
def check_human():
    global latest_frame, last_api_log_ts

    with camera_lock:
        if latest_frame is None:
            return jsonify({"status": "error", "message": "Camera chưa sẵn sàng."}), 503
        frame_to_process = latest_frame.copy()

    try:
        results = model(frame_to_process, classes=[0], conf=CONFIDENCE_THRESHOLD, verbose=False)
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
        is_human = human_count > 0
        motion_detected, movement_score = analyze_motion(person_boxes)

        now = time.time()
        if now - last_api_log_ts >= 2.0:
            print(
                f"[API] human={is_human} count={human_count} "
                f"motion={motion_detected} score={movement_score:.2f} "
                f"conf={max_confidence*100:.1f}%"
            )
            last_api_log_ts = now

        return jsonify({
            "status": "success",
            "human_detected": is_human,
            "human_count": human_count,
            "max_confidence": max_confidence,
            "motion_detected": motion_detected,
            "movement_score": movement_score,
            "boxes": person_boxes
        }), 200

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500


@app.route("/health", methods=["GET"])
def health():
    with camera_lock:
        ready = latest_frame is not None
        frame_age = None if latest_frame_ts == 0 else round(time.time() - latest_frame_ts, 3)

    return jsonify({
        "status": "success",
        "camera_ready": ready,
        "debug_camera": DEBUG_CAMERA,
        "model_loaded": True,
        "last_frame_age_seconds": frame_age,
    }), 200


if __name__ == "__main__":
    print(f"\nAPI YOLO Server chạy tại cổng {API_PORT}.")
    app.run(host="0.0.0.0", port=API_PORT, debug=False, use_reloader=False, threaded=True)