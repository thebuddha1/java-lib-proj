package library;


public interface Chargeable {

    int LATE_FEE_CAP = 30;

    int dailyLateFee();

    int loanDays();

    default int lateFee(long daysLate) {
        if (daysLate <= 0) {
            return 0;
        }
        return (int) Math.min(daysLate * dailyLateFee(), LATE_FEE_CAP);
    }

    static String formatMoney(int amount) {
        return String.format("%,d Usd", amount).replace(',', ' ');
    }
}
