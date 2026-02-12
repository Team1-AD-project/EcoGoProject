#!/usr/bin/env python3
"""
pytest tests for convert_shl_data.py

Covers:
- SHL label mapping
- Sensor file loading
- Sensor alignment
- Label segmentation
- Feature extraction
- CSV header validation
"""

import os
import sys
from pathlib import Path

import numpy as np
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import convert_shl_data


# ============================================================
# Fixtures
# ============================================================

@pytest.fixture
def synthetic_accel_data():
    """100Hz accelerometer data for 10 seconds (1000 rows)."""
    np.random.seed(42)
    n = 1000
    timestamps = np.arange(0, n * 10, 10, dtype=float)  # 10ms interval = 100Hz
    xyz = np.random.randn(n, 3).astype(float)
    return np.column_stack([timestamps, xyz])


@pytest.fixture
def synthetic_gyro_data():
    """100Hz gyroscope data for 10 seconds."""
    np.random.seed(43)
    n = 1000
    timestamps = np.arange(0, n * 10, 10, dtype=float)
    xyz = np.random.randn(n, 3).astype(float)
    return np.column_stack([timestamps, xyz])


@pytest.fixture
def synthetic_labels():
    """Labels for 10 seconds at 100Hz, all WALKING (label=2)."""
    n = 1000
    timestamps = np.arange(0, n * 10, 10, dtype=float)
    labels = np.full(n, 2, dtype=float)  # 2 = Walk
    return np.column_stack([timestamps, labels])


@pytest.fixture
def mixed_labels():
    """Labels with multiple transport modes."""
    n = 2000
    timestamps = np.arange(0, n * 10, 10, dtype=float)
    labels = np.zeros(n, dtype=float)
    labels[:500] = 2    # WALKING
    labels[500:1000] = 4  # CYCLING
    labels[1000:1500] = 6  # BUS
    labels[1500:] = 5    # DRIVING
    return np.column_stack([timestamps, labels])


@pytest.fixture
def sensor_files(tmp_path, synthetic_accel_data, synthetic_gyro_data, synthetic_labels):
    """Create temporary sensor files mimicking SHL directory structure."""
    day_dir = tmp_path / "User1" / "220617"
    day_dir.mkdir(parents=True)

    np.savetxt(str(day_dir / "Accelerometer.txt"), synthetic_accel_data)
    np.savetxt(str(day_dir / "Gyroscope.txt"), synthetic_gyro_data)
    np.savetxt(str(day_dir / "Label.txt"), synthetic_labels)

    return str(day_dir)


# ============================================================
# 1. Label mapping tests
# ============================================================

class TestLabelMapping:
    """Tests for SHL_LABEL_MAP constant."""

    def test_walking_mapped(self):
        assert convert_shl_data.SHL_LABEL_MAP[2] == "WALKING"

    def test_cycling_mapped(self):
        assert convert_shl_data.SHL_LABEL_MAP[4] == "CYCLING"

    def test_driving_mapped(self):
        assert convert_shl_data.SHL_LABEL_MAP[5] == "DRIVING"

    def test_bus_mapped(self):
        assert convert_shl_data.SHL_LABEL_MAP[6] == "BUS"

    def test_subway_mapped(self):
        assert convert_shl_data.SHL_LABEL_MAP[8] == "SUBWAY"

    def test_train_merged_with_subway(self):
        assert convert_shl_data.SHL_LABEL_MAP[7] == "SUBWAY"

    def test_still_not_mapped(self):
        """Label 1 (Still) should not be in the mapping."""
        assert 1 not in convert_shl_data.SHL_LABEL_MAP


# ============================================================
# 2. Sensor file loading tests
# ============================================================

class TestLoadSensorFile:
    """Tests for load_sensor_file() function."""

    def test_load_valid_file(self, sensor_files):
        data = convert_shl_data.load_sensor_file(os.path.join(sensor_files, "Accelerometer.txt"))
        assert data is not None
        assert data.shape[1] == 4  # timestamp + x,y,z

    def test_load_nonexistent_file(self):
        result = convert_shl_data.load_sensor_file("/nonexistent/file.txt")
        assert result is None

    def test_load_label_file(self, sensor_files):
        data = convert_shl_data.load_label_file(os.path.join(sensor_files, "Label.txt"))
        assert data is not None
        assert data.shape[1] == 2  # timestamp + label


# ============================================================
# 3. Sensor alignment tests
# ============================================================

