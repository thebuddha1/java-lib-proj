package library;


public class Book extends Item {

    public enum Genre {
        FICTION("Fiction", 0),
        CHILDREN("Childresn book", 7),
        SCIENCE("Scientific paper", 7),
        TEXTBOOK("Textbook", -14);

        private final String label;
        private final int loanDaysAdjustment;

        Genre(String label, int loanDaysAdjustment) {
            this.label = label;
            this.loanDaysAdjustment = loanDaysAdjustment;
        }

        public String getLabel() { return label; }

        public int getLoanDaysAdjustment() { return loanDaysAdjustment; }
    }

    private static final int BASE_LOAN_DAYS = 28;

    private final String author;
    private final Genre genre;
    private final int pages;

    public Book(String id, String title, int year, String author, Genre genre, int pages) {
        super(id, title, year);
        if (pages <= 0) {
            throw new IllegalArgumentException("Number of pages cannot be negative");
        }
        this.author = requireClean(author, "author");
        this.genre = java.util.Objects.requireNonNull(genre, "genre cannot be nol");
        this.pages = pages;
    }

    public String getAuthor() { return author; }

    public Genre getGenre() { return genre; }

    public int getPages() { return pages; }

    @Override
    public String type() { return "BOOK"; }

    @Override
    public int loanDays() {
        return BASE_LOAN_DAYS + genre.getLoanDaysAdjustment();
    }

    @Override
    public int dailyLateFee() {
        return pages > 500 ? 70 : 50;
    }

    @Override
    protected String csvTail() {
        return author + ";" + genre.name() + ";" + pages;
    }

    @Override
    public String describe() {
        return String.format("Book: %s - %s, %d. (%s, %d page, %d days untill return)",
                author, getTitle(), getYear(), genre.getLabel(), pages, loanDays());
    }
}
