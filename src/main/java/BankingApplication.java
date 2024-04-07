import domain.CurrencyType;
import domain.MoneyModel;
import domain.TransactionModel;
import seed.SeedInitializer;
import services.SavingsManagerService;
import services.TransactionManagerService;

import java.util.List;

import static seed.AccountsSeedData.*;

public class BankingApplication {

    public static void main(String[] args) {
        System.out.println("[SYSTEM] Initialize Application \n");
        SeedInitializer.seedData();
        System.out.println("[SYSTEM] Running Application \n\n");

        // TRANSACTION MANAGER FUNCTIONALITY

        System.out.printf("[Transaction Manager] 1. A: %s%n", TransactionManagerService.checkFunds(checkingAccountA.getId()));
        System.out.printf("[Transaction Manager] 2. B: %s%n", TransactionManagerService.checkFunds(checkingAccountB.getId()));

        TransactionModel transaction1 = TransactionManagerService.transfer(
                checkingAccountA.getId(),
                checkingAccountB.getId(),
                new MoneyModel(50, CurrencyType.RON)
        );

        System.out.printf("[Transaction Manager] 3. Transfer from A to B:%s%n", transaction1);
        System.out.printf("[Transaction Manager] 4. A: %s%n", TransactionManagerService.checkFunds(checkingAccountA.getId()));
        System.out.printf("[Transaction Manager] 5. B: %s%n", TransactionManagerService.checkFunds(checkingAccountB.getId()));

//         Uncomment the following lines if the withdrawal method is available in the TransactionManagerService

        System.out.printf("[Transaction Manager] 6. C: %s%n", TransactionManagerService.checkFunds(checkingAccountC.getId()));


        System.out.printf("[Transaction Manager] 6. C withdrawal: %s%n", TransactionManagerService.withdraw(
                checkingAccountC.getId(),
                new MoneyModel(5, CurrencyType.EUR)
        ));
        System.out.printf("[Transaction Manager] 8. C: %s%n", TransactionManagerService.checkFunds(checkingAccountC.getId()));
        System.out.println("\n------------------------------------\n");
        int counter = 9;
        for (double transactionSum : List.of(1., 2., 5., 10., 20., 50., 100., 200., 500.)) {
            try {
                System.out.printf("[Transaction Manager] %d. C withdrawal: %s%n", counter++, TransactionManagerService.withdraw(
                        checkingAccountC.getId(),
                        new MoneyModel(transactionSum, CurrencyType.EUR)
                ));
            } catch (RuntimeException rex) {
                System.out.printf("[Transaction Manager] %d. C cannot accommodate a withdrawal of %s %s%n", counter, transactionSum, checkingAccountC.getBalance().getCurrency().name());
                break;
            }
        }
        // SAVINGS MANAGER FUNCTIONALITY

        System.out.printf("[Saving Manager] 1. A: %s%n", TransactionManagerService.checkFunds(savingsAccountA.getId()));
        System.out.printf("[Saving Manager] 2. B: %s%n", TransactionManagerService.checkFunds(savingsAccountB.getId()));
        int monthCounter = 1;
        SavingsManagerService.passTime();
        System.out.printf("%d month went by%n", monthCounter++);
        System.out.printf("[Saving Manager] 3. A: %s%n", TransactionManagerService.checkFunds(savingsAccountA.getId()));

        SavingsManagerService.passTime();
        System.out.printf("%d months went by%n", monthCounter++);
        SavingsManagerService.passTime();
        System.out.printf("%d months went by%n", monthCounter++);
        SavingsManagerService.passTime();
        System.out.printf("%d months went by%n", monthCounter++);
        SavingsManagerService.passTime();
        System.out.printf("%d months went by%n", monthCounter++);
        SavingsManagerService.passTime();
        System.out.printf("%d months went by%n", monthCounter++);
        SavingsManagerService.passTime();
        System.out.printf("%d months went by%n", monthCounter++);
        SavingsManagerService.passTime();
        System.out.printf("%d months went by%n", monthCounter);
        System.out.println("[Saving Manager] 4. A: " + TransactionManagerService.checkFunds(savingsAccountA.getId()));
        System.out.println("[Saving Manager] 5. B: " + TransactionManagerService.checkFunds(savingsAccountB.getId()));
        System.out.println("[Saving Manager] 6. Checking A: " + TransactionManagerService.checkFunds(checkingAccountA.getId()));

        System.out.println("\n[SYSTEM] Application closed\n");
    }
}