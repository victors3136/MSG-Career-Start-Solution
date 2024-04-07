package services;

import domain.SavingsAccountModel;
import repository.AccountsRepository;

import java.time.LocalDate;
import java.util.List;

public class SavingsManagerService {
    private static LocalDate systemDate = LocalDate.now();

    public static void passTime() {
        List<SavingsAccountModel> savingAccounts = AccountsRepository.INSTANCE.getAll().stream()
                .filter(account -> account instanceof SavingsAccountModel)
                .map(account -> (SavingsAccountModel) account).toList();

        LocalDate nextSystemDate = systemDate.plusMonths(1);

        savingAccounts.forEach(savingAccount -> addInterestIfDue(savingAccount, nextSystemDate));

        systemDate = nextSystemDate;
    }

    private static void addInterestIfDue(SavingsAccountModel savingAccount, LocalDate currentInterestMonth) {
        LocalDate nextInterestDateForAccount = savingAccount
                .getLastInterestAppliedDate()
                .plusMonths(savingAccount
                        .getInterestFrequency()
                        .getMonthIncrement());

        if (isSameMonthAndYear(currentInterestMonth, nextInterestDateForAccount)) {
            addInterest(savingAccount);
            savingAccount.setLastInterestAppliedDate(currentInterestMonth);
        }
    }

    private static void addInterest(SavingsAccountModel savingAccount) {
        double interest = savingAccount.getBalance().getAmount() * savingAccount.getInterest();
        double newAccountBalance = savingAccount.getBalance().getAmount() + interest;
        // Add 0.5 to ensure correct rounding to the nearest cent
        // and then floor the result to remove fractional cents
        double roundedNewAccountBalance = Math.floor(newAccountBalance * 100. + .5) / 100.;
        savingAccount.getBalance().setAmount(roundedNewAccountBalance);
    }

    private static boolean isSameMonthAndYear(LocalDate date1, LocalDate date2) {
        return date1.getMonth() == date2.getMonth() && date1.getYear() == date2.getYear();
    }
}