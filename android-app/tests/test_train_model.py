#!/usr/bin/env python3
"""
pytest tests for train_model.py

Covers:
- Data loading & validation
- Feature preprocessing (StandardScaler)
- Model building & architecture
- Model training (on synthetic data)
- Model evaluation metrics
- TFLite conversion
"""

import os
import sys
import tempfile
from pathlib import Path

import numpy as np
import pandas as pd
import pytest

# Add parent directory to path so we can import train_model
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import train_model


# ============================================================
# Fixtures: reusable test data
# ============================================================

@pytest.fixture
def sample_csv(tmp_path):
    """Create a small synthetic CSV for testing."""
    np.random.seed(42)
    rows = []
    for mode in train_model.TRANSPORT_MODES:
        for _ in range(30):
            row = {col: float(np.random.randn()) for col in train_model.FEATURE_COLUMNS}
            row[train_model.TARGET_COLUMN] = mode
            rows.append(row)
    df = pd.DataFrame(rows)
    csv_path = tmp_path / "test_data.csv"
    df.to_csv(csv_path, index=False)
    return str(csv_path)


@pytest.fixture
def sample_csv_with_missing(tmp_path):
    """Create a CSV that contains some NaN values."""
    np.random.seed(42)
    rows = []
    for mode in train_model.TRANSPORT_MODES:
        for _ in range(20):
            row = {col: float(np.random.randn()) for col in train_model.FEATURE_COLUMNS}
            row[train_model.TARGET_COLUMN] = mode
            rows.append(row)
    df = pd.DataFrame(rows)
    # Inject NaN values
    df.iloc[0, 0] = np.nan
    df.iloc[5, 3] = np.nan
    csv_path = tmp_path / "test_data_missing.csv"
    df.to_csv(csv_path, index=False)
    return str(csv_path)


@pytest.fixture
def synthetic_dataset():
    """Generate synthetic feature arrays and labels for 5 classes."""
    np.random.seed(42)
    n_per_class = 40
    X_list, y_list = [], []
    for i in range(train_model.OUTPUT_CLASSES):
        X_list.append(np.random.randn(n_per_class, train_model.INPUT_FEATURES).astype(np.float32) + i)
        y_list.append(np.full(n_per_class, i))
    X = np.vstack(X_list)
    y = np.concatenate(y_list)
    return X, y


# ============================================================
# 1. Data loading tests
# ============================================================

class TestLoadData:
    """Tests for the load_data() function."""

    def test_load_valid_csv(self, sample_csv):
        """Should load CSV and return correct shapes."""
        X, y = train_model.load_data(sample_csv)
        assert X.shape == (150, train_model.INPUT_FEATURES)
        assert y.shape == (150,)

    def test_labels_in_range(self, sample_csv):
        """All labels should be valid class indices 0-4."""
        _, y = train_model.load_data(sample_csv)
        assert set(y).issubset({0, 1, 2, 3, 4})

    def test_feature_dtype(self, sample_csv):
        """Feature array should be float32."""
        X, _ = train_model.load_data(sample_csv)
        assert X.dtype == np.float32

    def test_each_class_represented(self, sample_csv):
        """Each transport mode should have samples."""
        _, y = train_model.load_data(sample_csv)
        for i in range(train_model.OUTPUT_CLASSES):
            assert np.sum(y == i) > 0, f"Class {i} ({train_model.TRANSPORT_MODES[i]}) has no samples"

    def test_handles_missing_values(self, sample_csv_with_missing):
        """Should drop rows with NaN and still return valid data."""
        X, y = train_model.load_data(sample_csv_with_missing)
        assert not np.isnan(X).any()
        assert len(X) == len(y)

    def test_nonexistent_file_raises(self):
        """Should raise an error for missing file."""
        with pytest.raises(Exception):
            train_model.load_data("/nonexistent/path/data.csv")


# ============================================================
# 2. Preprocessing tests
# ============================================================

class TestPreprocessFeatures:
    """Tests for the preprocess_features() function."""

    def test_output_shapes_match_input(self, synthetic_dataset):
        X, _ = synthetic_dataset
        split = int(len(X) * 0.8)
        X_train, X_test = X[:split], X[split:]
        X_train_s, X_test_s, scaler = train_model.preprocess_features(X_train, X_test)
        assert X_train_s.shape == X_train.shape
        assert X_test_s.shape == X_test.shape

    def test_train_mean_near_zero(self, synthetic_dataset):
        """After scaling, training data mean should be ~0."""
        X, _ = synthetic_dataset
        split = int(len(X) * 0.8)
        X_train_s, _, _ = train_model.preprocess_features(X[:split], X[split:])
        assert abs(X_train_s.mean()) < 0.1

    def test_train_std_near_one(self, synthetic_dataset):
        """After scaling, training data std should be ~1."""
        X, _ = synthetic_dataset
        split = int(len(X) * 0.8)
        X_train_s, _, _ = train_model.preprocess_features(X[:split], X[split:])
        assert abs(X_train_s.std() - 1.0) < 0.2

    def test_scaler_is_fitted(self, synthetic_dataset):
        X, _ = synthetic_dataset
        split = int(len(X) * 0.8)
        _, _, scaler = train_model.preprocess_features(X[:split], X[split:])
        assert hasattr(scaler, 'mean_')
        assert len(scaler.mean_) == train_model.INPUT_FEATURES


# ============================================================
# 3. Model architecture tests
# ============================================================

