package com.example.EcoGo.service.churn.impl;

import com.example.EcoGo.service.churn.ModelRunner;
import ai.onnxruntime.*;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.Set;

@Component
public class OnnxModelRunner implements ModelRunner {

    private static final String MODEL_PATH = "/ml/churn_model.onnx";

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;
    private final String outputName;

    public OnnxModelRunner() {
        try {
            this.env = OrtEnvironment.getEnvironment();
            byte[] modelBytes = readAllBytesFromResource(MODEL_PATH);

            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            this.session = env.createSession(modelBytes, opts);

            Set<String> inputs = session.getInputNames();
            Set<String> outputs = session.getOutputNames();

            this.inputName = inputs.iterator().next();
            this.outputName = pickProbabilityOutputName(outputs);

            System.out.println(">>> OnnxModelRunner loaded, input=" + inputName + ", output=" + outputName + ", allOutputs=" + outputs);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ONNX model from " + MODEL_PATH, e);
        }
    }

    private static String pickProbabilityOutputName(Set<String> outputs) {
        for (String o : outputs) {
            String s = o.toLowerCase();
            if (s.contains("prob") || s.contains("proba") || s.contains("probabilities")) return o;
        }
        String last = null;
        for (String o : outputs) last = o;
        return last != null ? last : outputs.iterator().next();
    }

    @Override
    public double predictProbability(float[] features) {
        if (features == null || features.length == 0) return Double.NaN;

        try {
            long[] shape = new long[]{1, features.length};
            try (OnnxTensor inputTensor = createInputTensor(features, shape);
                 OrtSession.Result result = runSession(inputTensor)) {

                return extractProbabilityFromResult(result);
            }
        } catch (Exception e) {
            System.out.println(">>> OnnxModelRunner exception: " + e.getMessage());
            return Double.NaN;
        }
    }

    private OnnxTensor createInputTensor(float[] features, long[] shape) throws OrtException {
        FloatBuffer fb = FloatBuffer.wrap(features);
        return OnnxTensor.createTensor(env, fb, shape);
    }

    private OrtSession.Result runSession(OnnxTensor inputTensor) throws OrtException {
        return session.run(Map.of(inputName, inputTensor));
    }

    private double extractProbabilityFromResult(OrtSession.Result result) throws OrtException {
        // 1) Try preferred output name first
        double p = tryParseNamedOutput(result, outputName);
        if (!Double.isNaN(p)) return p;

        // 2) Fallback: scan all outputs
        return tryParseAnyOutput(result);
    }

    private static double tryParseNamedOutput(OrtSession.Result result, String name) throws OrtException {
        if (name == null || name.isBlank()) return Double.NaN;

        var opt = result.get(name);
        if (opt.isEmpty()) return Double.NaN;

        Object value = opt.get().getValue();
        return tryParseProbability(value);
    }

    private static double tryParseAnyOutput(OrtSession.Result result) throws OrtException {
        for (Map.Entry<String, OnnxValue> e : result) {
            double p = tryParseProbability(e.getValue().getValue());
            if (!Double.isNaN(p)) {
                System.out.println(">>> OnnxModelRunner fallback output used: " + e.getKey());
                return p;
            }
        }
        return Double.NaN;
    }

    private static double tryParseProbability(Object out) {
        if (out == null) return Double.NaN;

        // float[][] probs = [[p0, p1]] / [[p]]
        if (out instanceof float[][] probs && probs.length > 0) {
            float[] row = probs[0];
            return pickPositiveClass(row);
        }

        // float[] probs = [p0, p1] / [p]
        if (out instanceof float[] arr) {
            return pickPositiveClass(arr);
        }

        // double[][] / double[]
        if (out instanceof double[][] dprobs && dprobs.length > 0) {
            double[] row = dprobs[0];
            return pickPositiveClass(row);
        }
        if (out instanceof double[] darr) {
            return pickPositiveClass(darr);
        }

        // Map 输出（zipmap=True）
        if (out instanceof Map<?, ?> map) {
            for (Object v : map.values()) {
                if (v instanceof Number n) return clamp01(n.doubleValue());
            }
        }

        return Double.NaN;
    }

    private static double pickPositiveClass(float[] row) {
        if (row == null || row.length == 0) return Double.NaN;
        if (row.length == 1) return clamp01(row[0]);
        return clamp01(row[Math.min(1, row.length - 1)]);
    }

    private static double pickPositiveClass(double[] row) {
        if (row == null || row.length == 0) return Double.NaN;
        if (row.length == 1) return clamp01(row[0]);
        return clamp01(row[Math.min(1, row.length - 1)]);
    }

    private static double clamp01(double x) {
        if (x < 0) return 0;
        if (x > 1) return 1;
        return x;
    }

    private static byte[] readAllBytesFromResource(String classpath) throws Exception {
        try (InputStream is = OnnxModelRunner.class.getResourceAsStream(classpath)) {
            if (is == null) throw new IllegalStateException("Resource not found: " + classpath);
            return is.readAllBytes();
        }
    }
}
