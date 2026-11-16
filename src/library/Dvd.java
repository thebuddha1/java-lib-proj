package library;

public class Dvd extends Item {

    private final int minutes;
    private final int ageLimit;

    public Dvd(String id, String title, int year, int minutes, int ageLimit) {
        super(id, title, year);
        if (minutes <= 0) {
            throw new IllegalArgumentException("Game time cannot be negative");
        }
        if (ageLimit < 0 || ageLimit > 18) {
            throw new IllegalArgumentException("Age limit must be between 0 and 18");
        }
        this.minutes = minutes;
        this.ageLimit = ageLimit;
    }

    public int getMinutes() { return minutes; }

    public int getAgeLimit() { return ageLimit; }

    public boolean isSuitableFor(int age) {
        return age >= ageLimit;
    }

    @Override
    public String type() { return "DVD"; }

    @Override
    public int loanDays() { return 7; }

    @Override
    public int dailyLateFee() { return 200; }

    @Override
    protected String csvTail() {
        return minutes + ";" + ageLimit;
    }

    @Override
    public String describe() {
        return String.format("DVD: %s, %d. (%d minutes, pegi %d, %d days untill return)",
                getTitle(), getYear(), minutes, ageLimit, loanDays());
    }
}
