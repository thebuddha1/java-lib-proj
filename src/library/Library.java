package library;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Library implements Iterable<Item> {

    public static class LibraryException extends Exception {
        private static final long serialVersionUID = 1L;

        public LibraryException(String message) {
            super(message);
        }

        public LibraryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final String name;
    private final Map<String, Item> items = new TreeMap<>();
    private final Map<String, Member> members = new HashMap<>();
    private final List<Loan> loans = new ArrayList<>();

    public Library(String name) {
        this.name = name;
    }

    public String getName() { return name; }


    public void addItem(Item item) throws LibraryException {
        if (items.putIfAbsent(item.getId(), item) != null) {
            throw new LibraryException("Document already exists with this id: " + item.getId());
        }
    }

    public void addMember(Member member) throws LibraryException {
        if (members.putIfAbsent(member.getId(), member) != null) {
            throw new LibraryException("Member already exists with this id: " + member.getId());
        }
    }

    public Optional<Item> findItem(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public Optional<Member> findMember(String id) {
        return Optional.ofNullable(members.get(id));
    }

    public Collection<Member> getMembers() {
        return Collections.unmodifiableCollection(members.values());
    }

    @Override
    public Iterator<Item> iterator() {
        return Collections.unmodifiableCollection(items.values()).iterator();
    }

    public Loan borrow(String memberId, String itemId, LocalDate date) throws LibraryException {
        Member member = findMember(memberId)
                .orElseThrow(() -> new LibraryException("No such member: " + memberId));
        Item item = findItem(itemId)
                .orElseThrow(() -> new LibraryException("No such document: " + itemId));

        if (!item.isAvailable()) {
            throw new LibraryException("„" + item.getTitle() + "” is currently loaned.");
        }
        if (!member.canBorrow()) {
            throw new LibraryException(member.getName() + " Loan limit reached ("
                    + member.getTier().getMaxLoans() + ").");
        }
        if (member.hasOverdue(date)) {
            throw new LibraryException(member.getName() + "'s loan date passed.");
        }
        if (item instanceof Dvd dvd && !dvd.isSuitableFor(member.ageOn(date))) {
            throw new LibraryException(member.getName() + " is not of „" + dvd.getTitle()
                    + "” age limit (" + dvd.getAgeLimit() + "+).");
        }

        Loan loan = new Loan(item, member, date);
        loans.add(loan);
        member.addLoan(loan);
        item.setAvailable(false);
        return loan;
    }

    public int giveBack(String itemId, LocalDate date) throws LibraryException {
        Loan loan = loans.stream()
                .filter(l -> l.getItem().getId().equals(itemId) && !l.isReturned())
                .findFirst()
                .orElseThrow(() -> new LibraryException("The " + itemId + " id document is not loaned."));
        try {
            int fee = loan.close(date);
            loan.getMember().removeLoan(loan, fee);
            loan.getItem().setAvailable(true);
            return fee;
        } catch (IllegalArgumentException e) {
            throw new LibraryException("Return failed: " + e.getMessage(), e);
        }
    }

    public List<Item> search(String keyword) {
        String k = keyword.toLowerCase();
        return items.values().stream()
                .filter(i -> i.getTitle().toLowerCase().contains(k)
                        || (i instanceof Book b && b.getAuthor().toLowerCase().contains(k)))
                .sorted()
                .collect(Collectors.toList());
    }

    public List<Book> booksByGenre(Book.Genre genre) {
        return items.values().stream()
                .filter(Book.class::isInstance)
                .map(Book.class::cast)
                .filter(b -> b.getGenre() == genre)
                .sorted()
                .toList();
    }

    public List<Item> sortedBy(Comparator<Item> comparator) {
        List<Item> copy = new ArrayList<>(items.values());
        copy.sort(comparator);
        return copy;
    }

    public List<Loan> overdueLoans(LocalDate date) {
        return loans.stream()
                .filter(l -> l.isOverdue(date))
                .sorted(Comparator.comparing(Loan::getDueDate))
                .toList();
    }

    public Map<String, Long> countByType() {
        return items.values().stream()
                .collect(Collectors.groupingBy(Item::type, TreeMap::new, Collectors.counting()));
    }

    public Optional<Item> mostBorrowedItem() {
        return loans.stream()
                .collect(Collectors.groupingBy(Loan::getItem, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<Item, Long>comparingByValue()
                        .thenComparing(e -> e.getKey().getTitle(), Comparator.reverseOrder()))
                .map(Map.Entry::getKey);
    }

    public int totalRevenue() {
        return loans.stream().mapToInt(Loan::getFeePaid).sum();
    }

    public String report(LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(name).append(" – stock (").append(date).append(") ===\n");
        sb.append("Stock: ").append(items.size()).append(countByType()).append('\n');
        sb.append("Members: ").append(members.size()).append(" member\n");
        sb.append("Active loans: ")
          .append(loans.stream().filter(l -> !l.isReturned()).count());
        sb.append(" Overdue loans: ").append(overdueLoans(date).size());
        sb.append(" Late fee: ").append(Chargeable.formatMoney(totalRevenue())).append('\n');
        mostBorrowedItem().ifPresent(i -> sb.append("Most popular: ").append(i.getTitle()).append('\n'));
        return sb.toString();
    }

    public void saveCatalog(Path file) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("# type;id;title;name;type specific data");
            out.newLine();
            for (Item item : items.values()) {
                out.write(item.toCsv());
                out.newLine();
            }
        }
    }

    public int loadCatalog(Path file) throws IOException, LibraryException {
        int count = 0;
        int lineNo = 0;
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                try {
                    addItem(Item.fromCsv(line));
                    count++;
                } catch (IllegalArgumentException e) {
                    throw new LibraryException(lineNo + ". line: " + e.getMessage(), e);
                }
            }
        }
        return count;
    }
}
