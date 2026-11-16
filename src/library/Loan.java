package library;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Loan {

    private final Item item;
    private final Member member;
    private final LocalDate loanDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;
    private int feePaid;

    Loan(Item item, Member member, LocalDate loanDate) {
        this.item = item;
        this.member = member;
        this.loanDate = loanDate;
        this.dueDate = loanDate.plusDays(item.loanDays());
    }

    public Item getItem() { return item; }

    public Member getMember() { return member; }

    public LocalDate getLoanDate() { return loanDate; }

    public LocalDate getDueDate() { return dueDate; }

    public LocalDate getReturnDate() { return returnDate; }

    public int getFeePaid() { return feePaid; }

    public boolean isReturned() { return returnDate != null; }

    public long daysLate(LocalDate asOf) {
        LocalDate end = isReturned() ? returnDate : asOf;
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, end));
    }

    public boolean isOverdue(LocalDate asOf) {
        return !isReturned() && asOf.isAfter(dueDate);
    }

    int close(LocalDate date) {
        if (isReturned()) {
            throw new IllegalStateException("This loan is already concluded");
        }
        if (date.isBefore(loanDate)) {
            throw new IllegalArgumentException("Return date cannot predate loandate");
        }
        this.returnDate = date;
        this.feePaid = member.getTier().applyDiscount(item.lateFee(daysLate(date)));
        return feePaid;
    }

    @Override
    public String toString() {
        return String.format("%s ← %s | loandate: %s, returndate: %s%s",
                item.getTitle(), member.getName(), loanDate, dueDate,
                isReturned() ? ", returned: " + returnDate : "");
    }
}
