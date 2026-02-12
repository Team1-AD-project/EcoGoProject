#!/usr/bin/env python3
"""
SHL (Sussex-Huawei Locomotion) Dataset → Training CSV Converter

Reads SHL dataset sensor files and converts them into the 17-feature CSV format
expected by train_model.py.

SHL dataset format:
  - Accelerometer.txt: timestamp(ms), x, y, z  (100 Hz)
  - Gyroscope.txt:     timestamp(ms), x, y, z  (100 Hz)
  - Label.txt:         timestamp(ms), label
    Labels: 1=Still, 2=Walk, 3=Run, 4=Bike, 5=Car, 6=Bus, 7=Train, 8=Subway

Label mapping (SHL → our model):
  Walk(2)  → WALKING
  Bike(4)  → CYCLING
  Bus(6)   → BUS
  Car(5)   → DRIVING
  Subway(8)→ SUBWAY
  Train(7) → SUBWAY  (merged with subway)

Usage:
    python3 convert_shl_data.py --input /path/to/SHL/User1/220617 --output shl_training.csv

    # Process multiple days:
    python3 convert_shl_data.py --input /path/to/SHL/User1 --recursive --output shl_training.csv

Dependencies:
    pip install numpy pandas
"""

import argparse
import logging
import os
from pathlib import Path
from typing import List, Dict, Tuple, Optional

import numpy as np
import pandas as pd

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# SHL label → our transport mode mapping
SHL_LABEL_MAP = {
    2: "WALKING",
    4: "CYCLING",
    5: "DRIVING",
    6: "BUS",
    7: "SUBWAY",   # Train → SUBWAY
    8: "SUBWAY",
}

# Window size in seconds for feature extraction
DEFAULT_WINDOW_SIZE = 30  # 30-second windows

# Minimum number of samples in a window (at 100Hz)
MIN_SAMPLES_PER_WINDOW = 500  # ~5 seconds at 100Hz

# Output CSV header matching train_model.py FEATURE_COLUMNS
CSV_HEADER = [
    "accelMeanX", "accelMeanY", "accelMeanZ",
    "accelStdX", "accelStdY", "accelStdZ",
    "accelMagnitude",
    "gyroMeanX", "gyroMeanY", "gyroMeanZ",
    "gyroStdX", "gyroStdY", "gyroStdZ",
    "journeyDuration",
    "gpsSpeedMean", "gpsSpeedStd", "gpsSpeedMax",
    "transportMode",
]


def load_sensor_file(filepath: str) -> Optional[np.ndarray]:
    """Load a sensor file (space/tab separated: timestamp, x, y, z)."""
    if not os.path.exists(filepath):
        logger.warning(f"File not found: {filepath}")
        return None
    try:
        data = np.loadtxt(filepath)
        logger.info(f"Loaded {filepath}: {data.shape[0]} rows")
        return data
    except Exception as e:
        logger.error(f"Failed to load {filepath}: {e}")
        return None


def load_label_file(filepath: str) -> Optional[np.ndarray]:
    """Load label file (space/tab separated: timestamp, label)."""
    if not os.path.exists(filepath):
        logger.warning(f"Label file not found: {filepath}")
        return None
    try:
        data = np.loadtxt(filepath)
        logger.info(f"Loaded {filepath}: {data.shape[0]} rows")
        return data
    except Exception as e:
        logger.error(f"Failed to load {filepath}: {e}")
        return None


def align_sensors(accel: np.ndarray, gyro: np.ndarray, labels: np.ndarray) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
    """
    Align sensor data and labels by timestamp.
    SHL data is synchronously sampled at 100Hz, so timestamps should align.
    We use nearest-neighbor matching with a tolerance of 50ms.
    """
    # Find overlapping time range
    t_start = max(accel[0, 0], gyro[0, 0], labels[0, 0])
    t_end = min(accel[-1, 0], gyro[-1, 0], labels[-1, 0])

    # Filter to overlapping range
    accel_mask = (accel[:, 0] >= t_start) & (accel[:, 0] <= t_end)
    gyro_mask = (gyro[:, 0] >= t_start) & (gyro[:, 0] <= t_end)
    label_mask = (labels[:, 0] >= t_start) & (labels[:, 0] <= t_end)

    accel = accel[accel_mask]
    gyro = gyro[gyro_mask]
    labels = labels[label_mask]

    logger.info(f"Aligned range: {t_start:.0f} - {t_end:.0f} ({(t_end - t_start) / 1000:.1f}s)")
    logger.info(f"  Accel: {accel.shape[0]}, Gyro: {gyro.shape[0]}, Labels: {labels.shape[0]}")

    return accel, gyro, labels