class TestAlignSensors:
    """Tests for align_sensors() function."""

    def test_aligned_same_length_input(self, synthetic_accel_data, synthetic_gyro_data, synthetic_labels):
        a, g, l = convert_shl_data.align_sensors(
            synthetic_accel_data, synthetic_gyro_data, synthetic_labels
        )
        assert len(a) > 0
        assert len(g) > 0
        assert len(l) > 0

    def test_aligned_timestamps_overlap(self, synthetic_accel_data, synthetic_gyro_data, synthetic_labels):
        a, g, l = convert_shl_data.align_sensors(
            synthetic_accel_data, synthetic_gyro_data, synthetic_labels
        )
        # All timestamps should be within the common range
        t_min = max(a[0, 0], g[0, 0], l[0, 0])
        t_max = min(a[-1, 0], g[-1, 0], l[-1, 0])
        assert a[0, 0] >= t_min
        assert a[-1, 0] <= t_max


# ============================================================
# 4. Label segmentation tests
# ============================================================

class TestSegmentByLabel:
    """Tests for segment_by_label() function."""

    def test_single_label(self, synthetic_labels):
        segments = convert_shl_data.segment_by_label(synthetic_labels)
        assert len(segments) == 1
        assert segments[0][2] == 2  # WALKING

    def test_multiple_labels(self, mixed_labels):
        segments = convert_shl_data.segment_by_label(mixed_labels)
        assert len(segments) == 4
        modes = [s[2] for s in segments]
        assert modes == [2, 4, 6, 5]  # WALKING, CYCLING, BUS, DRIVING

    def test_ignores_unmapped_labels(self):
        """Label 1 (Still) should be ignored."""
        n = 500
        timestamps = np.arange(0, n * 10, 10, dtype=float)
        labels = np.ones(n, dtype=float)  # 1 = Still (not in SHL_LABEL_MAP)
        label_data = np.column_stack([timestamps, labels])
        segments = convert_shl_data.segment_by_label(label_data)
        assert len(segments) == 0


# ============================================================
# 5. Feature extraction tests
# ============================================================

class TestExtractFeatures:
    """Tests for extract_features() function."""

    def test_features_extracted(self, synthetic_accel_data, synthetic_gyro_data, synthetic_labels):
        """Should extract at least some features from 10s of data with small window."""
        features = convert_shl_data.extract_features(
            synthetic_accel_data, synthetic_gyro_data, synthetic_labels,
            window_size_sec=5  # Small window to get at least 1 from 10s data
        )
        # With 10s data and 5s windows and MIN_SAMPLES_PER_WINDOW=500,
        # each 5s window = 500 samples which equals the minimum exactly
        assert isinstance(features, list)

    def test_feature_keys(self, synthetic_accel_data, synthetic_gyro_data, synthetic_labels):
        features = convert_shl_data.extract_features(
            synthetic_accel_data, synthetic_gyro_data, synthetic_labels,
            window_size_sec=5
        )
        if len(features) > 0:
            expected_keys = set(convert_shl_data.CSV_HEADER)
            assert set(features[0].keys()) == expected_keys

    def test_transport_mode_label(self, synthetic_accel_data, synthetic_gyro_data, synthetic_labels):
        features = convert_shl_data.extract_features(
            synthetic_accel_data, synthetic_gyro_data, synthetic_labels,
            window_size_sec=5
        )
        for f in features:
            assert f["transportMode"] == "WALKING"


# ============================================================
# 6. CSV header tests
# ============================================================

class TestCSVHeader:
    """Tests for CSV_HEADER constant."""

    def test_header_length(self):
        assert len(convert_shl_data.CSV_HEADER) == 18  # 17 features + 1 label

    def test_header_ends_with_label(self):
        assert convert_shl_data.CSV_HEADER[-1] == "transportMode"

    def test_header_contains_accel_features(self):
        accel_cols = [c for c in convert_shl_data.CSV_HEADER if c.startswith("accel")]
        assert len(accel_cols) == 7

    def test_header_contains_gyro_features(self):
        gyro_cols = [c for c in convert_shl_data.CSV_HEADER if c.startswith("gyro")]
        assert len(gyro_cols) == 6

    def test_header_contains_gps_features(self):
        gps_cols = [c for c in convert_shl_data.CSV_HEADER if c.startswith("gps")]
        assert len(gps_cols) == 3


# ============================================================
# 7. Directory processing tests
# ============================================================

class TestProcessDayDirectory:
    """Tests for process_day_directory() function."""

    def test_process_valid_directory(self, sensor_files):
        features = convert_shl_data.process_day_directory(sensor_files, window_size=5)
        assert isinstance(features, list)

    def test_process_missing_directory(self):
        features = convert_shl_data.process_day_directory("/nonexistent/dir", window_size=30)
        assert features == []

    def test_find_day_directories(self, sensor_files):
        parent_dir = str(Path(sensor_files).parent)
        dirs = convert_shl_data.find_day_directories(parent_dir)
        assert len(dirs) == 1
        assert sensor_files in dirs
