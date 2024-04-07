import domain.*;
import org.junit.Before;
import org.junit.Test;
import repository.AccountsRepository;
import seed.SeedInitializer;
import services.SavingsManagerService;
import services.TransactionManagerService;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static seed.AccountsSeedData.*;

public class BankingApplicationTest {

    @Before
    public void initSeed() {
        SeedInitializer.seedData();
    }

    @Test
    public void testSavingsService() {
        System.out.println("[Tester] -------------Testing Savings Service-----------------------------------------------------\n");
        SavingsAccountModel accountWithMonthlyInterest = new SavingsAccountModel(
                "ROBMSG100010",
                new MoneyModel(1000, CurrencyType.RON),
                new ArrayList<>(),
                InterestRate.THREE_MONTH_ACCOUNT,
                CapitalizationFrequency.MONTHLY,
                LocalDate.now());
        SavingsAccountModel accontWithQuarterlyInterest = new SavingsAccountModel(
                "ROBMSG100011",
                new MoneyModel(1000, CurrencyType.RON),
                new ArrayList<>(),
                InterestRate.THREE_MONTH_ACCOUNT,
                CapitalizationFrequency.QUARTERLY,
                LocalDate.now());
        double accountBalance = 1000;
        AccountsRepository.INSTANCE.add(accountWithMonthlyInterest.getId(), accountWithMonthlyInterest);
        AccountsRepository.INSTANCE.add(accontWithQuarterlyInterest.getId(), accontWithQuarterlyInterest);
        SavingsManagerService.passTime();
        System.out.println("[Tester] -------------Checking if interest was added for the monthly account and not for the other\n");
        assertEquals(accountWithMonthlyInterest.getBalance().getAmount(), accountBalance + accountBalance * accountWithMonthlyInterest.getInterest(), 0.01);
        assertEquals(accontWithQuarterlyInterest.getBalance().getAmount(), accountBalance, 0.01);
        SavingsManagerService.passTime();
        SavingsManagerService.passTime();
        System.out.println("[Tester] -------------Checking if interest was added after 3 months----------------------------------\n");
        assertEquals(accontWithQuarterlyInterest.getBalance().getAmount(), accountBalance + accountBalance * accontWithQuarterlyInterest.getInterest(), 0.01);
    }

    @Test
    public void testTransactionService() {
        System.out.println("[Tester] -------------Testing Transaction Service---------------------------------------------------\n");
        System.out.println("[Tester] -------------Creating a transaction from an account with insufficient funds----------------\n");
        AccountModel source = checkingAccountA;
        AccountModel dest = checkingAccountB;
        MoneyModel transactionOfMoreMoneyThanExistsInSource = new MoneyModel(source.getBalance().getAmount() + 1, CurrencyType.RON);
        try {
            TransactionManagerService.transfer(source.getId(), dest.getId(), transactionOfMoreMoneyThanExistsInSource);
            fail();
        } catch (RuntimeException rex) {
            assert (true);
        }
        System.out.println("[Tester] -------------Checking that the failed transaction was not logged--------------------------\n");
        assertEquals(source.getTransactions().size(), 0);
        assertEquals(dest.getTransactions().size(), 0);
        System.out.println("[Tester] -------------Creating a transaction from an account with sufficient funds----------------\n");
        MoneyModel validTransaction = new MoneyModel(source.getBalance().getAmount() - 1, CurrencyType.RON);
        TransactionManagerService.transfer(source.getId(), dest.getId(), validTransaction);
        System.out.println("[Tester] -------------Checking that the completed transaction was logged--------------------------\n");
        assertEquals(source.getTransactions().size(), 1);
        assertEquals(dest.getTransactions().size(), 1);
        System.out.println("[Tester] -------------Checking that withdrawals do not change account balance---------------------\n");
        AccountModel withdrawingAccount = checkingAccountC;
        double withdrawerFunds = checkingAccountC.getBalance().getAmount();
        MoneyModel withdrawalAmount = new MoneyModel(withdrawerFunds - 1, checkingAccountC.getBalance().getCurrency());
        TransactionManagerService.withdraw(withdrawingAccount.getId(), withdrawalAmount);
        assertEquals(withdrawingAccount.getBalance().getAmount(), withdrawerFunds, 0.01);
        System.out.println("[Tester] -------------Checking that withdrawals get logged twice----------------------------------\n");
        assertEquals(withdrawingAccount.getTransactions().size(), 2);
        System.out.println("[Tester] -------------Creating an account linked to an expired card-------------------------------\n");
        CardModel expiredCard = new CardModel(
                4567890123456788L,
                "Ion Pop",
                998,
                LocalDate.of(2023, 9, 25),
                LocalDate.of(2018, 4, 25),
                true,
                true,
                4000,
                8000
        );
        CheckingAccountModel accountWithExpiredCard = new CheckingAccountModel(
                "ROBMSG200005",
                new MoneyModel(1000, CurrencyType.EUR),
                new ArrayList<>(),
                expiredCard
        );
        System.out.println("[Tester] -------------Checking if withdrawing money fails----------------------------------------\n");
        try {
            TransactionManagerService.withdraw(accountWithExpiredCard.getId(), new MoneyModel(10, CurrencyType.EUR));
            fail();
        } catch (RuntimeException rex) {
            assert (true);
        }
        System.out.println("[Tester] -------------Creating an account with conservative limits------------------------------\n");
        CardModel cardWithLowLimits = new CardModel(
                4567890123456787L,
                "Ion Pop",
                998,
                LocalDate.now().plusMonths(1).plusYears(5),
                LocalDate.now().plusMonths(1),
                true,
                true,
                40,
                80
        ), normalCard = new CardModel(
                4567890123456786L,
                "Ion Popescu",
                918,
                LocalDate.now().plusMonths(-1).plusYears(5),
                LocalDate.now().plusMonths(-1),
                true,
                true,
                4000,
                8000);
        CheckingAccountModel worriedUser = new CheckingAccountModel(
                "ROBMSG200006",
                new MoneyModel(1000, CurrencyType.EUR),
                new ArrayList<>(),
                cardWithLowLimits
        ), normalUser = new CheckingAccountModel(
                "ROBMSG200007",
                new MoneyModel(1000, CurrencyType.EUR),
                new ArrayList<>(),
                normalCard
        );

        System.out.println("[Tester] -------------Checking if withdrawing money fails----------------------------------------\n");
        try {
            TransactionManagerService.withdraw(worriedUser.getId(), new MoneyModel(41, CurrencyType.EUR));
            fail();
        } catch (RuntimeException rex) {
            assert (true);
        }
        System.out.println("[Tester] -------------Checking if transferring money fails----------------------------------------\n");
        try {
            TransactionManagerService.transfer(worriedUser.getId(), normalUser.getId(), new MoneyModel(41, CurrencyType.EUR));
            fail();
        } catch (RuntimeException rex) {
            assert (true);
        }
    }
}
