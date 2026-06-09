# Operating Hand Recognition

This folder trains the left/right operating-hand model used by the Android client.

The raw touch track is sampled to 9 points. Each raw point has 6 float values:

```text
x, y, screenWidth, screenHeight, density, downTimeDeltaMillis
```

Before training and on-device inference, raw points are converted to the same relative feature vector:

```text
x / width,
y / height,
(x - startX) / width,
(y - startY) / height,
(time - startTime) / duration,
durationSeconds
```

This keeps training and runtime input stable across screen sizes and Compose layout coordinates.

## Dataset Layout

Put collected samples under a dataset directory. Labels can come from file or parent folder names:

```text
dataset/
  left/
    sample_001.json
  right/
    sample_001.json
```

Each JSON file can be either a raw point list:

```json
[[120, 540, 1080, 2400, 3, 0], [126, 535, 1080, 2400, 3, 16]]
```

or an object:

```json
{"label": "left", "points": [[120, 540, 1080, 2400, 3, 0]]}
```

## Train And Export

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r ml\operating_hand\requirements.txt
python ml\operating_hand\train_operating_hand.py --data dataset --output androidApp\src\main\assets\operating_hand.tflite
```

After the `.tflite` file is exported, rebuild the Android app. If the model asset is not present, the app keeps working with a low-confidence heuristic fallback.