class TestBuildModel:
    """Tests for the build_model() function."""

    def test_model_is_created(self):
        model = train_model.build_model()
        assert model is not None

    def test_input_shape(self):
        model = train_model.build_model()
        input_shape = model.input_shape
        assert input_shape[-1] == train_model.INPUT_FEATURES

    def test_output_shape(self):
        model = train_model.build_model()
        output_shape = model.output_shape
        assert output_shape[-1] == train_model.OUTPUT_CLASSES

    def test_output_activation_is_softmax(self):
        """Output layer should use softmax for probability distribution."""
        model = train_model.build_model()
        last_layer = model.layers[-1]
        activation_name = last_layer.get_config().get('activation', '')
        assert activation_name == 'softmax'

    def test_model_has_multiple_layers(self):
        model = train_model.build_model()
        assert len(model.layers) > 3

    def test_model_compiles_successfully(self):
        """Model should compile without errors (already compiled in build_model)."""
        model = train_model.build_model()
        assert model.optimizer is not None


# ============================================================
# 4. Training tests
# ============================================================

class TestTrainModel:
    """Tests for the train_model() function."""

    def test_training_runs(self, synthetic_dataset):
        """Model should train without errors on synthetic data."""
        X, y = synthetic_dataset
        split = int(len(X) * 0.8)
        X_train, X_val = X[:split], X[split:]
        y_train, y_val = y[:split], y[split:]

        model = train_model.build_model()
        history = train_model.train_model(model, X_train, y_train, X_val, y_val, epochs=3, batch_size=8)
        assert history is not None

    def test_loss_decreases(self, synthetic_dataset):
        """Training loss should decrease over epochs."""
        X, y = synthetic_dataset
        split = int(len(X) * 0.8)
        model = train_model.build_model()
        history = train_model.train_model(model, X[:split], y[:split], X[split:], y[split:], epochs=10, batch_size=8)
        losses = history.history['loss']
        assert losses[-1] < losses[0], "Loss should decrease during training"

    def test_history_contains_expected_keys(self, synthetic_dataset):
        X, y = synthetic_dataset
        split = int(len(X) * 0.8)
        model = train_model.build_model()
        history = train_model.train_model(model, X[:split], y[:split], X[split:], y[split:], epochs=3, batch_size=8)
        assert 'loss' in history.history
        assert 'accuracy' in history.history
        assert 'val_loss' in history.history
        assert 'val_accuracy' in history.history


# ============================================================
# 5. Evaluation tests
# ============================================================

class TestEvaluateModel:
    """Tests for the evaluate_model() function."""

    @pytest.fixture
    def trained_model_and_test(self):
        """Train a model with well-separated data for reliable evaluation."""
        np.random.seed(42)
        n_per_class = 100
        X_list, y_list = [], []
        for i in range(train_model.OUTPUT_CLASSES):
            # Very well-separated clusters (offset by 5) so model learns all classes
            X_list.append(np.random.randn(n_per_class, train_model.INPUT_FEATURES).astype(np.float32) * 0.3 + i * 5)
            y_list.append(np.full(n_per_class, i))
        X = np.vstack(X_list)
        y = np.concatenate(y_list)
        split = int(len(X) * 0.8)
        model = train_model.build_model()
        # Train directly without EarlyStopping to ensure model converges
        model.fit(X[:split], y[:split], epochs=50, batch_size=16, verbose=0)
        return model, X[split:], y[split:]

    def test_evaluation_returns_metrics(self, trained_model_and_test):
        model, X_test, y_test = trained_model_and_test
        results = train_model.evaluate_model(model, X_test, y_test)
        assert 'accuracy' in results
        assert 'confusion_matrix' in results
        assert 'report' in results

    def test_accuracy_is_valid(self, trained_model_and_test):
        model, X_test, y_test = trained_model_and_test
        results = train_model.evaluate_model(model, X_test, y_test)
        assert 0.0 <= results['accuracy'] <= 1.0

    def test_confusion_matrix_shape(self, trained_model_and_test):
        model, X_test, y_test = trained_model_and_test
        results = train_model.evaluate_model(model, X_test, y_test)
        cm = np.array(results['confusion_matrix'])
        assert cm.shape == (train_model.OUTPUT_CLASSES, train_model.OUTPUT_CLASSES)


# ============================================================
# 6. TFLite conversion tests
# ============================================================

class TestTFLiteConversion:
    """Tests for the save_model_as_tflite() function."""

    def test_tflite_file_created(self, synthetic_dataset, tmp_path):
        """Should successfully create a .tflite file."""
        X, y = synthetic_dataset
        split = int(len(X) * 0.8)
        model = train_model.build_model()
        train_model.train_model(model, X[:split], y[:split], X[split:], y[split:], epochs=3, batch_size=8)

        output_path = str(tmp_path / "test_model.tflite")
        success = train_model.save_model_as_tflite(model, output_path)
        assert success is True
        assert os.path.exists(output_path)

    def test_tflite_file_not_empty(self, synthetic_dataset, tmp_path):
        X, y = synthetic_dataset
        split = int(len(X) * 0.8)
        model = train_model.build_model()
        train_model.train_model(model, X[:split], y[:split], X[split:], y[split:], epochs=3, batch_size=8)

        output_path = str(tmp_path / "test_model.tflite")
        train_model.save_model_as_tflite(model, output_path)
        assert os.path.getsize(output_path) > 0


# ============================================================
# 7. Constants / config validation tests
# ============================================================

class TestConstants:
    """Tests for module-level constants."""

    def test_transport_modes_count(self):
        assert len(train_model.TRANSPORT_MODES) == 5

    def test_transport_modes_values(self):
        expected = {"WALKING", "CYCLING", "BUS", "DRIVING", "SUBWAY"}
        assert set(train_model.TRANSPORT_MODES) == expected

    def test_feature_columns_count(self):
        assert len(train_model.FEATURE_COLUMNS) == train_model.INPUT_FEATURES

    def test_output_classes_matches_modes(self):
        assert train_model.OUTPUT_CLASSES == len(train_model.TRANSPORT_MODES)