def segment_by_label(labels: np.ndarray) -> List[Tuple[int, int, int]]:
    """
    Segment continuous label sequences.
    Returns list of (start_idx, end_idx, label) tuples.
    Only includes labels we care about (in SHL_LABEL_MAP).
    """
    segments = []
    current_label = int(labels[0, 1])
    start_idx = 0

    for i in range(1, len(labels)):
        label = int(labels[i, 1])
        if label != current_label:
            if current_label in SHL_LABEL_MAP:
                segments.append((start_idx, i - 1, current_label))
            current_label = label
            start_idx = i

    # Last segment
    if current_label in SHL_LABEL_MAP:
        segments.append((start_idx, len(labels) - 1, current_label))

    logger.info(f"Found {len(segments)} labeled segments")
    return segments


def extract_features(
    accel: np.ndarray,
    gyro: np.ndarray,
    labels: np.ndarray,
    window_size_sec: int = DEFAULT_WINDOW_SIZE
) -> List[Dict]:
    """
    Extract 17-feature vectors from sensor windows.

    For each continuous label segment, split into fixed-size windows
    and compute statistical features.
    """
    samples_per_window = window_size_sec * 100  # 100 Hz
    segments = segment_by_label(labels)
    features_list = []

    for seg_start, seg_end, label in segments:
        seg_len = seg_end - seg_start + 1
        transport_mode = SHL_LABEL_MAP[label]

        # Split segment into windows
        for win_start in range(seg_start, seg_end + 1, samples_per_window):
            win_end = min(win_start + samples_per_window, seg_end + 1)

            if (win_end - win_start) < MIN_SAMPLES_PER_WINDOW:
                continue  # Skip short windows

            # Get sensor data for this window
            # Use label timestamps to find matching sensor indices
            t_start_ms = labels[win_start, 0]
            t_end_ms = labels[win_end - 1, 0]

            # Find matching accelerometer data
            a_mask = (accel[:, 0] >= t_start_ms) & (accel[:, 0] <= t_end_ms)
            a_win = accel[a_mask]

            # Find matching gyroscope data
            g_mask = (gyro[:, 0] >= t_start_ms) & (gyro[:, 0] <= t_end_ms)
            g_win = gyro[g_mask]

            if len(a_win) < MIN_SAMPLES_PER_WINDOW or len(g_win) < MIN_SAMPLES_PER_WINDOW:
                continue

            # Accelerometer features (7)
            ax, ay, az = a_win[:, 1], a_win[:, 2], a_win[:, 3]
            accel_magnitude = np.sqrt(ax**2 + ay**2 + az**2)

            accel_mean_x = float(np.mean(ax))
            accel_mean_y = float(np.mean(ay))
            accel_mean_z = float(np.mean(az))
            accel_std_x = float(np.std(ax))
            accel_std_y = float(np.std(ay))
            accel_std_z = float(np.std(az))
            accel_mag_mean = float(np.mean(accel_magnitude))

            # Gyroscope features (6)
            gx, gy, gz = g_win[:, 1], g_win[:, 2], g_win[:, 3]

            gyro_mean_x = float(np.mean(gx))
            gyro_mean_y = float(np.mean(gy))
            gyro_mean_z = float(np.mean(gz))
            gyro_std_x = float(np.std(gx))
            gyro_std_y = float(np.std(gy))
            gyro_std_z = float(np.std(gz))

            # Duration (seconds)
            duration = (t_end_ms - t_start_ms) / 1000.0

            # GPS speed features (3) - SHL dataset doesn't include GPS speed,
            # so we estimate from accelerometer data or set to 0.
            # For proper training, these should be supplemented with GPS data if available.
            gps_speed_mean = 0.0
            gps_speed_std = 0.0
            gps_speed_max = 0.0

            features_list.append({
                "accelMeanX": accel_mean_x,
                "accelMeanY": accel_mean_y,
                "accelMeanZ": accel_mean_z,
                "accelStdX": accel_std_x,
                "accelStdY": accel_std_y,
                "accelStdZ": accel_std_z,
                "accelMagnitude": accel_mag_mean,
                "gyroMeanX": gyro_mean_x,
                "gyroMeanY": gyro_mean_y,
                "gyroMeanZ": gyro_mean_z,
                "gyroStdX": gyro_std_x,
                "gyroStdY": gyro_std_y,
                "gyroStdZ": gyro_std_z,
                "journeyDuration": duration,
                "gpsSpeedMean": gps_speed_mean,
                "gpsSpeedStd": gps_speed_std,
                "gpsSpeedMax": gps_speed_max,
                "transportMode": transport_mode,
            })

    logger.info(f"Extracted {len(features_list)} feature windows")
    return features_list


