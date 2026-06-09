import argparse
import json
import re
from pathlib import Path

import numpy as np
import tensorflow as tf


SAMPLE_COUNT = 9
TENSOR_SIZE = 6
LABELS = {
    "left": 0,
    "l": 0,
    "0": 0,
    "right": 1,
    "r": 1,
    "1": 1,
}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True, help="Dataset root containing left/right JSON samples.")
    parser.add_argument(
        "--output",
        default="androidApp/src/main/assets/operating_hand.tflite",
        help="Output .tflite path consumed by the Android app.",
    )
    parser.add_argument("--epochs", type=int, default=80)
    parser.add_argument("--batch-size", type=int, default=32)
    args = parser.parse_args()

    x, y = load_dataset(Path(args.data))
    if len(x) < 2:
        raise SystemExit("Need at least two labeled samples.")

    left_count = int(np.sum(y == 0))
    right_count = int(np.sum(y == 1))
    print(f"loaded samples: left={left_count} right={right_count} total={len(y)}")

    rng = np.random.default_rng(42)
    order = rng.permutation(len(x))
    x = x[order]
    y = y[order]

    split = max(1, int(len(x) * 0.8))
    x_train, x_val = x[:split], x[split:]
    y_train, y_val = y[:split], y[split:]
    if len(x_val) == 0:
        x_val, y_val = x_train, y_train

    model = build_model(x_train)
    model.fit(
        x_train,
        y_train,
        validation_data=(x_val, y_val),
        epochs=args.epochs,
        batch_size=args.batch_size,
        verbose=2,
    )

    loss, accuracy = model.evaluate(x_val, y_val, verbose=0)
    print(f"validation_loss={loss:.4f} validation_accuracy={accuracy:.4f}")

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(tflite_model)
    print(f"exported {output} ({len(tflite_model)} bytes)")


def build_model(x_train: np.ndarray) -> tf.keras.Model:
    normalizer = tf.keras.layers.Normalization(axis=-1)
    normalizer.adapt(x_train.reshape((-1, TENSOR_SIZE)))

    inputs = tf.keras.Input(shape=(SAMPLE_COUNT, TENSOR_SIZE), name="touch_track")
    x = normalizer(inputs)
    x = tf.keras.layers.Flatten()(x)
    x = tf.keras.layers.Dense(48, activation="relu")(x)
    x = tf.keras.layers.Dropout(0.2)(x)
    x = tf.keras.layers.Dense(24, activation="relu")(x)
    outputs = tf.keras.layers.Dense(1, activation="sigmoid", name="right_hand_score")(x)

    model = tf.keras.Model(inputs=inputs, outputs=outputs)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
        loss="binary_crossentropy",
        metrics=["accuracy"],
    )
    return model


def load_dataset(root: Path) -> tuple[np.ndarray, np.ndarray]:
    samples: list[np.ndarray] = []
    labels: list[int] = []

    for file_path in root.rglob("*.json"):
        for label, points in load_json_samples(file_path):
            samples.append(resample(points))
            labels.append(label)

    if not samples:
        raise SystemExit(f"No JSON samples found under {root}")

    return (
        np.asarray(samples, dtype=np.float32),
        np.asarray(labels, dtype=np.float32),
    )


def load_json_samples(file_path: Path) -> list[tuple[int, np.ndarray]]:
    content = json.loads(file_path.read_text(encoding="utf-8"))
    inferred_label = infer_label(file_path)

    if isinstance(content, dict):
        label = LABELS.get(str(content.get("label", inferred_label)).lower())
        points = content.get("points") or content.get("data") or content.get("track")
        if label is None or points is None:
            return []
        return [(label, np.asarray(points, dtype=np.float32))]

    if isinstance(content, list) and content and isinstance(content[0], dict):
        loaded = []
        for item in content:
            label = LABELS.get(str(item.get("label", inferred_label)).lower())
            points = item.get("points") or item.get("data") or item.get("track")
            if label is not None and points is not None:
                loaded.append((label, np.asarray(points, dtype=np.float32)))
        return loaded

    if isinstance(content, list) and content and is_track_list(content[0]):
        label = LABELS.get(str(inferred_label).lower())
        if label is None:
            return []
        return [
            (label, np.asarray(track, dtype=np.float32))
            for track in content
            if is_track_list(track)
        ]

    label = LABELS.get(str(inferred_label).lower())
    if label is None:
        return []
    return [(label, np.asarray(content, dtype=np.float32))]


def infer_label(file_path: Path) -> str | None:
    for part in reversed(file_path.parts):
        normalized = Path(part).stem.lower()
        if normalized in LABELS:
            return normalized

        tokens = [token for token in re.split(r"[^a-zA-Z0-9]+", normalized) if token]
        for key in ("left", "right", "l", "r", "0", "1"):
            if key in tokens:
                return key
    return None


def resample(points: np.ndarray) -> np.ndarray:
    if points.ndim != 2 or points.shape[1] < TENSOR_SIZE:
        raise ValueError("Each sample must be a point list with at least 6 columns.")

    step = len(points) / SAMPLE_COUNT
    indexes = [min(int(i * step), len(points) - 1) for i in range(SAMPLE_COUNT)]
    return to_relative_features(points[indexes, :TENSOR_SIZE])


def to_relative_features(points: np.ndarray) -> np.ndarray:
    width = np.maximum(points[:, 2], 1.0)
    height = np.maximum(points[:, 3], 1.0)
    start_x = points[0, 0]
    start_y = points[0, 1]
    start_time = points[0, 5]
    duration = max(float(np.max(points[:, 5]) - np.min(points[:, 5])), 1.0)

    features = np.zeros((len(points), TENSOR_SIZE), dtype=np.float32)
    features[:, 0] = points[:, 0] / width
    features[:, 1] = points[:, 1] / height
    features[:, 2] = (points[:, 0] - start_x) / width
    features[:, 3] = (points[:, 1] - start_y) / height
    features[:, 4] = (points[:, 5] - start_time) / duration
    features[:, 5] = duration / 1000.0
    return features


def is_track_list(value: object) -> bool:
    return (
        isinstance(value, list)
        and bool(value)
        and isinstance(value[0], list)
        and len(value[0]) >= TENSOR_SIZE
    )


if __name__ == "__main__":
    main()
