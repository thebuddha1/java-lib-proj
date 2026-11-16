package library;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            if (args.length > 0 && args[0].equalsIgnoreCase("demo")) {
                runDemo();
            } else {
                runMenu();
            }
        } catch (Library.LibraryException | IOException e) {
            System.err.println("Fatal Error: " + e.getMessage());
        }
    }

    private static Library createSeededLibrary() throws Library.LibraryException {
        Library lib = new Library("City Library");

        lib.addItem(new Book("B001", "Egri csillagok", 1901, "Gárdonyi Géza", Book.Genre.FICTION, 520));
        lib.addItem(new Book("B002", "A Pál utcai fiúk", 1906, "Molnár Ferenc", Book.Genre.CHILDREN, 240));
        lib.addItem(new Book("B003", "Algoritmusok és adatszerkezetek", 2009, "Thomas H. Cormen", Book.Genre.TEXTBOOK, 1292));
        lib.addItem(new Book("B004", "Az idő rövid története", 1988, "Stephen Hawking", Book.Genre.SCIENCE, 256));
        lib.addItem(new Book("B005", "Effective Java", 2018, "Joshua Bloch", Book.Genre.TEXTBOOK, 416));
        lib.addItem(new Dvd("D001", "Interstellar", 2014, 169, 12));
        lib.addItem(new Dvd("D002", "Ponyvaregény", 1994, 154, 18));
        lib.addItem(new Dvd("D003", "A dzsungel könyve", 1967, 78, 0));

        lib.addMember(new Member("M01", "Kiss Anna", LocalDate.of(2003, 5, 14), Member.Tier.STUDENT));
        lib.addMember(new Member("M02", "Nagy Béla", LocalDate.of(2012, 9, 2), Member.Tier.STANDARD));
        lib.addMember(new Member("M03", "Tóth Cecília", LocalDate.of(1985, 1, 30), Member.Tier.PREMIUM));
        return lib;
    }

    
    private static void runDemo() throws Library.LibraryException, IOException {
        Library lib = createSeededLibrary();
        LocalDate start = LocalDate.of(2025, 3, 1);

        title("1. Stock (Natural order: by title)");
        for (Item item : lib.sortedBy(Comparator.naturalOrder())) {
            System.out.println(item);
        }

        title("2. Polimorphism: descriptions");
        for (Item item : lib) {
            System.out.println(item.describe());
        }

        title("3. Successful loans");
        tryBorrow(lib, "M01", "B003", start);
        tryBorrow(lib, "M02", "B002", start);
        tryBorrow(lib, "M03", "D001", start);

        title("4. Unsuccessful loans (exception handling)");
        tryBorrow(lib, "M02", "D002", start);
        tryBorrow(lib, "M03", "B003", start);
        tryBorrow(lib, "M09", "B001", start);
        tryBorrow(lib, "M02", "B001", start);
        tryBorrow(lib, "M02", "B004", start);
        tryBorrow(lib, "M02", "B005", start);

        title("5. Overdue loans 2025-03-20");
        LocalDate later = LocalDate.of(2025, 3, 20);
        lib.overdueLoans(later).forEach(l ->
                System.out.println(l + " | late by: " + l.daysLate(later) + " day(s)"));

        title("6. Return date and late fee");
        giveBack(lib, "B003", LocalDate.of(2025, 3, 25));
        giveBack(lib, "D001", LocalDate.of(2025, 3, 10));
        giveBack(lib, "B002", LocalDate.of(2025, 3, 15));
        giveBack(lib, "B002", LocalDate.of(2025, 3, 15));

        title("7. Search and filter");
        System.out.println("Search „java”: " + lib.search("java"));
        System.out.println("Search „géza”: " + lib.search("géza"));
        System.out.println("Textbooks: ");
        lib.booksByGenre(Book.Genre.TEXTBOOK).forEach(b -> System.out.println("  " + b));

        title("8. Save and load from file");
        Path tmp = Files.createTempFile("catalog", ".csv");
        lib.saveCatalog(tmp);
        Library copy = new Library("Copy");
        int loaded = copy.loadCatalog(tmp);
        System.out.println("Loaded: " + loaded + " (all inistances of Items: "
                + Item.getCreatedCount() + ")");
        Files.deleteIfExists(tmp);

        title("9. Report");
        System.out.print(lib.report(LocalDate.of(2025, 3, 26)));
    }

    private static void runMenu() throws Library.LibraryException, IOException {
        Library lib = createSeededLibrary();
        Scanner in = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("""

                    ===== %s =====
                    1) List stock              2) Search
                    3) Loan                    4) Return
                    5) Members                 6) Overdue Loans
                    7) Report                  8) Run Demo
                    0) Exit""".formatted(lib.getName()));
            System.out.print("Select: ");
            String choice = in.nextLine().trim();
            LocalDate today = LocalDate.now();

            switch (choice) {
                case "1" -> lib.forEach(System.out::println);
                case "2" -> {
                    System.out.print("Key word: ");
                    List<Item> found = lib.search(in.nextLine().trim());
                    if (found.isEmpty()) System.out.println("No such entry.");
                    found.forEach(i -> System.out.println(i.describe()));
                }
                case "3" -> {
                    System.out.print("Member id: ");
                    String m = in.nextLine().trim();
                    System.out.print("Document id: ");
                    String i = in.nextLine().trim();
                    tryBorrow(lib, m, i, today);
                }
                case "4" -> {
                    System.out.print("Document id: ");
                    giveBack(lib, in.nextLine().trim(), today);
                }
                case "5" -> lib.getMembers().stream()
                        .sorted(Comparator.comparing(Member::getId))
                        .forEach(System.out::println);
                case "6" -> {
                    List<Loan> overdue = lib.overdueLoans(today);
                    if (overdue.isEmpty()) System.out.println("No overdue loans.");
                    overdue.forEach(System.out::println);
                }
                case "7" -> System.out.print(lib.report(today));
                case "8" -> runDemo();
                case "0" -> running = false;
                default -> System.out.println("No such option!");
            }
        }
        System.out.println("Goodbye!");
        in.close();
    }

    private static void tryBorrow(Library lib, String memberId, String itemId, LocalDate date) {
        try {
            Loan loan = lib.borrow(memberId, itemId, date);
            System.out.println("OK:   " + loan);
        } catch (Library.LibraryException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private static void giveBack(Library lib, String itemId, LocalDate date) {
        try {
            int fee = lib.giveBack(itemId, date);
            System.out.println("OK:   " + itemId + " returned (" + date + "), late fee: "
                    + Chargeable.formatMoney(fee));
        } catch (Library.LibraryException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private static void title(String text) {
        System.out.println("\n--- " + text + " ---");
    }
}
