package library;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

public abstract class Item implements Comparable<Item>, Chargeable {

    private static final Comparator<Item> NATURAL_ORDER =
            Comparator.comparing(Item::getTitle, String.CASE_INSENSITIVE_ORDER)
                      .thenComparing(Item::getId);

    private static int createdCount = 0;

    private final String id;
    private final String title;
    private final int year;
    private boolean available = true;

    protected Item(String id, String title, int year) {
        this.id = requireClean(id, "id");
        this.title = requireClean(title, "title");
        if (year < 1400 || year > LocalDate.now().getYear()) {
            throw new IllegalArgumentException("impossible release date: " + year);
        }
        this.year = year;
        createdCount++;
    }

    protected static String requireClean(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The " + fieldName + " fiel cannot be empty");
        }
        if (value.contains(";")) {
            throw new IllegalArgumentException("The " + fieldName + " field cannot contain the character ';' ");
        }
        return value.trim();
    }

    public abstract String type();

    protected abstract String csvTail();

    public abstract String describe();


    public final String toCsv() {
        return type() + ";" + id + ";" + title + ";" + year + ";" + csvTail();
    }

    public static Item fromCsv(String line) {
        String[] p = line.split(";");
        try {
            return switch (p[0]) {
                case "BOOK" -> new Book(p[1], p[2], Integer.parseInt(p[3]), p[4],
                        Book.Genre.valueOf(p[5]), Integer.parseInt(p[6]));
                case "DVD" -> new Dvd(p[1], p[2], Integer.parseInt(p[3]),
                        Integer.parseInt(p[4]), Integer.parseInt(p[5]));
                default -> throw new IllegalArgumentException("Unknown type: " + p[0]);
            };
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException("Wrong CSV line: " + line, e);
        }
    }

    public static int getCreatedCount() {
        return createdCount;
    }


    public String getId() { return id; }

    public String getTitle() { return title; }

    public int getYear() { return year; }

    public boolean isAvailable() { return available; }

    void setAvailable(boolean available) { this.available = available; }


    @Override
    public int compareTo(Item other) {
        return NATURAL_ORDER.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %-6s %-34s (%d) %s",
                type(), id, title, year, available ? "available" : "loaned");
    }
}