def process_day_directory(day_dir: str, window_size: int) -> List[Dict]:
    """Process a single day directory containing sensor files."""
    day_path = Path(day_dir)

    accel_file = day_path / "Accelerometer.txt"
    gyro_file = day_path / "Gyroscope.txt"
    label_file = day_path / "Label.txt"

    if not accel_file.exists() or not gyro_file.exists() or not label_file.exists():
        logger.warning(f"Missing required files in {day_dir}")
        return []

    logger.info(f"Processing: {day_dir}")

    accel = load_sensor_file(str(accel_file))
    gyro = load_sensor_file(str(gyro_file))
    labels = load_label_file(str(label_file))

    if accel is None or gyro is None or labels is None:
        return []

    accel, gyro, labels = align_sensors(accel, gyro, labels)
    return extract_features(accel, gyro, labels, window_size)


def find_day_directories(root_dir: str) -> List[str]:
    """Recursively find directories containing SHL sensor files."""
    day_dirs = []
    for dirpath, dirnames, filenames in os.walk(root_dir):
        if "Accelerometer.txt" in filenames and "Label.txt" in filenames:
            day_dirs.append(dirpath)
    day_dirs.sort()
    return day_dirs


def main():
    parser = argparse.ArgumentParser(
        description="Convert SHL dataset to training CSV for transport mode classification"
    )
    parser.add_argument("--input", required=True, help="Path to SHL data directory (day folder or parent)")
    parser.add_argument("--output", default="shl_training.csv", help="Output CSV file path")
    parser.add_argument("--window-size", type=int, default=DEFAULT_WINDOW_SIZE,
                        help=f"Window size in seconds (default: {DEFAULT_WINDOW_SIZE})")
    parser.add_argument("--recursive", action="store_true",
                        help="Recursively search for day directories")

    args = parser.parse_args()
    input_path = Path(args.input)

    if not input_path.exists():
        logger.error(f"Input path does not exist: {args.input}")
        return

    # Find directories to process
    if args.recursive:
        day_dirs = find_day_directories(args.input)
        logger.info(f"Found {len(day_dirs)} day directories")
    else:
        day_dirs = [args.input]

    # Process all directories
    all_features = []
    for day_dir in day_dirs:
        features = process_day_directory(day_dir, args.window_size)
        all_features.extend(features)

    if not all_features:
        logger.error("No features extracted. Check input path and data files.")
        return

    # Create DataFrame and save CSV
    df = pd.DataFrame(all_features, columns=CSV_HEADER)

    # Print statistics
    logger.info("=" * 50)
    logger.info(f"Total samples: {len(df)}")
    logger.info("Transport mode distribution:")
    mode_counts = df["transportMode"].value_counts()
    for mode, count in mode_counts.items():
        logger.info(f"  {mode}: {count} ({count * 100 / len(df):.1f}%)")

    # Save CSV
    df.to_csv(args.output, index=False)
    logger.info(f"Saved to: {args.output} ({os.path.getsize(args.output) / 1024:.1f} KB)")
    logger.info("=" * 50)
    logger.info(f"Next step: python3 train_model.py --data {args.output} --output model.tflite")


if __name__ == "__main__":
    main()
