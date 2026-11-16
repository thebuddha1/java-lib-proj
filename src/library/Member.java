package library;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Member {

    public enum Tier {
        STANDARD("Normal", 3) {
            @Override
            public int applyDiscount(int fee) { return fee; }
        },
        STUDENT("Student", 4) {
            @Override
            public int applyDiscount(int fee) { return fee * 75 / 100; }
        },
        PREMIUM("Premium", 6) {
            @Override
            public int applyDiscount(int fee) { return fee / 2; }
        };

        private final String label;
        private final int maxLoans;

        Tier(String label, int maxLoans) {
            this.label = label;
            this.maxLoans = maxLoans;
        }

        public String getLabel() { return label; }

        public int getMaxLoans() { return maxLoans; }

        public abstract int applyDiscount(int fee);
    }

    private final String id;
    private final String name;
    private final LocalDate birthDate;
    private final Tier tier;
    private final List<Loan> activeLoans = new ArrayList<>();
    private int totalFeesPaid = 0;

    public Member(String id, String name, LocalDate birthDate, Tier tier) {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Id and name are required!");
        }
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Invalid birth date!");
        }
        this.id = id;
        this.name = name;
        this.birthDate = birthDate;
        this.tier = tier == null ? Tier.STANDARD : tier;
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public Tier getTier() { return tier; }

    public int getTotalFeesPaid() { return totalFeesPaid; }

    public int ageOn(LocalDate date) {
        return Period.between(birthDate, date).getYears();
    }

    public List<Loan> getActiveLoans() {
        return Collections.unmodifiableList(activeLoans);
    }

    public boolean canBorrow() {
        return activeLoans.size() < tier.getMaxLoans();
    }

    public boolean hasOverdue(LocalDate date) {
        return activeLoans.stream().anyMatch(l -> l.isOverdue(date));
    }

    void addLoan(Loan loan) { activeLoans.add(loan); }

    void removeLoan(Loan loan, int feePaid) {
        activeLoans.remove(loan);
        totalFeesPaid += feePaid;
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %s, %d/%d loan)",
                name, id, tier.getLabel(), activeLoans.size(), tier.getMaxLoans());
    }
}
