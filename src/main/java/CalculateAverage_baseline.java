import static java.util.stream.Collectors.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.TreeMap;
import java.util.stream.Collector;

public class CalculateAverage_baseline {

    private static final String MEASUREMENT_FILE = "./measurements.txt";
    private static final Path AVERAGE_BASELINE_FILE = Path.of("./calculated_average_baseline.txt");

    private record Measurement(String station, double value) {
        private Measurement(String[] parts) {
            this(parts[0], Double.parseDouble(parts[1]));
        }
    }

    private record ResultRow(double min, double mean, double max) {

        public String toString() {
            return round(min) + "/" + round(mean) + "/" + round(max);
        }

        private double round(double value) {
            return Math.round(value * 10.0) / 10.0;
        }
    }

    private static class MeasurementAggregator {
        private double min = Double.POSITIVE_INFINITY;
        private double max = Double.NEGATIVE_INFINITY;
        private double sum;
        private long count;
    }

    public static void main(String[] args) throws IOException {
        Collector<Measurement, MeasurementAggregator, ResultRow> collector = Collector.of(
                MeasurementAggregator::new,
                (a, m) -> {
                    a.min = Math.min(a.min, m.value);
                    a.max = Math.max(a.max, m.value);
                    a.sum += m.value;
                    a.count++;
                },
                (agg1, agg2) -> {
                    var res = new MeasurementAggregator();
                    res.min = Math.min(agg1.min, agg2.min);
                    res.max = Math.max(agg1.max, agg2.max);
                    res.sum = agg1.sum + agg2.sum;
                    res.count = agg1.count + agg2.count;

                    return res;
                },
                agg -> new ResultRow(agg.min, (Math.round(agg.sum * 10.0) / 10.0) / agg.count, agg.max));

        try {
            Files.deleteIfExists(AVERAGE_BASELINE_FILE);
            Files.createFile(AVERAGE_BASELINE_FILE);
        }
        catch (Exception e) {
            System.out.println("Error with deletion/creation of calculated_average_baseline.txt");
        }

        try(
                var measurements = Files.lines(Paths.get(MEASUREMENT_FILE));
                BufferedWriter bw = Files.newBufferedWriter(AVERAGE_BASELINE_FILE, StandardOpenOption.APPEND)
        ) {
            bw.write(
                    new TreeMap<>(measurements
                            .map(l -> new Measurement(l.split(";")))
                            .collect(groupingBy(Measurement::station, collector))).toString()
            );
        }
    }
}