package ru.malkov.renue;

import com.github.davidmoten.bigsorter.Serializer;
import com.github.davidmoten.bigsorter.Sorter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {

    public static boolean containPrefix(String string, String prefix) {
        if (prefix.length() + 1 > string.length()) return false;
        String substring = string.substring(1, prefix.length() + 1);
        return substring.equals(prefix);
    }

    public static void sort(int column, String filename) {
        Serializer<CSVRecord> serializer =
                Serializer.csv(
                        CSVFormat.DEFAULT.withQuote(null),
                        StandardCharsets.UTF_8);
        Comparator<CSVRecord> comparator = (x, y) -> {
            String a = x.get(column - 1);
            String b = y.get(column - 1);
            return CharSequence.compare(a, b);
        };
        Sorter
                .serializer(serializer)
                .comparator(comparator)
                .input(new File(filename))
                .output(new File("sort" + filename))
                .sort();
    }

    public static List<String> search(String prefix, int column, String filename) {
        List<String> results = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r");) {
            long bottom = raf.length();
            long top = 0;
            long middle;
            while (bottom >= top) {
                middle = (bottom + top) / 2;
                raf.seek(middle);
                String[] line = raf.readLine().split(",");
                if (line.length != 14) {
                    line = raf.readLine().split(",");
                }
                int comparison = line[column - 1].substring(1, 2).compareTo(prefix.substring(0, 1));
                if (comparison == 0) {
                    for (int i = 2; i <= prefix.length(); i++) {
                        comparison = line[column - 1].substring(1, i + 1).compareTo(prefix.substring(0, i));
                        if (comparison != 0) break;
                    }
                }
                if (comparison == 0) {
                    while (containPrefix(line[column - 1], prefix)) {
                        middle -= 100;
                        raf.seek(middle);
                        line = raf.readLine().split(",");
                        if (line.length != 14) {
                            line = raf.readLine().split(",");
                        }
                    }
                    while (!containPrefix(line[column - 1], prefix)) {
                        line = raf.readLine().split(",");
                    }
                    while (containPrefix(line[column - 1], prefix)) {
                        results.add(line[column - 1] + Arrays.toString(line).replace(line[column - 1], ""));
                        line = raf.readLine().split(",");
                    }
                    break;
                } else if (comparison > 0) {
                    bottom = raf.getFilePointer();
                } else {
                    top = raf.getFilePointer();
                }
            }
        } catch (Exception e) {
        }
        return results;
    }


    public static void main(String[] args) throws ConfigurationException, IOException {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.load("application.yml");
        int column = config.getInt("column");
        System.out.println("Введите строку:");
        Scanner scannerPrefix = new Scanner(System.in);
        String prefix = scannerPrefix.next();
        scannerPrefix.close();

        sort(column, "airports.dat");

        long start = System.currentTimeMillis();
        List<String> results = search(prefix, column, "sortairports.dat");
        long finish = System.currentTimeMillis();
        for (String row : results) {
            System.out.println(row);
        }
        System.out.println("Количество найденных строк: " + results.size()
                + " Время, затраченное на поиск: " + (finish - start) + " мс");
    }


}
