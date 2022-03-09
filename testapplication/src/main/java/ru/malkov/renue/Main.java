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
import java.util.function.Function;

public class Main {

    private static boolean containPrefix(String string, String prefix) {
        if (prefix.length() + 1 > string.length()) return false;
        String substring = string.substring(1, prefix.length() + 1);
        return substring.equals(prefix);
    }

    private static void sort(int column, String filename) {
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

    private static long getPreviousLineStart(long lineStart, RandomAccessFile raf) throws IOException {
        lineStart -= 2;
        raf.seek(lineStart);
        byte b = raf.readByte();
        while (b != 10) {
            lineStart--;
            raf.seek(lineStart);
            b = raf.readByte();
        }

        return lineStart;
    }

    private static List<String> search(String prefix, int column, String filename) {
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
                    long point = raf.getFilePointer();
                    middle = getPreviousLineStart(raf.getFilePointer(), raf);
                    results.add(line[column - 1] + Arrays.toString(line).replace(line[column - 1], ""));
                    boolean isAnyTopMatches = true;
                    boolean isAnyBottomMatches = true;
                    while (isAnyBottomMatches || isAnyTopMatches) {
                        if(isAnyTopMatches) {
                            middle = getPreviousLineStart(middle, raf) + 1;
                            raf.seek(middle);
                            line = raf.readLine().split(",");
                            isAnyTopMatches = containPrefix(line[column - 1], prefix);
                            if (isAnyTopMatches) {
                                results.add(line[column - 1] + Arrays.toString(line).replace(line[column - 1], ""));
                            }
                        }
                        if(isAnyBottomMatches) {
                            raf.seek(point);
                            line = raf.readLine().split(",");
                            isAnyBottomMatches = containPrefix(line[column - 1], prefix);
                            if (isAnyBottomMatches) {
                                results.add(line[column - 1] + Arrays.toString(line).replace(line[column - 1], ""));
                            }
                            point=raf.getFilePointer();
                        }
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
        results.sort(Comparator.comparing(Function.identity()));
        for (String row : results) {
            System.out.println(row);
        }
        System.out.println("Количество найденных строк: " + results.size()
                + " Время, затраченное на поиск: " + (finish - start) + " мс");
    }


}
